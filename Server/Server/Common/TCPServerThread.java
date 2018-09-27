package Server.Common;


import java.util.*;
import java.io.*;
import java.net.Socket;

import Server.Common.*;
import Util.Message;
import Util.MessageDecoder;

public class TCPServerThread implements Runnable{
	private Socket mw_socket;
	private String s_serverName;
	private ResourceManager rm;

	public TCPServerThread(Socket mw_socket, String s_serverName, ResourceManager rm){
		this.mw_socket = mw_socket;
		this.s_serverName = s_serverName;
		this.rm = rm;
	}


	@Override
	public void run(){

		BufferedReader fromMW = new BufferedReader(new InputStreamReader(mw_socket.getInputStream()));
		PrintWriter toMW = new PrintWriter(mw_socket.getOutputStream(),true);
		MessageDecoder decoder = new MessageDecoder();

		String msg = null;
		// listen to middleware 
		while ((msg = fromMW.readLine())!=null){
			String res = "";
			
			try{
				// the actual shit happening here: parse the message, call RM method, write back to middleware
				String command = decoder.decode_Method(msg);
				switch (command)
				{
					case "addFlight":
						FlightMessageDecoder flightDecoder = (FlightMessageDecoder) decoder;
						flightDecoder.decodeAddMsg(message);
						//FIXME: On the same server, ResourceManager should be synchronized !
						res = Boolean.toString(rm.addFlight(flightDecoder.xid,flightDecoder.flightNum, flightDecoder.flightSeats, flightDecoder.flightPrice));


					case "deleteFlight":
						FlightMessageDecoder flightDecoder = (FlightMessageDecoder) decoder;
						flightDecoder.decodeDelMsg(message);
						res = Boolean.toString(rm.deleteFlight(flightDecoder.xid,flightDecoder.flightNum));

					case "queryFlight":
						FlightMessageDecoder flightDecoder = (FlightMessageDecoder) decoder;
						flightDecoder.decodeQueryMsg(message);
						res = Integer.toString(rm.deleteFlight(flightDecoder.xid,flightDecoder.flightNum));


					// TODO: other cases for add/delete/query

					// FIXME: "reserverXXX" and "CustomerXXX" should be handled a bit differently here, or handled in Middleware?
					
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
	}
}