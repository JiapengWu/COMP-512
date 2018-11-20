package Server.RMI;

import Server.Common.TransactionCoordinator;
import Server.Common.DiskManager;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

public class TransactionManager{
	private String name = "MiddleWare"; 
	private int txnIdCounter;
	private HashSet<Integer> abortedTXN;
	private ConcurrentHashMap<Integer, Thread> timeTable;
	public HashMap< Integer,IResourceManager> stubs; // {1: flightRM, 2: roomRM, 3: carRM}
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
		HashMap< Integer,IResourceManager> = new HashMap<Integer,IResourceManager>();
		txnIdCounter = 0;
		abortedTXN = new HashSet<Integer>();
		timeTable = new ConcurrentHashMap<Integer, Thread>();
		txns = new HashMap<Integer, TransactionCoordinator>();
	}


	// return a TM for middleware to use later
	public TransactionManager restore(){
		// DM need to read logs about all transactions
		try{
			HashMap<Integer, TransactionCoordinator> old_txns = DiskManager.readLog(name);
		}
		// if no prior TM log exist, just create a new one and return
		catch (IOException e){
			return new TransactionManager();
		}
		// else process leftover transactions
		TransactionManager tm = new TransactionManager();
		tm.txns = old_txns;
		for (TransactionCoordinator trans: old_txns.values()){
			if (trans.started == 1){
				if(trans.committed==1){
					sendDecision(trans,1);
				}
				else{
					sendDecision(trans,0);
				}
			}
			else{
				sendDecision(trans,0);
			}
		}
		
		return tm;
	}


	// client abort, no need to vote
	public void abort(int txnID) throws RemoteException, InvalidTransactionException {
		if (timeTable.get(txnID) == null)
			throw new InvalidTransactionException(txnID);
		TransactionCoordinator trans = txns.get(txnId);
		if (trans==null) throw new InvalidTransactionException(txnID);
		killTimer(txnID);
		sendDecision(trans, decision);
		synchronized (abortedTXN) {
			abortedTXN.add(txnID);
		}
	}


	public void commit(int txnId) 
		throws RemoteException, InvalidTransactionException, TransactionAbortedException{
		// prepare for 2PC
		TransactionCoordinator trans = txns.get(txnId);
		synchronized (abortedTXN) {
			if (abortedTXN.contains(txnId))
				throw new TransactionAbortedException(txnId);
		}
		if (trans==null) throw new InvalidTransactionException(txnID);
		trans.started = 1;
		txns.put(txnId, trans);
		// write "start2PC" to logs
		DiskManager.writeLog("Coordinator", name, txns);
		if (crashMode ==1) System.exit(1);

		// TODO: add timeout to all RMs
		// get votes from participants
		int decision = 1;
		for (Integer rmIdx: trans.RMSet) decision &= stubs.get(rmIdx).voteReply(txnId); // if any stub vote no, decision will be 0

		// write decision to log
		trans.commited = (decision==1)? 1:-1;
		txns.put(txnId, trans);
		DiskManager.writeLog("Coordinator", name, txns);
		
		// send decision to all participants
		sendDecision(trans, decision);

	}

	

	// send commit or abort decision to all participants. Decision: 1 -- commit, 0 -- abort
	public void sendDecision(TransactionCoordinator trans, int decision) 
		throws RemoteException, InvalidTransactionException, TransactionAbortedException{
		
		killTimer(trans.xid);
		if (decision==1) {
			for (Integer rm: trans.RMSet) stubs.get(rmIdx).commit(txnId);
			removeTxn(trans.xid);
		}
		else{
			abort(trans.xid);
		}
		
	}


	public synchronized int start(){
		int xid = txnIdCounter;
		start(txnIdCounter);
		txnIdCounter +=1;
		return xid;
	}

	public void start(int txnId){
		initTimer(txnId);
		for (Integer rmIdx: trans.RMSet) stubs.get(rmIdx).start(txnId);
	}


	// update the RM stub invovled with this transaction. 
	// @param: rm: 1: flight, 2: car, 3:room
	public void updateRMSet(int xid, int rm){
		TransactionCoordinator trans = txns.get(txnId);
		trans.RMSet.add(rm);
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
			ConcurrentHashMap.Entry pair = (ConcurrentHashMap.Entry) it.next();
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