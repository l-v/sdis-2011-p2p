import java.io.IOException;
import java.io.File;
import java.net.*;
import java.util.*;


public class MulticastP2P {
	
	 public static final int MAXID = 9999999;
	 public static final int CHUNKSIZE = 1024;
	 
	 Random randomGenerator; // stores the Random Number Generator
	 
	//MulticastSocket sockData; comented because it has to be createad every single thread for concurrent access
	//MulticastSocket sockControl;
	String currentSearchID; // Saves the current search id. We should not answer searches from ourselves.
	InetSocketAddress controlAddr; // IP and Port for control
	InetSocketAddress dataAddr; // IP and port for data

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
		
		// TODO gerar e guardar sha
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
	 * Saves a search result
	 */
	private class SearchResult {
		String searchID;
		String sha;
		long filesize;
		String filename;
		int peers; // Number of peers that have the file
	}

	
	public MulticastP2P(){
		
		randomGenerator = new Random(); // Generates a random number sequence
		
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
		
		/* //Novo teste
		MulticastP2P p2p = new MulticastP2P();
		
		p2p.controlAddr = new InetSocketAddress("224.0.2.10",8967);
		p2p.dataAddr = new InetSocketAddress("224.0.2.10",8966);
		
		try {
			p2p.search("teste");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
	

	/**
	 * Joins a multicast group and returns a Multicast socket
	 * 
	 * @param addr Multicast Group Address
	 * @return socket to the multicast group
	 * @throws IOException
	 */
	private MulticastSocket joinGroup(InetSocketAddress addr) throws IOException{

		MulticastSocket mSocket = null;

		//controlAddr = new InetSocketAddress(InetAddress.getByName(multicastAddress),controlPort);
		//dataAddr = new InetSocketAddress(InetAddress.getByName(multicastAddress),dataPort);
		
		mSocket = new MulticastSocket(addr.getPort());
		mSocket.joinGroup(addr.getAddress());
		mSocket.setTimeToLive(1); // TODO ?

		return mSocket;

		
	}
	
	private String genSearchID(){
				
		int number = randomGenerator.nextInt(MAXID);
		return "id"  + number;
		
	}
	
	/**
	 * Sends the searchString to the group and listens for results(?)
	 * @param searchString
	 * @throws IOException 
	 * @throws SocketException 
	 */
	private void search(String keywordList) throws IOException{
		
		currentSearchID = genSearchID();
		
		/* Create a new MulticastSocket so we can concurrently read and write
		 * from the multicast group.
		 */
		MulticastSocket mSocket = joinGroup(controlAddr);

		String searchString = "SEARCH" + " " + currentSearchID + " " 
								+ keywordList; // Generates search string.
		
		DatagramPacket searchPacket = null;
		
		// Creates the searchPacket and sends it.
		searchPacket = new DatagramPacket(
				searchString.getBytes(), searchString.length(),controlAddr);
		
		mSocket.send(searchPacket);
		
		// TODO While para guardar e apresentar resultados
		byte[] buf = new byte[512];
		DatagramPacket receivePacket = new DatagramPacket(buf,512);
		mSocket.receive(receivePacket);
		String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
		System.out.println(received);
		
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
