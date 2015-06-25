package server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import server.game.CommandException;
import server.game.CompassDirection;
import server.game.GameLogic;
import server.game.PlayerListener;

/**
 * An abstract class to handle the parsing and handling of textual commands,
 * e.g. MOVE and PICKUP.
 * 
 * This class could be extended to provide a server instance for every user who
 * connects over the network.
 */
public abstract class CommandLineUser implements PlayerListener, Runnable {
	// The game which the command line user will operate on.
	// This is private to enforce the use of "processCommand".
	protected final GameLogic game;

	// The player must be added onto the map. Initially it is not.
	private boolean playerAdded = false;

	//The ID of the player on the map.
	int playerID = -1;

	// In order to ensure the specification is met, we need to ensure that
	// a response is sent after a command, before anything else is sent.
	// Therefore, we use "waitingForResponse" as a flag for this, and
	// store any other messages, e.g. shouts from players, in a List.
	// Due to network latency, this could still happen at the client side. 
	private boolean waitingForResponse = false;
	private final List<String> messageBuffer;

	CommandLineUser(GameLogic game) {
		this.game = game;

		this.messageBuffer = Collections.synchronizedList(new ArrayList<String>());
	}

	/**
	 * Allows the class to be "run" by a thread, in the matter specified by the
	 * sub-class.
	 * 
	 * Perhaps this could be used to service a new client on the network?
	 */
	@Override
	public abstract void run();

	/**
	 * Sends a message to the player from the game.
	 * 
	 * @ param message The message to be sent
	 */
	@Override
	public void sendMessage(String message) {
		outputMessage("MESSAGE " + message, false);
	}

	/**
	 * Informs the user of the beginning of a player's turn
	 */
	@Override
	public void startTurn() {
		outputMessage("STARTTURN", false);
	}

	/**
	 * Informs the user of the end of a player's turn
	 */
	@Override
	public void endTurn() {
		outputMessage("ENDTURN", false);
	}

	/**
	 * Inform the user that the player has won or lost
	 */
	@Override
	public void win() {
		outputMessage("WIN", false);
	}
	
	@Override
	public void lose() {
		outputMessage("LOSE",false);
	}
	
	/**
	 * Inform the user that something visible has changed
	 */
	@Override
	public void notifyChange(){
		outputMessage("CHANGE",false);
	}

	/**
	 * Informs the user that the player's hit points have changed
	 */
	@Override
	public void hpChange(int value) {
		outputMessage("HITMOD " + value, false);
	}

	/**
	 * Informs the user that the player's gold count has changed
	 */
	@Override
	public void treasureChange(int value) {
		outputMessage("TREASUREMOD " + value, false);
	}

	/**
	 * Processes a text command from the user.
	 * 
	 * @param commandString
	 *            the string containing the command and any argument
	 */
	protected final void processCommand(String commandString) {
		if(commandString.length() == 0) {
			return;
		}

		// Process the command string e.g. MOVE N
		final String commandStringSplit[] = commandString.split(" ", 2);
		final String command = commandStringSplit[0];
		final String arg = ((commandStringSplit.length == 2)
				? commandStringSplit[1]
						: null);

		try {
			processCommandAndArgument(command, arg);
		} catch (final CommandException e) {
			outputMessage("FAIL " + e.getMessage(), true);
		}
	}

	/**
	 * Adds the player to the game. This could not be done in the constructor
	 * because the sub-class must be properly constructed first in some cases,
	 * e.g. on a network.
	 * 
	 */
	protected void addPlayer() {
		if (this.playerAdded) {
			throw new RuntimeException("Player already added");
		}
		this.playerAdded = true;

		// The first message must be GOLD
		outputMessage("GOLD " + this.game.getGoal(), true);

		// Ensures that the instance will listen to the player in the
		// game for messages from the game
		this.playerID = this.game.addPlayer(this);
	}

	protected void removePlayer() {
		if (!this.playerAdded) {
			throw new RuntimeException("Player not added");
		}
		this.playerAdded = false;

		this.game.removePlayer(this.playerID, true);
	}

	/**
	 * Inherited by the base class to handle outputting textual messages in the
	 * correct manner, e.g. printing to the screen or read by the bot.
	 * 
	 * Perhaps this could be used to send text over a network?
	 * 
	 * @param message Message to output or act upon
	 */
	protected abstract void doOutputMessage(String message);

	/**
	 * Processes the command and an optional argument
	 * 
	 * @param command
	 *            the text command
	 * @param arg
	 *            the text argument (null if no argument)
	 * @throws CommandException
	 */
	private void processCommandAndArgument(String command, String arg)
			throws CommandException {
		if (!this.playerAdded) {
			throw new RuntimeException("Player not added");
		}
 
		if (command.equals("HELLO")) {
			if (arg == null) {
				throw new CommandException("HELLO needs an argument");
			}

			final String name = sanitiseMessage(arg);
			this.waitingForResponse = true;
			this.game.clientHello(name, this.playerID);
			outputMessage("HELLO " + name, true);

		} else if (command.equals("LOOK")) {
			if (arg != null) {
				throw new CommandException("LOOK does not take an argument");
			}
			this.waitingForResponse = true;
			outputMessage("LOOKREPLY" + System.getProperty("line.separator")
					+ this.game.clientLook(this.playerID), true);

		} else if (command.equals("PICKUP")) {
			if (arg != null) {
				throw new CommandException("PICKUP does not take an argument");
			}

			this.waitingForResponse = true;
			this.game.clientPickup(this.playerID);
			outputSuccess();

		} else if (command.equals("MOVE")) {
			// We need to know which direction to move in.
			if (arg == null) {
				throw new CommandException("MOVE needs a direction");
			}

			this.waitingForResponse = true;
			this.game.clientMove(getDirection(arg), this.playerID);

			outputSuccess();

		} else if (command.equals("ATTACK")) {
			// We need to know which direction to move in.
			if (arg == null) {
				throw new CommandException("ATTACK needs a direction");
			}

			this.waitingForResponse = true;
			this.game.clientAttack(getDirection(arg), this.playerID);

			outputSuccess();

		} else if (command.equals("ENDTURN")) {
			this.game.clientEndTurn(this.playerID, false);

		} else if (command.equals("SHOUT")) {
			// Ensure they have given us something to shout.
			if (arg == null) {
				throw new CommandException("need something to shout");
			}

			this.game.clientShout(sanitiseMessage(arg));

		} else if (command.equals("SETPLAYERPOS")) {
			if (arg == null) {
				throw new CommandException("need a position");
			}

			// Obtain two co-ordinates
			final String coordinates[] = arg.split(" ");

			if (coordinates.length != 2) {
				throw new CommandException("need two co-ordinates");
			}

			try {
				final int col = Integer.parseInt(coordinates[0]);
				final int row = Integer.parseInt(coordinates[1]);

				this.game.setPlayerPosition(col, row, this.playerID);
				outputSuccess();
			} catch (final NumberFormatException e) {
				throw new CommandException("co-ordinates must be integers");
			}

		} else {
			// If it is none of the above then it must be a bad command.
			throw new CommandException("invalid command");
		}
	}

	/**
	 * Obtains a compass direction from a string. Used to ensure the correct
	 * exception type is thrown, and for consistency between MOVE and ATTACK.
	 * 
	 * @param string
	 *            the direction string
	 * 
	 * @return the compass direction
	 * @throws CommandException
	 */
	private CompassDirection getDirection(String string)
			throws CommandException {
		try {
			return CompassDirection.fromString(string);
		} catch (final IllegalArgumentException e) {
			throw new CommandException("invalid direction");
		}
	}

	/**
	 * Sanitises the given message - there are some characters that we can put
	 * in the messages that we don't want in other stuff that we sanitise.
	 * 
	 * @param s
	 *            The message to be sanitised
	 * @return The sanitised message
	 */
	private static String sanitiseMessage(String s) {
		return sanitise(s, "[a-zA-Z0-9-_ \\.,:!\\(\\)#]");
	}

	/**
	 * Strip out anything that isn't in the specified regex.
	 * 
	 * @param s
	 *            The string to be sanitised
	 * @param regex
	 *            The regex to use for sanitisiation
	 * @return The sanitised string
	 */
	private static String sanitise(String s, String regex) {
		String rv = "";

		for (int i = 0; i < s.length(); i++) {
			final String tmp = s.substring(i, i + 1);

			if (tmp.matches(regex)) {
				rv += tmp;
			}
		}

		return rv;
	}

	/**
	 * Sends a success message in the event that a command has succeeded
	 */
	private void outputSuccess() {
		outputMessage("SUCCESS", true);
	}

	/**
	 * Outputs a message to the player, using the abstract "doOutputMessage"
	 * method, which allows the sub-class to handle it in different ways, e.g.
	 * simply printing it, or perhaps sending it over the network?
	 * 
	 * Since the specification says that the response to a command must be
	 * returned before anything else, this message will be saved and output to
	 * the player after the response.
	 * 
	 * @param message
	 *            the message to send to the player.
	 * @param isResponse
	 *            whether or not the message is a response to a command, e.g.
	 *            "SUCCESS" or "FAIL".
	 */
	private final void outputMessage(String message, boolean isResponse) {
		// If the user is waiting for a response, buffer the message
		if (this.waitingForResponse) {
			if (isResponse) {
				// Output the response
				doOutputMessage(message);

				// We can now send everything from the buffer and clear it
					for (final String line : this.messageBuffer) {
						doOutputMessage(line);
					}
					this.messageBuffer.clear();
			

				// We are no longer waiting for a response
				this.waitingForResponse = false;
			} else {
				// Add it to the buffer to be sent when the response has been
				// sent
				this.messageBuffer.add(message);
			}
		} else {
			// The user is not waiting for a response. Send it immediately.
			doOutputMessage(message);
		}
	}
}
