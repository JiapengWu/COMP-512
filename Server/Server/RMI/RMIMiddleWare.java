package Server.RMI;

import Server.Interface.*;
import Server.Common.*;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIMiddleWare implements IResourceManager {
  private static String s_serverName = "MiddleWare";
  private static final String s_rmiPrefix = "group6_";
  private static final String RM_Suffix = "_RM";

  private static ResourceManager flightRM;
  private static ResourceManager carRM;
  private static ResourceManager roomRM;
  private static ResourceManager customerRM;
  private static Registry client_registry;

  protected RMHashMap m_itemHT = new RMHashMap();
  private static int middleware_port = 1100;
  private static int server_port = 1099;

  public RMIMiddleWare(String s_serverName2) {
  }

  public static void main(String args[]) {

    if (args.length > 0) {
      s_serverName = args[0];
    }

    // Create a new Server object
    RMIMiddleWare mw = new RMIMiddleWare(s_serverName);

    // get 3 registry from server

    // create client registry

    try {
      client_registry = LocateRegistry.createRegistry(middleware_port);
    } catch (RemoteException e) {

      try {
        client_registry = LocateRegistry.getRegistry(middleware_port);
      } catch (RemoteException e1) {
        Trace.error("Cannot get client registry.");
      }
    }

    try {
      getResourceManagers(args);
    } catch (Exception e) {
      Trace.error("Error getting resource manager");
    }

    
  }

  public static void getResourceManagers(String args[]) throws Exception {

    Registry flightRegistry = LocateRegistry.getRegistry(args[1], server_port);
    flightRM = (ResourceManager) flightRegistry.lookup(s_rmiPrefix + "Flights" + "_RM");
    if (flightRM == null)
      throw new AssertionError();
    
    Registry carRegistry = LocateRegistry.getRegistry(args[2], server_port);
    carRM = (ResourceManager) carRegistry.lookup(s_rmiPrefix + "Cars" + "_RM");
    if (carRM == null)
      throw new AssertionError();
    
    Registry roomRegistry = LocateRegistry.getRegistry(args[3],server_port);
    roomRM = (ResourceManager) roomRegistry.lookup(s_rmiPrefix + "Rooms" + "_RM");
    if (roomRM == null)
      throw new AssertionError();

    Registry customerRegistry = LocateRegistry.getRegistry(args[4],server_port);
    customerRM = (ResourceManager) customerRegistry.lookup(s_rmiPrefix + "Customer" + "_RM");
    if (roomRM == null)
      throw new AssertionError();
  }

  public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
      throws RemoteException {
    return flightRM.addFlight(id, flightNum, flightSeats, flightPrice);
  }

  @Override
  public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
    // TODO Auto-generated method stub
    return carRM.addCars(id, location, numCars, price);
  }

  @Override
  public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
    return roomRM.addRooms(id, location, numRooms, price);
  }

  @Override
  public int newCustomer(int id) throws RemoteException {
    return customerRM.newCustomer(id);
  }

  @Override
  public boolean newCustomer(int id, int cid) throws RemoteException {
    return customerRM.newCustomer(id, cid);
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
    return false;
  }

  @Override
  public String getName() throws RemoteException {
    return roomRM.getName();
  }
}
