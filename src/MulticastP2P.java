import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.net.*;
import java.nio.channels.Selector;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.swing.DefaultListModel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;


public class MulticastP2P {

	public static final int MAXID = 9999999;
	public static final int CHUNKSIZE = 1024;
	public static final int HEADERSIZE = 64;
	
	DefaultListModel listModel;
	JTextArea console;
	
	Random randomGenerator; // stores the Random Number Generator
	SearchResults currentSearchResults; // stores the results from the current search
	String currentSearchID; // Saves the current search id. We should not answer searches from ourselves.
	InetSocketAddress controlAddr; // IP and Port for control
	InetSocketAddress dataAddr; // IP and port for data
	DownloadingFile currentDownload;
	
	private  Vector<fileStruct> fileArray;
	

	
	/**
	 * Constructor
	 */
	public MulticastP2P(){
		randomGenerator = new Random(); // Generates a random number sequence
		currentSearchResults = null; // only created when after a search
		fileArray = new Vector<fileStruct>();
		listModel = new DefaultListModel();
		console = new JTextArea();
	}

	void consolePrint(final String str){
		Runnable doWorkRunnable = new Runnable() {
		    public void run() { 
				console.append(str + "\n");
				console.setCaretPosition(console.getDocument().getLength());
		    }
		};
		SwingUtilities.invokeLater(doWorkRunnable);

	}

	/**
	 * Stores the results from the current search
	 */
	private class SearchResults{
		
		Vector<SearchResult> results;
		
		SearchResults(){
			listModel.clear();
			results = new Vector<SearchResult>();
		}
		
		/**
		 * Inserts a search result and returns the number of peers that have that file
		 * @param sha
		 * @param filesize
		 * @param filename
		 * @return
		 */
		int insertResult(String sha, long filesize, String filename){
			final SearchResult sr = new SearchResult(sha,filesize,filename);
			
			final int resultIndex = results.indexOf(sr);
			if(resultIndex != -1){ // if Object exits
				results.elementAt(resultIndex).addPeer();
				// swing makes us do this when working with threads because its not thread safe
				// http://java.sun.com/products/jfc/tsc/articles/threads/threads1.html
				Runnable doWorkRunnable = new Runnable() {
				    public void run() { 
				    	listModel.set(resultIndex,results.elementAt(resultIndex).toString());
				    }
				};
				SwingUtilities.invokeLater(doWorkRunnable);
				return results.elementAt(resultIndex).peers;
			}
			else{
				results.add(sr);
				Runnable doWorkRunnable = new Runnable() {
				    public void run() { 
						listModel.addElement(sr.toString());
				    }
				};
				SwingUtilities.invokeLater(doWorkRunnable);
				return 1;
			}
		}
		@Override
		public String toString(){
			Iterator<SearchResult> it = results.iterator();
			int  i = 0;
			String s = "";
			while(it.hasNext()){
				s += ( i + "\t" + it.next().toString() + "\n");
				i++;
			}
			return s;
		}
	}
	


	/**
	 * Stores information about files
	 */
	private class fileStruct {
		String sha;
		String fileName;
		long fileSize;
		long totalChunks;
		String completePath;

		fileStruct(String hash, String name, long size, long chunks, String path) {
			sha = hash;
			fileName = name;
			fileSize = size;
			totalChunks = chunks;
			completePath = path;
		}

		void printStruct() {
			consolePrint("File: " + fileName + "\n\tSize: " + fileSize);
			consolePrint("\tTotalChunks: " + totalChunks);
			consolePrint("\tHashValue (sha): " + sha);
		}
	}


	/**
	 * Saves a search result
	 */
	private class SearchResult {
		String sha;
		long filesize;
		String filename;
		int peers; // Number of peers that have the file
		
		/**
		 * Constructor
		 */
		SearchResult(String sha, long filesize, String filename){
			this.sha = sha;
			this.filesize = filesize;
			this.filename = filename;
			peers = 1;
		}
		
		/**
		 * Adds a peer
		 */
		public void addPeer(){
			this.peers++;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((sha == null) ? 0 : sha.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SearchResult other = (SearchResult) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (sha == null) {
				if (other.sha != null)
					return false;
			} else if (!sha.equals(other.sha))
				return false;
			return true;
		}
		private MulticastP2P getOuterType() {
			return MulticastP2P.this;
		}

		@Override
		public String toString() {
			return filename + " | " + filesize + "B | " + peers  + " | " + sha ;
		}
		
	}



	/**
	 * @param args
	 * 
	 * args = <>
	 */
	
	//TODO -p Path -i IP -c CONTROLPORT -d DATAPORT
	
	public void start(){
		
		/* Check that native byte order is little_endian */
		/* 
		 * ByteBuffer.order(ByteOrder bo)
         *      Modifies this buffer's byte order.
		 */
		 // consolePrint((java.nio.ByteOrder.nativeOrder().toString()));
		 


		// Index files directory
		try {
			indexFiles("./files/", CHUNKSIZE); //TODO verificar se dir existe
	 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Sets udp group address
		controlAddr = new InetSocketAddress("224.0.2.10",8967); //TODO
		dataAddr = new InetSocketAddress("224.0.2.10",8966);
		
		
//		// Lançar thread pesquisa
//		new Thread() {
//			public void run() {			
//				try {
//					for(;;){
//						search();
//					}
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		}.start();
		
		// Lançar thread resposta
		new Thread() {
			public void run() {			
				try {
					searchReply();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
		
		// Lancar thread de envio do ficheiro
		new Thread() {
			public void run() {
				try {
					while (true) {
						sendFile();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();		
		
		
		/* testes_temp*/
		/*
		try {
			p2p.indexFiles("/home/liliana/Documents", CHUNKSIZE);

			consolePrint(p2p.intToByte(12));
			consolePrint("\n"+p2p.byteToInt(p2p.intToByte(999999999)));
			
			for (int i = 0; i!=fileArray.size(); i++) {
				fileArray.get(i).printStruct();
			}
	
			 consolePrint("Presente? " + p2p.hasFile("scraps"));
			 consolePrint("Path: " + fileArray.get( p2p.hasFile("scraps") ).completePath + "\n");
			 
			 Vector<byte[]> chunkResult = getChunks(fileArray.get( p2p.hasFile("boletim.html")));
			 
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
		mSocket.setTimeToLive(5); // TODO ?

		return mSocket;


	}

	private String genSearchID(){

		int number = randomGenerator.nextInt(MAXID);
		return "id"  + number;

	}

	/**
	 * Sends the searchString to the group and listens for results(?)
	 * @throws IOException 

	 * @throws IOException 
	 * @throws SocketException 
	 */
	void search(String keywordList){

		currentSearchID = genSearchID();
		String ownID = new String(currentSearchID); // copies the string, probably not needed
		currentSearchResults = new SearchResults();
		
		/* Create a new MulticastSocket so we can concurrently read and write
		 * from the multicast group.
		 */
		MulticastSocket mSocket = null;
		try {
			mSocket = joinGroup(controlAddr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String searchString = "SEARCH" + " " + currentSearchID + " " 
		+ keywordList; // Generates search string.
		consolePrint("OUT: " + searchString);

		DatagramPacket searchPacket = null;

		// Creates the searchPacket and sends it.
		try {
			searchPacket = new DatagramPacket(
					searchString.getBytes(), searchString.length(),controlAddr);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			mSocket.send(searchPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		while (currentSearchID.equals(ownID) ){ // Stops search when ID changes
			byte[] buf = new byte[512];
			DatagramPacket receivePacket = new DatagramPacket(buf,512);
			try {
				mSocket.receive(receivePacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
			// Splitting the answer in tokens
			StringTokenizer st = new StringTokenizer(received);
			//consolePrint(received);
			
			if(st.nextToken().equalsIgnoreCase("FOUND")){ // Only parses FOUNDs
				if (st.nextToken().equals(currentSearchID)){ // Compares to currentSearchID
					consolePrint("IN: " + received);
					String receivedSha = st.nextToken();
					long receivedSize = Long.parseLong(st.nextToken());
					
					// now to get the filename
					String receivedFilename = st.nextToken();
					while(st.hasMoreTokens()){
						receivedFilename = receivedFilename + " " + st.nextToken();
					}
					// Inserts the new search result in currentSearchResults.
					currentSearchResults.insertResult(receivedSha, receivedSize, receivedFilename);
					
				}
				
			}
		}
		

	}


	
	/***
	 * Constructs the FOUND message
	 * 
	 * @param searchID
	 * @param file
	 * @return String: NULL if the file is not found; FOUND message otherwise. 
	 */
	private String foundMessage(fileStruct file, String searchID) {
		
		String foundString = "FOUND " + searchID + " " + file.sha + " " + file.fileSize + " " + file.fileName;
		return foundString;
	}
	
	
	
	/**
	 * Searches directory files and indexes file info in fileIndex.
	 *  
	 * @param directory: location of the files to be indexed.
	 * @param chunkSize: default size of the chunks in Bytes.
	 * @throws IOException 
	 */
	void indexFiles(String directory, int chunkSize) throws IOException
	{
		File folder = new File(directory);
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {

			if (listOfFiles[i].isFile()) {

				long size = listOfFiles[i].length();
				long numChunks = ((size-1)/chunkSize) +1;
				String hashString = null;
				
				/* compute hash values */
				try {
					hashString = SHACheckSum(listOfFiles[i].getAbsolutePath());
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				fileStruct newFile = new fileStruct(hashString, listOfFiles[i].getName(), size, numChunks, listOfFiles[i].getAbsolutePath());  
				fileArray.add(newFile);  
			} 
		}
	}
	
	/***
	 * Searches file array by hash value (sha)
	 * 
	 * @param sha
	 * @return
	 */
	private fileStruct getFileByHash(String sha) {
		
		for (int i=0; i!=fileArray.size(); i++) {
			fileStruct file = fileArray.get(i);
			
			if(file.sha.equals(sha))
				return file;
		}
		
		return null;
	}
 
	/***
	 * Converts integer values to byte[].
	 * 
	 * @param integer
	 * @return 
	 */
	private byte[] longToByte(long number) {
        byte[] byteValue =  {
        		(byte)(number >>> 56),
        		(byte)(number >>> 48),
        		(byte)(number >>> 40),
        		(byte)(number >>> 32),
                (byte)(number >>> 24),
                (byte)(number >>> 16),
                (byte)(number >>> 8),
                (byte)number};
        
        return byteValue;
	}
	
	/***
	 * Converts byte[] values to integer. 
	 * 
	 * @param byteValue
	 * @return 
	 */
	private long byteToLong(byte[] byteValue) {
		long number = ((byteValue[0]& 0xFF) << 56)
						+ ((byteValue[1]& 0xFF) << 48)
						+ ((byteValue[2]& 0xFF) << 40)
						+ ((byteValue[3]& 0xFF) << 32)
						+ ((byteValue[4]& 0xFF) << 24)
						+ ((byteValue[5] & 0xFF) << 16) 
						+ ((byteValue[6] & 0xFF) << 8) 
						+ (byteValue[7] & 0xFF);
		
		return number;
	}


	
	/***
	 * Divide um ficheiro em chunks. (chunks devem ser armazenados de forma ORDENADA!)
	 * 
	 * @param fileReq: ficheiro pedido
	 * @return Vector<byte>: vector com os varios chunks do ficheiro.
	 * @throws IOException
	 */
	private Vector<byte[]> getChunks(fileStruct fileReq) throws IOException {

		Vector<byte[]> chunkVector = new Vector<byte[]>();
	
		FileInputStream file = new FileInputStream(fileReq.completePath);
		long fLength = fileReq.fileSize;
		long bytesRead = 0;
		long chunkCounter = 0;  
		
		while (bytesRead != fLength) {
			// TODO falta testar
			byte[] fChunk =  new byte[CHUNKSIZE]; 
			long bytes = file.read(fChunk);  
			bytesRead += bytes;  
			
			
			/* add chunk header */
			byte[] fileID = new byte[32];
			byte[] chunkNumber = new byte[8];
			byte[] reserved = new byte[24];
			
			try {
				fileID = SHACheckSumBytes(fileReq.completePath); //32bytes for 1st header part
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
			
			System.arraycopy(longToByte(chunkCounter), 0, chunkNumber, 0, longToByte(chunkCounter).length);
			//System.out.println(":::: " + byteToLong(chunkNumber));
			
			
			/* concatenate byte arrays with header */
			byte[] header = new byte[64];
			
			System.arraycopy(fileID, 0, header, 0, fileID.length);
			System.arraycopy(chunkNumber, 0, header, 32, chunkNumber.length);
			System.arraycopy(reserved, 0, header, 40, reserved.length);
			
		/*	
			System.out.println("-> " + fileID.length);
			System.out.println("fileID[254]:" + fileID[31] + " -ID255: " + chunkNumber[0]);
			System.out.println("header length: " + header.length + " -header[30]:" + header[31] + " -31:" + header[32]);
			*/
			byte[] finalChunk = new byte[64 + CHUNKSIZE];
			System.arraycopy(header, 0, finalChunk, 0, 64);
			System.arraycopy(fChunk, 0, finalChunk, 64, fChunk.length);
			
			/* add chunk to vector */
			chunkVector.add(finalChunk);
			chunkCounter++;
		}
		
		if (chunkVector.size() != fileReq.totalChunks) {
			consolePrint("\nWarning: number of chunks generated is not the expected.");
		}
		/*
		consolePrint("File: " + fileReq.fileName);
		consolePrint("fLength: " + fLength);
		System.out.print("Bytes total: " + bytesRead + "\tnChunks: " + chunkVector.size());
		*/
		
		file.close();
		return chunkVector;
	}
	
	


	/***
	 * Checksum method (returns byte[] value)
	 * 
	 * @param filePath
	 * @return byte[]
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public byte[] SHACheckSumBytes(String fileName) throws NoSuchAlgorithmException, IOException{

		MessageDigest md = MessageDigest.getInstance("SHA-256");	 
		FileInputStream fis = new FileInputStream(fileName);
		
		
		byte[] dataBytes = new byte[1024];

		int nread = 0;
		while ((nread = fis.read(dataBytes)) != -1) {
			md.update(dataBytes, 0, nread);
		};

		byte[] mdbytes = md.digest();
		return mdbytes;
	} 
	
	/***
	 * CheckSum method (returns String value)
	 * 
	 * @param fileName
	 * @return String
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public String SHACheckSum(String fileName) throws NoSuchAlgorithmException, IOException {
		
		byte[] mdbytes = SHACheckSumBytes(fileName);
		
		//convert the byte to hex format
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < mdbytes.length; i++) {
			sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		} 

		/*
		consolePrint("Hex format : " + sb.toString());
		consolePrint("Hex format length : " + sb.toString().length());
		consolePrint("MD length : " + mdbytes.length);
		*/ 
		
		String hashString = sb.toString();
		return hashString;
	}
	
	
	void searchReply() throws IOException{

		// Joins multicast group and creates socket
		MulticastSocket mSocket = joinGroup(controlAddr);

		byte[] buf = new byte[512];

		while(true) {
			DatagramPacket receivePacket = new DatagramPacket(buf,512);
			mSocket.receive(receivePacket);
			String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
			
			//consolePrint(">: "+ received);
			
			StringTokenizer st = new StringTokenizer(received);

			if(st.nextToken().equalsIgnoreCase("SEARCH")){ // Only parses SEARCHs
				
				
				String receivedSearchID = st.nextToken();
				

				if (!receivedSearchID.equals(currentSearchID)){ // Compares to currentSearchID
					consolePrint("IN: "+ received);
					// now to get the filename
					String receivedKeywords = st.nextToken();
					while(st.hasMoreTokens()){
						receivedKeywords = receivedKeywords + " " + st.nextToken();
					}

					// We search for the keywords
					for (int i=0; i!=fileArray.size(); i++) {
						fileStruct fs = fileArray.get(i);
						if (fs.fileName.toLowerCase().contains(receivedKeywords.toLowerCase())){
							String answer = foundMessage(fs, receivedSearchID); // Creates the Answer
							DatagramPacket answerPacket = new DatagramPacket(
									answer.getBytes(), answer.length(),controlAddr);
							mSocket.send(answerPacket);
							consolePrint("OUT: "+ answer);

						}
					}
				}
			}
		}
	}
	
	/***
	 * Sends requested file through data port.
	 * 
	 * @throws IOException
	 */
	void sendFile() throws IOException {
		
		// Joins multicast group and creates socket
		MulticastSocket mSocket = joinGroup(controlAddr);
	
		byte[] buf = new byte[512];
		DatagramPacket receivePacket = new DatagramPacket(buf,512);
		mSocket.receive(receivePacket);
		String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
	
		final StringTokenizer st = new StringTokenizer(received); // TODO in theory, no conflicts should emerge - still to test though.
		

		if(st.nextToken().equalsIgnoreCase("GET")){ // Only parses GETs
			consolePrint("IN: "+ received);
			
			// Launch send thread for each GET detected // TODO and test
			new Thread() {
				public void run() {

					String fileID = st.nextToken();

					
					// get chunk numbers
					String chunks = st.nextToken();
					Vector<Long> chunksReq = new Vector<Long>(); /* vector with chunk numbers requested */
					consolePrint("chunks requested: " + chunks);
					 

					/* chunks format: a-b; a; a,b,c,d*/ 
					if (chunks.indexOf("-") != -1) {
						
						String[] firstLast = chunks.split("-");
						
						for (long i=Long.parseLong(firstLast[0]); i<=Long.parseLong(firstLast[1]); i++) {
							chunksReq.add(i);
						}
					} 
					else if (chunks.indexOf(",") != -1) {
						
						StringTokenizer cToken = new StringTokenizer(chunks, ",");
						
						while(cToken.hasMoreTokens()) {
							chunksReq.add(Long.parseLong(cToken.nextToken()));
						}
					}
					else {
						chunksReq.add(Long.parseLong(chunks));
					}
					
					// sends chunks requested
					try {
						sendChunks(fileID, chunksReq);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}.start();		
		}
	}
	
	
	/***
	 * Sends requested chunks through data port 
	 * 
	 * @param sha
	 * @param chunkNumbers
	 * @throws IOException
	 */
	void sendChunks(String sha, Vector<Long> cNumbers) throws IOException { // TODO testing
		
		fileStruct fReq= getFileByHash(sha);
		Vector<byte[]> chunkVector= getChunks(fReq);
		final Vector<Long> chunkNumbers = cNumbers;
		
		// Joins multicast group and creates socket
		MulticastSocket mSocket = joinGroup(dataAddr);
		DatagramPacket sendPacket = null;
		
		Random randGenerator = new Random();
		


		// Launch thread that verifies for repeated chunks  // TODO test and check if no sync problems occur
		Thread checkRepeated = new Thread()  {
			public void run() {
				while(true) {
					// Joins multicast group and creates socket
					MulticastSocket mSocket;
					try {
						mSocket = joinGroup(dataAddr);

						byte[] buf = new byte[512];
						DatagramPacket cPacket = new DatagramPacket(buf,512);
						mSocket.receive(cPacket);

						byte[] chunkData = cPacket.getData();

						// Gets the chunk number
						byte[] chunkNumber = new byte[8];
						System.arraycopy(chunkData, 32, chunkNumber, 0, 8);

						long packetNumber = byteToLong(chunkNumber);
						if (chunkNumbers.contains(packetNumber)) {
							chunkNumbers.remove(packetNumber);
						}				

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		
		checkRepeated.start();
		
		//TODO erase between lines: just for testing
		/*for (int i=0; i!=cNumbers.size(); i++) {
			System.out.println("->" + cNumbers.get(i));
		}
		
		Vector<Integer> tempNumbers = new Vector<Integer>();
		for (int i=0; i!= chunkVector.size(); i++) {
			tempNumbers.add(i);
		}
		/////////////////*/
		
		while(!chunkNumbers.isEmpty()) {
			
			int randChunk = randGenerator.nextInt(chunkNumbers.size()); //TODO test of int/long doesn't give problems
			
			long chunkChosen = chunkNumbers.get(randChunk);
			byte[] chunk = chunkVector.get((int)chunkChosen);
			
			sendPacket = new DatagramPacket(
					chunk, chunk.length,dataAddr);
			
			//consolePrint("DEBUG: Sent Data Packet.");
			//System.out.println("cNumb: " + tempNumbers.get(randChunk));
			mSocket.send(sendPacket);
			chunkNumbers.remove(randChunk);
			//tempNumbers.remove(randChunk); //TODO erase test lines
			
			// To avoid network flood
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		checkRepeated.interrupt(); //TODO check if needed or if destroyed automatically
		
		
	}
	
	/**
	 * sends a GET message to start receiving files
	 */
	void getFile(int choice){
		SearchResult sr = currentSearchResults.results.elementAt(choice);
		if(sr!= null){
			long chunks = (int) ((sr.filesize-1)/CHUNKSIZE)+1; // calculates total chunks( max chunk + 1)
			//String getStr = "GET " + sr.sha + " 0-" + (chunks-1); // creates the message
			String getStr = null;
			
			DownloadingFile newFile = new DownloadingFile(chunks,sr.filename,sr.sha); // creates the structure to save the downloading file
			MulticastSocket dataSocket = null;
			MulticastSocket mSocket = null;
			DatagramPacket getPacket = null;

			try {
				dataSocket = joinGroup(dataAddr); // Joins the data group to receive the files;
				mSocket = joinGroup(controlAddr); // Joins the control group to send the get command;
				dataSocket.setSoTimeout(500); // Makes the socket timeout after 500 ms
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// Waits for chunks
			byte[] buf = new byte[CHUNKSIZE+HEADERSIZE];
			DatagramPacket dataPacket = new DatagramPacket(buf,CHUNKSIZE+HEADERSIZE);
			try {	
				do {
					
					/* 
					 * Get some chunks if the chunk queue is empty or enough time has passed since
					 * the last added chunk.
					 * */
					long elapsedTime = System.currentTimeMillis()-newFile.timeLastAdded;
					if(((newFile.requestedChunks <= 0) || (elapsedTime > 1000)) && !newFile.isDone()){
						getStr = "GET " + sr.sha + " "+ newFile.getSome(); // creates the message
						getPacket = new DatagramPacket(
								getStr.getBytes(), getStr.length(),controlAddr);
						mSocket.send(getPacket);
						consolePrint("OUT: "+ getStr);
					}
					
					try{
					dataSocket.receive(dataPacket);
					}catch (SocketTimeoutException e){
						consolePrint("Download stalled: received no data.");
//						getStr = "GET " + sr.sha + " "+ newFile.missingStr(); // creates the message
//						getPacket = new DatagramPacket(
//								getStr.getBytes(), getStr.length(),controlAddr);
//						mSocket.send(getPacket);
					}
					
					byte[] receivedData = dataPacket.getData();
					
					if(receivedData != null){ // only if we received data
						// registers chunk camps
						
						//Does not work.
						/*
						byte[] cSha = new byte[32];
						System.arraycopy(receivedData, 0, cSha, 0, 32);
						String sha = cSha.toString();
						*/
						
	
						// This metod works.
						// Converts the sha bytes to string so we can compare
						StringBuffer sb = new StringBuffer();
						for (int i = 0; i < 32; i++) {
							sb.append(Integer.toString((receivedData[i] & 0xff) + 0x100, 16).substring(1));
						} 
						String sha = sb.toString();
						
						if(sha.equals(newFile.shaStr)){ // Compares the SHA with the file SHA
						
							// Gets the chunk number
							byte[] cNumber = new byte[8];
							System.arraycopy(receivedData, 32, cNumber, 0, 8);
							
							// Gets the hashCheck
							byte[] cHashCheck = new byte[24];
							System.arraycopy(receivedData, 40, cHashCheck, 0, 24);
							
							
							//Gets the data
							byte[] cData = new byte[CHUNKSIZE];
							System.arraycopy(receivedData, 64, cData, 0, CHUNKSIZE);
							//System.out.println(">>" + byteToLong(cNumber) + "|" + byteToLong(longToByte(8)));
							
							newFile.addChunk(byteToLong(cNumber), cData, cHashCheck );
							
							consolePrint("DEBUG: Received chunk "+ byteToLong(cNumber)+" | SHA: " + sha);
							
							System.out.println(newFile.missingStr());
						}
					}
					


				} while(!newFile.isDone());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			// TODO implement selector to retry download if stalled;
			
			consolePrint("DOWNLOAD COMPLETED!");
			try {
				newFile.writeToDisk();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}	
	}
	
}
