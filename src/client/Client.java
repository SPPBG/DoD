package client;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import client.gui.GamePanel;
/**
 * Not to be mistaken for a derivative of AbstractClient
 * This class is actually responsible for creating the window containing the
 * menu and the GamePanel
 */
public class Client extends JFrame implements ActionListener{

	private static final long serialVersionUID = 3807055865004985517L;
	
	private JPanel menuPane=new JPanel();
	//Used to input the host for the game (default: localhost)
	private JTextField hostname=new JTextField("localhost",16);
	//Used to create a new GamePanel from the 2 text fields' values
	private JTextField port = new JTextField("60000", 5);
	//Connects to a game server
	private JButton connect = new JButton("Connect");
	//Closes the connection to the server
	private JButton close = new JButton("Close");
	
	//The game panel which contains the connection to the current game server
	private GamePanel currentGame=null;
	
	//Determines the size with which the GamePanel will be created
	private int panelSize=400;
	
	/**
	 * Creates a new game window
	 * @param newPanelSize The size of the game panel once it is created
	 */
	public Client(int newPanelSize) {

		panelSize=newPanelSize;
				
		getContentPane().setLayout(new BoxLayout(getContentPane(),BoxLayout.Y_AXIS));
		
		//Set-up the menu pane
		menuPane.setLayout(new FlowLayout());
		
		//Set up the buttons
		connect.setName("Connect");
		close.setName("Close");
		close.setEnabled(false);
		//Add the action listener to the buttons
		connect.addActionListener(this);
		close.addActionListener(this);
		
		//Titled borders for every text field
		Border border=BorderFactory.createTitledBorder("Hostname");
		hostname.setBorder(border);
		
		border=BorderFactory.createTitledBorder("Port");
		port.setBorder(border);
		
		//Add the components
		menuPane.add(hostname);
		menuPane.add(port);
		menuPane.add(connect);
		menuPane.add(close);
		
		//Add the menu pane
		this.add(menuPane);
		
		this.setTitle("Dungeon of Doom (Client)");
		
		//Clean-up the GamePanel if necessary when closing
		this.addWindowListener(new WindowAdapter(){
			@Override public void windowClosing(WindowEvent e){
				if(currentGame!=null)
					currentGame.close();
			}
		});

		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		//Visualize 
		this.setVisible(true);
		pack();

	}
	
	@Override public void actionPerformed(ActionEvent e){
		JButton source=(JButton)e.getSource();
		//Determine which of the 2 buttons was clicked
		switch(source.getName()){
			//Connect was clicked
			case "Connect":
				try {
					//Try to create a new panel
					GamePanel gp = new GamePanel(panelSize, hostname.getText(),
							Integer.valueOf(port.getText()));
					currentGame=gp;
					//Add the game panel and resize the frame accordingly
					add(gp);
					pack();
					//Disable the connect button and enable the close button
					connect.setEnabled(false);
					close.setEnabled(true);
				} catch (NumberFormatException error) {
					//In case of invalid port number
					JOptionPane.showMessageDialog(Client.this,
							"Port number contains non-digits!", "Error",
							JOptionPane.ERROR_MESSAGE);
				} catch (IOException | RuntimeException error) {
					// No need to do anything - this is handled by the GamePanel
				}
				break;
			//Close was clicked
			case "Close":
				//Remove the game panel (if it exists) and resize accordingly
				if(currentGame!=null){
					currentGame.close();
					remove(currentGame);
					currentGame=null;
					pack();
				}
				//Enable the 
				connect.setEnabled(true);
				close.setEnabled(false);
				break;
		}
	}
	
	public static void main(String[] args) {
		final String[] fargs=args;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if(fargs.length==0)
					new Client(240);
				else 
					new Client(Integer.parseInt(fargs[0]));
			}
		});
	}
}
