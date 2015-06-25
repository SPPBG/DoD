package server.game;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import server.game.items.Armour;
import server.game.items.GameItem;
import server.game.items.Sword;

/**
 * This class controls the game logic and interaction between players. Caution:
 * not thread-safe, but will need to be made so for a networked game.
 */
public class GameLogic {
	Map map;

	// Has a player won already?
	private boolean playerWon = false;

	private final List<Player> players;

	// The current player's turn, -1 indicates game not started
	private int currentPlayer = -1;

	/**
	 * Constructor that specifies the map which the game should be played on.
	 * 
	 * @param mapFile
	 *            The name of the file to load the map from.
	 * @throws FileNotFoundException
	 *             , ParseException
	 */
	public GameLogic(String mapFile) throws FileNotFoundException,
			ParseException {
		this.map = new Map(mapFile);

		// Check if there is enough gold to win
		if (this.map.remainingGold() < this.map.getGoal()) {
			throw new IllegalStateException(
					"There isn't enough gold on this map for you to win");
		}

		this.players = new ArrayList<Player>();
	}

	/**
	 * Adds a new player to the game.
	 * 
	 * @param player
	 *            take a reference to the player, so messages can be sent and
	 *            interactions communicated
	 * 
	 * @return the id of the player
	 */
	synchronized public int addPlayer(PlayerListener player) {
			final int playerID = this.players.size();
	
			boolean deadFlag=(allPlayersDead());
			
			//Inform all alive players that there is a change in the game
			if(!deadFlag)
				for(Player p:players){
					if(!p.isDead())
						p.notifyChange();
				}
			
			this.players.add(new Player("Player " + playerID,
					generateRandomStartLocation(), player));
	
			if (this.players.size() == 1) {
				startNewGame();
			}
			
			//In case that all other players are dead/removed and the game is not won
			if(deadFlag&&!playerWon){
				currentPlayer=playerID;
				startTurn();
			}
			

	
			return playerID;
	}

	/**
	 * Removes a player from the game. The player is killed within the game, but
	 * the reference is held. This might need some changing for a network.
	 */
	synchronized public void removePlayer(int playerID) {
		removePlayer(playerID, false);
	}

	/**
	 * Removes a player from the game. The player is killed within the game, but
	 * the reference is held. This might need some changing for a network.
	 * @param silent - when a player has exited from the client side, do not send them ENDTURN
	 */
	synchronized public void removePlayer(int playerID, boolean silent) {
		
			this.players.get(playerID).kill();
			if (this.currentPlayer == playerID) {
				// Advance turn to handle death on player's turn
				advanceTurn(playerID, silent);
			}
	}

	/**
	 * Starts a new game of the Dungeon of Dooooooooooooom.
	 */
	synchronized public void startNewGame() {
		if (this.currentPlayer != -1) {
			throw new RuntimeException("The game has already started.");
		}

		if (this.players.size() == 0) {
			throw new RuntimeException(
					"The game cannot be started with a single player");
		}

		this.currentPlayer = 0;

		startTurn();
	}

	/**
	 * Handles the client message HELLO
	 * 
	 * @param newName
	 *            the name of the player
	 * @return the message to be passed back to the command line
	 * @throws CommandException
	 */
	synchronized public void clientHello(String newName, int playerID)
			throws CommandException {
		assertPlayerExists(playerID);

		// Change the player name and then say hello to them
		this.players.get(playerID).setName(newName);
	}

	/**
	 * Handles the client message LOOK Shows the portion of the map that the
	 * player can currently see.
	 * 
	 * @return the part of the map that the player can currently see.
	 */
	synchronized public String clientLook(int playerID) {
		assertPlayerExists(playerID);

		final Player player = this.players.get(playerID);

		// Work out how far the player can see
		final int distance = player.lookDistance();

		String lookReply = "";
		// Iterate through the rows.
		for (int rowOffset = -distance; rowOffset <= distance; ++rowOffset) {
			String line = "";

			// Iterate through the columns.
			for (int colOffset = -distance; colOffset <= distance; ++colOffset) {

				// Work out the location
				final Location location = player.getLocation().atOffset(
						colOffset, rowOffset);

				char content = '?';
				if (!player.canSeeTile(rowOffset, colOffset)) {
					// It's outside the FoV so we don't know what it is.
					content = 'X';
				} else if (!this.map.insideMap(location)) {
					// It's outside the map, so just call it a wall.
					content = '#';
				} else if (otherPlayerOnTile(location, playerID)) {
					content = 'P';
				} else {
					// Look up and see what's on the map
					content = this.map.getMapCell(location).toChar();
				}

				// Add to the line
				line += content;
			}

			// Send a line of the look message
			lookReply += line + System.getProperty("line.separator");
		}

		return lookReply;
	}

	/**
	 * Handles the client message MOVE
	 * 
	 * Move the player in the specified direction - assuming there isn't a wall
	 * in the way
	 * 
	 * @param direction
	 *            The direction (NESW) to move the player
	 * @return An indicator of the success or failure of the movement.
	 * @throws CommandException
	 */
	synchronized public void clientMove(CompassDirection direction, int playerID)
			throws CommandException {

			assertPlayerExists(playerID);
	
			ensureNoWinner();
			assertPlayersTurn(playerID);
			assertPlayerAP(playerID);
	
			final Player player = this.players.get(playerID);
	
			// Work out where the move would take the player
			final Location location = player.getLocation().atCompassDirection(
					direction);
	
			// Ensure that the movement is within the bounds of the map and not
			// into a wall
			if (!this.map.insideMap(location)
					|| !this.map.getMapCell(location).isWalkable()) {
				throw new CommandException("can't move into a wall");
			}
	
			if (otherPlayerOnTile(location, playerID)) {
				throw new CommandException("can't move into another player");
			}
	
			// Costs one action point
			player.decrementAp();
	
			// Move the player
			player.setLocation(location);
	
			advanceTurn(playerID, false);
			return;
	}

	/**
	 * Handles the client message ATTACK
	 * 
	 * Note: In the single player version of the game this doesn't do anything
	 * 
	 * @param direction
	 *            The direction in which to attack
	 * @return A message indicating the success or failure of the attack
	 * @throws CommandException
	 */
	synchronized public void clientAttack(CompassDirection direction, int playerID)
		throws CommandException {
		assertPlayerExists(playerID);
		ensureNoWinner();
		assertPlayersTurn(playerID);
		assertPlayerAP(playerID);
		
		final Player player = this.players.get(playerID);
		
		// Work out which square we're targeting
		Location location =
			this.players.get(playerID).getLocation().atCompassDirection(direction);
		/**
		 *1. Work out which player the attack is on...
		 */
		 Player target=null;
		 //Used when removing the target
		 int targetIndex=0;
		 for(Player p:players){
			 targetIndex++;
			 if(p.getLocation().equals(location)){
				 target=p;
				 break;
			 }
		 }
		 if(target==null)
			 throw new CommandException("attacking (" + direction.toString()
					 + ") a non-player tile");
		 /** 2. Have you hit the target? - hint, you might want to make the chance
		 * of a successful attack 75%?
		 */
		 Random rng=new Random();
		 boolean success=(rng.nextInt(4)!=0);
		 // Attacking depletes the AP, regardless of outcome
		 player.zeroAP();
		 if(success){
			 /** 2.1 if the player has hit the target then hp of the target should be
			 * reduced based on this formula...
			 */
			 int armour=0,sword=0;
			 if(player.hasItem(new Sword()))
				 sword=1;
			 if(target.hasItem(new Armour()))
				 armour=1;
			 int damage=1+sword-armour;
			 /**
			 * Player and target are informed about the attack as set out in the
			 * wire_spec
			 */
			 advanceTurn(playerID, false);
			 target.decrementHealth(damage);
			 //If the target is dead - remove it
			 if(target.isDead()){
				 target.sendMessage("How sad - you died...");
				 removePlayer(targetIndex, false);
			 }
		 }else {
			 //On a miss report the failure
			 advanceTurn(playerID, false);
			 throw new CommandException(" you missed!");
		 }

	}

	/**
	 * Handles the client message PICKUP. Generally it decrements AP, and gives
	 * the player the item that they picked up Also removes the item from the
	 * map
	 * 
	 * @return A message indicating the success or failure of the action of
	 *         picking up.
	 * @throws CommandException
	 */
	synchronized public void clientPickup(int playerID) throws CommandException {
		assertPlayerExists(playerID);
		ensureNoWinner();
		assertPlayersTurn(playerID);
		assertPlayerAP(playerID);

		final Player player = this.players.get(playerID);

		final Tile playersTile = this.map.getMapCell(player.getLocation());

		// Check that there is something to pick up
		if (!playersTile.hasItem()) {
			throw new CommandException("nothing to pick up");
		}

		// Get the item
		final GameItem item = playersTile.getItem();

		if (player.hasItem(item)) {
			throw new CommandException("already have item");
		}

		player.giveItem(item);
		playersTile.removeItem();

		advanceTurn(playerID, false);
	}

	/**
	 * Sends a message to all players of the game.
	 * 
	 * @param message
	 *            The message to be shouted
	 */
	public void clientShout(String message) {
		synchronized (this){
			for (final Player player : this.players) {
				player.sendMessage(message);
			}
		}
	}

	/**
	 * Handles the client message ENDTURN
	 * 
	 * Just sets the AP to zero and advances as normal.
	 */
	synchronized public void clientEndTurn(int playerID) {
		clientEndTurn(playerID, false);
	}

	/**
	 * Handles the client message ENDTURN
	 * 
	 * Just sets the AP to zero and advances as normal.
	 * @param silent - for when a players has exited on the client-side, it does not send them ENDTURN
	 */
	synchronized public void clientEndTurn(int playerID, boolean silent) {
			assertPlayerExists(playerID);
			if(!silent)this.players.get(playerID).endTurn();
			int tmp=currentPlayer;
			// Advance to the next alive player
			do {
				this.currentPlayer++;
				if(this.currentPlayer>=players.size())currentPlayer=0;
				//If everybody is dead stop looping
				if(currentPlayer==tmp&&players.get(tmp).isDead())return;
			} while (this.players.get(this.currentPlayer).isDead());
			startTurn();
		
	}

	/**
	 * Sets the player's position. This is used as a cheating or debug command.
	 * It is particularly useful for testing, as it gets rounds the randomness
	 * of the player start position.
	 * 
	 * @param col
	 *            the column of the location to put the player
	 * @param row
	 *            the row to location to put the player
	 * @throws CommandException
	 */
	synchronized public void setPlayerPosition(int col, int row, int playerID)
		throws CommandException {
		assertPlayerExists(playerID);
		final Location location = new Location(col, row);

		if (!this.map.insideMap(location)) {
			throw new CommandException("invalid position");
		}

		if (!this.map.getMapCell(location).isWalkable()) {
			throw new CommandException("cannot walk on this tile");
		}

		this.players.get(playerID).setLocation(location);
	}

	/**
	 * Passes the goal back
	 * 
	 * @return the current goal
	 */
	public int getGoal() {
		return this.map.getGoal();
	}

	/**
	 * Generates a randomised start location
	 * 
	 * @return a random location where a player can start
	 */
	private Location generateRandomStartLocation() {
		if (!atLeastOneNonWallLocation()) {
			throw new IllegalStateException(
					"There is no free tile available for the player to be placed");
		}

		while (true) {
			// Generate a random location
			final Random random = new Random();
			final int randomRow = random.nextInt(this.map.getMapHeight());
			final int randomCol = random.nextInt(this.map.getMapWidth());

			final Location location = new Location(randomCol, randomRow);

			if (this.map.getMapCell(location).isWalkable()
					&& !otherPlayerOnTile(location, -1)) {
				// If it's not a wall then we can put them there
				return location;
			}
		}
	}

	/**
	 * Searches a possible tile to use by the player, i.e. non-wall. The map is
	 * traversed from (0,0) to (maxY,MaxX).
	 * 
	 * @return true if there is at least one non-wall location, false otherwise
	 */
	private boolean atLeastOneNonWallLocation() {
		for (int x = 0; x < this.map.getMapWidth(); x++) {
			for (int y = 0; y < this.map.getMapHeight(); y++) {

				if (this.map.getMapCell(new Location(x, y)).isWalkable()) {
					// If it's not a wall then we can put them there
					return true;
				}
			}
		}

		return false;
	}

	private boolean otherPlayerOnTile(Location location, int currentPlayerID) {
		for (int otherPlayerID = 0; otherPlayerID < this.players.size(); otherPlayerID++) {
			if ((otherPlayerID != currentPlayerID)
					&& this.players.get(otherPlayerID).getLocation()
							.equals(location) && !players.get(otherPlayerID).isDead()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Ensures a player has been added to the map. Otherwise, an exception is
	 * raised. In a multiplayer scenario, this could ensure a player by given ID
	 * exists.
	 * 
	 * @throws RuntimeException
	 */
	private void assertPlayerExists(int playerID) throws RuntimeException {
		if ((playerID < 0) || (playerID >= this.players.size())) {
			throw new IllegalStateException(": Player has not been added.");
		}
	}

	/**
	 * Ensures a player has enough AP, otherwise a runtime error is raised,
	 * since the turn should have been advanced. In a multiplayer example, this
	 * is still a bug, since the server should have checked whose turn it was.
	 * 
	 * @throws RuntimeException
	 */
	private void assertPlayerAP(int playerID) throws RuntimeException {
		if (this.players.get(playerID).remainingAp() == 0) {
			throw new IllegalStateException("Player has 0 ap");
		}
	}

	/**
	 * @throws CommandException
	 * 
	 */
	private void assertPlayersTurn(int playerID) throws CommandException {
		if (playerID != this.currentPlayer) {
			throw new CommandException("not your turn");
		}
	}

	/**
	 * Ensure that no player has won the game. Throws a CommandException if
	 * someone has one, preventing the command from executing
	 * 
	 * @throws CommandException
	 */
	private void ensureNoWinner() throws CommandException {
		if (this.playerWon) {
			throw new CommandException("the game is over");
		}
	}

	private void startTurn() {
		this.players.get(this.currentPlayer).startTurn();
	}

	/**
	 * Once a player has performed an action the game needs to move onto the
	 * next turn to do this the game needs to check for a win and then test to
	 * see if the current player has more AP left.
	 * 
	 * Note that in this implementation we currently playing this as a single
	 * player game so the next turn will always be the current player so we
	 * simply start their turn again.
	 * @param the ID of a player
	 * @param silent - in case a player has exited on their own volition, do not inform them of end turn
	 */
	private void advanceTurn(int playerID, boolean silent) {
		final Player player = this.players.get(playerID);
		//Notify nearby players who can see the current one, of any changes
		notifyAllNearby(playerID);
		// Check if the player has won
		if ((player.getGold() >= this.map.getGoal())
				&& (this.map.getMapCell(player.getLocation()).isExit())) {

			// Player should not be able to move if they have won
			if(this.playerWon)return;

			this.playerWon = true;
			player.win();
			
			// Other players  informed of their loss
			for(int otherID=0;otherID<players.size();otherID++)
				if(otherID!=playerID)
					players.get(otherID).lose();
			
		} else if ((player.remainingAp() == 0) || player.isDead()) {
			// Force the end of turn
			clientEndTurn(playerID, silent);
		}
	}
	
	//Notifies players that can see 
	private void notifyAllNearby(int playerID){
		Location target=players.get(playerID).getLocation();
		for(int id=0;id<players.size();id++){
			Player candidate=players.get(id);
			//If the player is not dead, is not the same as the one provided as parameter
			// and can see the target, he must be notified
			if(!players.get(id).isDead()
				&&id!=playerID
				&&candidate.canSeeTile(target))
					candidate.notifyChange();
		}
	}
	
	//Used when a player connects after all others have disconnected in order to give him a turn
	private boolean allPlayersDead(){
		if(players.size()==0)return false;
		for(Player p:players){
			if(!p.isDead())return false;
		}
		return true;
	}

	public char[][] getMapView() {
		char[][] result = map.view();
		for(Player p:players)
			if(!p.isDead())
				result[p.getLocation().getRow()][p.getLocation().getCol()]='P';
		return result;
	}
}


