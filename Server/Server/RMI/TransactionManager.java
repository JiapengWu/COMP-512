package Server.RMI;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import Server.Common.DiskManager;
import Server.Common.InvalidTransactionException;
import Server.Common.TransactionAbortedException;
import Server.Common.TransactionCoordinator;
import Server.Interface.IResourceManager;

public class TransactionManager{
	public static final int TIMEOUT_IN_SEC = 60;
	private String name = "MiddleWare"; 
	private int txnIdCounter;
	private HashSet<Integer> abortedTXN;
	private ConcurrentHashMap<Integer, Thread> timeTable;
	public HashMap<Integer, IResourceManager> stubs; // {1: flightRM, 2: roomRM, 3: carRM}
	protected HashMap<Integer, TransactionCoordinator> txns;

	/* crash mode: 0 - unset
	1. Crash before sending vote request
	2. Crash after sending vote request and before receiving any replies
	3. Crash after receiving some replies but not all
	4. Crash after receiving all replies but before deciding
	5. Crash after deciding but before sending decision
	6. Crash after sending some but not all decisions
	7. Crash after having sent all decisions
	8. Recovery of the coordinator (if you have decided to implement coordinator recovery)
	*/
	protected int crashMode = 0; 

	public TransactionManager(){
		stubs = new HashMap<Integer,IResourceManager>();
		txnIdCounter = 0;
		abortedTXN = new HashSet<Integer>();
		timeTable = new ConcurrentHashMap<Integer, Thread>();
		txns = new HashMap<Integer, TransactionCoordinator>();
	}


	// return a TM for middleware to use later
	@SuppressWarnings("unchecked")
	public TransactionManager restore() throws RemoteException, InvalidTransactionException, TransactionAbortedException{
		// DM need to read logs about all transactions
		HashMap<Integer, TransactionCoordinator> old_txns = null;
		try{
			old_txns = (HashMap<Integer, TransactionCoordinator>) DiskManager.readLog(name);
		}
		// if no prior TM log exist, just create a new one and return
		catch (IOException e){
			return new TransactionManager();
		}
		// else process ongoing transactions before crash
		for (TransactionCoordinator trans: old_txns.values()){
			// for transactions that started 2PC:
			if (trans.started == 1){
				// already made decision: resend decision to all participants
				if(trans.decision==1){
					sendDecision(trans,true);
				}
				// haven't made decision: abort
				else{
					trans.decision=-1;
					old_txns.put(trans.xid, trans);
					sendDecision(trans,false);
				}
			}
			else{
				// for transactions that haven't started 2PC: abort
				trans.started =1;
				trans.decision=-1;
				old_txns.put(trans.xid, trans);
				sendDecision(trans,false);
			}
		}
		DiskManager.writeLog("Coordinator", name, old_txns);
		TransactionManager tm = new TransactionManager();
		tm.txns = old_txns;

		return tm;
	}


	// client abort, no need to vote
	public void abort(int txnID) throws RemoteException, InvalidTransactionException {
		if (timeTable.get(txnID) == null)
			throw new InvalidTransactionException(txnID);
		TransactionCoordinator trans = txns.get(txnID);
		if (trans==null) throw new InvalidTransactionException(txnID);
	
		try {
			sendDecision(trans, false);
		} catch (TransactionAbortedException e) {
			;
		}
		synchronized (abortedTXN) {
			abortedTXN.add(txnID);
		}
	}


	public void commit(int txnId) 
		throws RemoteException, InvalidTransactionException, TransactionAbortedException{
		TransactionCoordinator trans = txns.get(txnId);
		synchronized (abortedTXN) {
			if (abortedTXN.contains(txnId))
				throw new TransactionAbortedException(txnId);
		}
		if (trans==null) throw new InvalidTransactionException(txnId);

		prepare(trans);

		if (crashMode ==1) System.exit(1);

		// TODO: add timeout to all RMs
		// get votes from participants
		boolean decision = true;
		for (Integer rmIdx: trans.rmSet) {
			decision &= stubs.get(rmIdx).voteReply(txnId); // if any stub vote no, decision will be 0
			if (crashMode ==3) System.exit(1);
		}
		if (crashMode ==4) System.exit(1);
		// write decision to log
		trans.decision = (decision==true)? 1:-1;
		txns.put(txnId, trans);
		DiskManager.writeLog("Coordinator", name, txns);
		if (crashMode ==5) System.exit(1);
		// send decision to all participants
		sendDecision(trans, decision);
		if (crashMode ==7) System.exit(1);

	}

	
	public void prepare(TransactionCoordinator trans){
		// prepare for 2PC
		trans.started = 1;
		txns.put(trans.xid, trans);
		// write "start2PC" to logs
		DiskManager.writeLog("Coordinator", name, txns);
	}


	/* send commit or abort decision to all participants. 
	@param decision: 1 -- commit, 0 -- abort
	*/
	public void sendDecision(TransactionCoordinator trans, boolean decision) 
		throws RemoteException, InvalidTransactionException, TransactionAbortedException{
		
		killTimer(trans.xid);
		for (Integer rmIdx: trans.rmSet) {
			if (decision) stubs.get(rmIdx).commit(trans.xid);
			else stubs.get(rmIdx).abort(trans.xid);

			// crash after sending some but not all decisions
			if (crashMode ==6) System.exit(1);
		}
		removeTxn(trans.xid);
		
	}


	public synchronized int start() throws RemoteException{
		int xid = txnIdCounter;
		start(txnIdCounter);
		txnIdCounter +=1;
		return xid;
	}

	public void start(int txnId) throws RemoteException{
		initTimer(txnId);
		TransactionCoordinator trans = txns.get(txnId);
		for (Integer rmIdx: trans.rmSet) stubs.get(rmIdx).start(txnId);
	}


	// update the RM stub invovled with this transaction. 
	// @param: rm: 1: flight, 2: car, 3:room
	public void updateRMSet(int xid, int rm){
		TransactionCoordinator trans = txns.get(xid);
		trans.rmSet.add(rm);
		txns.put(xid, trans);
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
    for (IResourceManager rm: stubs.values()) rm.shutdown();
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