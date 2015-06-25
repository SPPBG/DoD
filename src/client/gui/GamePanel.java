package client.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class GamePanel extends JPanel implements ActionListener,AutoCloseable {

	private static final long serialVersionUID = -3338537058744374697L;
	
	/**
	 * The client which will serve as this panels I/O
	 */
	private GUIClient client = null;

	/**
	 * The canvas which will draw the in-game objects
	 */
	private GameCanvas canvas = new GameCanvas();

	/**
	 * Carries out currently chosen action
	 */
	private JButton doAction = new JButton("Do action");
	
	/**
	 * Radio buttons which allow the user to choose what action to perform
	 */
	private JRadioButton move = new JRadioButton("Move"),
						pickup = new JRadioButton("Pick up"), 
						attack = new JRadioButton("Attack");

	/**
	 * Radio buttons which allow the user to choose a direction for his actions
	 */
	private JRadioButton north = new JRadioButton("North"),
						east = new JRadioButton("East"), 
						south = new JRadioButton("South"),
						west = new JRadioButton("West");
	/**
	 * Contains action radio buttons and the other buttons as well
	 */
	JPanel actionsPanel = new JPanel();

	/**
	 * Contains directional radio buttons
	 */
	JPanel directionsPanel = new JPanel();

	/**
	 * Responds to any type of error which may occur
	 */
	private void errorHandler(Throwable error) {
		JOptionPane.showMessageDialog(this, error.getMessage(), "Error",
				JOptionPane.ERROR_MESSAGE);
		//Close the panel - something is not working
		close();
	}
	
	/**
	 * Sets up the action radio buttons and the close and doAction buttons in a sub-panel
	 * @param width
	 * @param height
	 */
	private void actionPanelSetup(int width, int height) {
		
		//Make sure the radio buttons are in the same group and there is always at least one selected
		ButtonGroup actions = new ButtonGroup();
		actions.add(move);
		actions.add(pickup);
		actions.add(attack);
		move.setSelected(true);
		
		//Set-up the panel
		actionsPanel.setPreferredSize(new Dimension(width / 4, height));
		actionsPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
		actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.Y_AXIS));
		
		//Add the components
		
		move.setPreferredSize(new Dimension(width / 4, height / 4));
		actionsPanel.add(move);

		pickup.setPreferredSize(new Dimension(width / 4, height / 4));
		actionsPanel.add(pickup);

		attack.setPreferredSize(new Dimension(width / 4, height / 4));
		actionsPanel.add(attack);

		doAction.setPreferredSize(new Dimension(width / 4, height / 4));
		doAction.addActionListener(this);
		actionsPanel.add(doAction);

		//Add the subpanel 
		
		this.add(actionsPanel);
	}
	
	/**
	 * Sets up the directional radio buttons in a sub-panel
	 * @param width
	 * @param height
	 */
	private void directionsPanelSetup(int width, int height) {
		
		//Make sure the radio buttons are in the same group and there is always at least one selected
		ButtonGroup directions = new ButtonGroup();
		directions.add(north);
		directions.add(west);
		directions.add(south);
		directions.add(east);
		north.setSelected(true);
		
		//Set up the subpanel
		
		directionsPanel.setPreferredSize(new Dimension(width / 4, height));
		directionsPanel.setBorder(BorderFactory
				.createTitledBorder("Directions"));
		directionsPanel.setLayout(new BoxLayout(directionsPanel,
				BoxLayout.Y_AXIS));
		
		//Add the components

		north.setPreferredSize(new Dimension(width / 4, height / 4));
		directionsPanel.add(north);

		east.setPreferredSize(new Dimension(width / 4, height / 4));
		directionsPanel.add(east);

		south.setPreferredSize(new Dimension(width / 4, height / 4));
		directionsPanel.add(south);

		west.setPreferredSize(new Dimension(width / 4, height / 4));
		directionsPanel.add(west);

		//Add the subpanel
		
		this.add(directionsPanel);

	}
	
	/**
	 * Creates a new GamePanel
	 * @param size - determines the size of the components inside of the panel
	 * 				 and the panel's dimensions (width=size*2 ,height=size)  
	 * @param host - which host will be connected to via a client
	 * @param port - which port on the host will the client connect to
	 * @throws IOException
	 * @throws RuntimeException
	 */
	public GamePanel(int size, String host, int port) throws IOException,
			RuntimeException {
		try {
			
			//Attempt to open a connection
			client = new GUIClient(host, port, this);
			client.start();

			this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

			canvas.setPreferredSize(new Dimension(size, size));
			this.add(canvas);
			
			actionPanelSetup(2 * size, size);
			directionsPanelSetup(2 * size, size);

		} catch (RuntimeException | IOException e) {
			errorHandler(e);
			// Let the caller decide how to further handle this
			throw e;
		}

	}
	
	/**
	 * Updates the GameCanvas
	 * @param lines
	 */
	public void update(ArrayList<String> lines) {
		canvas.update(lines);
	}
	
	/**
	 * Determines what command should be sent to the server, added to doAction
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		String command = "";
		//Pick up is directionless
		if (pickup.isSelected())
			command = "PICKUP";
		else {
			//Determine whether the action is an attack or a move
			if (attack.isSelected())
				command = "ATTACK ";
			else if (move.isSelected())
				command = "MOVE ";
			//Determine the direction of the action
			if (north.isSelected())
				command += "N";
			if (east.isSelected())
				command += "E";
			if (south.isSelected())
				command += "S";
			if (west.isSelected())
				command += "W";
		}
		//Try to send it to the server
		try {
			client.send(command);
		} catch (IOException ioe) {
			//Clean-up
			errorHandler(ioe);	
		}
	}
	
	/**
	 * Used to close the panel in case of exceptions or if the user decides to
	 */
	@Override public void close(){
		try{
			if(client.isActive())
				client.send("");
		}catch(IOException ioe){
			//The user doesn't really care about this
		}
		doAction.setEnabled(false);
		client.close();
	}

}
