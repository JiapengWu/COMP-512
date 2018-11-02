package Client;

import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.FileInputStream;

import java.rmi.RemoteException;
import Server.LockManager.DeadlockException;
import Server.Interface.IResourceManager;

public class TestClient
{
	
	static float LOAD = 10; // transactions per second
	static int NUM_CLIENTS = 10;
	static float FREQ = LOAD/NUM_CLIENTS ; // num transaction per sec per client
	

	
	private static String s_serverHost = "localhost";
	private static int s_serverPort = 3099;
	private static String s_serverName = "RMIMiddleware";
	private static String s_rmiPrefix = "group6_";

	RMIClient client;



	public static void main(String args[])
	{
		if (args.length > 0) {
	      s_serverHost = args[0];
	    }
	    if (args.length > 1) {
	      s_serverPort = Integer.parseInt(args[1]);
	    }
	    

	    // setUps(s_serverHost, s_serverPort, s_serverName);
		
		
		ExecutorService es = Executors.newCachedThreadPool();
		for (int i = 0; i < NUM_CLIENTS; i++) {
			client = new RMIClient();
			client.connect_server(s_serverHost, s_serverPort, s_serverName);
			TestClientThread ct = new TestClientThread(client, commands, FREQ, i);
			es.execute(ct);
		}

		es.shutdown();
		try {
			es.awaitTermination(30, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
		
		

	// setup middleware and server, initialize some flight/room/cars
	public static void setUps(String server, int port, String name) 
		throws RemoteException, DeadlockException
	{
		IResourceManager mw;
		try {
			boolean first = true;
			while (true) {
				try {
					Registry registry = LocateRegistry.getRegistry(server, port);
					mw = (IResourceManager)registry.lookup(s_rmiPrefix + name);
					System.out.println("TestClient::Connected to '" + name + "' middleware [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
					break;
				}
				catch (NotBoundException|RemoteException e) {
					if (first) {
						System.out.println("TestClient:: Waiting for '" + name + "' middleware [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
						first = false;
					}
				}
				Thread.sleep(500);
			}
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

		// Initialize servers
		int txnId = mw.start();
    	mw.addCars(txnId, "TRT", 1000, 100);
	    mw.addFlight(txnId, 300, 2000, 200);
	    mw.addFlight(txnId, 200, 1000, 100);
	    mw.addRooms(txnId, "MTL", 500, 300);
	    mw.commit(txnId);
	}


 }