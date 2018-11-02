package Client;

import java.rmi.RemoteException;
import java.util.Random;

import Server.Common.InvalidTransactionException;
import Server.Common.TransactionAbortedException;
import Server.LockManager.DeadlockException;

public class TestClientThread implements Runnable {
	static int ROUNDS = 1; // number of transactions to test
	float freq; // number of transactions to test
	private Thread t;
	private RMIClient client;

	public TestClientThread(RMIClient client, float freq) {
		this.client = client;
		this.freq = freq;

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
		for (int i = 0; i < ROUNDS; i++) {
			Random random = new Random();
			long offset = random.nextInt(41) - 20; 
			long waitTime = (long) (1 / freq * 1000) + offset;

			long start_execution = System.currentTimeMillis();
			
				// Do one set of transaction
				try {
					executeCmds();
				} catch (RemoteException | DeadlockException | InvalidTransactionException
						| TransactionAbortedException e1) {
					e1.printStackTrace();
					Thread.currentThread().interrupt();
				}
				long duration = System.currentTimeMillis() - start_execution;
				if(duration > waitTime) {
					System.out.println("Execution time longer than specified time interval. Existing...");
					return;
				}
				try {
					System.out.println(String.format("Ready to sleep for %s milliseconds", Long.toString(waitTime - duration)));
					Thread.sleep(waitTime - duration);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
	}

	// execute transaction
	public boolean executeCmds() throws RemoteException, DeadlockException, InvalidTransactionException, TransactionAbortedException {
		int txnId = client.m_resourceManager.start();
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

			client.m_resourceManager.commit(txnId);
		} catch (DeadlockException e) {
			client.m_resourceManager.abort(txnId);
			System.out.println("Deadlock!");
			return false;
		}
		return true;
	}

}