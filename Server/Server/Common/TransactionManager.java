package Server.Common;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import Server.Interface.IResourceManager;

public class TransactionManager {
	public static final int TIMEOUT_IN_SEC = 1000;
	public static final int TIMEOUT_VOTE_IN_SEC = 10;
	private String name = "MiddleWare";
	private int txnIdCounter;
	private HashSet<Integer> abortedTXN;
	private ConcurrentHashMap<Integer, Thread> timeTable;
	public Hashtable<Integer, IResourceManager> stubs; // {1: flightRM, 2: roomRM, 3: carRM}
	protected Hashtable<Integer, TransactionCoordinator> txns;

	/* crash mode: 0 - unset
	1. Crash before sending vote request --ok
	2. Crash after sending vote request and before receiving any replies
	3. Crash after receiving some replies but not all -- ok
	4. Crash after receiving all replies but before deciding --ok
	5. Crash after deciding but before sending decision -- ok
	6. Crash after sending some but not all decisions --ok
	7. Crash after having sent all decisions --ok
	8. Recovery of the coordinator (if you have decided to implement coordinator recovery)
	*/
	public int crashMode = 0; 

	public static class TMMeta implements Serializable{
		int counter;
		HashSet<Integer> aborted;
		public TMMeta(int counter, HashSet<Integer> aborted){
			this.counter = counter;
			this.aborted = aborted;
		}


	}

	public TransactionManager(){
		this.stubs = new Hashtable<Integer, IResourceManager>();
		txnIdCounter = 0;
		abortedTXN = new HashSet<Integer>();
		timeTable = new ConcurrentHashMap<Integer, Thread>();
		txns = new Hashtable<Integer, TransactionCoordinator>();
		//Trace.info("construct new TM");
	}

	public TransactionManager(Hashtable< Integer,IResourceManager> stubs){
		this.stubs =stubs;

		txnIdCounter = 0;
		abortedTXN = new HashSet<Integer>();
		timeTable = new ConcurrentHashMap<Integer, Thread>();
		txns = new Hashtable<Integer, TransactionCoordinator>();
		// Trace.info("construct new TM");
	}

	// return a TM for middleware to use later
	@SuppressWarnings("unchecked")

	public TransactionManager restore()
			throws RemoteException, InvalidTransactionException, TransactionAbortedException {
		Trace.info("restoring...");
		// DM need to read logs about all transactions
		Hashtable<Integer, TransactionCoordinator> old_txns = null;
		TMMeta old_tmMeta = null;
		try {
			old_txns = (Hashtable<Integer, TransactionCoordinator>) DiskManager.readLog(name);
			old_tmMeta = DiskManager.readTMMetaLog(name);
			// priorTxns = DiskManager.readAliveTransactions(name);
		}
		// if no prior TM log exist, just create a new one and return
		catch (FileNotFoundException e){
			return new TransactionManager(stubs);
		}
		catch (Exception e){
			e.printStackTrace();
		}
		Trace.info("Recovering old transaction");
		// else process ongoing transactions before crash
		
		Iterator<Integer> itr = old_txns.keySet().iterator();
		while (itr.hasNext())
		{
			int xid = (int) itr.next();
			TransactionCoordinator trans =  old_txns.get(xid);
			Trace.info(String.format("Transaction #%d, started=%d, decision=%d,EOT=%d",xid, trans.started, trans.decision, trans.EOT));
			if (crashMode == 8) System.exit(1);
			// already end-of-transactions: just ignore
			
			if (trans.EOT == 1)
			{
				Trace.info(String.format("removing trans #%d",xid));
				itr.remove();
				continue;
			}
			// for transactions that started 2PC:
			else if (trans.started == 1)
			{
				// already made decision: resend decision to all participants
				if(trans.decision==1)
				{
					Trace.info(String.format("sending COMMIT to trans #%d",xid));
					sendDecision(trans,true);
				}
				// haven't made decision: abort
				else
				{
					Trace.info(String.format("sending ABORT to trans #%d",xid));
					trans.decision=-1;
					old_txns.put(trans.xid, trans);
					sendDecision(trans,false);
					old_tmMeta.aborted.add(xid);
				}
				trans.EOT = 1;
				old_txns.put(xid, trans);
			}
			else
			{
				// for transactions that haven't started 2PC: abort
				Trace.info(String.format("sending ABORT to trans #%d",xid));
				sendDecision(trans,false);
				old_tmMeta.aborted.add(xid);
				trans.EOT = 1;
				old_txns.put(xid, trans);

			}
		}
		Trace.info(String.format("From log: \n map <xid, Transaction> has size %d; txnCounter=%d",old_txns.size(),old_tmMeta.counter));
		if (crashMode == 8) System.exit(1);
		// full (?) recovery of "abortedTXN" and "txnCounter"
		TransactionManager tm = new TransactionManager(stubs);
		tm.txns = old_txns;
		tm.abortedTXN = old_tmMeta.aborted;
		tm.txnIdCounter = old_tmMeta.counter + 1;

		DiskManager.writeLog(name, old_txns);
		DiskManager.writeTMMetaLog(name, new TMMeta(tm.txnIdCounter, tm.abortedTXN));
		return tm;
	}


	// client abort, no need to vote
	public void abort(int txnID) throws RemoteException, InvalidTransactionException {
		if (timeTable.get(txnID) == null)
			throw new InvalidTransactionException(txnID);
		killTimer(txnID);
		TransactionCoordinator trans = txns.get(txnID);
		if (trans == null)
			throw new InvalidTransactionException(txnID);

		try {
			sendDecision(trans, false);
		} catch (TransactionAbortedException e) {
			e.printStackTrace();
		}
		synchronized (abortedTXN) {
			abortedTXN.add(txnID);
		}
		DiskManager.writeTMMetaLog(name, new TMMeta(txnIdCounter, abortedTXN));

	}

	public void commit(int txnId) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
		TransactionCoordinator trans = txns.get(txnId);

		synchronized (abortedTXN) {
			if (abortedTXN.contains(txnId))
				throw new TransactionAbortedException(txnId);
		}
		if (trans==null) throw new InvalidTransactionException(txnId);
		killTimer(trans.xid);

		prepare(trans);

		if (crashMode == 1)
			System.exit(1);

		// get votes from participants
		boolean decision = true;
		boolean timeout = false;
		LinkedList<Boolean> voteResults = new LinkedList<Boolean>();
		LinkedList<Boolean> timeoutList = new LinkedList<Boolean>();
		LinkedList<Thread> voteThreads = new LinkedList<Thread>();

		Trace.info("start sending vote request...");

//		ExecutorService service = Executors.newCachedThreadPool();

//		long start = System.currentTimeMillis();
//		for (Integer rmIdx : trans.rmSet) {
//			ExecutionWithTimeout execution = new ExecutionWithTimeout(new VoteReqThread(txnId, rmIdx, voteResults), 
//					timeoutList);
//			service.execute(execution);
//		}
//		service.shutdown();
//
//		try {
//			service.awaitTermination(TIMEOUT_VOTE_IN_SEC + 1, TimeUnit.SECONDS);
//			timeout = timeoutList.contains(false);
//		} catch (InterruptedException e) {
//			service.shutdownNow();
//		}
		for (Integer rmIdx : trans.rmSet) {
			Thread voteThread = new Thread(new VoteReqThread(txnId, rmIdx, voteResults, timeoutList));
			voteThreads.add(voteThread);
			voteThread.start();
		}
		
		for(Thread t: voteThreads) {
			try {
				t.join(TIMEOUT_VOTE_IN_SEC * 1000);
				t.interrupt();
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		timeout = timeoutList.contains(false);

		decision = !voteResults.contains(false) && !timeout;
		Trace.info(String.format("Coordinator decision : %s. Timeout: %s", decision, timeout));
		
		if (crashMode ==4) System.exit(1);
		// write decision to log
		trans.decision = (decision == true) ? 1 : -1;
		txns.put(txnId, trans);
		DiskManager.writeLog(name, txns);
		if (crashMode == 5)
			System.exit(1);
		// send decision to all participants
		sendDecision(trans, decision);
		if (crashMode == 7)
			System.exit(1);

		trans.EOT = 1;
		txns.put(txnId, trans);
		DiskManager.writeLog(name, txns);
		removeTxn(trans.xid);
	}

	public class ExecutionWithTimeout implements Runnable {
		Runnable voteReqThread;
		LinkedList<Boolean> timeoutList;

		public ExecutionWithTimeout(Runnable voteReqThread, LinkedList<Boolean> timeoutList) {
			this.voteReqThread = voteReqThread;
			this.timeoutList = timeoutList;
		}

		@Override
		public void run() {
			ExecutorService service = Executors.newSingleThreadExecutor();
			final Future<?> future = service.submit(voteReqThread);
			try {
				future.get(TIMEOUT_VOTE_IN_SEC, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				synchronized(timeoutList) {					
					this.timeoutList.add(false);
				}
				Trace.info("Time out");
			} finally {
				service.shutdownNow();
				Trace.info("Shutted down");
			}
		}
	}

	public class VoteReqThread implements Runnable {
		private int txnId = 0;
		private int rmIdx = 0;
		private LinkedList<Boolean> voteResults;
		private LinkedList<Boolean> timeoutList;
		
		public VoteReqThread(int txnId, int rmIdx, LinkedList<Boolean> voteResults, LinkedList<Boolean> timeoutList) {
			this.txnId = txnId;
			this.rmIdx = rmIdx;
			this.voteResults = voteResults;
			this.timeoutList = timeoutList;
		}

		@Override
		public void run() {
			while(!Thread.currentThread().isInterrupted()){				
				try {
					boolean decision = stubs.get(rmIdx).voteReply(txnId); // if any stub vote no, decision will be 0
					Trace.info(String.format("Vote request received from #%d RM, the result is %s", rmIdx, decision));
					synchronized (voteResults) {
						voteResults.add(decision);
					}
					break;
				}catch(RemoteException e) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						Trace.info("Timeout");
						synchronized(timeoutList) {							
							timeoutList.add(false);
						}
						return;
					}
				}
				catch (Exception e) {
					Trace.info(String.format("Exception receiving vote request", rmIdx));
					synchronized (voteResults) {
						voteResults.add(false);
					}
					break;
//					e.printStackTrace();
				}
			}
		}
	}

	public void prepare(TransactionCoordinator trans) {
		// prepare for 2PC
		trans.started = 1;
		txns.put(trans.xid, trans);
		// write "start2PC" to logs
		DiskManager.writeLog(name, txns);
	}

	/*
	 * send commit or abort decision to all participants.
	 * 
	 * @param decision: 1 -- commit, 0 -- abort
	 */
	public void sendDecision(TransactionCoordinator trans, boolean decision) throws InvalidTransactionException, TransactionAbortedException {
		
//		for (Integer rmIdx: trans.rmSet) {	
//			
//			new Thread(new DecisionThread(rmIdx, trans.xid, decision)).start();
//			// crash after sending some but not all decisions
//			if (crashMode == 6)
//				System.exit(1);
//		}
		
		for (Integer rmIdx: trans.rmSet) {	
			try {
				if (decision)stubs.get(rmIdx).commit(trans.xid);
				else stubs.get(rmIdx).abort(trans.xid);
			} catch (RemoteException e) {
				Trace.warn(String.format("Romote exception at server #%d", rmIdx));
			}
			// crash after sending some but not all decisions
			if (crashMode == 6)
				System.exit(1);
		}
		Trace.info(String.format("Sent decision of transaction #%d to all Participants",trans.xid));
	}
	
	public class DecisionThread implements Runnable{
		int rmIdx;
		int xid;
		boolean decision;
		public DecisionThread(int rmIdx, int xid, boolean decision){
			this.rmIdx = rmIdx;
			this.xid = xid;
			this.decision = decision;
		}
		@Override
		public void run() {
			try {
				Trace.info(String.format("send commit decision %s to RM #%d", decision, rmIdx));
				if (decision)stubs.get(rmIdx).commit(xid);
				else stubs.get(rmIdx).abort(xid);
			} catch (RemoteException e) {
				Trace.warn("Remote Exception");
			} catch (InvalidTransactionException e) {
				Trace.warn("Invalid transaction.");
			} catch (TransactionAbortedException e) {
				Trace.warn("Transaction aborted.");
			}
		}
		
	}


	public synchronized int start() throws RemoteException {
		int xid = txnIdCounter;
		start(txnIdCounter);
		txnIdCounter += 1;
		return xid;
	}

	public void start(int txnId) throws RemoteException {
		
		initTimer(txnId);
		TransactionCoordinator trans = new TransactionCoordinator(txnId);
		txns.put(txnId, trans);
		DiskManager.writeLog(name, txns);
		DiskManager.writeTMMetaLog(name, new TMMeta(txnIdCounter, abortedTXN));

		for (IResourceManager stub : stubs.values()) {
//			Trace.info("Starting at stub " + stub);
			try {
				stub.start(txnId);
			}catch(RemoteException e) {
//				e.printStackTrace();
				Trace.info(String.format("Server %s failed to start", stub));
			}
		}
	}

	// update the RM stub invovled with this transaction.
	// @param: rm: 1: flight, 2: car, 3:room
	public void updateRMSet(int xid, int rm) {
		TransactionCoordinator trans = txns.get(xid);
		if (trans.rmSet.contains(rm))
			return;

		trans.rmSet.add(rm);
		txns.put(xid, trans);
		DiskManager.writeLog(name, txns);
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
				// System.out.println(Integer.toString(xid) + " interrupted.");
				Thread.currentThread().interrupt();
				return;
			}
			try {
				System.out.println(Integer.toString(xid) + " timeout, aborting...");
				abort(this.xid);
			} catch (InvalidTransactionException e) {
				System.out.println(Integer.toString(xid) + " abort invalid transaction.");
			} catch (RemoteException e) {
				System.out.println(Integer.toString(xid) + " abort remote exception.");
			}
		}
	}

	public void initTimer(int xid) {
		Thread cur = new Thread(new TimeOutThread(xid));
		cur.start();
		timeTable.put(xid, cur);
	}

	public synchronized void resetTimer(int xid) throws InvalidTransactionException, TransactionAbortedException {
		if (timeTable.get(xid) != null) {
			killTimer(xid);
			initTimer(xid);
		} else {
			if (abortedTXN.contains(xid)) {
				throw new TransactionAbortedException(xid);
			}
			throw new InvalidTransactionException(xid);
		}
	}

	public void killTimer(int id) {
		Thread cur = timeTable.get(id);
		if (cur != null) {
			cur.interrupt();
			// System.out.println(Integer.toString(id) + " interrupted timer...");
		}
	}

	public void removeTxn(int xid) {
		timeTable.remove(xid);
	}

	public boolean shutdown() throws RemoteException {
		Iterator it = timeTable.entrySet().iterator();
		while (it.hasNext()) {
			HashMap.Entry pair = (ConcurrentHashMap.Entry) it.next();
			try {
				abort((int) pair.getKey());
			} catch (InvalidTransactionException e) {
				continue;
			}
		}
		for (IResourceManager rm : stubs.values())
			rm.shutdown();
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
}