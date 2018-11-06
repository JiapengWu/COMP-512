package Client;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import Server.Common.InvalidTransactionException;
import Server.Common.TransactionAbortedException;
import Server.Interface.IResourceManager;
import Server.LockManager.DeadlockException;

public class TestClient
{
	
	static final float NUM_CLIENTS = 10;
	static final int STEP_SIZE = 100;
	static final int MAX_LOAD = 1000;

	private static String s_serverHost = "localhost";
	private static int s_serverPort = 3099;
	private static String s_serverName = "mw_server";
	
	private ArrayList<ArrayList<Long>> responseTimePerLoad = new ArrayList<ArrayList<Long>>();

	private static final char DEFAULT_SEPARATOR = ',';
	static boolean xtraDim = false;
	
	@SuppressWarnings("unchecked")
	public static void main(String args[]) throws IOException
	{
		if (args.length > 0) {
	      s_serverHost = args[0];
	    }
		if (args.length==2 ) {
			xtraDim=true;
		}
		TestClient testClient = new TestClient();
	    
		// extra dimensions
	    try {
			setUps(s_serverHost, s_serverPort, s_serverName, "group6_");
		} catch (DeadlockException | InvalidTransactionException | TransactionAbortedException e1) {
			System.out.println("Initialize server failed");
			e1.printStackTrace();
		}
	    // extra dimensions end
	    
	    for(long load = STEP_SIZE; load <= MAX_LOAD; load += STEP_SIZE) {
	    	float freq = load / NUM_CLIENTS;
	    	ExecutorService es = Executors.newCachedThreadPool();
	    	ArrayList<Long> executionTime = new ArrayList<Long>();
			executionTime.add(load);
	    	for (int i = 0; i < NUM_CLIENTS; i++) {
	    		RMIClient client = new RMIClient();
	    		client.connectServer(s_serverHost, s_serverPort, s_serverName);
	    		TestClientThread ct = testClient.new TestClientThread(client, freq, load, executionTime, NUM_CLIENTS, xtraDim);
	    		es.execute(ct);
	    	}
	    	es.shutdown();
	    	
	    	try {
	    		es.awaitTermination(30, TimeUnit.MINUTES);;
	    		testClient.responseTimePerLoad.add((ArrayList<Long>) executionTime.clone());
	    	} catch (InterruptedException e) {
	    		System.out.println("Achiving 30 minutes, quiting...");
	    		es.shutdown();
	    	}
	    }
//	    for(ArrayList<Long> value: testClient.responseTimePerLoad) {	    	
//	    	System.out.println(Arrays.toString(value.toArray()));
//	    }
	    String csvFile = "/home/2016/jwu558/COMP-512/test.csv";
        FileWriter writer = new FileWriter(csvFile);

        for(ArrayList<Long> value: testClient.responseTimePerLoad){
        	writeLine(writer, value);
        }
        
        writer.flush();
        writer.close();
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
		mw.commit(xid);
		
	}


	public class TestClientThread implements Runnable {
		static final int ROUNDS = 100; // number of transactions to test
		long load;
		float freq; // number of transactions to test
		private Thread t;
		private RMIClient client;
		private ArrayList<Long> executionTime;
		private boolean xtraDim;
		private float numClients;
	
		public TestClientThread(RMIClient client, float freq, long load, ArrayList<Long> executionTime, float numClients, boolean xtraDim) {
			this.client = client;
			this.freq = freq;
			this.load = load;
			this.executionTime = executionTime;
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
			ArrayList<Long> executionTime = new ArrayList<Long>();
			for (int i = 0; i < ROUNDS; i++) {
				Random random = new Random();
				long fixed = (long) (1 / freq * 1000);
				long offset = (long) (random.nextInt((int) (0.1 * fixed + 1))- 0.05 * fixed); 
				long waitTime = (long) (1 / freq * 1000) + offset;

//				System.out.println(String.format("Wait time: %s milliseconds", Long.toString(waitTime)));
				long start_execution = System.currentTimeMillis();
				
				// Do one set of transaction
				try {
					if (xtraDim) executeCmdSameTid();
					else executeCmds();
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
					continue;
				}
				try {
					executionTime.add(duration); // total duration for each round
					if(numClients != 1) {						
						Thread.sleep(waitTime - duration);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			synchronized (this.executionTime) {
				this.executionTime.addAll(executionTime);
			}
			
		}
	
		// execute transaction
		public boolean executeCmds() throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
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
	
				client.m_resourceManager.commit(txnId);
			} catch (DeadlockException e) {
				client.m_resourceManager.abort(txnId);
				System.out.println("Deadlock!");
				return false;
			}
			return true;
		}
		
		public boolean executeCmdSameTid() throws RemoteException, InvalidTransactionException, TransactionAbortedException {
			int txnId = client.m_resourceManager.start();
			try {
				client.m_resourceManager.queryCars(txnId, "MTL");
				client.m_resourceManager.commit(txnId);
			}catch(DeadlockException e) {
				client.m_resourceManager.abort(txnId);
				System.out.println("Deadlock!");
				return false;
			}
			return true;
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