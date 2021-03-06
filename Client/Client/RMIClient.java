package Client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import Server.Interface.IResourceManager;

public class RMIClient extends Client
{
	private static String s_serverHost = "localhost";
	private static int s_serverPort = 3099;
	private static String s_serverName = "Server";


	private static String s_rmiPrefix = "group6_";

	public static void main(String args[])
	{
		if (args.length > 0)
		{
			s_serverHost = args[0];
		}
		if (args.length > 1)
		{
			s_serverName = args[1];
		}
		if (args.length > 2)
		{
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java client.RMIClient [server_hostname [server_rmiobject]]");
			System.exit(1);
		}

		// Set the security policy
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}

		// Get a reference to the RMIRegister
		try {
			RMIClient client = new RMIClient();
			client.connectServer();
			new Thread(client.new MiddleWareHealthCheckThread()).start();
			System.out.println("Main thread continued");
			client.start();
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public class MiddleWareHealthCheckThread implements Runnable{
		public MiddleWareHealthCheckThread() {
		}
		@Override
		public void run() {
			System.out.println("Health checking thread is up");
			while(true) {
				try {
					m_resourceManager.ping();
//						Trace.info("Ping successful");
				} catch (RemoteException e) {
					try {
						System.out.println("Reconnecting middleware");
						connectServer();
						System.out.println("Reconnected");
					}catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					System.out.println("Health check interrupted.");
				}
			}
		}
	}
	

	public RMIClient()
	{
		super();
	}

	public void connectServer()
	{
		connectServer(s_serverHost, s_serverPort, s_serverName);
	}

	public void connectServer(String server, int port, String name)
	{
		try {
			boolean first = true;
			while (true) {
				try {
					Registry registry = LocateRegistry.getRegistry(server, port);
					m_resourceManager = (IResourceManager)registry.lookup(s_rmiPrefix + name);
					System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
					break;
				}
				catch (NotBoundException|RemoteException e) {
					if (first) {
						System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
						first = false;
					}
				}
				Thread.sleep(100);
			}
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
