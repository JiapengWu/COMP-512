package Client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestClient
{
	
	static float LOAD = 50; // transactions per second
	static float NUM_CLIENTS = 10;
	static float FREQ = LOAD/NUM_CLIENTS ; // num transaction per sec per client
	

	
	private static String s_serverHost = "localhost";
	private static int s_serverPort = 3099;
	private static String s_serverName = "mw_server";

	public static void main(String args[])
	{
		if (args.length > 0) {
	      s_serverHost = args[0];
	    }
	    if (args.length > 1) {
	    	LOAD = Integer.parseInt(args[1]);
	    }
	    
	    // setUps(s_serverHost, s_serverPort, s_serverName);
		
		ExecutorService es = Executors.newCachedThreadPool();
		for (int i = 0; i < NUM_CLIENTS; i++) {
			RMIClient client = new RMIClient();
			client.connectServer(s_serverHost, s_serverPort, s_serverName);
			TestClientThread ct = new TestClientThread(client, FREQ);
			es.execute(ct);
		}

		es.shutdown();
		try {
			es.awaitTermination(30, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
 }