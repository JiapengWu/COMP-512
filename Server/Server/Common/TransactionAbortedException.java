package Server.Common;

public class TransactionAbortedException extends Exception {
	public TransactionAbortedException(int txnID) {
		super("Transaction " + txnID + " is has been aborted");
	}
}