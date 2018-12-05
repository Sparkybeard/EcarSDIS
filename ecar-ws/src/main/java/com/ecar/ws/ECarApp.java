package com.ecar.ws;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Timer;

import com.ecar.domain.ECarManager;

/**
 * ECarApp
 * 
 * Class that registers itself in UDDI and waits for connections Addresses are
 * passed via pom.xml
 *
 */
public class ECarApp {

	public static void main(String[] args) throws Exception {
		// Check arguments
		if (args.length == 0 || args.length == 3) {
			System.err.println("Argument(s) missing!");
			System.err.println("Usage: java " + ECarApp.class.getName() + " wsURL OR uddiURL wsName wsURL");
			return;
		}
		String uddiURL = null;
		String wsName = null;
		String wsURL = null;
		String primary = null;
		
		Timer _timerPrimary = new Timer(true);

		// Create server implementation object, according to options
		ECarEndpointManager endpoint = null;
		if (args.length == 1) {
			wsURL = args[0];
			endpoint = new ECarEndpointManager(wsURL);
			
		} else if (args.length >= 4) {
			uddiURL = args[0];
			wsName = args[1];
			wsURL = args[2];
			primary = args[3];
			
			endpoint = new ECarEndpointManager(uddiURL, wsName, wsURL, primary);
			endpoint.setVerbose(true);

		}

		ECarManager.getInstance().initUddiURL(uddiURL);

		// load ecar properties
		String parkName = null;
		try {
			InputStream inputStream = ECarApp.class.getResourceAsStream("/ecar.properties");

			Properties properties = new Properties();
			properties.load(inputStream);

			System.out.println("Loaded properties:");
			System.out.println(properties);

			parkName = properties.getProperty("park.ws.name");

			ECarManager.getInstance().initParkTemplateName(parkName);

		} catch (IOException e) {
			System.out.printf("Failed to load configuration: %s%n", e);
		}

		try {
			endpoint.start();
			LifeProof lifeProof = new LifeProof(endpoint);

			_timerPrimary.schedule(lifeProof, 7*1000, 5*1000);
			endpoint.awaitConnections();
			
			
		} finally {
			
			endpoint.stop();
		}

	}

}