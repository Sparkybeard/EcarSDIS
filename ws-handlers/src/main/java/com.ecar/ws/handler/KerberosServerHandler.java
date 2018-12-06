package com.ecar.ws.handler;

import java.security.Key;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;


import pt.ulisboa.tecnico.sdis.kerby.Ticket;
import pt.ulisboa.tecnico.sdis.kerby.SecurityHelper;
import pt.ulisboa.tecnico.sdis.kerby.RequestTime;
import pt.ulisboa.tecnico.sdis.kerby.CipheredView;
import pt.ulisboa.tecnico.sdis.kerby.CipherClerk;
import pt.ulisboa.tecnico.sdis.kerby.Auth;

public class KerberosServerHandler implements SOAPHandler<SOAPMessageContext> {

	public static final String AUTH_USER_PROPERTY = "ks.property1";
	public static final String TICKET_USER_PROPERTY = "ks.property2";
	private static final String TIMEREQ_PROPERTY = "ks.property3";
	public static final String SESSIONKEY_PROPERTY = "ks.property4";

	private static boolean verbose = true; // true for debug

	@Override
	public boolean handleMessage(SOAPMessageContext smc) {

		try {
			Boolean outboundElement = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
			// get SOAP envelope header
			SOAPMessage msg = smc.getMessage();
			SOAPPart sp = msg.getSOAPPart();
			SOAPEnvelope se = sp.getEnvelope();
			SOAPHeader sh = se.getHeader();
			CipherClerk cc = new CipherClerk();

			if (outboundElement.booleanValue()) {
				System.out.println("\n\nKerberosServerHandler: Handling outbound message...");

				CipheredView trCipher = (CipheredView) smc.get(TIMEREQ_PROPERTY);

				if (trCipher == null) {
					System.out.println("KerberosServerHandler: Failed to get Time Request from Ticket");
					return true;
				}

				// add header
				if (sh == null)
					sh = se.addHeader();

				// add header element (name, namespace prefix, namespace)
				Name name = se.createName("timeReq", "tr", "http://timereq");
				SOAPHeaderElement element = sh.addHeaderElement(name);

				// add TimeRequest to header
				byte[] trBytes = cc.cipherToXMLBytes(trCipher, "ACipher");
				String trBytesString = DatatypeConverter.printBase64Binary(trBytes);
				element.addTextNode(trBytesString);

				System.out.println("KerberosServerHandler: Header added.");

			} else {

				System.out.println("\n\nKerberosServerHandler: Handling inbound message...");

				// check header
				if (sh == null) {
					System.out.println("Header not found.");
					return true;
				}

				// get first header element
				Name name = se.createName("ticket", "t", "http://ticket");
				Iterator<?> it = sh.getChildElements(name);
				// check header element
				if (!it.hasNext()) {
					System.out.println("Ticket not found.");
					return true;
				}
				SOAPElement element = (SOAPElement) it.next();

				// get ticket from header element
				String valueString = element.getValue();
				byte[] ticketBytes = DatatypeConverter.parseBase64Binary(valueString);
				CipheredView ticketCipher = cc.cipherFromXMLBytes(ticketBytes);

				Ticket clientTicket = new Ticket(ticketCipher, getKey("uq7qYKtO"/* pass do binas */));

				// get second header element
				name = se.createName("auth", "a", "http://auth");
				it = sh.getChildElements(name);
				// check header element
				if (!it.hasNext()) {
					System.out.println("Auth not found.");
					return true;
				}
				element = (SOAPElement) it.next();

				// get auth from header element with tag authTag
				valueString = element.getValue();
				byte[] authBytes = DatatypeConverter.parseBase64Binary(valueString);
				CipheredView authCipher = cc.cipherFromXMLBytes(authBytes);

				Auth auth = new Auth(authCipher, clientTicket.getKeyXY());

				smc.put(AUTH_USER_PROPERTY, auth.getX());
				smc.setScope(AUTH_USER_PROPERTY, Scope.HANDLER);

				smc.put(TICKET_USER_PROPERTY, clientTicket.getX());
				smc.setScope(TICKET_USER_PROPERTY, Scope.HANDLER);

				smc.put(SESSIONKEY_PROPERTY, clientTicket.getKeyXY());
				smc.setScope(SESSIONKEY_PROPERTY, Scope.HANDLER);

				if (verbose) {
					System.out.println("Added sessionkey email from ticket and auth to message properties");
				}
				Date timeReq = auth.getTimeRequest();
				Date t1 = clientTicket.getTime1();
				Date t2 = clientTicket.getTime2();

				if (verbose) {
					System.out.println("\nt1\t" + t1.toString());
					System.out.println("t2\t" + t2.toString());
					System.out.println("tr\t" + timeReq.toString());
					System.out.println();
				}

				if (timeReq.before(t1)) {
					if (verbose) {
						System.out.println("Applying tolerance...");
					}
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(t1);
					calendar.add(Calendar.SECOND, -1);
					t1 = calendar.getTime(); // delays timeReq by a millisec for good comparison
				}
				if (timeReq.before(t1) || timeReq.after(t2)) {
					if (verbose) {
						System.out.println("Ticket is bad");
					}
					throw new RuntimeException("Bad ticket.");
				}

				else {
					if (verbose) {
						System.out.println("Ticket is good");
					}
					RequestTime rt = new RequestTime(timeReq);
					CipheredView rtCypher = rt.cipher(clientTicket.getKeyXY());
					// put header in a property context
					smc.put(TIMEREQ_PROPERTY, rtCypher);
					// set property scope to application client/server class can
					// access it
					smc.setScope(TIMEREQ_PROPERTY, Scope.HANDLER);
				}
				if (verbose) {
					System.out.println("KerberosServerHandler: Received Ticket and Auth.");
				}
			}
			System.out.println("KerberosServerHandler: Finished with success.\n\n");

		} catch (Exception e) {
			throw new RuntimeException("\nKerberosServerHandler: Failed. " + e.getMessage());
		}

		return true;
	}

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		if (verbose)
			System.out.println("Ignoring fault message...");

		return true;
	}

	@Override
	public void close(MessageContext context) {
	}

	@Override
	public Set<QName> getHeaders() {
		return null;
	}

	private Key getKey(String password) throws Exception {
		return SecurityHelper.generateKeyFromPassword(password);
	}

}
