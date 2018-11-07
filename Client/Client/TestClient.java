package Client;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import Server.Common.InvalidTransactionException;
import Server.Common.TransactionAbortedException;
import Server.Interface.IResourceManager;
import Server.LockManager.DeadlockException;

public class TestClient
{

	private static boolean fixClient = false;
	
	private static final int NUM_LOAD = 100;
	private static final int CLIENT_STEP_SIZE = 100;
	private static final int MAX_CLIENT = 1000;
	
	private static final int NUM_CLIENT = 10;
	private static final int LOAD_STEP_SIZE = 10;
	private static final int MAX_LOAD = 1000;
	
	private static String s_serverHost = "localhost";
	private static int s_serverPort = 3099;
	private static String s_serverName = "mw_server";
	private int failCount = 0;
	
	private ArrayList<ArrayList<Long>> clientResponseTimePerLoad = new ArrayList<ArrayList<Long>>();
	private ArrayList<ArrayList<Long>> mwResponseTimePerLoad = new ArrayList<ArrayList<Long>>();

	private static final char DEFAULT_SEPARATOR = ',';
	static boolean xtraDim = false;
	
	@SuppressWarnings({ "unchecked", "unused" })
	public static void main(String args[]) throws IOException
	{
		if (args.length > 0) {
	      s_serverHost = args[0];
	    }
		if (args.length == 2) {
			xtraDim = true;
		}
		
		TestClient testClient = new TestClient();
	    
		// extra dimensions
		if(xtraDim) {			
			try {
				setUps(s_serverHost, s_serverPort, s_serverName, "group6_");
			} catch (DeadlockException | InvalidTransactionException | TransactionAbortedException e1) {
				System.out.println("Initialize server failed");
				e1.printStackTrace();
			}
			// extra dimensions end
		}
	    
		if(fixClient) {
			for(long load = LOAD_STEP_SIZE; load <= MAX_LOAD; load += LOAD_STEP_SIZE) {
				System.out.println(String.format("%d clients, %d load.", NUM_CLIENT, load));
				float freq = ((float)load) / NUM_CLIENT;
				ExecutorService es = Executors.newCachedThreadPool();
				ArrayList<Long> clientExecutionTime = new ArrayList<Long>();
				ArrayList<Long> mwExecutionTime = new ArrayList<Long>();
				clientExecutionTime.add(load);
				for (int i = 0; i < NUM_CLIENT; i++) {
					RMIClient client = new RMIClient();
					client.connectServer(s_serverHost, s_serverPort, s_serverName);
					TestClientThread ct = 
							testClient.new TestClientThread(testClient, client, freq, load, 
									clientExecutionTime, mwExecutionTime, NUM_CLIENT, xtraDim);
					es.execute(ct);
				}
				es.shutdown();
				
				try {
					es.awaitTermination(30, TimeUnit.MINUTES);
					testClient.clientResponseTimePerLoad.add((ArrayList<Long>) clientExecutionTime.clone());
					if(testClient.failCount > 0.9 * NUM_CLIENT * TestClientThread.ROUNDS) {
						System.out.println("Early stopping.");break;
					}
					testClient.failCount = 0;
				} catch (InterruptedException e) {
					System.out.println("Achiving 30 minutes, quiting...");
					es.shutdown();
				}
			}
		}
		
		else {
			for(int numClient = CLIENT_STEP_SIZE; numClient <= MAX_CLIENT; numClient += CLIENT_STEP_SIZE) {
				System.out.println(String.format("%d clients, %d load.", numClient, NUM_LOAD));
				float freq = ((float)NUM_LOAD) / numClient;
				ExecutorService es = Executors.newCachedThreadPool();
				ArrayList<Long> clientExecutionTime = new ArrayList<Long>();
				ArrayList<Long> mwExecutionTime = new ArrayList<Long>();
				clientExecutionTime.add((long) numClient);
				for (int i = 0; i < numClient; i++) {
					RMIClient client = new RMIClient();
					client.connectServer(s_serverHost, s_serverPort, s_serverName);
					TestClientThread ct = 
							testClient.new TestClientThread(testClient, client, freq, NUM_LOAD, 
									clientExecutionTime, mwExecutionTime, numClient, xtraDim);
					es.execute(ct);
				}
				es.shutdown();
				
				try {
					es.awaitTermination(30, TimeUnit.MINUTES);
					testClient.clientResponseTimePerLoad.add((ArrayList<Long>) clientExecutionTime.clone());
					if(testClient.failCount > 0.9 * numClient * TestClientThread.ROUNDS) {
						System.out.println("Early stopping.");break;
					}
					testClient.failCount = 0;
				} catch (InterruptedException e) {
					System.out.println("Achiving 30 minutes, quiting...");
					es.shutdown();
				}
			}
		}
	    
//	    for(ArrayList<Long> value: testClient.mwResponseTimePerLoad) {	    	
//	    	System.out.println(Arrays.toString(value.toArray()));
//	    }
		String client_csvFile = null;
		String mw_csvFile = null;
		if(NUM_CLIENT == 1 && fixClient) {
			client_csvFile = "/home/2016/jwu558/COMP-512/test_client_single_client.csv";
			mw_csvFile = "/home/2016/jwu558/COMP-512/test_mw_single_client.csv";
		}else {			
			client_csvFile = String.format("/home/2016/jwu558/COMP-512/test_client_%s%s.csv", 
					fixClient?"fix_client_num":"fix_load_num", xtraDim? "_extra_dim":"");
			mw_csvFile = String.format("/home/2016/jwu558/COMP-512/test_mw_%s%s.csv", 
					fixClient?"fix_client_num":"fix_load_num", xtraDim? "_extra_dim":"");
		}
        FileWriter writer_client = new FileWriter(client_csvFile);
        for(ArrayList<Long> value: testClient.clientResponseTimePerLoad){
        	writeLine(writer_client, value);
        }
        
        FileWriter writer_mw = new FileWriter(client_csvFile);
        for(ArrayList<Long> value: testClient.mwResponseTimePerLoad){
        	writeLine(writer_mw, value);
        }
        writer_client.flush();
        writer_client.close();
        writer_mw.flush();
        writer_mw.close();
        
	}
	

	private static void setUps(String server, int port, String name, String s_rmiPrefix) throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		IResourceManager mw = null;
		try {
			boolean first = true;
			while (true) {
				try {
					Registry registry = LocateRegistry.getRegistry(server, port);
					mw = (IResourceManager)registry.lookup(s_rmiPrefix + name);
					System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
					break;
				}
				catch (NotBoundException|RemoteException e) {
					if (first) {
						System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
						first = false;
					}
				}
				Thread.sleep(500);
			}
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
		int xid = mw.start();
		mw.addCars(xid,"MTL",100000,100000);
		mw.addFlight(xid, 1, 1, 1);
		mw.addRooms(xid, "MTL", 1, 1);
		mw.newCustomer(xid, 1);
		mw.commit(xid);
		
	}


	public class TestClientThread implements Runnable {
		static final int ROUNDS = 10; // number of transactions to test
		long load;
		float freq; // number of transactions to test
		private Thread t;
		private TestClient testClient;
		private RMIClient client;
		private ArrayList<Long> clientExecutionTime;
		private ArrayList<Long> mwExecutionTime;
		private boolean xtraDim;
		private float numClients;
	
		public TestClientThread(TestClient testClient, RMIClient client, float freq, long load, 
				ArrayList<Long> clientExecutionTime, ArrayList<Long> mwExecutionTime, float numClients, boolean xtraDim) {
			this.testClient = testClient;
			this.client = client;
			this.freq = freq;
			this.load = load;
			this.clientExecutionTime = clientExecutionTime;
			this.mwExecutionTime = mwExecutionTime;
			this.xtraDim = xtraDim;
			this.numClients = numClients;
		}
	
		public void start() {
			if (t == null) {
				Thread t = new Thread(this);
				t.start();
			}
		}
	
		@Override
		public void run() {
			// execute transactions in loop
			ArrayList<Long> clientExecutionTime = new ArrayList<Long>();
			ArrayList<Long> mwExecutionTime = new ArrayList<Long>();
			
			for (int i = 0; i < ROUNDS; i++) {
				Random random = new Random();
				long fixed = (long) (1 / freq * 1000);
				long offset = (long) (random.nextInt((int) (0.1 * fixed + 1))- 0.05 * fixed); 
				long waitTime = (long) (1 / freq * 1000) + offset;

				System.out.println(String.format("Wait time: %s milliseconds", Long.toString(waitTime)));
				long start_execution = System.currentTimeMillis();
				long middlewareExecTime = 0;
				try {
					if (xtraDim) middlewareExecTime = executeCmdSameTid();
					else middlewareExecTime = executeCmds();
				} catch (RemoteException | DeadlockException | InvalidTransactionException
						| TransactionAbortedException e1) {
					synchronized (client) {
						System.out.println(String.format("Test failed at round %d", i));
						e1.printStackTrace();
					}
					return;
				}
				long duration = System.currentTimeMillis() - start_execution;
				if(duration > waitTime) {
					System.out.println("Execution time longer than specified time interval. Existing...");
					synchronized (testClient) {						
						this.testClient.failCount++;
					}
					continue;
				}
				try {
					clientExecutionTime.add(duration); // total duration for each round
					mwExecutionTime.add(middlewareExecTime);
					if(numClients != 1) {						
						Thread.sleep(waitTime - duration);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			synchronized (this.clientExecutionTime) {
				this.clientExecutionTime.addAll(clientExecutionTime);
			}
			synchronized (this.mwExecutionTime) {
				this.mwExecutionTime.addAll(mwExecutionTime);
			}
			
		}
	
	
		// execute transaction
		public long executeCmds() throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
			int txnId = client.m_resourceManager.start();
//			System.out.println("Executing transaction " + Integer.toString(txnId));
			try {
				client.m_resourceManager.newCustomer(txnId, txnId);
				client.m_resourceManager.addFlight(txnId, txnId, 100, 100);
				client.m_resourceManager.addCars(txnId, Integer.toString(txnId), 100, 100);
				client.m_resourceManager.addRooms(txnId, Integer.toString(txnId), 100, 100);
	
				client.m_resourceManager.reserveCar(txnId, txnId, Integer.toString(txnId));
				client.m_resourceManager.reserveRoom(txnId, txnId, Integer.toString(txnId));
				client.m_resourceManager.reserveFlight(txnId, txnId, txnId);
	
				client.m_resourceManager.queryRooms(txnId, Integer.toString(txnId));
				client.m_resourceManager.queryCars(txnId, Integer.toString(txnId));
				client.m_resourceManager.queryCustomerInfo(txnId, txnId);
	
				long mwExecTime = client.m_resourceManager.commit(txnId);
				return mwExecTime;
			} catch (DeadlockException e) {
				client.m_resourceManager.abort(txnId);
				System.out.println("Deadlock!");
				return 0;
			}
		}
		
		public long executeCmdSameTid() throws RemoteException, InvalidTransactionException, TransactionAbortedException {
			int txnId = client.m_resourceManager.start();
			try {
				client.m_resourceManager.queryCars(txnId, "MTL");
				client.m_resourceManager.queryCarsPrice(txnId, "MTL");
				client.m_resourceManager.queryFlight(txnId, 1);
				client.m_resourceManager.queryFlightPrice(txnId, 1);
				client.m_resourceManager.queryRooms(txnId, "MTL");
				client.m_resourceManager.queryRoomsPrice(txnId, "MTL");
				client.m_resourceManager.queryCustomerInfo(txnId, 1);
				client.m_resourceManager.queryCars(txnId, "MTL");
				long mwExecTime = client.m_resourceManager.commit(txnId);
				return mwExecTime;
			}catch(DeadlockException e) {
				client.m_resourceManager.abort(txnId);
				System.out.println("Deadlock!");
				return 0;
			}
		}
	}
	
    public static void writeLine(Writer w, ArrayList<Long> values) throws IOException {

        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Long v : values) {
        	String value = Long.toString(v);
            if (!first) {
                sb.append(DEFAULT_SEPARATOR);
            }
            sb.append(value);
            first = false;
        }
        sb.append("\n");
        w.append(sb.toString());
    }
 }