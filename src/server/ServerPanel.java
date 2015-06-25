package server;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * Visualizes ServerLogic output via a Swing JPanel
 */
public class ServerPanel extends JPanel implements ServerUI{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//A text field for inputing the port the server will listen on
	private JTextField portField=new JTextField(5);
	//A label displaying the IP
	private JLabel ipLabel=new JLabel();
	//Radio buttons determining the server's mode of operation
	private JRadioButton 
		serve=new JRadioButton("Serve"),
		noServe=new JRadioButton("Don't serve");
	
	//A canvas responsible for visualizing the map
	private GameCanvas canvas=new GameCanvas();
	
	//Displays server output and input from clients
	private JEditorPane log=new JEditorPane();
	
	//The server which this panel will visualize
	private ServerLogic server=null;
	
	/**
	 * Construct a new server panel
	 * @param mapFilePath
	 * @param size - determines width and height, (width=size*2.5 , height=size)
	 */
	public ServerPanel(String mapFilePath, int size){
		try{
			//Create a new server from a map file
			server=new ServerLogic(mapFilePath,this);
			
			setLayout(new FlowLayout());
			
			//Set default port
			portField.setText("60000");
			
			
			//Set up the initial map view
			updateMapView(server.getGame().getMapView());
			
			//Set up a scroll pane for the map canvas ( the map might be too big)
			JScrollPane canvasScroll=new JScrollPane(canvas);
			canvasScroll.setPreferredSize(new Dimension(size,size));
			add(canvasScroll);
			
			//Set up the log
			log.setEditable(false);
			log.setContentType("text/html");
			log.setPreferredSize(new Dimension(size,size));
			
			//Set up a scroll pane for the log
			JScrollPane logScroll=new JScrollPane(log);
			logScroll.setPreferredSize(new Dimension(size,size));
			add(logScroll);
			
			//A panel containing the IP label, the radio buttons and the text field for the port
			JPanel options=new JPanel();
			options.setPreferredSize(new Dimension(size/2,size));
			options.setLayout(new BoxLayout(options,BoxLayout.Y_AXIS));
			
			//Add the IP label
			ipLabel.setText("IP: "+server.getIP());
			options.add(ipLabel);
			
			//Add the port text field
			portField.setColumns(5);
			portField.setBorder(BorderFactory.createTitledBorder("Port"));
			options.add(portField);
			
			//Set up the radio buttons
			ButtonGroup group=new ButtonGroup();
			group.add(serve);
			group.add(noServe);
			noServe.setSelected(true);
			noServe.setEnabled(true);
			
			//For when "Serve" is pressed
			serve.addActionListener(new ActionListener(){
				@Override public void actionPerformed(ActionEvent e){
					try{
						server.setPort(Integer.valueOf(portField.getText()));
						noServe.setEnabled(true);
						serve.setEnabled(false);
						//Make sure we can't type to the port while listening
						portField.setEnabled(false);
						
						//Begin listening
						server.startListening();
						//Make sure we can't click on this twice
						
					}catch(RuntimeException re){
						handleError(re);
					}
				}
			});
			
			//For when "Don't serve" is pressed
			noServe.addActionListener(new ActionListener(){
				@Override public void actionPerformed(ActionEvent e){
					serve.setEnabled(true);
					//Stop listening
					server.stopListening();
					//Enable the user to change the port
					portField.setEnabled(true);
					//Make sure we can't click on this twice
					noServe.setEnabled(false);
				}
			});
			
			//Add the buttons
			options.add(serve);
			options.add(noServe);
			
			//Add the entire panel
			add(options);
			
		}catch(Exception e){
			//We can't create a server, because the map file is missing, so we quit
			handleError(e);
			System.exit(0);
		}
	}
	
	/**
	 * Handles exceptions by displaying them
	 */
	@Override public void handleError(Throwable error){
		JOptionPane.showMessageDialog(this,error.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Handles messages by displaying them in the log
	 */
	@Override public void handleMessage(String message){
		Document doc=log.getDocument();
		try{
			doc.insertString(doc.getLength(),message+"\n",null);
		}catch(BadLocationException ble){
			//Never occurs
		}
	}
	
	/**
	 * Updates the map view
	 */
	@Override public void updateMapView(char[][] mapView){
		canvas.update(mapView);	
	}
}
