import java.io.IOException;
import java.io.File;
import java.net.*;
import java.util.*;


public class MulticastP2P {

	private MulticastSocket sockData;
	private MulticastSocket sockControl;
	
	private static Vector<fileStruct> fileArray = new Vector<fileStruct>();
	
	/**
	 * Stores information about files
	 */
	private class fileStruct {
		/* The size of the chunks must be 1024 bytes plus protocol headers. 
		 * The chunk must be numbered from 0 to C 
		 * where C is given by int( (S-1)/1024) 
		 * where S is the file's size in bytes. 
		 * The control messages are always strings 
		 * while the data messages are binary packets.  */
		
		// cha
		String fileName;
		long fileSize;
		long totalChunks;
		
		fileStruct(String name, long size, long chunks) {
			fileName = name;
			fileSize = size;
			totalChunks = chunks;
		}
		
		void printStruct() {
			System.out.println("File: " + fileName + "\n\tSize: " + fileSize);
			System.out.println("\tTotalChunks: " + totalChunks);
		}
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		/* testes_temp*/
		/*
		MulticastP2P p2p = new MulticastP2P();
		
		p2p.indexFiles("/home/liliana/Documents", 1024);
		
		for (int i = 0; i!=fileArray.size(); i++) {
			fileArray.get(i).printStruct();
		}
		*/
		
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
	
	/**
	 * Searches directory files and indexes file info in fileIndex. 
	 * @param directory: location of the files to be indexed.
	 * @param chunkSize: default size of the chunks in Bytes.
	 */
	private void indexFiles(String directory, int chunkSize)
	{
		File folder = new File(directory);
	    File[] listOfFiles = folder.listFiles();

	    for (int i = 0; i < listOfFiles.length; i++) {
	    	
	      if (listOfFiles[i].isFile()) {

	    	long size = listOfFiles[i].length();
	    	long numChunks = ((size-1)/chunkSize) +1;
	    	
	        fileStruct newFile = new fileStruct(listOfFiles[i].getName(), size, numChunks);  
	        fileArray.add(newFile);  
	      } 
	    }
	}

}
