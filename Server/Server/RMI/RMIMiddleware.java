package Server.RMI;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import Server.Common.Trace;
import Server.Interface.IResourceManager;

public class RMIMiddleware implements IResourceManager {
  private static String s_serverName = "MiddleWare";
  private static final String s_rmiPrefix = "group6_";

  private static IResourceManager flightRM;
  private static IResourceManager carRM;
  private static IResourceManager roomRM;

  private ArrayList<Integer> customerIdx = new ArrayList<Integer>();
  private static int middleware_port = 3099;
  private static int server_port = 3099;

  public RMIMiddleware(String s_serverName2) {
  }

  public static void main(String args[]) {
  	try{

	    if (args.length == 4) {
	      s_serverName = args[0];
	    }
	    else{
	    	Trace.error("RMIMiddleWare:: Expect 4 arguments. $0: hostname of MiddleWare, $1-$3: hostname of servers");
	    	System.exit(1);
	    }

	    // Create a new Server object
	    IResourceManager mw_RM = null;
	    try {
		    RMIMiddleware mw = new RMIMiddleware(s_serverName);
		    // Dynamically generate the stub (MiddleWare proxy
		    mw_RM = (IResourceManager) UnicastRemoteObject.exportObject(mw, middleware_port);
	    }
	    catch(Exception e){
	    	e.printStackTrace();
	    }

	    Registry client_registry;
	   	try {
	       client_registry = LocateRegistry.createRegistry(middleware_port);
	    } catch (RemoteException e) {
	       client_registry = LocateRegistry.getRegistry(middleware_port);
	       e.printStackTrace();
	    }
	    final Registry registry = client_registry;
	    registry.rebind(s_rmiPrefix + s_serverName, mw_RM); //group6_MiddleWare

	    try {
	      getResourceManagers(args);
	    } catch (Exception e) {
	      Trace.error("RMIMiddleware: Error getting resource manager");
	      e.printStackTrace();
	    }
	    Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						registry.unbind(s_rmiPrefix + s_serverName);
						System.out.println("'" + s_serverName + "' MiddleWare unbound");
					}
					catch(Exception e) {
						System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
					}
				}
			});
		System.out.println("'" + s_serverName + "' Middleware server ready and bound to '" + s_rmiPrefix + s_serverName + "'" + "at port:"+String.valueOf(middleware_port));
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

  public static void getResourceManagers(String args[]) throws Exception {

    Registry flightRegistry = LocateRegistry.getRegistry(args[1], server_port);
    flightRM = (IResourceManager) flightRegistry.lookup(s_rmiPrefix + "Flights");
    if (flightRM == null)
      throw new AssertionError();

    Registry carRegistry = LocateRegistry.getRegistry(args[2], server_port);
    carRM = (IResourceManager) carRegistry.lookup(s_rmiPrefix + "Cars");
    if (carRM == null)
      throw new AssertionError();

   	Registry roomRegistry = LocateRegistry.getRegistry(args[3],server_port);
    roomRM = (IResourceManager) roomRegistry.lookup(s_rmiPrefix + "Rooms");
    if (roomRM == null)
      throw new AssertionError();
      
  }

  public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
      throws RemoteException {
    return flightRM.addFlight(id, flightNum, flightSeats, flightPrice);
  }

  @Override
  public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
    return carRM.addCars(id, location, numCars, price);
  }

  @Override
  public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
    return roomRM.addRooms(id, location, numRooms, price);
  }


  @Override
  public boolean deleteFlight(int id, int flightNum) throws RemoteException {
    return flightRM.deleteFlight(id, flightNum);
  }

  @Override
  public boolean deleteCars(int id, String location) throws RemoteException {
    return carRM.deleteCars(id, location);
  }

  @Override
  public boolean deleteRooms(int id, String location) throws RemoteException {
    return roomRM.deleteRooms(id, location);
  }

  @Override
  public boolean deleteCustomer(int id, int customerID) throws RemoteException {
    return flightRM.deleteCustomer(id, customerID) && carRM.deleteCustomer(id, customerID) && roomRM.deleteCustomer(id, customerID);
  }

  @Override
  public int queryFlight(int id, int flightNumber) throws RemoteException {
    return flightRM.queryFlight(id, flightNumber);
  }

  @Override
  public int queryCars(int id, String location) throws RemoteException {
    return carRM.queryCars(id, location);
  }

  @Override
  public int queryRooms(int id, String location) throws RemoteException {
    return roomRM.queryRooms(id, location);
  }

  @Override
  public String queryCustomerInfo(int id, int customerID) throws RemoteException {
    return flightRM.queryCustomerInfo(id, customerID)
        + carRM.queryCustomerInfo(id, customerID).split("/n", 2)[1] + roomRM.queryCustomerInfo(id, customerID).split("/n", 2)[1];
  }

  @Override
  public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
    return flightRM.queryFlightPrice(id, flightNumber);
  }

  @Override
  public int queryCarsPrice(int id, String location) throws RemoteException {
    return carRM.queryCarsPrice(id, location);
  }

  @Override
  public int queryRoomsPrice(int id, String location) throws RemoteException {
    return roomRM.queryRoomsPrice(id, location);
  }

  @Override
  public int newCustomer(int id) throws RemoteException {
    int cid = Collections.max(customerIdx);
    this.newCustomer(id, cid);
    return cid;
  }

  @Override
  public boolean newCustomer(int id, int cid) throws RemoteException {
    this.customerIdx.add(cid);
    return flightRM.newCustomer(id, cid) && carRM.newCustomer(id, cid) && roomRM.newCustomer(id, cid);
  }

  @Override
  public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {
    return flightRM.reserveFlight(id, customerID, flightNumber);
  }

  @Override
  public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
    return carRM.reserveCar(id, customerID, location);
  }

  @Override
  public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
    return roomRM.reserveRoom(id, customerID, location);
  }


  @Override
  public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location,
      boolean car, boolean room) throws RemoteException {
    	boolean res = true;
		for (String fn:flightNumbers) res &=reserveFlight(id,customerID,Integer.parseInt(fn));
		if (car) res &= reserveCar(id, customerID, location);
		if (room) res &= reserveRoom(id, customerID, location);
		return res; // return False if any of the above failed
  }

  @Override
  public String getName() throws RemoteException {
    return roomRM.getName();
  }

}
