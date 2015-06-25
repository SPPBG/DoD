package server;

/**
 * Specifies the methods which a class should
 * provide if it must display the output of ServerLogic.
 * Provided for flexibility ( it can implemented via a GUI or command line)
 */
public interface ServerUI {
	public void handleError(Throwable error);
	public void handleMessage(String message);
	public void updateMapView(char[][] mapView);
}
