package Server.RMI;

import Server.Interface.*;
import Server.Common.*;

import java.util.*;
import Util.*;
import java.net.Socket;
import java.io.*;

public class TCPResourceManager extends ResourceManager 
{
	private static String s_serverName = "Server";
	private static String mw_host = "localhost";
	private static int mw_port = 1099;


	public static void main(String args[])
	{
		if (args.length > 0)
		{
			s_serverName = args[0];
		}

		if (args.length > 1)
		{
			mw_port = args[1];
		}
			
		// setup server
		TCPResourceManager server = new TCPResourceManager(s_serverName);

		// setup connection with middleware
		Socket mw_socket = new Socket(mw_host, mw_port);
		BufferedReader fromMW = new BufferedReader(new InputStreamReader(mw_socket.getInputStream()));
		PrintWriter toMW = new PrintWriter(mw_socket.getOutputStream(),true);

		String msg = null;
		// listen to middleware
		while ((msg = fromMW.readLine())!=null){
			String res = "";
			
			try{
				// TODO the actual shit
				// parse the message, call RM

			}
			catch(IOException e){
				Trace.error("Server "+server_host+" get IOException");
				res = "<IOException>";
			}
			catch( IllegalArgumentException e){
				Trace.error("Server "+server_host+" get IllegalArgumentException");
				res = "IllegalArgumentException";
			}


			toMW.println(res);
			toMW.flush();
		}
	
			
	}


	public TCPResourceManager(String name)
	{
		super(name);
	}
}
