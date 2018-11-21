package Server.Common;

import java.io.Serializable;

public class Transaction implements Serializable{
	public int xid;
	
	public Transaction(int xid) {
		this.xid = xid;
	}
}
