package Client;

import java.util.Vector;


import LockManager.DeadlockException;


public class TestClient
{
	
	static float LOAD = 20; // transactions per second
	static int PERIOD = (int) (1 / LOAD * 1000); // transaction period in milliseconds
	static int ROUNDS = 10; // number of transactions to test
	
	private static String s_serverHost = "localhost";
	private static int s_serverPort = 3099;
	private static String s_serverName = "Server";

	RMIClient client;



	public static void main(String args[])
	{
		client = new RMIClient();
		client.connect_server(s_serverHost, s_serverPort, s_serverName);
		// load all commands
		Vector<Command> commands = new Vector<Command>();
		FileInputStream fstream = new FileInputStream("textfile.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		String strLine;
		while ((strLine = br.readLine()) != null)   {
			commands.add(Command.fromString(strLine));
		}
		br.close();

		// execute transactions in loop
		for (int i=0; i<ROUNDS;i++){
			Random rand = new Random();
			long waitTime = (long) rand.nextInt(period);
      		try {
        		Thread.sleep(waitTime * 2);
      		} catch (InterruptedException e) {
        		e.printStackTrace();
      		}
      
      		long start = System.nanoTime() / 1000;
      		try {
        		//TODO: execute commands
      			executeCmds(commands);
        		long duration = System.nanoTime() / 1000 - start;
        		System.out.println(duration);
			} catch (Exception e) {
				System.out.println("Test failed");
				e.printStackTrace();
			}
    
		}

		// TODO
		public static void executeCmds(Vector<Command> commands)
		{

		}


	}

 }