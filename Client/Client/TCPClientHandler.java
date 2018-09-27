package Client;

import Utils.Message;
import java.io.*;
import java.net.Socket;

public class TCPClientHandler{
	private int mw_port;
	private String mw_hostname;

	public TCPClientHandler(String mw_hostname, int mw_port){
		this.mw_hostname=mw_hostname;
		this.mw_port = mw_port;
	}

	public String sendRecvStr(Message msg) throws IOException, IllegalArgumentException{
		Socket socket = new Socket(mw_hostname, mw_port);
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);

		writer.println(msg.toString());
		writer.flush();
		String res = reader.readLine();
		if (res.equals("<IOException>")) throw new IOException();
		if (res.equals("<IllegalArgumentException>")) throw new IllegalArgumentException();
		return res;
	}


	public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) 
	throws IOException, IllegalArgumentException{
		Message msg = new Message("addFlight");
		msg.addFlightCommand(id,flightNum,flightSeats,flightPrice); // sets the content of the message
		return Boolean.parseBoolean(sendRecvStr(msg));
	}
    
    
    public boolean addCars(int id, String location, int numCars, int price) 
	throws IOException, IllegalArgumentException{
		Message msg = new Message("addCars");
		msg.addCommand(id, location, numCars, price); // sets the content of the message
		return Boolean.parseBoolean(sendRecvStr(msg));
	
	}
   
    
    public boolean addRooms(int id, String location, int numRooms, int price) 
	throws IOException, IllegalArgumentException{
		Message msg = new Message("addRooms");
		msg.addCommand(id, location, numRooms, price); // sets the content of the message
		return Boolean.parseBoolean(sendRecvStr(msg));
	}	    
			    
    
    public int newCustomer(int id) 
	throws IOException, IllegalArgumentException{
		Message msg = new Message("newCustomer");
		msg.addCustomerCommand(id); // sets the content of the message
		return Integer.parseInt(sendRecvStr(msg));
	}	    
    
    public boolean newCustomer(int id, int cid)
        throws RemoteException;

    
    public boolean deleteFlight(int id, int flightNum) 
	throws IOException, IllegalArgumentException{
		Message msg = new Message("deleteFlight");
		msg.deleteFlightCommand(id,flightNum);
		return Boolean.parseBoolean(sendRecvStr(msg));
	}
    
    	    
    public boolean deleteCars(int id, String location) 
	throws IOException, IllegalArgumentException{
		Message msg = new Message("deleteCars");
		msg.deleteCommand(id,location);
		return Boolean.parseBoolean(sendRecvStr(msg));
	}

    
    public boolean deleteRooms(int id, String location) 
	throws IOException, IllegalArgumentException{
		Message msg = new Message("deleteRooms");
		msg.deleteCommand(id,location);
		return Boolean.parseBoolean(sendRecvStr(msg));
	}
    
    public boolean deleteCustomer(int id, int customerID) 
	throws RemoteException; 

    
    public int queryFlight(int id, int flightNumber) 
	throws RemoteException; 

    
    public int queryCars(int id, String location) 
	throws RemoteException; 

    
    public int queryRooms(int id, String location) 
	throws RemoteException; 

   
    public String queryCustomerInfo(int id, int customerID) 
	throws RemoteException; 
    
    
    public int queryFlightPrice(int id, int flightNumber) 
	throws RemoteException; 

    
    public int queryCarsPrice(int id, String location) 
	throws RemoteException; 

    
    public int queryRoomsPrice(int id, String location) 
	throws RemoteException; 

    
    public boolean reserveFlight(int id, int customerID, int flightNumber) 
	throws RemoteException; 

    
    public boolean reserveCar(int id, int customerID, String location) 
	throws RemoteException; 

    
    public boolean reserveRoom(int id, int customerID, String location) 
	throws RemoteException; 

    
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)
	throws IOException, IllegalArgumentException{
		Message msg = new Message("bundle");
		msg.bundleCommand(id, customerID, flightNumbers, location, car, room);
		return Boolean.parseBoolean(sendRecvStr(msg));
	}

}