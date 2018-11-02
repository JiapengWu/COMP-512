package Client;

import java.util.Vector;
import java.util.Random;

import java.rmi.RemoteException;
import Server.LockManager.DeadlockException;


public class TestClientThread implements Runnable
{
	static int ROUNDS = 10; // number of transactions to test
	static int freq ; // number of transactions to test
	private Thread t;
	private RMIClient client;

	// Transaction parameters
	private static int customerId;
	

	public TestClientThread(RMIClient client, int freq, int customerId){
		this.client = client;

		this.freq = freq;
		this.customerId=customerId;

	}

	public void start()
	{
		if (t == null) {
      		Thread t = new Thread(this);
      		t.start();
    	}
	}

	@Override
	public void run()
	{
		// execute transactions in loop
		for (int i=0; i<ROUNDS;i++){
			Random rand = new Random();
			long waitTime = (long) rand.nextInt(1/freq * 1000);
      		try {
        		Thread.sleep(waitTime);
      		} catch (InterruptedException e) {
        		e.printStackTrace();
      		}
      
      		long start = System.nanoTime() / 1000;
      		try {
        		//Do one set of transaction
      			executeCmds();
        		long duration = System.nanoTime() / 1000 - start;
        		System.out.println(duration);
			} catch (Exception e) {
				System.out.println("Test failed");
				e.printStackTrace();
			}
		}
	}

	// execute transaction
	public static boolean executeCmds() throws RemoteException, DeadlockException
	{
		int txnId = client.m_resourceManager.start();
		try {
			client.m_resourceManager.newCustomer(txnId, txnId);
			client.m_resourceManager.addFlights(txnId, txnId, 100, 100);
			client.m_resourceManager.addCars(txnId, Integer.toString(txnId),100,100);
			client.m_resourceManager.addRooms(txnId, Integer.toString(txnId),100,100);
			
			client.m_resourceManager.reserveCar(txnId, txnId, Integer.toString(txnId));
			client.m_resourceManager.reserveRoom(txnId, txnId, Integer.toString(txnId));
			client.m_resourceManager.reserveFlight(txnId, txnId,txnId );

			client.m_resourceManager.queryRooms(txnId, Integer.toString(txnId));
			client.m_resourceManager.queryCars(txnId,Integer.toString(txnId));

			client.m_resourceManager.commit(txnId);
		} catch (DeadlockException e) {
		  mw.abort(txnId);
		  System.out.println("Deadlock!");
		  return false;
		}
		return true;
	}

}