// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.RMI;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import Server.Common.InvalidTransactionException;
import Server.Common.ResourceManager;
import Server.Common.TransactionAbortedException;
import Server.Interface.IResourceManager;

public class RMIResourceManager extends ResourceManager
{
	private static String s_serverName = "Server";
	private static String s_rmiPrefix = "group6_";
	private static int port = 3099;

	public static void main(String args[])
	{
		if (args.length > 0)
		{
			s_serverName = args[0];
		}

		// Create the RMI server entry
		try {
			// Create a new Server object
			RMIResourceManager server = new RMIResourceManager(s_rmiPrefix + s_serverName);
			server.restore();
//			while(true) {
//				try {
//					server.restore();
//					break;
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}

			// Dynamically generate the stub (MiddleWare proxy)
			IResourceManager resourceManager = (IResourceManager) UnicastRemoteObject.exportObject(server, port);

			// Bind the remote object's stub in the registry
			Registry l_registry;
			try {
				l_registry = LocateRegistry.createRegistry(port);
			} catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(port);
			}
			final Registry registry = l_registry;

			registry.rebind(s_rmiPrefix + s_serverName, resourceManager);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						registry.unbind(s_rmiPrefix + s_serverName);
						System.out.println("'" + s_serverName + "' resource manager unbound");
					}
					catch(Exception e) {
						System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
						e.printStackTrace();
					}
				}
			});
			System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}
	}

	public RMIResourceManager(String name)
	{
		super(name);
	}
}
