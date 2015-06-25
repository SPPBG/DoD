package server;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Represents the window containing the server panel
 */
public class Server extends JFrame {

	private static final long serialVersionUID = 1L;

	public Server(String map,int size){
		//Add a server panel
		add(new ServerPanel(map,size));
		
		this.setTitle("Dungeon of Doom (Server)");
		
		//Add additional clean-up in case there are still active players when the window is closed
		addWindowListener(new WindowAdapter(){
			@Override public void windowClosing(WindowEvent e){
				//Get all threads
				Set<Thread> threads=Thread.getAllStackTraces().keySet();
				//Politely ask all running threads to terminate
				for(Thread thread:threads)
					if(thread.getState()==Thread.State.RUNNABLE)
						thread.interrupt();
			}
		});
		
		//Some standard options
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
		pack();
	}
	
	/**
	 * Create a new window
	 * @param args
	 */
	static public void main(String[] args){
		final String[] fargs=args;
		SwingUtilities.invokeLater(new Runnable(){
			@Override public void run(){
				switch(fargs.length){
				case 0:
					new Server("maps/Default Map.txt",250);
					break;
				case 1:
					new Server(fargs[0],250);
					break;
				default:
					new Server(fargs[0],Integer.parseInt(fargs[1]));
				}
				
			}
		});
	}
}
