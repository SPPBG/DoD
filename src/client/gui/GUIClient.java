package client.gui;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

/**
 * A client class which is used in conjunction with a GUI 
 * which is either of the GamePanel type or it's derivatives.
 * 
 */
public class GUIClient extends client.AbstractClient {
	
	//The GUI which we'll be working with
	private GamePanel owner;

	/**
	 * Extends the parent class' constructor
	 * @param host
	 * @param port
	 * @param nOwner
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public GUIClient(String host, int port, GamePanel nOwner)
			throws IOException, UnknownHostException {
		super(host, port);
		owner = nOwner;
	}

	/**
	 * 
	 */
	
	@Override
	protected void handleGold(String msg) throws IOException{
		super.handleGold(msg);
		JOptionPane.showMessageDialog(owner, "You'll need "+msg+" gold to escape!", "You're DOOOMED!s",
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Sub-class specific way of handling look replies
	 */
	@Override
	protected void handleLookReply(ArrayList<String> lines) {
		super.handleLookReply(lines);
		owner.update(lines);
	}
	
	/**
	 * Handles success messages by sending a look message in order to update the screen
	 */
	@Override
	protected void handleSuccess() throws IOException {
		send("LOOK");
	}
	
	/**
	 * Handles failures by invoking a pop-up ( on the GUI)
	 */
	@Override
	protected void handleFail(String msg) {
		JOptionPane.showMessageDialog(owner, msg, "Faliure",
				JOptionPane.WARNING_MESSAGE);
	}

	/**
	 * Handles winning by invoking a pop-up
	 */
	@Override
	protected void handleWin() {
		JOptionPane.showMessageDialog(owner, "You Win!", "Game Over",
				JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Handles losing by invoking a pop-up
	 */
	@Override
	protected void handleLose() {
		JOptionPane.showMessageDialog(owner, "You Lose!", "Game Over",
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Handles HP changes by invoking a pop-up
	 */
	@Override
	protected void handleHPChange(String msg) {
		//Add the hp change itself in the message
		int i = Integer.valueOf(msg);
		if (i < 0)
			msg = "You take " + (-i) + " damage!";
		else
			msg = "You are healed for " + i;
		JOptionPane.showMessageDialog(owner, msg, "Update",
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Handles gaining/losing gold by invoking a pop-up
	 */
	@Override
	protected void handleTreasureChange(String msg) {
		//Add the hp change itself in the message
		int i = Integer.valueOf(msg);
		if (i < 0)
			msg = "You lose " + (-i) + " gold!";
		else
			msg = "You get " + i + " gold!";
		JOptionPane.showMessageDialog(owner, msg, "Update",
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Handles the start of the player turn via pop-up notification
	 */
	@Override
	protected void handleStartTurn(){
		JOptionPane.showMessageDialog(owner, "Your turn!", "Update",
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * 
	 */
	@Override
	protected void handleEndTurn(){
		JOptionPane.showMessageDialog(owner, "End of your turn.", "Update",
				JOptionPane.INFORMATION_MESSAGE);
	}

}
