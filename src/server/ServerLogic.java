package server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Enumeration;

import server.game.GameLogic;

/**
 * General server logic , such as listening for clients and serving them
 * 
 */
public class ServerLogic implements Runnable{
	
	//Server socket and its port
	private ServerSocket listener=null;
	private int port=60000;
	
	//The thread responsible for listening for clients and accepting them
	private Thread listenerThread=new Thread(this);
	
	//The game which the server will run on
	private GameLogic game=null;
	
	//The UI responsible for visualizing the server's output
	private ServerUI serverUI=null;
	
	
	public ServerLogic(String mapFilePath,ServerUI ui) throws FileNotFoundException,ParseException{
		game=new GameLogic(mapFilePath);
		serverUI=ui;
	}
	
	/**
	 * Returns the port the server will be/is listening on
	 * @return
	 */
	public int getPort(){
		return port;
	}
	
	/**
	 * Sets the port to listen on
	 * @param prt
	 */
	public void setPort(int prt){
		port=prt;
	}
	
	/**
	 * Returns the currently used instance of GameLogic
	 * @return
	 */
	public GameLogic getGame(){
		return game;
	}
	
	/**
	 * Returns the IP of the server
	 * @return
	 */
	public String getIP(){
	    try {
	    	//We may have more than 1 network interface
	        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
	        for(NetworkInterface interfc: Collections.list(interfaces)) {
	    
	            // filters out 127.0.0.1 and inactive interfaces
	            if (interfc.isLoopback() || !interfc.isUp())
	                continue;
	            //Return first active IP address
	            String ip=interfc.getInetAddresses().nextElement().getHostAddress();
	            return ip;
	        }
	        return "";

	    } catch (SocketException e) {
	        return "";
	    }
	}
	
	/**
	 * Determines if the Server is currently listening for clients
	 * @return
	 */
	public boolean isListening(){
		return listenerThread.isAlive();
	}
	
	/**
	 * Initiate serving/listening
	 */
	public void startListening(){
		handleMessage("Now listening for clients");
		//We can't restart interrupted threads so a new one must be created
		listenerThread=new Thread(this);
		listenerThread.start();
	}
	
	/**
	 * Stop serving/listening
	 */
	public void stopListening(){
		handleMessage("Stopped listening for clients");
		listenerThread.interrupt();
	}
	
	/**
	 * Creates a new NetworkUser object and starts its thread
	 * @param client
	 * @throws IOException
	 */
	public void registerClient(Socket client) throws IOException{
		//Do not worry about closing this - the user closes on it's own when its thread finishes
		NetworkUser user=new NetworkUser(game,this,client);
		user.start();
		//Inform the administrator
		handleMessage("A new client ("+client.getInetAddress().getHostAddress()+") has arrived");
	}
	
	/**
	 * The listener thread's loop
	 */
	@Override public void run(){
		try{
			//Create a new ServerSocket to listen from
			listener=new ServerSocket(port);
			//Enable it to be a (kind of) non-blocking
			listener.setSoTimeout(200);
			while(true){
				try{
					registerClient(listener.accept());
				}catch(SocketTimeoutException ste){
					//Ignore this
				}catch(IOException e){
					//Problem with opening the client's streams
					handleError(e);
				}
				Thread.sleep(50);
			}
		}catch(IOException e){
			handleError(e);
		}catch(InterruptedException e){
			//Ignore this - it is the standard method of exiting the loop
		}finally{
			try{
				listener.close();
			}catch(NullPointerException|IOException e){
				//Nothing we can do
			}
		}
	}
	
	/**
	 * Handles recovery from exceptions, currently just informing the administrator
	 * @param error
	 */
	public void handleError(Throwable error){
		serverUI.handleError(error);
	}
	
	/**
	 * Handles messages, currently just informing the administrator
	 */
	public void handleMessage(String message){
		serverUI.handleMessage(message);
	}
	
	/**
	 * Updates the map view on the UI
	 */
	public void updateMapView(){
		serverUI.updateMapView(game.getMapView());
	}
}
