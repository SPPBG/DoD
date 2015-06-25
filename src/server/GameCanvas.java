package server;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Copied over from the Client part of the project
 * with some modifications, such as that tiles are always squares
 * with a fixed size and the canvas changes size to hold them.
 * This GameCanvas should be used in a JScrollPane
 */
public class GameCanvas extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;

	int tileSize=64;
	
	private char[][] view = null;
	private int rows = 0, cols = 0;

	private BufferedImage wall, gold, player, sword, armor, potion, latern,
			exit,floor;

	public GameCanvas(){
		this(64);
	}
	
	public GameCanvas(int newTileSize) {
		super();
		tileSize=newTileSize;
		// Attempt to load the files containing the images
		try {
			wall = ImageIO.read(new File("img/wall.png"));
			gold = ImageIO.read(new File("img/gold.png"));
			player = ImageIO.read(new File("img/player.png"));
			sword = ImageIO.read(new File("img/sword.png"));
			armor = ImageIO.read(new File("img/armor.png"));
			potion = ImageIO.read(new File("img/potion.png"));
			latern = ImageIO.read(new File("img/latern.png"));
			exit = ImageIO.read(new File("img/exit.png"));
			floor = ImageIO.read(new File("img/floor.png"));
		} catch (IOException e) {
			// Do nothing, if they are missing someone has messed with the game
			// files
		}
	}

	/**
	 * Sets the view( the character matrix) which will be used to visualize the
	 * map.
	 * 
	 * @WARNING This method assumes it is handed date in a correct format
	 * @param lines
	 *          
	 */
	public void update(char[][] newView) {
		view=newView;
		rows=newView.length;
		cols=newView[0].length;
		setPreferredSize(new Dimension(cols*tileSize,rows*tileSize));
		revalidate();
		repaint();
	}

	/**
	 * Calculates the tile size with respect to the size of the canvas
	 * 
	 * @return tile size
	 */

	public int getTileSize() {
		return tileSize;
	}

	/**
	 * Returns the x offset (leftmost point) of tiles at a given column
	 * 
	 * @param col
	 * @return x offset
	 */
	public int getTileXOffset(int col) {
		return getTileSize() * col;
	}

	/**
	 * Returns the y offset (uppermost point) of tiles at a given row
	 * 
	 * @param row
	 * @return y offset
	 */
	public int getTileYOffset(int row) {
		return getTileSize() * row;
	}

	/**
	 * Draws a floor at a given tile
	 * 
	 * @param g
	 * @param row
	 * @param col
	 */
	protected void paintFloor(Graphics g, int row, int col) {
		g.drawImage(rescale(floor), col * getTileSize(), row * getTileSize(),
				null);
	}

	/**
	 * Draws a wall at a given tile
	 * 
	 * @param g
	 * @param row
	 * @param col
	 */
	protected void paintWall(Graphics g, int row, int col) {
		g.drawImage(rescale(wall), col * getTileSize(), row * getTileSize(),
				null);
	}

	/**
	 * Draws a coin at a given tile
	 * 
	 * @param g
	 * @param row
	 * @param col
	 */
	protected void paintGold(Graphics g, int row, int col) {
		paintFloor(g, row, col);
		g.drawImage(rescale(gold), col * getTileSize(), row * getTileSize(),
				null);
	}

	/**
	 * Draws a player at a given tile
	 * 
	 * @param g
	 * @param row
	 * @param col
	 */
	protected void paintPlayer(Graphics g, int row, int col) {
		paintFloor(g, row, col);
		g.drawImage(rescale(player), col * getTileSize(),
				row * getTileSize(), null);
	}

	/**
	 * Draws a sword at a given tile
	 * 
	 * @param g
	 * @param row
	 * @param col
	 */
	protected void paintSword(Graphics g, int row, int col) {

		paintFloor(g, row, col);
		g.drawImage(rescale(sword), col * getTileSize(), row * getTileSize(),
				null);
	}

	/**
	 * Draws an armor at a given tile
	 * 
	 * @param g
	 * @param row
	 * @param col
	 */
	protected void paintArmor(Graphics g, int row, int col) {
		paintFloor(g, row, col);
		g.drawImage(rescale(armor), col * getTileSize(), row * getTileSize(),
				null);
	}

	/**
	 * Draws a potion at a given tile
	 * 
	 * @param g
	 * @param row
	 * @param col
	 */
	protected void paintHealthPotion(Graphics g, int row, int col) {
		paintFloor(g, row, col);
		g.drawImage(rescale(potion), col * getTileSize(),
				row * getTileSize(), null);
	}

	/**
	 * Draws an exit at a given tile
	 * 
	 * @param g
	 * @param row
	 * @param col
	 */
	protected void paintExit(Graphics g, int row, int col) {
		g.drawImage(rescale(exit), col * getTileSize(), row * getTileSize(),
				null);
	}

	/**
	 * Draws a latern at a given tile
	 * 
	 * @param g
	 * @param row
	 * @param col
	 */
	protected void paintLatern(Graphics g, int row, int col) {
		paintFloor(g, row, col);
		g.drawImage(rescale(latern), col * getTileSize(), row * getTileSize(), null);
	}

	/**
	 * Determines what needs to be drawn at a given tile
	 * 
	 * @param g
	 * @param row
	 * @param col
	 */
	protected void paintTile(Graphics g, int row, int col) {
		try {

			switch (view[row][col]) {
			case '.':
				paintFloor(g, row, col);
				break;
			case 'P':
				paintPlayer(g, row, col);
				break;
			case '#':
				paintWall(g, row, col);
				break;
			case 'G':
				paintGold(g, row, col);
				break;
			case 'A':
				paintArmor(g, row, col);
				break;
			case 'S':
				paintSword(g, row, col);
				break;
			case 'H':
				paintHealthPotion(g, row, col);
				break;
			case 'E':
				paintExit(g, row, col);
				break;
			case 'L':
				paintLatern(g, row, col);
				break;
			}
		} catch (RuntimeException e) {
		}
	}

	/**
	 * Paints the entire component
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(new Color(0));
		g.fillRect(0, 0, getWidth(), getHeight());
		for (int row = 0; row < rows; row++)
			for (int col = 0; col < cols; col++)
				paintTile(g, row, col);
	}

	/**
	 * Rescales the image according to tile size
	 * 
	 * @param i - image to be resized
	 * @return - resized image
	 */
	private Image rescale(Image i) {
		Image rescaled = i.getScaledInstance(getTileSize(), getTileSize(),
				Image.SCALE_DEFAULT);
		return rescaled;
	}
}
