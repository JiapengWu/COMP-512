// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import Server.Interface.IResourceManager;
import Server.LockManager.DeadlockException;
import Server.LockManager.LockManager;
import Server.LockManager.TransactionLockObject;

public class ResourceManager implements IResourceManager {

	private HashSet<Integer> abortedTXN;
	int crashMode = -1;
	protected String m_name = "";
	protected RMHashMap m_data = new RMHashMap();
	protected HashMap<Integer, TransactionParticipant> map = new HashMap<Integer, TransactionParticipant>();
	protected LockManager lm = new LockManager();
	// Transaction history record from start to commit, to handle abort

	public ResourceManager(String p_name) {
		m_name = p_name;
	}

	/*
	 * Transaction related operations

	 */
//	- abort(xid):
//	map[xid].commited=-1 
//	write "abort"+xid
//	map.remove(xid)
	@Override
	public void abort(int xid) throws InvalidTransactionException {
		map.get(xid).commited = -1;
		DiskManager.writeLog(this.m_name, map);
		if (map.get(xid)==null) throw new InvalidTransactionException(xid);

		DiskManager.writeLog(this.m_name, map);
		Trace.info("ResourceManager_" + m_name + ":: transtaction Abort with id " + Integer.toString(xid));
		this.map.remove(xid);
		lm.UnlockAll(xid);
		abortedTXN.add(xid);
	}

//	- commit(xid):
//	write "commit"+xid into log
//	map[xid].commited=1
//	applyCommit(xid)
//	map.remove(xid)
	@Override
	public void commit(int xid) throws InvalidTransactionException,TransactionAbortedException {
		if(this.crashMode == 4) System.exit(0);
		
		map.get(xid).commited = 1;
		DiskManager.writeLog(this.m_name, map);
		TransactionParticipant transaction = map.get(xid);
		if (transaction == null) throw new InvalidTransactionException(xid);
		Trace.info("ResourceManager_" + m_name + ":: transtaction commit with id " + Integer.toString(xid));
		// Apply writes (including deletes)
		synchronized (m_data) {
			RMHashMap writes = transaction.xWrites;
			Set<String> keys = writes.keySet();
			for (String key : keys) {
				m_data.put(key, writes.get(key));
			}
			RMHashMap deletes = transaction.xDeletes;
			keys = deletes.keySet();
			for (String key : keys) {
				m_data.remove(key);
			}
		}
		// empty history
		this.map.remove(xid);
		lm.UnlockAll(xid);
	}

	// helper to debug
	private void printMem(int xid) {
		TransactionParticipant transaction = this.map.get(xid);
		System.out.println(">>> Server memory info for xid=" + Integer.toString(xid));
		System.out.println("==== m_data.keys() ====");
		System.out.println(m_data.keySet());
		System.out.println("==== xCopies[" + Integer.toString(xid) + "] ====");
		System.out.println(transaction.xCopies.keySet());
		System.out.println("==== xWrites[" + Integer.toString(xid) + "].key() ====");
		System.out.println(transaction.xWrites.keySet());
		System.out.println("==== xDeletes[" + Integer.toString(xid) + "].key() ====");
		System.out.println(transaction.xDeletes.keySet());
	}

	@Override
	public void start(int xid) {
		Trace.info("ResourceManager_" + m_name + ":: transtaction start with id " + Integer.toString(xid));
		synchronized (m_data) {
			TransactionParticipant transaction = new TransactionParticipant(xid, m_data);
			map.put(xid, transaction);
		}
		// printMem(xid);
	}

	// Reads a data item
	protected RMItem readData(int xid, String key) throws DeadlockException {
		// if we haven deleted it, then we don't return anything
		TransactionParticipant transaction = this.map.get(xid);
		RMHashMap deletes = transaction.xDeletes;
		synchronized (deletes) {
			if(deletes.containsKey(key)) {
//				printMem(xid);
				return null;
			}
		}
		RMHashMap copy = transaction.xCopies;
		synchronized (m_data) {
			RMItem item = copy.get(key);
			if (item != null) {
				lm.Lock(xid, key, TransactionLockObject.LockType.LOCK_READ);
				
				return (RMItem) item.clone();
			} else {
				item = m_data.get(key);
				if (item != null) {
					lm.Lock(xid, key, TransactionLockObject.LockType.LOCK_READ);
					copy.put(key, item);
					return (RMItem) item.clone();
				}
				return null;
			}
		}
	}

	// Writes a data item
	protected void writeData(int xid, String key, RMItem value) throws DeadlockException {
		lm.Lock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
		TransactionParticipant transaction = this.map.get(xid);
		RMHashMap copy = transaction.xCopies;
		synchronized (copy) {
			copy.put(key, value);
		}
		RMHashMap writes = transaction.xWrites;
		synchronized (writes) {
			writes.put(key, value);
		}
		RMHashMap deletes = transaction.xDeletes;
		synchronized (deletes) {
			if (deletes.containsKey(key)) deletes.remove(key);
		}
		// printMem(xid);
	}

	// Remove the item out of storage
	protected void removeData(int xid, String key) throws DeadlockException {
		TransactionParticipant transaction = this.map.get(xid);
		lm.Lock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
		RMHashMap deletes = transaction.xDeletes;
		RMHashMap copy = transaction.xCopies;
		synchronized (deletes) {
			deletes.put(key, null);
			copy.remove(key);
		}
		// printMem(xid);
	}

	// Deletes the encar item
	protected boolean deleteItem(int xid, String key) throws DeadlockException {
		Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem) readData(xid, key);
		// Check if there is such an item in the storage
		if (curObj == null) {
			Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
			return false;
		} else {
			if (curObj.getReserved() == 0) {
				removeData(xid, curObj.getKey());
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
				return true;
			} else {
				Trace.info("RM::deleteItem(" + xid + ", " + key
						+ ") item can't be deleted because some customers have reserved it");
				return false;
			}
		}
	}

	// Query the number of available seats/rooms/cars
	protected int queryNum(int xid, String key) throws DeadlockException {
		Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem) readData(xid, key);
		int value = 0;
		if (curObj != null) {
			value = curObj.getCount();
		}
		Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
		return value;
	}

	// Query the price of an item
	protected int queryPrice(int xid, String key) throws DeadlockException {
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem) readData(xid, key);
		int value = 0;
		if (curObj != null) {
			value = curObj.getPrice();
		}
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
		return value;
	}

	// unReserve an Item
	protected boolean unReserveItem(int xid, int customerID, String key, String location) throws DeadlockException {
		Trace.info("RM::unReserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called");
		// Read customer object if it exists (and read lock it)
		Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
		if (customer == null) {
			Trace.warn("RM::unReserveItem(" + xid + ", " + customerID + ", " + key + ", " + location
					+ ")  failed--customer doesn't exist");
			return false;
		}

		// Check if the item is available
		ReservableItem item = (ReservableItem) readData(xid, key);
		if (item == null) {
			Trace.warn("RM::unReserveItem(" + xid + ", " + customerID + ", " + key + ", " + location
					+ ") failed--item doesn't exist");
			return false;
		} else if (item.getCount() == 0) {
			Trace.warn("RM::unReserveItem(" + xid + ", " + customerID + ", " + key + ", " + location
					+ ") failed--No more items");
			return false;
		} else {
			customer.unReserve(key, location, item.getPrice());
			writeData(xid, customer.getKey(), customer);

			// Increase the number of available items in the storage
			item.setCount(item.getCount() + 1);
			item.setReserved(item.getReserved() - 1);
			writeData(xid, item.getKey(), item);

			Trace.info("RM::unReserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}
	}

	// Reserve an item
	protected boolean reserveItem(int xid, int customerID, String key, String location) throws DeadlockException {
		Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called");
		// Read customer object if it exists (and read lock it)
		Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
		if (customer == null) {
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location
					+ ")  failed--customer doesn't exist");
			return false;
		}

		// Check if the item is available
		ReservableItem item = (ReservableItem) readData(xid, key);
		if (item == null) {
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location
					+ ") failed--item doesn't exist");
			return false;
		} else if (item.getCount() == 0) {
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location
					+ ") failed--No more items");
			return false;
		} else {
			customer.reserve(key, location, item.getPrice());
			writeData(xid, customer.getKey(), customer);

			// Decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);
			writeData(xid, item.getKey(), item);

			Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its
	// current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice)
			throws RemoteException, DeadlockException, InvalidTransactionException,TransactionAbortedException {
		Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		Flight curObj = (Flight) readData(xid, Flight.getKey(flightNum));
		if (curObj == null) {
			// Doesn't exist yet, add it
			Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats
					+ ", price=$" + flightPrice);
		} else {
			// Add seats to existing flight and update the price if greater than zero
			curObj.setCount(curObj.getCount() + flightSeats);
			if (flightPrice > 0) {
				curObj.setPrice(flightPrice);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats="
					+ curObj.getCount() + ", price=$" + flightPrice);
		}
		return true;
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current
	// price
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException, DeadlockException, InvalidTransactionException,TransactionAbortedException {
		Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Car curObj = (Car) readData(xid, Car.getKey(location));
		if (curObj == null) {
			// Car location doesn't exist yet, add it
			Car newObj = new Car(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$"
					+ price);
		} else {
			// Add count to existing car location and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0) {
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count="
					+ curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its
	// current price
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException, DeadlockException, InvalidTransactionException,TransactionAbortedException{
		Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Room curObj = (Room) readData(xid, Room.getKey(location));
		if (curObj == null) {
			// Room location doesn't exist yet, add it
			Room newObj = new Room(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count
					+ ", price=$" + price);
		} else {
			// Add count to existing object and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0) {
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count="
					+ curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException, DeadlockException , InvalidTransactionException,TransactionAbortedException{
		return deleteItem(xid, Flight.getKey(flightNum));
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location) throws RemoteException, DeadlockException ,InvalidTransactionException,TransactionAbortedException{
		return deleteItem(xid, Car.getKey(location));
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location) throws RemoteException, DeadlockException,InvalidTransactionException ,TransactionAbortedException{
		return deleteItem(xid, Room.getKey(location));
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum) throws RemoteException, DeadlockException , InvalidTransactionException,TransactionAbortedException{
		return queryNum(xid, Flight.getKey(flightNum));
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException, DeadlockException ,InvalidTransactionException,TransactionAbortedException{
		return queryNum(xid, Car.getKey(location));
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location) throws RemoteException, DeadlockException,InvalidTransactionException,TransactionAbortedException {
		return queryNum(xid, Room.getKey(location));
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException, DeadlockException ,InvalidTransactionException, TransactionAbortedException{
		return queryPrice(xid, Flight.getKey(flightNum));
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location) throws RemoteException, DeadlockException ,InvalidTransactionException,TransactionAbortedException{
		return queryPrice(xid, Car.getKey(location));
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location) throws RemoteException, DeadlockException,InvalidTransactionException ,TransactionAbortedException{
		return queryPrice(xid, Room.getKey(location));
	}

	public String queryCustomerInfo(int xid, int customerID) throws RemoteException, DeadlockException ,InvalidTransactionException, TransactionAbortedException{
		Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
		if (customer == null) {
			Trace.warn("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			// NOTE: don't change this--WC counts on this value indicating a customer does
			// not exist...
			return "";
		} else {
			Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
			System.out.println(customer.getBill());
			return customer.getBill();
		}
	}
	
	public int newCustomer(int xid) throws RemoteException, DeadlockException ,InvalidTransactionException, TransactionAbortedException{
		Trace.info("RM::newCustomer(" + xid + ") called");
		// Generate a globally unique ID for the new customer
		int cid = Integer
				.parseInt(String.valueOf(xid) + String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND))
						+ String.valueOf(Math.round(Math.random() * 100 + 1)));
		Customer customer = new Customer(cid);
		writeData(xid, customer.getKey(), customer);
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	public boolean newCustomer(int xid, int customerID) throws RemoteException, DeadlockException,InvalidTransactionException ,TransactionAbortedException{
		Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
		if (customer == null) {
			customer = new Customer(customerID);
			writeData(xid, customer.getKey(), customer);
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
			return true;
		} else {
			Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
			return false;
		}
	}

	public boolean deleteCustomer(int xid, int customerID) throws RemoteException, DeadlockException ,InvalidTransactionException, TransactionAbortedException{
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
		if (customer == null) {
			Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			return false;
		} else {
			// Increase the reserved numbers of all reservable items which the customer
			// reserved.
			RMHashMap reservations = customer.getReservations();
			for (String reservedKey : reservations.keySet()) {
				ReservedItem reserveditem = customer.getReservedItem(reservedKey);
				Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey()
						+ " " + reserveditem.getCount() + " times");
				ReservableItem item = (ReservableItem) readData(xid, reserveditem.getKey());
				Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey()
						+ " which is reserved " + item.getReserved() + " times and is still available "
						+ item.getCount() + " times");
				item.setReserved(item.getReserved() - reserveditem.getCount());
				item.setCount(item.getCount() + reserveditem.getCount());
				writeData(xid, item.getKey(), item);
			}

			// Remove the customer from the storage
			removeData(xid, customer.getKey());
			Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
			return true;
		}
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException, DeadlockException,InvalidTransactionException, TransactionAbortedException {
		return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException, DeadlockException,InvalidTransactionException , TransactionAbortedException{
		return reserveItem(xid, customerID, Car.getKey(location), location);
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException, DeadlockException ,InvalidTransactionException, TransactionAbortedException{
		return reserveItem(xid, customerID, Room.getKey(location), location);
	}

	// undo reserve
	public boolean unReserveFlight(int xid, int customerID, int flightNum) throws RemoteException, DeadlockException ,InvalidTransactionException, TransactionAbortedException{
		return unReserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}

	public boolean unReserveCar(int xid, int customerID, String location) throws RemoteException, DeadlockException ,InvalidTransactionException, TransactionAbortedException{
		return unReserveItem(xid, customerID, Car.getKey(location), location);
	}

	public boolean unReserveRoom(int xid, int customerID, String location) throws RemoteException, DeadlockException,InvalidTransactionException, TransactionAbortedException {
		return unReserveItem(xid, customerID, Room.getKey(location), location);
	}

	// Reserve bundle
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car,
			boolean room) throws RemoteException, DeadlockException ,InvalidTransactionException, TransactionAbortedException{
		return false;
	}

	public String getName() throws RemoteException {
		return m_name;
	}

	@Override
	public int start() throws RemoteException {
		return 0;
	}

	public boolean shutdown() throws RemoteException{

		//System.exit(0);
		new Thread() {
	    @Override
	    public void run() {
	      System.out.print("Shutting down...");
	      try {
	        sleep(500);
	      } catch (InterruptedException e) {
	        // I don't care
	      }
	      System.out.println("done");
	      System.exit(0);
	    }

  		}.start();
	
		return true;
	}

	@Override
	public boolean voteReply(int id)
			throws RemoteException, InvalidTransactionException {
		boolean decision = true;
		if(this.crashMode == 1) System.exit(0);
		if(abortedTXN.contains(id)) {
			decision = false;
		}
		//desision
		
		
		map.get(id).votedYes = decision? 1:-1;
		// TODO: when do we vote no?
		DiskManager.writeLog(this.m_name, map);

		if(this.crashMode == 2) System.exit(0);
		if(this.crashMode == 3) shutdown();
		return decision;
	}
	
	@SuppressWarnings("unchecked")
	public void restore() {

		HashMap<Integer, TransactionParticipant> log = null;
		try {
			log = (HashMap<Integer, TransactionParticipant>) DiskManager.readLog(m_name);
		} catch (IOException e) {
			System.out.println("File dones't exist, nothing to restore.");
		}

		if(this.crashMode == 4) System.exit(0);
		this.map = log;
		Iterator it = map.entrySet().iterator();
		while (it.hasNext()) {
			@SuppressWarnings("unchecked")
			HashMap.Entry<Integer, TransactionParticipant> pair = (HashMap.Entry<Integer, TransactionParticipant>) it.next();
			try {
				int xid = (int) pair.getKey();
				TransactionParticipant transaction = (TransactionParticipant)pair.getValue();
				if(transaction.votedYes == 1) {
					if(transaction.commited == 0) {
						// on middleware giving commit command, escape and commit
						while(true) {
							
						}
					}
					else if (transaction.commited == 1) {
						try {
							commit(xid);
						} catch (TransactionAbortedException e) {
							System.out.println("Transaction aborted.");
						}
					}
					else {
						abort(xid);
					}
				}else abort(xid);
						
			} catch (InvalidTransactionException e) {
				
			}
		}
		
	}

	@Override
	public boolean prepare(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return true;
	}

	@Override
	public void resetCrashes() throws RemoteException {
		this.crashMode = -1;
	}

	@Override
	public void crashMiddleware(int mode) throws RemoteException {
	}

	@Override
	public void crashResourceManager(String name, int mode) throws RemoteException {
		this.crashMode = mode; 
	}
	
}
