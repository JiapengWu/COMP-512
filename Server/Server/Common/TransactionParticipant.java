package Server.Common;

import java.io.Serializable;

public class TransactionParticipant extends Transaction implements Serializable {

	public RMHashMap xCopies = new RMHashMap();
	public RMHashMap xWrites = new RMHashMap();
	public RMHashMap xDeletes = new RMHashMap();
	public int votedYes = 0;
	public int commited = 0;
	public int EOT = 0;

	public TransactionParticipant(int xid, RMHashMap copies) {
		super(xid);
		this.xCopies = copies;
	}
}
