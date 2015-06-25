package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The basic client for humans.
 * 
 */
abstract public class AbstractClient implements Runnable, AutoCloseable {

	// Socket via which we'll connect to the server
	private Socket clientSocket = null;

	// Read/Write to server
	private BufferedReader netIn = null;
	private BufferedWriter netOut = null;

	/**
	 * Protects against multiple calls of close and start
	 */
	private volatile AtomicBoolean active = new AtomicBoolean(false);

	Thread netReaderThread = null;

	/**
	 * Create a new client using a server address and port to instantiate the
	 * socket
	 * 
	 * @param host
	 *            - server address
	 * @param port
	 *            - port on which the game server is
	 */
	public AbstractClient(String host, int port) throws UnknownHostException,
			IOException {
		clientSocket = new Socket(host, port);
		netReaderThread = new Thread(this);
	}

	final public boolean isActive() {
		return active.get();
	}

	/**
	 * The main responsible for receiving handling server messages
	 */
	@Override
	public void run() {
		try {
			while (true) {
				// Handle server output
				handleServerMessage(receive());
				Thread.sleep(50);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			close();
		} catch (RuntimeException re) {
			re.printStackTrace();
			close();
		} catch (InterruptedException ie) {
			// Break out of the loop normally
		}
	}

	/**
	 * Starts a game session
	 */
	public void start() throws IOException {
		if (isActive())
			throw new RuntimeException("Already started!");
		active.set(true);
		// Open the IO streams
		netIn = new BufferedReader(new InputStreamReader(
				clientSocket.getInputStream()));
		netOut = new BufferedWriter(new OutputStreamWriter(
				clientSocket.getOutputStream()));
		// Start listening to the server
		netReaderThread.start();
	}

	/**
	 * Writes a message to the server ensuring that the buffered writer flushes
	 * it
	 * 
	 * @param msg
	 *            - message to be sent
	 */
	public void send(String msg) throws IOException {
		if (!isActive())
			throw new IOException("Connection");
		netOut.write(msg);
		netOut.newLine();
		netOut.flush();
	}

	/**
	 * This method decides which message handler to call depending, on the
	 * message it has received responses from the server
	 */
	final protected void handleServerMessage(String msg) throws IOException,
			RuntimeException {
		// Empty message, do nothing
		if (msg.isEmpty())
			return;
		// Extract the entire look reply and handle it
		if (msg.equals("LOOKREPLY")) {
			handleLookReply(extractLookReply());
		} else if (msg.equals("WIN")) {
			// Decide how to handle winning
			handleWin();
		} else if (msg.equals("LOSE")) {
			// Decide how to handle losing
			handleLose();
		} else if (msg.startsWith("FAIL")) {
			// Decide how to handle a failure
			handleFail(clipMessage(msg));
		} else if (msg.startsWith("SUCCESS")) {
			// Decide how to handle a success
			handleSuccess();
		} else if (msg.equals("CHANGE")) {
			// Decide how to handle a change
			handleChange();
		} else if (msg.equals("STARTTURN")) {
			// Decide how to handle the start of your turn
			handleStartTurn();
		} else if (msg.equals("ENDTURN")) {
			// Decide how to handle the end of your turn
			handleEndTurn();
		} else if (msg.startsWith("MESSAGE")) {
			// Decide how to handle a message from a player
			handleMessage(clipMessage(msg));
		} else if (msg.startsWith("HITMOD")) {
			// Decide how to handle a change in HP
			handleHPChange(clipMessage(msg));
		} else if (msg.startsWith("TREASUREMOD")) {
			// Decide how to handle a change in gold
			handleTreasureChange(clipMessage(msg));
		} else if (msg.startsWith("HELLO")) {
			// Decide how to handle the greetings
			handleHello(clipMessage(msg));
		} else if (msg.startsWith("GOLD")) {
			// Decide how to handle the initial gold message
			handleGold(clipMessage(msg));
		}
	}

	/**
	 * The stubs of the handlers for every type of message, their titles explain
	 * what they should do
	 * 
	 * @param msg
	 *            (not for all)
	 * @throws IOException
	 *             (if we write or read from the server)
	 */

	protected void handleGold(String msg) throws IOException {
		// Initial LOOK message
		send("LOOK");
	}

	protected void handleChange() throws IOException {
		// Someone joined the game or came near enough for us to see them
		send("LOOK");
	}

	protected void handleHello(String msg) throws IOException {
	}

	protected void handleLose() throws IOException {
	}

	protected void handleWin() throws IOException {
	}

	protected void handleFail(String msg) throws IOException {
	}

	protected void handleSuccess() throws IOException {
	}

	protected void handleStartTurn() throws IOException {
	}

	protected void handleEndTurn() throws IOException {
	}

	protected void handleMessage(String msg) throws IOException {
	}

	protected void handleHPChange(String msg) throws IOException {
	}

	protected void handleTreasureChange(String msg) throws IOException {
	}

	/**
	 * Reads from the server
	 * 
	 * @return - returns empty string if there is no pending input, otherwise
	 *         returns whatever's in the buffer
	 * @throws IOException
	 */
	protected String receive() throws IOException {
		if (netIn.ready())
			return netIn.readLine();
		return "";
	}

	/**
	 * Clean-up
	 */
	@Override
	public void close() {
		// Make sure we haven't closed twice
		if (!active.get())
			return;
		active.set(false);
		try {
			// Kill threads
			netReaderThread.interrupt();
			netIn.close();
			netOut.close();
			clientSocket.close();
		} catch (IOException ioe) {
			System.err.println("Error closing: " + ioe.getMessage());
		}
	}

	/**
	 * Gets the rest of the look reply
	 * 
	 * @return array list of all the lines of the look reply
	 */
	protected ArrayList<String> extractLookReply() throws IOException {
		String line;
		ArrayList<String> lines = new ArrayList<String>();
		// Read the first line - it determines how many other lines there will
		// be
		line = receive();
		lines.add(line);
		// Read the rest of the lines
		int length = line.length();
		for (int i = 0; i < length; i++) {
			line = receive();
			lines.add(line);
		}
		return lines;
	}

	/**
	 * Handles the LOOKREPLY from the game
	 */
	protected void handleLookReply(ArrayList<String> lines) {
		if (lines.size() < 2) {
			System.out.println("Not enough symbols");
			throw new RuntimeException("FAIL: Invalid LOOKREPLY dimensions");
		}

		final int lookReplySize = lines.get(0).length();
		if (lines.size() != lookReplySize + 1) {
			throw new RuntimeException("FAIL: Invalid LOOKREPLY dimensions");
		}
		// Remove the empty line
		lines.remove(lookReplySize);
		// Check if all lines are equal sizes
		for (String line : lines)
			if (line.length() != lookReplySize)
				throw new RuntimeException("FAIL: Invalid LOOKREPLY dimensions");
		// Finished checking, define what to do next in subclasses
	}

	/**
	 * Remove the message header as to pass the contents of the message to its
	 * handler
	 * 
	 * @param msg
	 *            - message to be clipped
	 */
	final private String clipMessage(String msg) {
		int indx = msg.indexOf(' ');
		return msg.substring(indx+1);
	}

}
