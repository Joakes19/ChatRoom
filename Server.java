/** Client.Java - A client program used to connect to a server program.
* @author James Oakes
* @version 1.0
*/
/** Used for input and output streams
*/
import java.io.*;
/** Used for access to socket and address functions
*/
import java.net.*;
/** Used for hashsets 
*/
import java.util.*;
/** Used for math functions when working out time
*/
import java.text.*;
/** The primary point of connection to clients.
* Has methods for handling clients connecting and has details for help with commands (such as the server start time etc.)
*/
public class Server {
	private int portNumber = 4200;	//Port number of the server.
	private String welcomeMsg = "Please type your username.";	//The server's welcome message.
	private String acceptedMsg = "Username accepted";			//The server's username acceptance message.
	private String noName =  "You have not entered a username!";	//The server's null username response.
	private String nameUnavailable = "Sorry, this username is unavailable";	//The server's duplicate username response.
	private static long serverStartTime;	//Server start time in milliseconds
	private static String address;	//Servers IP Address
	private ServerSocket ss;		//Server socket to allow for client connections.
	private String[] commands = {"!help", "!uptime", "!jointime", "!address", "!online", "!quit"};	//Array of available commands.
	private String[] cHelp = {"Shows command list", "Shows server uptime", "Shows the time elapsed since joining the serer" , 
	"Shows the server's IP Address", "Shows who's online", "Leave the Server"};	//Array of help comments for each command respectively.
	private HashSet<String> clientNames = new HashSet<String>();	//Set of client names for currently connected clients.
	private HashSet<PrintWriter> clientWriters = new HashSet<PrintWriter>();	//Set of writers for currently connected clients.
	/** Starts a new instance of the Server and executes the start function.
	*@param args	Arguements passed in when the program is launched. Not used here
	*@throws IOException	Used for if there is an io exception in the socket setup / shutdownn or if there is
	*an exception during communication via the input and output streams of the server and client.
	*/
	public static void main (String[] args) throws IOException {	
		Server server = new Server(); 
		server.start();
	}
	/** Creates the server socket and prints out the details about the address of the server to the servers system
	* output. It then waits for a connection. When a connection is found it sets it to a socket and creates a new
	* thread of newClientSession to deal with the connectionn from then on. All of this is done in a try block to
	* check for IO errors during the connection.
	@throws IOException		Used for possible io exception during socket setup. Is thrown to main.
	*/
	void start() throws IOException {	
		serverStartTime = System.currentTimeMillis();
		ss = new ServerSocket( portNumber );
		System.out.println( "Echo server at "
			+ InetAddress.getLocalHost()+ " is waiting for connections ...\nPress Ctrl+C or type !shutdown to close the server." );
		InetAddress temp = InetAddress.getLocalHost();
		address = temp.toString();
		Socket socket;
		Thread thread;
		Thread systemMsg;
		try {
			while ( true ) {
				systemMsg = new Thread(new sysFunc());
				systemMsg.start();
				socket = ss.accept();
				thread = new Thread( new newClientSession( socket ) );
				
				thread.start();
			}
		} 
		catch (Exception e)  {
			System.out.println( e.getMessage() );
		}
		finally {
			shutDown(); 
		}
	}	
	/** Shuts down the server. it does this in a try block to catch any errors.
	*/
	public void shutDown() {
		try { 
			ss.close(); 
			System.out.println( "The server is shut down." );	
		} 
		catch (Exception e) {
			System.err.println( "Problem shutting down the server." );
			System.err.println( e.getMessage() );
		}
	}
	/** Broadcasts the message given to it via {@link newClientSession#listenForClientMsg()}.
	* It uses the hashset of all the writers connected to the server to print the message given to all of the
	* output streams that are connected.
	* @param message 	The message given to priint out to the streams.
	* @param sender		The writer of the person sending the message. Matches with the writer in the hash set
	* and makes it so that the client sending the message does not see their own message read back to them.
	*/
	private synchronized void broadcastMsg (String message, PrintWriter sender) {
		for (PrintWriter writer : clientWriters) {
			if(writer != sender){
				writer.println( message ); writer.flush();
			}
		}
		System.out.println( message );
	}	
	/** Getter for the server start time. This is initialised on server start up and given in Milliseconds.
	@return The start time of the server in Miliseconds.
	*/
	public static long getStartTime(){
		return serverStartTime;
	}
	/** Getter for the address of the server.
	@return The IP address of the server as a string.
	*/
	static String getAddress(){
		return address;
	}
	/** Checks for input from the user serverside. Checks if the user has entered the shutdown command and if so,
	* shuts down the server.
	*/
	class sysFunc implements Runnable {
		/** Creates a reader for user input and checks the input continuously for commands.
		*/
		public void run() {
			String message = null;
			while(true){
				try {
					BufferedReader input = new BufferedReader(
						new InputStreamReader(System.in));
					message = input.readLine();
					if(message.equals("!shutdown")) System.exit(0);
				}
				catch (IOException e) {
					System.err.println("Exception in getClientInput(): " + e + "\nShutting Down...");
					System.exit(0);
				}
			}
		}
	}
	/** Handles the client connection when messages are sent and recieved. It implements {@link Runnable} to
	* allow for multiple instances and thus, multiple clients.
	*/
	class newClientSession implements Runnable {
		private Socket socket;
		private String clientName;
		BufferedReader in = null;
		PrintWriter out = null;
		long clientConnectTime = System.currentTimeMillis();
		/** Makes the socket created in this class equal to the socket given.
		@param socket	The socket pairing the client to the server given by The servers start function.
		*/
		newClientSession (Socket socket) {
			this.socket = socket;
		}
		/** Calls all of the methods involved in setup and recieving messages
		* in the correct order. When all methods are complete it uses the Servers clientDisconnect method to
		* shutdown the server.
		*/
		public void run() {
			try {
				createSessionStreams();
				getClientName();
				listenForClientMsg();
			} 
			catch (IOException e) {
				
			}
			finally {
				clientDisconnect();
			}
		}
		/** Creates the streams for the connection instance. And prints to the server the status of the connection.
		* Catches IO Exceptions.
		*/
		private void createSessionStreams() {
			try {
				in = new BufferedReader( new
				InputStreamReader( socket.getInputStream() ) );
				out = new PrintWriter( new
				OutputStreamWriter( socket.getOutputStream() ) );
				clientWriters.add( out );
				System.out.println( "Someone is connecting..." );
			}
			catch (IOException e) {
				System.err.println( "Exception in createSessionStreams(): " + e );
			}		
		}
		/** Sets up the client username. It contains all of the validation for the username: checking
		* if a username was typed and checking it against the names currentlty connected to the server to see if there are any
		* duplicates.
		*/
		private synchronized void getClientName() {
			out.println( welcomeMsg ); out.flush();
			while(clientName == null){
				try { clientName = in.readLine(); }
				catch (IOException e) { System.err.println(e); }
				if ( clientName == null || clientName.isEmpty() ) {
					out.println(noName); out.flush();
					clientName = null;
				}
				else if ( ! clientNames.contains( clientName ) ) {
					clientNames.add( clientName );
					break;
				} 
				else{
					out.println(nameUnavailable); out.flush();	
					clientName = null;
				}
			}
			out.println( acceptedMsg + ", You have joined the chat.\nType messages or type !help for a command list" ); 
			out.flush();
			System.out.println( clientName + " has entered the chat.");		
		}
		/** Obtains messages from the client. It checks if the user typed a command or a message.
		* then depending on the text it proccesses the command request or simply outputs the message using
		* the servers broadcastMsg method.
		* When calling broadcastMsg, we send the PrinterWriter of this client so it will match in that method
		* meaning the same message the user typed wont be sent back to them.
		* @throws IOException	Used for possible io exception between client and server input streams. Thrown to {@link #run()}
		*/
		private void listenForClientMsg() throws IOException {
			String message;
			while ( in != null ) { 
				message = in.readLine();
				if ( (message == null) || (message.equals("!quit") ) ) break;
				if ( message == null ) break;
				if ( message.startsWith("!") ) {
					if ( ! proccessCommand( message ) ) return;
				}
				else broadcastMsg( clientName + ": " + message, this.out);					
			}
		}
		/** Proccesses commands given by the client. It checks which command the user typed
		* and carries out the appropriate action.
		* @param command	A command given by the user that corresponds to one of the server commands.
		* @return	Helps to tell if a command was carried out or not. If a command was carried out, it returns true
		* and no message is printed out in {@link #listenForClientMsg()} (as you don't want others to see a command input).
		*/
		boolean proccessCommand (String command) {
			if ( command.equals("!quit") ) return false;
			if ( command.equals("!help") ) {
				for (int i = 0; i < commands.length; i++) {
					out.println(commands[i] + " :- " + cHelp[i] ); out.flush();
				}
			}
			else if(command.equals("!uptime")){
				out.println( getServerUpTime() ); out.flush();
			}
			else if(command.equals("!jointime")){
				out.println( getClientUpTime() ); out.flush();
			}
			else if(command.equals("!address")){
				out.println( "The server address is: " + Server.getAddress() ); out.flush();
			}
			else if(command.equals("!online")){
				getClientNames();
			}
			else return false;
			return true;
		}
		/** Closes the connection between the client and the server. It removes the clients name and the
		* printer writer ascociated with the client from their respective hash sets and outputs to all of the other
		* clients that the client in question has disconnected.
		* We send a null PrinterWriter to the method because we want everyone one to know that someone has
		* disconnected and a null writer wont match, therefore nobody is prevented from seeing the message.
		*/
		private void clientDisconnect() {
			PrintWriter empty = null;
			if ( clientName != null ) {
				broadcastMsg( clientName + " has left the chat.", empty);
				clientNames.remove( clientName );
			}
			if ( out != null ) {
				clientWriters.remove( out );
			}
		}
		/** Calculates the servers up time in minutes and seconds. Uses the current time and the server
		* start time and devides by 60 to obtain minutes. It also uses the mod function for seconds.
		* return the server up time in minutes and seconds as a string.
		*@return The server uptime given as a string to be printed out on the screen of the client.
		*/
		String getServerUpTime(){
			long currentTime = System.currentTimeMillis();
			long serverStart = Server.getStartTime();
			long totalUpTimeMins = ((currentTime - serverStart) /1000 ) / 60;
			long totalUpTimeSecs = ((currentTime - serverStart) /1000 ) % 60;
			return "The total uptime of the Server is: " + totalUpTimeMins + " Minutes and " + totalUpTimeSecs + " Seconds";
		}
		/** Calculates the time since connecting for the clients in minutes and seconds. 
		* Uses the current time and the time when the client connected
		* it then devides by 60 to obtain minutes. It also uses the mod function for seconds.
		* @return The time since the client joined in minutes and seconds as a string to be printed to the screen of the client.
		*/
		String getClientUpTime(){
			long currentTime = System.currentTimeMillis();
			long totalUpTimeMins = ((currentTime - clientConnectTime) /1000 ) / 60;
			long totalUpTimeSecs = ((currentTime - clientConnectTime) /1000 ) % 60;
			return "You have been connected for: " + totalUpTimeMins + " Minutes and " + totalUpTimeSecs + " Seconds";
		}
		/** Prints out the names of all the connected clients in the hash set to client who requested them.
		*/
		void getClientNames(){
			out.println("Connected Clients: "); out.flush();
			for (String name : clientNames){
				if (name.equals(clientName)) {
					out.println(name + " (You)"); out.flush();
				}
				else out.println(name); out.flush();
			}
			out.flush();
			
		}
	} // newClientSession ends here
}	//Server ends here
