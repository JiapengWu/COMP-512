package Server.RMI;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import Server.Common.InvalidTransactionException;
import Server.Common.Trace;
import Server.Common.TransactionAbortedException;
import Server.Common.TransactionManager;
import Server.Interface.IResourceManager;
import Server.LockManager.DeadlockException;

public class RMIMiddleware implements IResourceManager {
	private static String s_serverName = "MiddleWare";
	private static final String s_rmiPrefix = "group6_";

	static RMIMiddleware mw;

	private static IResourceManager flightRM;
	private static IResourceManager carRM;
	private static IResourceManager roomRM;

	private HashSet<Integer> customerIdx = new HashSet<Integer>();
	private static int middleware_port = 3099;
	private static int server_port = 3099;

	protected TransactionManager tm = new TransactionManager();

	public RMIMiddleware(String s_serverName2, Hashtable<Integer, IResourceManager> stubs) {
		TransactionManager restoredTM = null;
		while (true) {
			try {
				tm.stubs = stubs;
				restoredTM = tm.restore();
				break;
			} catch (RemoteException | TransactionAbortedException e) {
				e.printStackTrace();
				break;
			}
		}

		tm = restoredTM;
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

			try {
				getResourceManagers(args);
			} catch (Exception e) {
				Trace.error("RMIMiddleware: Error getting resource manager");
				e.printStackTrace();
			}

			// let TransactionManager know about the stubs
			Hashtable<Integer, IResourceManager> stubs = new Hashtable<Integer, IResourceManager>();
			stubs.put(1, flightRM);
			stubs.put(2, roomRM);
			stubs.put(3, carRM);
			// Create a new Server object
			IResourceManager mw_RM = null;
			try {
				mw = new RMIMiddleware(s_serverName, stubs);
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

			new Thread(mw.new ServerHealthCheckThread(args)).start();

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

	public class ServerHealthCheckThread implements Runnable {
		String[] args;

		public ServerHealthCheckThread(String[] args) {
			this.args = args;
		}

		@Override
		public void run() {
			while (true) {
				for (IResourceManager ir : new IResourceManager[] { flightRM, carRM, roomRM }) {
					try {
						ir.ping();
					} catch (RemoteException e) {
						boolean first = true;
						while (true) {
							try {
								if (first) {
									Trace.info("Reconnecting servers...");
								}
								getResourceManagers(this.args);
								tm.stubs.put(1, flightRM);
								tm.stubs.put(2, roomRM);
								tm.stubs.put(3, carRM);
								Trace.info("Reconnected.");
								break;
							} catch (ConnectException e1) {
								first = false;
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						}
					}
				}

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Trace.warn("Health check interrupted.");
				}
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
		tm.resetTimer(id);
		tm.updateRMSet(id, 1);
		// System.out.println(flightRM);
		return flightRM.addFlight(id, flightNum, flightSeats, flightPrice);
	}

	@Override
	public boolean addCars(int id, String location, int numCars, int price)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 2);
		return carRM.addCars(id, location, numCars, price);
	}

	@Override
	public boolean addRooms(int id, String location, int numRooms, int price)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 3);
		return roomRM.addRooms(id, location, numRooms, price);
	}

	@Override
	public boolean deleteFlight(int id, int flightNum)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 1);
		return flightRM.deleteFlight(id, flightNum);
	}

	@Override
	public boolean deleteCars(int id, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 2);
		return carRM.deleteCars(id, location);
	}

	@Override
	public boolean deleteRooms(int id, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 3);
		return roomRM.deleteRooms(id, location);
	}

	@Override
	public boolean deleteCustomer(int id, int customerID)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		customerIdx.remove(customerID);
		tm.updateRMSet(id, 1);
		tm.updateRMSet(id, 2);
		tm.updateRMSet(id, 3);
		return flightRM.deleteCustomer(id, customerID) && carRM.deleteCustomer(id, customerID)
				&& roomRM.deleteCustomer(id, customerID);
	}

	@Override
	public int queryFlight(int id, int flightNumber)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 1);
		return flightRM.queryFlight(id, flightNumber);
	}

	@Override
	public int queryCars(int id, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 2);
		return carRM.queryCars(id, location);
	}

	@Override
	public int queryRooms(int id, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 3);
		return roomRM.queryRooms(id, location);
	}

	@Override
	public String queryCustomerInfo(int id, int customerID)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		if (!customerIdx.contains(customerID)) {
			return String.format("Customer %d doesn't exists.", customerID);
		}
		tm.updateRMSet(id, 1);
		tm.updateRMSet(id, 2);
		tm.updateRMSet(id, 3);
		String carSummary = "";
		try {
			carSummary = carRM.queryCustomerInfo(id, customerID).split("\n", 2)[1];
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		String roomSummary = "";
		try {
			roomSummary = roomRM.queryCustomerInfo(id, customerID).split("\n", 2)[1];
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		return flightRM.queryCustomerInfo(id, customerID) + carSummary + roomSummary;
	}

	@Override
	public int queryFlightPrice(int id, int flightNumber)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 1);
		return flightRM.queryFlightPrice(id, flightNumber);
	}

	@Override
	public int queryCarsPrice(int id, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 2);
		return carRM.queryCarsPrice(id, location);
	}

	@Override
	public int queryRoomsPrice(int id, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 3);
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
		tm.resetTimer(id);
		tm.updateRMSet(id, 1);
		tm.updateRMSet(id, 2);
		tm.updateRMSet(id, 3);
		return flightRM.newCustomer(id, cid) && carRM.newCustomer(id, cid) && roomRM.newCustomer(id, cid);
	}

	@Override
	public boolean reserveFlight(int id, int customerID, int flightNumber)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 1);
		return flightRM.reserveFlight(id, customerID, flightNumber);
	}

	@Override
	public boolean reserveCar(int id, int customerID, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 2);
		return carRM.reserveCar(id, customerID, location);
	}

	@Override
	public boolean reserveRoom(int id, int customerID, String location)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 3);
		return roomRM.reserveRoom(id, customerID, location);
	}

	@Override
	public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car,
			boolean room)
			throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		tm.resetTimer(id);
		tm.updateRMSet(id, 1);
		tm.updateRMSet(id, 2);
		tm.updateRMSet(id, 3);
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
	public synchronized int start() throws RemoteException {
		int xid = tm.start();
		return xid;
	}

	@Override
	public void start(int txnId) throws RemoteException {
		tm.start(txnId);
	}

	@Override
	public void commit(int txnId) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
		tm.commit(txnId);
	}

	@Override
	public void abort(int txnId) throws RemoteException, InvalidTransactionException {
		tm.abort(txnId);
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
		return tm.shutdown();
	}

	@Override
	public boolean prepare(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		// do nothing at the middleware
		return true;
	}

	@Override
	public void resetCrashes() throws RemoteException {
		tm.crashMode = 0;
		flightRM.resetCrashes();
		carRM.resetCrashes();
		roomRM.resetCrashes();
	}

	@Override
	public void crashMiddleware(int mode) throws RemoteException {
		tm.crashMode = mode;
	}

	@Override
	public void crashResourceManager(String name, int mode) throws RemoteException {
		if (name.equals("flight"))
			flightRM.crashResourceManager(name, mode);
		else if (name.equals("car"))
			carRM.crashResourceManager(name, mode);
		else
			roomRM.crashResourceManager(name, mode);
	}

	@Override
	public boolean voteReply(int id) throws RemoteException, InvalidTransactionException {
		return true;
	}

	@Override
	public boolean ping() throws RemoteException {
		return true;
	}

}
