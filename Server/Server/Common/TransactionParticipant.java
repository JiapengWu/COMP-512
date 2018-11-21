package Server.Common;

public class TransactionParticipant extends Transaction{

	public RMHashMap xCopies = new RMHashMap();
	public RMHashMap xWrites = new RMHashMap();
	public RMHashMap xDeletes = new RMHashMap();
	public int votedYes = 0;
	public int commited = 0;
	public TransactionParticipant(int xid, RMHashMap copies) {
		super(xid);
		this.xCopies = copies;
	}
}
