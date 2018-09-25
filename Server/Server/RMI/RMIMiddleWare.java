package Server.RMI;

import Server.Interface.*;
import Server.Common.*;

import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIMiddleWare implements IResourceManager{
	private static String s_serverName = "Server";
	private static String s_rmiPrefix = "group6_";

	private ResourceManager rm;
	private ResourceManager flightRM;
	private ResourceManager carRM;
	private ResourceManager hotelRM;
	private Registry registry;
    
    protected RMHashtable m_itemHT = new RMHashtable();
	private int port = 1099;


	public static void main(String args[]){

		if (args.length > 0)
		{
			s_serverName = args[0];
		}

		try {
			// Create a new Server object
			RMIMiddleWare mw = new RMIMiddleWare(s_serverName);

			// Dynamically export the stub (client proxy)
			rm = (ResourceManager) UnicastRemoteObject.exportObject(mw, 1099);

			// Bind the server's stub in the registry
			try {
				l_registry = LocateRegistry.createRegistry(1099);
			} catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(1099);
			}
			final Registry registry = l_registry;
			registry.rebind(s_rmiPrefix + s_serverName, rm);

			

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
	}

	public void connectRM(){
		//
		flightRM = registy.lookup(args[1]);
		carRM = registy.lookup(args[2]);
		hotelRM = registy.lookup(args[3]);

		if (flightRM == null) {
    		System.out.println("Faliure to connect to FlightRM");
    	
    	} else{ 
    		System.out.println("Successfully connected to FlightRM");
    	
    	}
    	if (carRM == null) {
    		System.out.println("Faliure to connect to CarRM");
    	
    	} else{ 
    		System.out.println("Successfully connected to CarRM");
    	
    	}
    	if (hotelRM == null) {
    		System.out.println("Faliure to connect to HotleRM");
    	
    	} else{ 
    		System.out.println("Successfully connected to HotelRM");
    	
    	}
	}

    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
		return flightRM.addFlight(id, flightNum, flightSeats, flightPrice);
    }
}
