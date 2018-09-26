package Server.Common;

import Server.Interface.IResourceManager;

public class CustomerUtility {
  
  public static boolean reserveItem(IResourceManager customerRM, IResourceManager targetRM, int id, int customerID, String key, String location) {
    Trace.info("RM::reserveItem(" + id + ", customer=" + customerID + ", " + customerID + ", " + location + ") called" ); 
    // get customer from m_data
    Customer customer = (Customer) ((ResourceManager) customerRM).readData(id, Customer.getKey(customerID));
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
  
  public static void reserve(Customer customer, String key, String location, int price)
  {
    ReservedItem reservedItem = customer.getReservedItem(key);

    if (reservedItem == null){
      // Customer doesn't already have a reservation for this resource, so create a new one now
      reservedItem = new ReservedItem(key, location, 1, price);
    }
    else
    {
      reservedItem.setCount(reservedItem.getCount() + 1);
      // NOTE: latest price overrides existing price
      reservedItem.setPrice(price);
    }
    customer.getReservations().put(reservedItem.getKey(), reservedItem);
  }
  
}
