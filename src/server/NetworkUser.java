package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import server.game.GameLogic;
/**
 * A class for reading/writing to a user over a network
 */
public class NetworkUser extends CommandLineUser implements AutoCloseable{
	
	//The thread listening for input from a client
	private Thread clientThread=new Thread(this);
	
	AtomicBoolean closed=new AtomicBoolean(true);
	
	//The socket responsible for the IO
	private Socket client=null;
	
	private BufferedReader netIn=null;
	private BufferedWriter netOut=null;
	
	//The server who initiated this client
	private ServerLogic server;
	
	public NetworkUser(GameLogic game, ServerLogic newServer,Socket newClient){
		super(game); 
		server=newServer;
		client=newClient;
	}
	
	/**
	 * Closes the socket and the IO streams
	 */
	@Override synchronized public void close(){
		//Do not close twice
		if(closed.get())return;
		closed.set(true);
		
		try{
			//Inform the server administrator that we're closing a connection
			server.handleMessage(
					"Closing connection to "+client.getInetAddress().getHostName()
					);
			//Close the streams and the socket
			netOut.close();
			netIn.close();
			client.close();
			//Remove the player from GameLogic
			removePlayer();
			//Update server view
			server.updateMapView();
			//Stop the thread
			clientThread.interrupt();
		} catch (IOException e) {
			//Inform the administrator of an error
			server.handleError(e);
		}
	}
	
	/**
	 * Start the client
	 * @throws IOException
	 */
	public void start()throws IOException{
		netIn=new BufferedReader(new InputStreamReader(client.getInputStream()));
		netOut=new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
		addPlayer();
		closed.set(false);
		clientThread.start();
	}
	
	/**
	 * The client handling 
	 */
	@Override
	public void run() {
		try {
			while (true) {
			
				// Try to grab a command from the network
				final String command = netIn.readLine();

				// Test for EOF (ctrl-D)
				if (command==null||command.isEmpty()) {
					break;
				}
				
				server.handleMessage("From "+client.getInetAddress().getHostAddress()+": "+command);
				
				processCommand(command);
			}
			
		} catch (final RuntimeException e) {
			server.handleError(e);
		} catch (final IOException e) {
			// Die if something goes wrong.
			server.handleError(e);
		}finally{
			close();
		}
	}
	
	/**
	 * Responsible for sending server output to the client 
	 */
	@Override
	protected void doOutputMessage(String message) {
		try{
			server.updateMapView();
			server.handleMessage("TO "+client.getInetAddress().getHostAddress()+": "+message);
			netOut.write(message);
			netOut.newLine();
			netOut.flush();
		}catch(IOException e){
			server.handleError(e);
			close();
		}catch(NullPointerException e){
		}
	}
}
