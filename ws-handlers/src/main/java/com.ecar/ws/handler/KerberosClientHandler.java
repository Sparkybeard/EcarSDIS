package com.ecar.ws.handler;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Calendar;
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

public class KerberosClientHandler implements SOAPHandler<SOAPMessageContext> {

	public static final String SESSIONKEY_PROPERTY = "kc.property1";

	private static String USER = "";
	private static String PASS = "";

	private static boolean verbose = true; // true for debug

	@Override
	public boolean handleMessage(SOAPMessageContext smc) {
		Boolean outboundElement = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		try {
			// get SOAP envelope
			SOAPMessage msg = smc.getMessage();
			SOAPPart sp = msg.getSOAPPart();
			SOAPEnvelope se = sp.getEnvelope();
			CipherClerk cc = new CipherClerk();

			if (verbose) {
				System.out.println("\nUSER\t" + USER + "\nPASS\t" + PASS + "\n");
			}

			if (outboundElement.booleanValue()) {
				System.out.println("\nKerberosClientHandler: Handling outbound message...");

				SecureRandom randomGetter = SecureRandom.getInstance("SHA1PRNG");
				Calendar calendar = Calendar.getInstance();
				KerbyClient client = new KerbyClient("http://sec.sd.rnl.tecnico.ulisboa.pt:8888/kerby");
				SessionKeyAndTicketView result = client.requestTicket(USER, "binas@T18.binas.org",
						randomGetter.nextLong(), 60 /* seconds */);
				CipheredView sessionKeyCipher = result.getSessionKey();
				CipheredView ticketCipher = result.getTicket();

				SessionKey sessionKey = new SessionKey(sessionKeyCipher, getKey(PASS));

				// add header
				SOAPHeader sh = se.getHeader();
				if (sh == null)
					sh = se.addHeader();

				// Send Ticket
				// add header element (name, namespace prefix, namespace)
				Name name = se.createName("ticket", "t", "http://ticket");
				SOAPHeaderElement element = sh.addHeaderElement(name);

				// add ticket to header
				byte[] ticketBytes = cc.cipherToXMLBytes(ticketCipher, "TCipher");
				String ticketBytesString = DatatypeConverter.printBase64Binary(ticketBytes);
				// System.out.println("Ticket Bytes len:\t" +
				// ticketBytesString.getBytes().length);
				element.addTextNode(ticketBytesString);

				// Send Auth
				Auth auth = new Auth(USER, calendar.getTime());
				CipheredView authCipher = auth.cipher(sessionKey.getKeyXY());

				// add header element (name, namespace prefix, namespace)
				name = se.createName("auth", "a", "http://auth");
				element = sh.addHeaderElement(name);

				// add auth to header
				byte[] authBytes = cc.cipherToXMLBytes(authCipher, "ACipher");
				String authBytesString = DatatypeConverter.printBase64Binary(authBytes);
				// System.out.println("Ticket Bytes len:\t" +
				// authBytesString.getBytes().length);
				element.addTextNode(authBytesString);

				smc.put(SESSIONKEY_PROPERTY, sessionKey.getKeyXY());
				smc.setScope(SESSIONKEY_PROPERTY, Scope.HANDLER);

				System.out.println("KerberosClientHandler: Header added");

			} else {
				System.out.println("\nKerberosClientHandler: Handling inbound message...");

				SOAPHeader sh = se.getHeader();

				// check header
				if (sh == null) {
					throw new RuntimeException("Received header is null.");
				}

				// get first header element
				Name name = se.createName("timeReq", "tr", "http://timereq");
				Iterator<?> it = sh.getChildElements(name);

				// check header element
				if (!it.hasNext()) {
					throw new RuntimeException("TimeReq not found.");
				}
				SOAPElement element = (SOAPElement) it.next();

				// get ticket from header element
				String valueString = element.getValue();
				byte[] timeReqBytes = DatatypeConverter.parseBase64Binary(valueString);
				CipheredView timeReqCipher = cc.cipherFromXMLBytes(timeReqBytes);

				RequestTime timeReq = new RequestTime(timeReqCipher,
						(Key) smc.get(SESSIONKEY_PROPERTY) /* session key */);

				System.out.println("Answer from Server for request made on " + timeReq.getTimeRequest());
			}

		} catch (Exception e) {
			throw new RuntimeException("\nKerberosClientHandler: Failed. " + e.getMessage());
		}

		System.out.println("KerberosClientHandler: Finished with success.\n");
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

	public static void login(String email, String pass) {
		setEmail(email);
		setPass(pass);
	}

	private static void setEmail(String email) {
		USER = email;
	}

	private static void setPass(String pass) {
		PASS = pass;
	}

}
