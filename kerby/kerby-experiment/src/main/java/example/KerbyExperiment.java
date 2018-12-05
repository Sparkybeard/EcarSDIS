package example;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class KerbyExperiment {

    public static void main(String[] args) throws Exception {
        System.out.println("Hi!");

        System.out.println();

        // receive arguments
        System.out.printf("Received %d arguments%n", args.length);

        System.out.println();

        // load configuration properties
        try {
            InputStream inputStream = KerbyExperiment.class.getClassLoader().getResourceAsStream("config.properties");
            // variant for non-static methods:
            // InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.properties");

            Properties properties = new Properties();
            properties.load(inputStream);

            System.out.printf("Loaded %d properties%n", properties.size());

        } catch (IOException e) {
            System.out.printf("Failed to load configuration: %s%n", e);
        }

        System.out.println();

		// client-side code experiments
        System.out.println("Experiment with Kerberos client-side processing");
		System.out.println("...TODO...");

        System.out.println();

		// server-side code experiments
        System.out.println("Experiment with Kerberos server-side processing");
		System.out.println("...TODO...");

        System.out.println();
		
		System.out.println("Bye!");
    }
}
