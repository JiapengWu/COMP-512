package Server.Common;

public class InvalidTransactionException extends Exception {
	public InvalidTransactionException(int txnID) {
		super("Transaction " + txnID + " is not a valid transaction");
	}
}