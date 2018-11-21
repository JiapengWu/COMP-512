package Server.Common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Hashtable;

public class DiskManager {
	@SuppressWarnings("unchecked")
	public static Hashtable<Integer, ? extends Transaction> readLog(String RMName) throws FileNotFoundException, IOException{
		Hashtable<Integer, Transaction> result = new Hashtable<Integer, Transaction>();
		try (
			  InputStream file = new FileInputStream(String.format("%s.ser", RMName));
		      InputStream buffer = new BufferedInputStream(file);
		      ObjectInput input = new ObjectInputStream (buffer);
		    ){
			 result = (Hashtable<Integer,Transaction>) input.readObject();;
		    }  
		    catch(ClassNotFoundException ex){
		    	ex.printStackTrace();
		    }
		return result;
	}
	
//	name is hostname+xid. Write RMHashtable to disk
	public static void writeLog(String RMName, Hashtable<Integer, ? extends Transaction> map) {
		try (
	      OutputStream file = new FileOutputStream(String.format("%s.ser", RMName));
	      OutputStream buffer = new BufferedOutputStream(file);
	      ObjectOutput output = new ObjectOutputStream(buffer);
	    ){
	      output.writeObject(map);
	    }  
	    catch(IOException ex){
	    	ex.printStackTrace();
	    }
	}
	
	@SuppressWarnings("unchecked")
	public static HashSet<Integer> readAliveTransactions() throws FileNotFoundException, IOException {
		HashSet<Integer> result = null;
		try (
				InputStream file = new FileInputStream(String.format("active_transaction.ser"));
			      InputStream buffer = new BufferedInputStream(file);
			      ObjectInput input = new ObjectInputStream (buffer);
	    ){
			 result = (HashSet<Integer>) input.readObject();;
	    }
	    catch(ClassNotFoundException ex){
	    	ex.printStackTrace();
	    }
		return result;
	}
	
	public static void writeAliveTransactions(HashSet<Integer> activeSet) {
		try (
	      OutputStream file = new FileOutputStream(String.format("active_transaction.ser"));
	      OutputStream buffer = new BufferedOutputStream(file);
	      ObjectOutput output = new ObjectOutputStream(buffer);
	    ){
	      output.writeObject(activeSet);
	    }  
	    catch(IOException ex){
	    	ex.printStackTrace();
	    }
	}
	
	public static RMHashMap readMData(String RMName) throws FileNotFoundException, IOException {
		RMHashMap result = null;
		try (
				InputStream file = new FileInputStream(String.format("%s_m_data.ser", RMName));
			      InputStream buffer = new BufferedInputStream(file);
			      ObjectInput input = new ObjectInputStream (buffer);
	    ){
			 result = (RMHashMap) input.readObject();;
	    }
	    catch(ClassNotFoundException ex){
	    	ex.printStackTrace();
	    }
		return result;
	}
	
	public static void writeMData(String RMName, RMHashMap m_data) {
		try (
	      OutputStream file = new FileOutputStream(String.format("%s_m_data.ser", RMName));
	      OutputStream buffer = new BufferedOutputStream(file);
	      ObjectOutput output = new ObjectOutputStream(buffer);
	    ){
	      output.writeObject(m_data);
	    }  
	    catch(IOException ex){
	    	ex.printStackTrace();
	    }
	}
	
	
	
	
	
	
	
	
	
	
}
