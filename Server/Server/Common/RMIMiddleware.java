package Server.Common;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Vector;

import Server.Interface.IResourceManager;

public class RMIMiddleware implements IResourceManager {
  private static String s_serverName = "MiddleWare";
  private static final String s_rmiPrefix = "group6_";
  private static final String RM_Suffix = "_RM";

  private static IResourceManager flightRM;
  private static IResourceManager carRM;
  private static IResourceManager roomRM;
  private static IResourceManager customerRM;
  
  protected static HashMap<Integer, Customer> customer_map = new HashMap<Integer, Customer>();
  private static int middleware_port = 1099;
  private static int server_port = 1099;

  public RMIMiddleware(String s_serverName2) {
  }

  public static void main(String args[]) {
  	try{


	    if (args.length ==5) {
	      s_serverName = args[0];
	    }
	    else{
	    	Trace.error("RMIMiddleWare:: Expect 5 arguments. $0: hostname of MiddleWare, $1-$4: hostname of servers");
	    	System.exit(1);
	    }

	    // Create a new Server object
	    RMIMiddleware mw = new RMIMiddleware(s_serverName);
	    // Dynamically generate the stub (MiddleWare proxy
	    IResourceManager mw_RM = (IResourceManager) UnicastRemoteObject.exportObject(mw, middleware_port);
	    

	    Registry client_registry;
	   	try {
	      client_registry = LocateRegistry.createRegistry(middleware_port);
	    } catch (RemoteException e) {
	        client_registry = LocateRegistry.getRegistry(middleware_port);
	      
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
						e.printStackTrace();
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
  	Registry flightRegistry = null;
  	Registry carRegistry = null;
  	Registry roomRegistry = null;
  	Registry customerRegistry = null;
  	
    flightRegistry = LocateRegistry.getRegistry(args[1], server_port);
    flightRM = (IResourceManager) flightRegistry.lookup(s_rmiPrefix + "Flights");
    if (flightRM == null)
      throw new AssertionError();
    
    carRegistry = LocateRegistry.getRegistry(args[2], server_port);
    carRM = (IResourceManager) carRegistry.lookup(s_rmiPrefix + "Cars");
    if (carRM == null)
      throw new AssertionError();
    
   	roomRegistry = LocateRegistry.getRegistry(args[3],server_port);
    roomRM = (IResourceManager) roomRegistry.lookup(s_rmiPrefix + "Rooms");
    if (roomRM == null)
      throw new AssertionError();

  	
    customerRegistry = LocateRegistry.getRegistry(args[4],server_port);
    customerRM = (IResourceManager) customerRegistry.lookup(s_rmiPrefix + "Customers");
    if (roomRM == null)
      throw new AssertionError();
  	Trace.info("RMIMiddleware: All RMs get");
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
    return customerRM.deleteCustomer(id, customerID);
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
    return customerRM.queryCustomerInfo(id, customerID);
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
    return customerRM.newCustomer(id);
  }

  @Override
  public boolean newCustomer(int id, int cid) throws RemoteException {
    return customerRM.newCustomer(id, cid);
  }

  public boolean reserveItem(IResourceManager customerRM, IResourceManager targetRM, int id, int customerID, String key, String location) {
    Trace.info("RM::reserveItem(" + id + ", customer=" + customerID + ", " + customerID + ", " + location + ") called" ); 
    // get customer from m_data
    Customer customer = customer_map.get(customerID);
    if (customer == null)
    {
      Trace.warn("RM::reserveItem(" + id + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
      return false;
    }

    // Check if the item is available
    ReservableItem item = (ReservableItem)((ResourceManager)targetRM).readData(id, key);
    if(item instanceof Flight) {
      
    }
    
    
/*
    if (item == null)
    {
      Trace.warn("RM::reserveItem(" + id + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
      return false;
    }
    else if (item.getCount() == 0)
    {
      Trace.warn("RM::reserveItem(" + id + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
      return false;
    }
    else{
      reserve(customer, key, location, item.getPrice());
      customerRM.writeData(id, customer.getKey(), customer);
      // Decrease the number of available items in the storage
      item.setCount(item.getCount() - 1);
      item.setReserved(item.getReserved() + 1);
      customerRM.writeData(id, item.getKey(), item);

      Trace.info("RM::reserveItem(" + id + ", " + customerID + ", " + key + ", " + location + ") succeeded");
      return true;
    }
    
    //*/
    return true;
  }
  
  @Override
  public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {
    return this.reserveItem(customerRM, flightRM, id, customerID, Flight.getKey(flightNumber), String.valueOf(flightNumber));
  }

  @Override
  public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
    return customerRM.reserveCar(id, customerID, location);
  }

  @Override
  public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
    return customerRM.reserveRoom(id, customerID, location);
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
