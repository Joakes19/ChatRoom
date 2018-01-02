/** Client.Java - A client program used to connect to a server program.
* @author James Oakes
* @version 1.0
*/
/** Used for input and output streams
*/
import java.io.*;
/** used for sockets
*/
import java.net.*;
/** Main class used for joining the server. It creates an instance of a client with a socket and 
* details of setup functions
*/
public class Client {
	
	/** Creates an instance of the client and runs the start function
	* @param args 	Not used.
	* @throws IOException	Used incase there is an io exception in the socket setup / when joining the server.
	*/
	public static void main(String[] args) throws Exception {
		ClientInstance client = new ClientInstance();
		client.startClient();
	}
}
/** The instance of the client. Has its own socket for connecting to the server and setup details.
* Also has all of the methods for setup and closing the connection.
*/		
class ClientInstance {
	private int portNumber = 4200;	//Port number of the server.
	private String welcomeMsg = "Please type your username.";	//The server's welcome message.
	private String acceptedMsg = "Username accepted";			//The server's username acceptance message.
	private String noName =  "You have not entered a username!";	//The server's null username response.
	private String nameUnavailable = "Sorry, this username is unavailable";	//The server's duplicate username response.
	private Socket socket = null;	//Client socket for connecting to a server.
	private BufferedReader in;		//Reader for inputs from server.
	private PrintWriter out;		//Printer for sending messages to the server.
	private boolean canChat = false;	//Checking if the user can send messages.
	private boolean connectedToServer = false;		//Used to check if the client is connected to a server.
	private String clientName;		//The username of the client.
	
	/** Calls the setup functions and enables the client to listen for messages
	*/
	public void startClient() {
		beginConnection();
		handleOutMsg();
		handleInMsg();
	}
	/** Asks the user for a server address and trys to connect to that address. Uses a try block with a socket and attempts
	* to create input and output streams for the client instance.
	* Catches IO exceptions and calls {@link #setupProfile()}
	*/
	private void beginConnection() {
			String serverAddress = getClientInput( "What is the address of the server that you wish to connect to?" );
			try {
				socket = new Socket( serverAddress, portNumber );
				in = new BufferedReader( new InputStreamReader( socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
				connectedToServer = true;
			} 
			catch (IOException e) {
				System.err.println("Exception in handleConnection(): " + e + "\nShutting down...");
				System.exit(0);
			}
			setupProfile();
		}
	
	/** Handles the setup of the client. It allouws the user to select a username.
	* The method then varifies the given name making sure it is not already taken
	* and checking if the user has even entered anything.
	*/
	private void setupProfile() {
		String line = null;
		while ( ! canChat ) {
			try {
				line = in.readLine();
			}
			catch (IOException e) {
				System.err.println( "Exception in setupProfile:" + e + "\nShutting Down...");
			}
			if ( line.startsWith( welcomeMsg ) ) {
				out.println( getClientInput( welcomeMsg ) );
			}
			else if (line.startsWith( acceptedMsg ) ) {
				canChat = true;
				System.out.println(acceptedMsg);
			}
			else if (line.startsWith (noName)){
				out.println( getClientInput( noName ) );
				return;
			}
			else if (line.startsWith(nameUnavailable)){
				out.println( getClientInput( nameUnavailable ) );
				return;
			} else if (line.startsWith(clientName)){
				clientDisconnect();
			}
			else System.out.println( line );
		}
	}	
	/** Handles sending messages given by the user to the server. It checks the client is still connected
	* and if so, prints the user input taken from {@link #getClientInput(String)} to the output stream.
	*/
	private void handleOutMsg() { 
		Thread senderThread = new Thread( new Runnable(){
			public void run() {
				while ( connectedToServer  ){
					out.println( getClientInput( null ) );
				}
			}
		});
		senderThread.start();
	}
	/** Gets the input from the user by creating an input stream and reader and checking if the client
	* is allowed to chat or not. 
	* @param clientInstructions		Any value given by the server passed in via {@link #handleOutMsg()}.
	* @return message	The input from the user returned as a string.
	*/
	private String getClientInput (String clientInstructions) {
		String message = null;
		try {
			BufferedReader reader = new BufferedReader(
				new InputStreamReader(System.in));
			if ( clientInstructions != null ) {
				System.out.println( clientInstructions );
			}
			message = reader.readLine();
			if ( ! canChat ) {
				clientName = message;
			}
			if (message.equals("!quit")) System.exit(0);
		}
		catch (IOException e) {
			System.err.println("Exception in getClientInput(): " + e + "\nShutting Down...");
			System.exit(0);
		}
		return message;
	}
	/** Creates its own thread for listening for messages. the run methed of this thread checks if the
	* the client is connected to the server. It then checks if the line is null. If so then it assumes the client has
	* disconnected from the server and calls {@link #clientDisconnect()}. it also catches IO Exceptions.
	* if the line is not null then it simply prints out the line to the user.
	*/
	private void handleInMsg() {
		Thread listenerThread = new Thread( new Runnable() {
			public void run() {
				while ( connectedToServer ) {
					String line = null;
					try {
						line = in.readLine();
						if ( line == null ) {
							connectedToServer = false;
							System.err.println( "Disconnected from the server" );
							clientDisconnect();
							break;
						}
						System.out.println( line );
					}
					catch (IOException e) {
						connectedToServer = false;
						System.err.println( "Lost connection to server, Shutting Down..." );
						System.exit(0);
						break;
					}
				}
			}
		});
		listenerThread.start();			
	}
	/** The connection from the server is closed. The method closes the socket and Exits the program with
	* System exit. This is placed in a try block to catch exceptions when closing the socket.
	*/
	void clientDisconnect() {
		try { 
			socket.close(); 
			System.exit(0);
		} 
		catch (IOException e) {
			System.err.println( "Exception when closing the socket, Shutting Downn..." );						
			System.err.println( e.getMessage() );
			System.exit(0);
		}
	}
}