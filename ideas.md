Middleware (ie, coordinator):
	+ 3 RMs (stubs)
	+ TransactionManager TM
	+ CustomerIdx List
	+ map: <Operation,which RM>
	- commit(xid): TM.commit(xid)
	

TransactionManager/Coordinator:
	+ TxnCounter (Atomic)
	+ HealthManager HM
	+ map: <xid, set of RMs involved>
	- commit(xid): 
		1) log "start commit"+xid+[RMs involved]
		2) RM.voteReply(xid) for all RM in map[xid]. get responses from all RMs in list
		3) if all(list)==Yes: sendDecision(commit) for all RM in map[xid]
			else: sendDecision(abort) for all RMs
		4) log "commited/aborted"+ xid+[all RMs involved]
		5) return true/false
	- abort(xid):
		1) log "start abort"+xid+[RMs involved]
		2) sendDecision(abort) for all RMs
		3) log "aborted"+xid+[all RMs involved]
		4) return
	- restoreDecision(): {required?}
		1) Commits = set of xid that has "commit" in log
		2) finished = set of xid that has "commited/aborted" in log
		3) resendSet = Commits\finished
		4) sendDecision(xid) for xid in resendSet
	- sendDecision(commit/abort,RMset):
		send commit/abort decision to all RMs in RMset


HealthManager:`
	+ map <xid, Thread>: check if client is still alive
	+ set(xid): aborted transactions
	>> need to check if RMs are still connected?


ResourceManager/Coordinator:
	+ DiskManager DM
	+ LockManager LM
	+ RMHashtable m_data: the final stable version of data
	+ Image logs: store after images, writes:<xid, RMHashtable> & deletes:<xid, RMHashtable>
	+ Decision logs: store "yes/no" (after vote before commit) + "commited/aborted" (finished transactions)
	+ Map <xid, RMHashtable>
	- voteReq(xid): 
	- commit(xid):
	- start():
		1) DM.restore()
		2) >> send reply again for all logged xids?
	- applyWrites(xid, writes, delets)
	- read/write data 


DiskManager:
	- restore(xid): RMHashtable
		check if there's halfway data for xid
	- writeWAL(name, RMHashtable):
		name is hostname+xid. Write RMHashtable to disk
	- deleteWAL(name):
		delete WAL if there exist one
	- commitAndDeleteWAL(xid):
		get WAL, apply changes to disk, delete the WAL.
		1) data = restore(xid)
		2) apply changes to actual data
		3) write "commited" to log
		4) deleteWAL
	- checkUponVote(xid):
		// write "yes/no"+xid to DecisionLog
