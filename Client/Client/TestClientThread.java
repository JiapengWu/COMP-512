package Client;

import java.util.Vector;
import java.util.Random;

import java.rmi.RemoteException;
import Server.LockManager.DeadlockException;


public class TestClientThread implements Runnable
{
	static int ROUNDS = 10; // number of transactions to test
	static int period ; // number of transactions to test
	private Thread t;
	private RMIClient client;
	Vector<Command> commands; // arguments?

	public TestClientThread(RMIClient client, Vector<Command> commands, int period){
		this.client = client;
		this.commands = commands;
		this.period = period;

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
			long waitTime = (long) rand.nextInt(period);
      		try {
        		Thread.sleep(waitTime * 2);
      		} catch (InterruptedException e) {
        		e.printStackTrace();
      		}
      
      		long start = System.nanoTime() / 1000;
      		try {
        		//TODO: execute commands
      			executeCmds(commands);
        		long duration = System.nanoTime() / 1000 - start;
        		System.out.println(duration);
			} catch (Exception e) {
				System.out.println("Test failed");
				e.printStackTrace();
			}
		}
	}

	// TODO
	public static boolean executeCmds(Vector<Command> commands) throws RemoteException, DeadlockException
	{
		int txnId = mw.start();
		try {
		  // TODO 
		  client.mw.commit(txnId);
		} catch (DeadlockException e) {
		  mw.abort(txnId);
		  System.out.println("Deadlock!");
		  return false;
		}
		return true;
	}

}