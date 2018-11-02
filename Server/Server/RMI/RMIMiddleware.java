package Server.RMI;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import Server.Common.Trace;
import Server.Interface.IResourceManager;
import Server.LockManager.DeadlockException;
import Server.Common.InvalidTransactionException;
import Server.Common.TransactionAbortedException;

public class RMIMiddleware implements IResourceManager {
	private static String s_serverName = "MiddleWare";
	private static final String s_rmiPrefix = "group6_";
	private static final int TIMEOUT_IN_SEC = 50;

	static RMIMiddleware mw;

	private static IResourceManager flightRM;
	private static IResourceManager carRM;
	private static IResourceManager roomRM;

	private ArrayList<Integer> customerIdx = new ArrayList<Integer>();
	private static int middleware_port = 3099;
	private static int server_port = 3099;

	private int txnIdCounter = 0;
	private HashSet<Integer> abortedTXN = new HashSet<Integer>();

	private ConcurrentHashMap<Integer, Thread> timeTable = new ConcurrentHashMap<Integer, Thread>();

	public RMIMiddleware(String s_serverName2) {
	}

	public static void main(String args[]) {
		try {

			if (args.length == 4) {
				s_serverName = args[0];
			} else {
				Trace.error(
						"RMIMiddleWare:: Expect 4 arguments. $0: hostname of MiddleWare, $1-$3: hostname of servers");
				System.exit(1);
			}

			// Create a new Server object
			IResourceManager mw_RM = null;
			try {
				mw = new RMIMiddleware(s_serverName);
				// Dynamically generate the stub (MiddleWare proxy
				mw_RM = (IResourceManager) UnicastRemoteObject.exportObject(mw, middleware_port);
			} catch (Exception e) {
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
			registry.rebind(s_rmiPrefix + s_serverName, mw_RM); // group6_MiddleWare

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
					} catch (Exception e) {
						System.err
								.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
					}
				}
			});
			System.out.println("'" + s_serverName + "' Middleware server ready and bound to '" + s_rmiPrefix
					+ s_serverName + "'" + "at port:" + String.valueOf(middleware_port));
		} catch (Exception e) {
			System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

		// Create and install a security manager

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
	}

	public class TimeOutThread implements Runnable {
		private int xid = 0;

		public TimeOutThread(int xid) {
			this.xid = xid;
		}

		@Override
		public void run() {
			try {
				Thread.sleep(TIMEOUT_IN_SEC * 1000);
			} catch (InterruptedException e) {
				System.out.println(Integer.toString(xid) + " interrupted.");
				Thread.currentThread().interrupt();
				return;
			}
			try {
				abort(this.xid);
			} catch (RemoteException | InvalidTransactionException e) {
				System.out.println(Integer.toString(xid) + " abort failed.");
			}
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

		Registry roomRegistry = LocateRegistry.getRegistry(args[3], server_port);
		roomRM = (IResourceManager) roomRegistry.lookup(s_rmiPrefix + "Rooms");
		if (roomRM == null)
			throw new AssertionError();

	}

	public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return flightRM.addFlight(id, flightNum, flightSeats, flightPrice);
	}

	@Override
	public boolean addCars(int id, String location, int numCars, int price)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return carRM.addCars(id, location, numCars, price);
	}

	@Override
	public boolean addRooms(int id, String location, int numRooms, int price)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return roomRM.addRooms(id, location, numRooms, price);
	}

	@Override
	public boolean deleteFlight(int id, int flightNum)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return flightRM.deleteFlight(id, flightNum);
	}

	@Override
	public boolean deleteCars(int id, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return carRM.deleteCars(id, location);
	}

	@Override
	public boolean deleteRooms(int id, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return roomRM.deleteRooms(id, location);
	}

	@Override
	public boolean deleteCustomer(int id, int customerID)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return flightRM.deleteCustomer(id, customerID) && carRM.deleteCustomer(id, customerID)
				&& roomRM.deleteCustomer(id, customerID);
	}

	@Override
	public int queryFlight(int id, int flightNumber)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return flightRM.queryFlight(id, flightNumber);
	}

	@Override
	public int queryCars(int id, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return carRM.queryCars(id, location);
	}

	@Override
	public int queryRooms(int id, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return roomRM.queryRooms(id, location);
	}

	@Override

	public String queryCustomerInfo(int id, int customerID)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		String carSummary = "";
		try {
			carSummary = carRM.queryCustomerInfo(id, customerID).split("/n", 2)[1];
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		String roomSummary = "";
		try {
			roomSummary = roomRM.queryCustomerInfo(id, customerID).split("/n", 2)[1];
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		return flightRM.queryCustomerInfo(id, customerID) + carSummary + roomSummary;
	}

	@Override
	public int queryFlightPrice(int id, int flightNumber)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return flightRM.queryFlightPrice(id, flightNumber);
	}

	@Override
	public int queryCarsPrice(int id, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return carRM.queryCarsPrice(id, location);
	}

	@Override
	public int queryRoomsPrice(int id, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return roomRM.queryRoomsPrice(id, location);
	}

	@Override
	public int newCustomer(int id)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		int cid;
		if (customerIdx.size() == 0)
			cid = 0;
		else
			cid = Collections.max(customerIdx) + 1;
		this.newCustomer(id, cid);
		return cid;
	}

	@Override
	public boolean newCustomer(int id, int cid)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		this.customerIdx.add(cid);
		resetTimer(id);
		return flightRM.newCustomer(id, cid) && carRM.newCustomer(id, cid) && roomRM.newCustomer(id, cid);
	}

	@Override
	public boolean reserveFlight(int id, int customerID, int flightNumber)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return flightRM.reserveFlight(id, customerID, flightNumber);
	}

	@Override
	public boolean reserveCar(int id, int customerID, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return carRM.reserveCar(id, customerID, location);
	}

	@Override
	public boolean reserveRoom(int id, int customerID, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		return roomRM.reserveRoom(id, customerID, location);
	}

	@Override
	public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car,
			boolean room)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		resetTimer(id);
		boolean res = true;
		Vector<String> history = new Vector<String>();
		for (String fn : flightNumbers) {
			if (reserveFlight(id, customerID, Integer.parseInt(fn)))
				history.add(fn);
			else {
				unReserveFlights(id, customerID, history);
				return false;
			}
		}
		if (car) {
			if (!reserveCar(id, customerID, location)) {
				unReserveFlights(id, customerID, history);
				return false;
			}
		}
		if (room) {
			if (!reserveRoom(id, customerID, location)) {
				unReserveFlights(id, customerID, history);
				if (car)
					unReserveCar(id, customerID, location);
				return false;
			}
		}
		return true; // return False if any of the above failed
	}

	@Override
	public String getName() throws RemoteException {
		return roomRM.getName();
	}

	@Override
	public int start() throws RemoteException {
		txnIdCounter += 1;
		start(txnIdCounter);
		return txnIdCounter;
	}

	@Override
	public void start(int txnId) throws RemoteException {
		initTimer(txnId);
		roomRM.start(txnId);
		carRM.start(txnId);
		flightRM.start(txnId);
	}

	@Override
	public void commit(int txnId) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
		if (timeTable.get(txnId) == null)
			throw new InvalidTransactionException(txnId);
		synchronized (abortedTXN) {
			if (abortedTXN.contains(txnId))
				throw new TransactionAbortedException(txnId);
		}
		killTimer(txnId);
		roomRM.commit(txnId);
		carRM.commit(txnId);
		flightRM.commit(txnId);
		removeTxn(txnId);

	}

	@Override
	public void abort(int txnID) throws RemoteException, InvalidTransactionException {
		if (timeTable.get(txnID) == null)
			throw new InvalidTransactionException(txnID);
		killTimer(txnID);
		roomRM.abort(txnID);
		carRM.abort(txnID);
		flightRM.abort(txnID);
		removeTxn(txnID);
		synchronized (abortedTXN) {
			abortedTXN.add(txnID);
		}
		Trace.info(abortedTXN.toString());
	}

	public boolean unReserveFlights(int id, int customerID, Vector<String> history)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		boolean res = true;
		for (String fn : history) {
			res &= unReserveFlight(id, customerID, Integer.parseInt(fn));
		}
		return res;
	}

	@Override
	public boolean unReserveFlight(int id, int customerID, int flightNumber)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		return flightRM.unReserveFlight(id, customerID, flightNumber);
	}

	@Override
	public boolean unReserveCar(int id, int customerID, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		return carRM.unReserveCar(id, customerID, location);
	}

	@Override
	public boolean unReserveRoom(int id, int customerID, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		return roomRM.unReserveRoom(id, customerID, location);
	}

	@Override
	public boolean shutdown() throws RemoteException {
		Iterator it = timeTable.entrySet().iterator();
		while (it.hasNext()) {
			ConcurrentHashMap.Entry pair = (ConcurrentHashMap.Entry) it.next();
			try {
				abort((int) pair.getKey());
			} catch (InvalidTransactionException e) {
				continue;
			}
		}
		return true;
	}

	private void initTimer(int xid) {
		Thread cur = new Thread(new TimeOutThread(xid));
		cur.start();
		timeTable.put(xid, cur);
		System.out.println(Integer.toString(xid) + " initiated timer...");
	}

	private void resetTimer(int xid) throws InvalidTransactionException, TransactionAbortedException {
		if (timeTable.get(xid) != null) {
			killTimer(xid);
			initTimer(xid);
		} else {
			synchronized (abortedTXN) {
				if (abortedTXN.contains(xid))
					throw new TransactionAbortedException(xid);
			}
			throw new InvalidTransactionException(xid);
		}
	}

	public void killTimer(int id) {
		Thread cur = timeTable.get(id);
		if (cur != null) {
			cur.interrupt();
			System.out.println(Integer.toString(id) + " interrupted timer...");
		}
	}
	
	private void removeTxn(int xid) {
		timeTable.remove(xid);
	}
}
