Middleware (ie, coordinator):
	+ 3 RMs (stubs)
	+ TransactionManager TM
	+ CustomerIdx List
	+ map: <Operation,which RM>
	- commit(xid): TM.commit(xid)
	

TransactionManager/Coordinator:
	+ timetable <xid, Thread>: check if client is still alive
	+ set(xid): aborted transactions
	+ TxnCounter (Atomic)
	+ map: <xid, Transactions(C)>
	- commit(xid): 
		1) log "start commit"+xid[RMs involved]
		2) RM.voteReply(xid) for all RM in map[xid].RMs. get responses  from all RMs in list. If remoteException, decision=abort
		3) if all(list)==Yes, decision=commit. 
		4) sendDecision(decision) to all RM in map[xid]
		5) return true/false
	- abort(xid):
		1) log "start abort"
		2) sendDecision(abort) for all RMs
		3) log "aborted"
		4) return
	- prepare(): 
		0) (<xid, Transactions(C)>) map=DM.restore()
		1) started = set of xid that has "start2PC" in log
		2) decided = set of xid that has "commit/abort" decision in log
		3) resendSet = started\decided. ie, started2PC, but crashed before all participants reply, so abort all
		4) sendDecision(xid, decision) for xid in decided
		5) sendDecision(xid, abort) for all xid in resendSet
		6) sendDevision(xid, abort) for all transactions that haven't started 2PC
 	- sendDecision(commit/abort, xid):
		1) Log decision+ xid
		2) send commit/abort decision to all RMs in map[xid].RMs


ResourceManager/participant:
	+ DiskManager DM
	+ map <xid, Transactions(P)>
	+ LockManager LM
	+ RMHashtable m_data: the final stable version of data
	- voteReply(xid): 
		// when to vote no?
		return true/false
	- commit(xid):
		write "commit"+xid into log
		map[xid].commited=1
		applyCommit(xid)
		map.remove(xid)
	- abort(xid):
		map[xid].commited=-1 
		write "abort"+xid
		map.remove(xid)
	- prepare():
		1) (<xid, Transactions(P)>) map=DM.restore()
		2) if votedYes == 1
			if commited == 0: (ask around) wait
			elif: commited == 1: commit
			else: abort
		   else: abort
	- applyWrites(xid, writes, delets)
	- read/write data 


Transactions (P):
	+ votedYes: int (0: not set, 1: voted Yes, -1: voted No)
	+ commited: int (0: not set, 1: commited, -1: aborted)
	+ set of [RMHashtable], ie, after image
	+ xid


Transactions (C):
	+ started: int (0: not set, 1: started 2PC)
	+ xid
	+ decision: int (0: no decision, 1: everyone commit, -1: everyone abort)
	+ set of RMs involved: RMSet -- 1: flight, 2: car, 3:room


DiskManager:
	- restore( class ): <xid, Transaction>
		check if there's any log.
		if class==1: 
			read log file, get a map of <xid, Transaction(P)>.
		else  get a map of <xid, Transaction(C)>
		return this map

	- writeWAL(xid, Object(Transaction), RMName):

		update Transaction(?) with xid in hashmap format "RMName.json"
		name is hostname+xid. Write RMHashtable to disk
	- deleteWAL(name):
		delete WAL with xid entry if there exist one
	- commitAndDeleteWAL(xid):
		get WAL, apply changes to disk, delete the WAL.
		1) data = restore(xid)
		2) apply changes to actual data
		3) write "commited" to log
		4) deleteWAL
	- checkUponVote(xid):
		// write "yes/no"+xid to DecisionLog
