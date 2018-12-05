package com.ecar.ws;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import com.ecar.ws.cli.ECarClient;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.TimerTask;

public class LifeProof extends TimerTask {

	private String wsURL2 = "http://localhost:8072/ecar-ws/endpoint";
	private String wsURL1 = "http://localhost:8071/ecar-ws/endpoint";

	// end point manager
	private ECarEndpointManager endpointManager;

	public LifeProof(ECarEndpointManager endpointManager) {
		this.endpointManager = endpointManager;
	}
	/**
	 * output option
	 **/
	//private boolean verbose = false;
	public void run() {
		System.out.println(endpointManager.getPrimaryStatus());
		try {
			if (endpointManager.getPrimaryStatus().equals("true")) {
				ECarClient eCarClient = new ECarClient(wsURL2);
				eCarClient.imAlive();
			} else {
				Calendar calendar = Calendar.getInstance();
				java.util.Date now = calendar.getTime();
				Timestamp currentTimeStamp2 = new Timestamp(now.getTime());
				if (((currentTimeStamp2.getTime()) - (ECarPortImpl.getCurrentTimeStamp())) >= 20 * 1000) {
					endpointManager.setPrimaryStatus("true");
					endpointManager.setURL(wsURL2);
					endpointManager.publishToUDDI();
					System.out.println("assumi");
				}
			}
		} catch (WebServiceException wse) {
			wse.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();

		}
	}
}
	

