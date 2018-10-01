package main.java.Client.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;

import org.json.JSONException;

import main.java.Util.Message;

public class TCPClientHandler{
	private int mw_port;
	private String mw_hostname;

	public TCPClientHandler(String mw_hostname, int mw_port){
		this.mw_hostname=mw_hostname;
		this.mw_port = mw_port;
	}

	public String sendRecvStr(Message msg) throws IOException, IllegalArgumentException{
		@SuppressWarnings("resource")
		Socket middleware_socket = new Socket(mw_hostname, mw_port);
		BufferedReader reader = new BufferedReader(new InputStreamReader(middleware_socket.getInputStream()));
		PrintWriter writer = new PrintWriter(middleware_socket.getOutputStream(),true);
		writer.println(msg.toString());
		writer.flush();
		String res = "";
		try {
			StringBuffer stringBuffer = new StringBuffer("");
			String line = null;
			while ((line = reader.readLine()) != null) {
				stringBuffer.append("\n");
			    stringBuffer.append(line);
			}
			res = stringBuffer.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (res.equals("<JSONException>")) throw new IOException(); // server throws JSON, so it's message transporting exception
		if (res.equals("<IllegalArgumentException>")) throw new IllegalArgumentException();
		return res;
	}


	public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
	throws IOException, IllegalArgumentException, JSONException {
		Message msg = new Message("addFlight");
		msg.addFlightCommand(id,flightNum,flightSeats,flightPrice); // sets the content of the message
		return Boolean.parseBoolean(sendRecvStr(msg));
	}
    
    
    public boolean addCars(int id, String location, int numCars, int price) 
	throws IOException, IllegalArgumentException, JSONException{
		Message msg = new Message("addCars");
		msg.addCommand(id, location, numCars, price); // sets the content of the message
		return Boolean.parseBoolean(sendRecvStr(msg));
	
	}
   
    
    public boolean addRooms(int id, String location, int numRooms, int price) 
	throws IOException, IllegalArgumentException, JSONException{
		Message msg = new Message("addRooms");
		msg.addCommand(id, location, numRooms, price); // sets the content of the message
		return Boolean.parseBoolean(sendRecvStr(msg));
    }
			    
    
    public int newCustomer(int id) throws IOException, IllegalArgumentException, JSONException{
		Message msg = new Message("newCustomer");
		msg.addCustomerCommand(id); // cid not provided case
		return Integer.parseInt(sendRecvStr(msg));
	}	    
    
    public boolean newCustomer(int id, int cid) throws IOException, IllegalArgumentException, JSONException{
		Message msg = new Message("newCustomerID");
		msg.addDeleteQueryCustomerCommand(id, cid); // sets the content of the message
		return Boolean.parseBoolean(sendRecvStr(msg));
	}	    

    
    public boolean deleteFlight(int id, int flightNum) 
	throws IOException, IllegalArgumentException, JSONException{
		Message msg = new Message("deleteFlight");
		msg.delOrQueryFlightCommand(id, flightNum);
		return Boolean.parseBoolean(sendRecvStr(msg));
	}
    
    	    
    public boolean deleteCars(int id, String location) 
	throws IOException, IllegalArgumentException, JSONException{
		Message msg = new Message("deleteCars");
		msg.delOrQueryCommand(id, location);
		return Boolean.parseBoolean(sendRecvStr(msg));
	}

    
    public boolean deleteRooms(int id, String location) 
	throws IOException, IllegalArgumentException, JSONException{
		Message msg = new Message("deleteRooms");
		msg.delOrQueryCommand(id,location);
		return Boolean.parseBoolean(sendRecvStr(msg));
	}
    
    public boolean deleteCustomer(int id, int customerID) 
	throws IOException, IllegalArgumentException, JSONException{
		Message msg = new Message("deleteCustomer");
		msg.addDeleteQueryCustomerCommand(id, customerID);
		return Boolean.parseBoolean(sendRecvStr(msg));
	}

    
    public int queryFlight(int id, int flightNumber) 
	throws IOException, IllegalArgumentException, JSONException{
    	Message msg = new Message("queryFlight");
		msg.delOrQueryFlightCommand(id, flightNumber);
		return Integer.parseInt(sendRecvStr(msg));
    }

    public int queryCars(int id, String location) 
	throws IOException, IllegalArgumentException, JSONException{
    	Message msg = new Message("queryCars");
		msg.delOrQueryCommand(id, location);
		return Integer.parseInt(sendRecvStr(msg));
    	
    }

    public int queryRooms(int id, String location) 
	throws IOException, IllegalArgumentException, JSONException{
    	Message msg = new Message("queryRooms");
		msg.delOrQueryCommand(id, location);
		return Integer.parseInt(sendRecvStr(msg));
    	
    }

    public String queryCustomerInfo(int id, int customerID) 
	throws IOException, IllegalArgumentException{
		Message msg = new Message("queryCustomerInfo");
		msg.addDeleteQueryCustomerCommand(id,customerID);
		return sendRecvStr(msg);
	}
    
    public int queryFlightPrice(int id, int flightNumber) 
    		throws IOException, IllegalArgumentException, JSONException{
    	Message msg = new Message("queryFlightPrice");
		msg.delOrQueryFlightCommand(id, flightNumber);
		return Integer.parseInt(sendRecvStr(msg));
    }

    public int queryCarsPrice(int id, String location) 
    		throws IOException, IllegalArgumentException, JSONException{
	    Message msg = new Message("queryCarsPrice");
		msg.delOrQueryCommand(id, location);
		return Integer.parseInt(sendRecvStr(msg));
    }
    
    public int queryRoomsPrice(int id, String location) 
    		throws IOException, IllegalArgumentException, JSONException{
    	Message msg = new Message("queryRoomsPrice");
		msg.delOrQueryCommand(id, location);
		return Integer.parseInt(sendRecvStr(msg));
    }

    
    public boolean reserveFlight(int id, int customerID, int flightNumber) 
	throws IOException, IllegalArgumentException{
		Message msg = new Message("reserveFlight");
		msg.reserveFlightCommand(id, customerID, flightNumber);
		return Boolean.parseBoolean(sendRecvStr(msg));
	}

    
    public boolean reserveCar(int id, int customerID, String location) 
	throws IOException, IllegalArgumentException{
		Message msg = new Message("reserveCar");
		msg.reserveCommand(id, customerID, location);
		return Boolean.parseBoolean(sendRecvStr(msg));
	}
    
    
    public boolean reserveRoom(int id, int customerID, String location) 
	throws IOException, IllegalArgumentException{
		Message msg = new Message("reserveRoom");
		msg.reserveCommand(id, customerID, location);
		return Boolean.parseBoolean(sendRecvStr(msg));
	}

    
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)
	throws IOException, IllegalArgumentException{
		Message msg = new Message("bundle");
		msg.bundleCommand(id, customerID, flightNumbers, location, car, room);
		return Boolean.parseBoolean(sendRecvStr(msg));
	}

}