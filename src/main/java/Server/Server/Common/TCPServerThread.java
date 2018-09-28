package main.java.Server.Server.Common;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import main.java.Util.MessageDecoder;
import main.java.Util.MessageDecoder.FlightMessageDecoder;

public class TCPServerThread implements Runnable{
	private Socket mw_socket;
	private String s_serverName;
	private ResourceManager rm;
	private ArrayList<Integer> customerIdx = new ArrayList<Integer>();

	public TCPServerThread(Socket mw_socket, String s_serverName, ResourceManager rm){
		this.mw_socket = mw_socket;
		this.s_serverName = s_serverName;
		this.rm = rm;
	}


	@Override
	public void run(){

		BufferedReader fromMW = null;
		PrintWriter toMW = null;
    try {
      fromMW = new BufferedReader(new InputStreamReader(mw_socket.getInputStream()));
      toMW = new PrintWriter(mw_socket.getOutputStream(),true);
    } catch (IOException e1) {
      e1.printStackTrace();
    }
		MessageDecoder decoder = new MessageDecoder();

		String msg = null;
		// listen to middleware 
      try {
        while ((msg = fromMW.readLine())!=null){
        	String res = "";
        	
        	try{
        		// the actual shit happening here: parse the message, call RM method, write back to middleware
        		String command = decoder.getCommand(msg);
        		String content = decoder.getContent(msg);
        		FlightMessageDecoder itemDecoder = null;
        		switch (command)
        		{
        			case "addFlight":
        			  itemDecoder = (FlightMessageDecoder) decoder;
        				itemDecoder.decodeAddMsg(content);
        				//FIXME: On the same server, ResourceManager should be synchronized !
        				res = Boolean.toString(rm.addFlight(itemDecoder.xid,itemDecoder.flightNum, itemDecoder.flightSeats, itemDecoder.flightPrice));


        			case "deleteFlight":
        				itemDecoder = (FlightMessageDecoder) decoder;
        				itemDecoder.decodeDelMsg(content);
        				res = Boolean.toString(rm.deleteFlight(itemDecoder.xid,itemDecoder.flightNum));

        			case "queryFlight":
        				itemDecoder = (FlightMessageDecoder) decoder;
        				itemDecoder.decodeQueryMsg(content);
        				res = Boolean.toString(rm.deleteFlight(itemDecoder.xid,itemDecoder.flightNum));

        			case "reserveFlight":
        				itemDecoder = (FlightMessageDecoder) decoder;
        				itemDecoder.decodeReserveMsg(content);
        				res = Boolean.toString(rm.reserveFlight(itemDecoder.xid,itemDecoder.customerID, itemDecoder.flightNum));

        			// TODO: other cases for add/delete/query/reserve car/room
        			

        			// if command doesn't match any of the above
        			default:
        				res = "<IllegalArgumentException>";
        		}

        	}
        	catch(IOException e){
        		Trace.error("Server "+s_serverName+" get IOException");
        		res = "<IOException>";
        	}
        	catch( IllegalArgumentException e){
        		Trace.error("Server "+s_serverName+" get IllegalArgumentException");
        		res = "IllegalArgumentException";
        	}


        	toMW.println(res);
        	toMW.flush();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    
	}
}