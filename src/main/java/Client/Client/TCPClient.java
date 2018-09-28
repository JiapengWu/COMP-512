package main.java.Client.Client;

public class TCPClient extends Client{

	static int mw_port = 1099; //middleware port
	static String s_serverHost = "localhost"; // middleware hostname

	public TCPClient(){
		super();

		}

	public static void main(String[] args){
		if (args.length > 0)
		{
			s_serverHost = args[0];
		}

		if (args.length >1)
		{
			mw_port = Integer.parseInt(args[1]);
		}
		
		if (args.length > 2)
		{
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java client.RMIClient [server_hostname [server_port]]");
			System.exit(1);
		}
		try {
			TCPClient client = new TCPClient();
			client.connectServer();
			client.start();
		} 
		catch (Exception e) {    
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

	}

	public void connectServer(){
		this.connectServer(s_serverHost,mw_port);
	}

	public void connectServer(String s_serverHost, int mw_port){
		m_resourceManager = new TCPClientHandler(s_serverHost,mw_port);
	}

	
}