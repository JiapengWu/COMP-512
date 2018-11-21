package Server.Common;

import java.util.HashSet;

public class TransactionCoordinator extends Transaction{

	public int started = 0;
	public int decision = 0;
	public HashSet<Integer> rmSet = new HashSet<Integer>();

	public TransactionCoordinator(int xid) {
		super(xid);
	}
}
