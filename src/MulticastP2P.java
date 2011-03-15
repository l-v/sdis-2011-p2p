import java.io.IOException;
import java.net.*;
import java.util.*;


public class MulticastP2P {

	private MulticastSocket sockData;
	private MulticastSocket sockControl;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * Joins the multicast group
	 * 
	 * @param multicastAddress Multicast Group IP Address
	 * @param controlPort
	 * @param dataPort 
	 */
	private void joinGroup(String multicastAddress, int controlPort, int dataPort){

		try {
    	
    		sockControl = new MulticastSocket(controlPort);
    		sockControl.joinGroup(InetAddress.getByName(multicastAddress));
			sockControl.setTimeToLive(1); // TODO ?
			
			sockData = new MulticastSocket(dataPort);
    		sockData.joinGroup(InetAddress.getByName(multicastAddress));
			sockData.setTimeToLive(1); // TODO ?
			
    	
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Sends the searchString to the group and listens for results(?)
	 * @param searchString
	 */
	private void search(String searchString){
		
		
	}

}
