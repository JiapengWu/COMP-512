//ckage Server.Common;
//
//import java.rmi.RemoteException;
//import java.util.HashMap;
//
//import Server.Interface.IResourceManager;
//
//public class CustomerResouceManager extends ResourceManager{
//  
//  public CustomerResouceManager(String p_name) {
//    super(p_name);
//  }
//  
//  
//  public boolean reserveRMItem(int id, int customerID, String key, String location) {
//    
//
//    // Check if the item is available on the target ResourceManager(either flight, car or room)
//    ReservableItem item = (ReservableItem)readData(id, key); 
//    
////*
//    if (item == null)
//    {
//      Trace.warn("RM::reserveItem(" + id + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
//      return false;
//    }
//    else if (item.getCount() == 0)
//    {
//      Trace.warn("RM::reserveItem(" + id + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
//      return false;
//    }
//    else{
//      customer.reserve(key, location, item.getPrice());     
//      writeCustomer(id, customer.getKey(), customer);
////      // Decrease the number of available items in the storage
//      setCount(targetRM, id, key, location);
//      setReserved(targetRM, id, key, location);
////      customerRM.writeData(id, item.getKey(), item);
////
////      Trace.info("RM::reserveItem(" + id + ", " + customerID + ", " + key + ", " + location + ") succeeded");
//      return true;
//    }
//    
//  public boolean addCars(int xid, String location, int count, int price) throws RemoteException
//  {
//    Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
//  
//  public void decrementCount(int xid, int customerID, String location) throws RemoteException {
//    Trace.info("RM::decrementCount(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
//  }
//  
//}
