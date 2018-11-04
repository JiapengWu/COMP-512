package Client;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import Server.Common.InvalidTransactionException;
import Server.Common.TransactionAbortedException;
import Server.LockManager.DeadlockException;

public class TestClient
{
	
	static final float NUM_CLIENTS = 10;
	static final int STEP_SIZE = 10;
	static final int MAX_LOAD = 30;

	private static String s_serverHost = "localhost";
	private static int s_serverPort = 3099;
	private static String s_serverName = "mw_server";
	
	private HashMap<Long, ArrayList<Long>> responseTimePerLoad = new HashMap<Long, ArrayList<Long>>();

	private static final char DEFAULT_SEPARATOR = ',';
	
	@SuppressWarnings("unchecked")
	public static void main(String args[]) throws IOException
	{
		if (args.length > 0) {
	      s_serverHost = args[0];
	    }
		TestClient testClient = new TestClient();
	    
	    // setUps(s_serverHost, s_serverPort, s_serverName);
	    for(long load = STEP_SIZE; load < MAX_LOAD; load += STEP_SIZE) {
	    	float freq = load / NUM_CLIENTS;
	    	ExecutorService es = Executors.newCachedThreadPool();
	    	ArrayList<Long> executionTime = new ArrayList<Long>();
			executionTime.add(load);
	    	for (int i = 0; i < NUM_CLIENTS; i++) {
	    		RMIClient client = new RMIClient();
	    		client.connectServer(s_serverHost, s_serverPort, s_serverName);
	    		TestClientThread ct = testClient.new TestClientThread(client, freq, load, executionTime);
	    		es.execute(ct);
	    	}
	    	es.shutdown();
	    	
	    	try {
	    		es.awaitTermination(30, TimeUnit.MINUTES);;
	    		testClient.responseTimePerLoad.put(load, (ArrayList<Long>) executionTime.clone());
	    	} catch (InterruptedException e) {
	    		System.out.println("Achiving 30 minutes, quiting...");
	    		es.shutdown();
	    	}
	    }
	    for(ArrayList<Long> value: testClient.responseTimePerLoad.values()) {	    	
	    	System.out.println(Arrays.toString(value.toArray()));
	    }
	    String csvFile = "/home/2016/jwu558/COMP-512/test.csv";
        FileWriter writer = new FileWriter(csvFile);

        for(ArrayList<Long> value: testClient.responseTimePerLoad.values()){
        	writeLine(writer, convertToStringList(value));
        }
        
        writer.flush();
        writer.close();
	}
	

	public class TestClientThread implements Runnable {
		static final int ROUNDS = 2; // number of transactions to test
		long load;
		float freq; // number of transactions to test
		private Thread t;
		private RMIClient client;
		private ArrayList<Long> executionTime;
	
		public TestClientThread(RMIClient client, float freq, long load, ArrayList<Long> executionTime) {
			this.client = client;
			this.freq = freq;
			this.load = load;
			this.executionTime = executionTime;
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
				long offset = random.nextInt(41) - 20; 
				long waitTime = (long) (1 / freq * 1000) + offset;

				System.out.println(String.format("Wait time: %s milliseconds", Long.toString(waitTime)));
				long start_execution = System.currentTimeMillis();
				
				// Do one set of transaction
				try {
					executeCmds();
				} catch (RemoteException | DeadlockException | InvalidTransactionException
						| TransactionAbortedException e1) {
					System.out.println(String.format("Test failed at round %d", i));
					return;
				}
				long duration = System.currentTimeMillis() - start_execution;
				if(duration > waitTime) {
					System.out.println("Execution time longer than specified time interval. Existing...");
					return;
				}
				try {
					executionTime.add(duration); // total duration for each round
					Thread.sleep(waitTime - duration);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			synchronized (this.executionTime) {
				this.executionTime.addAll(executionTime);
//				System.out.println(Arrays.toString(this.executionTime.toArray()));
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
	}
	
	public static ArrayList<String> convertToStringList(ArrayList<Long> values) {
		ArrayList<String> result = new ArrayList<String>();
		for (Long value : values) {
			result.add(Long.toString(value)); 
		}
		return result;
	}
	
    public static void writeLine(Writer w, ArrayList<String> values) throws IOException {

        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
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