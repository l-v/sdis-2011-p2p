import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.swing.DefaultListModel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;


public class MulticastP2P {

	/** Maximum value for searchID */
	public static final int MAXID = 9999999;
	/** Size of one chunk of data */
	public static final int CHUNKSIZE = 1024;
	/** Size of the header of the packet */
	public static final int HEADERSIZE = 64;
	/** Size of a buffer used for a command */
	public static final int BUFF = 1024;
	/** Timeout for a read from a MulticastSocket */
	public static final int SOCKETTIMEOUT = 1000;
	/** Used for displaying search results */
	DefaultListModel listModel;
	/** Used for displaying messages to the user */
	JTextArea console;
	/** stores the Random Number Generator */
	Random randomGenerator;
	/** stores the results from the current search */
	SearchResults currentSearchResults;
	/** Saves the current search id */
	String currentSearchID; 
	/** IP and Port for control */
	InetSocketAddress controlAddr;
	/** IP and port for data */
	InetSocketAddress dataAddr;
	/** saves the files on disk */
	private  Vector<fileStruct> fileArray;
	/** keeps info about the current uploads */
	Vector<UploadingFile> currentUploads;
	/** hash type used */
	String hashType;
	/** directory with files for sharing */
	String localDirectory;
	
	private boolean DEBUG = false;

	
	/**
	 * Constructor
	 */
	public MulticastP2P(){
		randomGenerator = new Random(); // Generates a random number sequence
		currentSearchResults = null; // only created when after a search
		fileArray = new Vector<fileStruct>();
		listModel = new DefaultListModel();
		console = new JTextArea();
		currentUploads = new Vector<UploadingFile>();
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
	 * @param args
	 * 
	 * args = <>
	 */
	public void start(String path, String ip, int controlPort, int dataPort, String hash){
		
		// set hash type used and files directory
		hashType = hash;
		localDirectory = path;
		
		// Index files directory
		indexFiles(); 

		
		// Sets UDP group address
		controlAddr = new InetSocketAddress(ip,controlPort); 
		dataAddr = new InetSocketAddress(ip,dataPort);
		
		

		// Replies to SEARCH
		new Thread() {
			public void run() {			
				try {
					searchReply();
				} catch (IOException e) {
					System.out.println("IOException");
					if (!DEBUG) System.exit(-1);
					e.printStackTrace();
				}
			}
		}.start();
		
		// Replies to GET
		new Thread() {
			public void run() {
				try {
						sendFile();
				} catch (IOException e) {
					System.out.println("IOException");
					if (!DEBUG) System.exit(-1);
					e.printStackTrace();
				}
			}
		}.start();		
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

		mSocket = new MulticastSocket(addr.getPort());
		mSocket.joinGroup(addr.getAddress());
		mSocket.setTimeToLive(5);

		return mSocket;
	}


	/***
	 * Generates a search id. 
	 * 
	 * @return
	 */
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
			System.out.println("File not found. ");
			if (!DEBUG) System.exit(-1);
			e.printStackTrace();
		}

		String searchString = "SEARCH" + " " + currentSearchID + " " 
		+ keywordList; // Generates search string.
		if(DEBUG)consolePrint("OUT: " + searchString);

		DatagramPacket searchPacket = null;

		// Creates the searchPacket and sends it.
		try {
			searchPacket = new DatagramPacket(
					searchString.getBytes(), searchString.length(),controlAddr);
		} catch (SocketException e) {
			System.out.println("Socket error. ");
			if (!DEBUG) System.exit(-1);
			e.printStackTrace();
		}

		try {
			mSocket.send(searchPacket);
		} catch (IOException e) {
			System.out.println("IOException");
			if (!DEBUG) System.exit(-1);
			e.printStackTrace();
		}
		
		while (currentSearchID.equals(ownID) ){ // Stops search when ID changes
			byte[] buf = new byte[BUFF];
			DatagramPacket receivePacket = new DatagramPacket(buf,BUFF);
			try {
				mSocket.receive(receivePacket);
			} catch (IOException e) {
				System.out.println("IOException");
				if (!DEBUG) System.exit(-1);
				e.printStackTrace();
			}
			String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
			// Splitting the answer in tokens
			StringTokenizer st = new StringTokenizer(received);
			//consolePrint(received);
			
			if(st.nextToken().equalsIgnoreCase("FOUND")){ // Only parses FOUNDs
				if (st.nextToken().equals(currentSearchID)){ // Compares to currentSearchID
					if(DEBUG) consolePrint("IN: " + received);
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
	public void indexFiles() 
	{
		File folder = new File(localDirectory);
		if (!folder.exists()) {
			System.out.println("Path chosen does not exist." );
		    
			// Creates the directory
		    boolean success = (new File(localDirectory)).mkdir();
		    if (success) {
		      System.out.println("Directory: " + localDirectory + " created");
		    }
		    else{
		    	System.out.println("ERROR: Could not create new directory. Exiting." );
		    	System.exit(-1);
		    }
		}
		
		File[] listOfFiles = folder.listFiles();

		fileArray.clear();
		consolePrint("Indexing files:");
		for (int i = 0; i < listOfFiles.length; i++) {

			if (listOfFiles[i].isFile()) {

				long size = listOfFiles[i].length();
				long numChunks = ((size-1)/CHUNKSIZE) +1;
				String hashString = null;
				
				/* compute hash values */
				try {
					hashString = SHACheckSum(listOfFiles[i].getAbsolutePath());
				} catch (NoSuchAlgorithmException e) {
					
					System.out.println("Hash algorythm chosen does not exist. ");
					if (!DEBUG) System.exit(-1);
					e.printStackTrace();
				} catch (IOException e1) {
					System.out.println("File not found. ");
					if (!DEBUG) System.exit(-1);
					e1.printStackTrace();
				}
				
				fileStruct newFile = new fileStruct(hashString, listOfFiles[i].getName(), size, numChunks, listOfFiles[i].getAbsolutePath());  
				fileArray.add(newFile); 
				consolePrint("Indexed file: " + newFile.fileName);
			} 
		}
		
		consolePrint("All files indexed.");
	}
	
	/***
	 * Searches file array by fileID value (sha)
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
	static byte[] longToByte(long number) {
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
	 * Splits a file in chunks.
	 * 
	 * @param fileReq: asked file
	 * @return Vector<Chunk>: vector with the all the file chunks.
	 * @throws IOException
	 */
	private Vector<Chunk> getChunks(fileStruct fileReq) throws IOException {

		Vector<Chunk> chunkVector = new Vector<Chunk>();
	
		FileInputStream file = new FileInputStream(fileReq.completePath);
		long fLength = fileReq.fileSize;
		long bytesRead = 0;
		long chunkCounter = 0;  
		byte[] fileID = new byte[32];
		
		/*
		 * Generates the SHA for the file
		 */
		try {
			fileID = SHACheckSumBytes(fileReq.completePath); //32bytes for 1st header part
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Hash algorythm does not exist. ");
			if (!DEBUG) System.exit(-1);
			e.printStackTrace();
		} 
		
		while (bytesRead != fLength) {
			byte[] fChunk =  new byte[CHUNKSIZE]; 
			long bytes = file.read(fChunk);  
			bytesRead += bytes;  
			

			/* add chunk to vector */
			chunkVector.add(new Chunk(chunkCounter,fChunk,fileID));
			chunkCounter++;
		}
		
		if (chunkVector.size() != fileReq.totalChunks) {
			consolePrint("\nWarning: number of chunks generated is not the expected.");
		}
		
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

		MessageDigest md = MessageDigest.getInstance(hashType);	
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

		String hashString = sb.toString();
		return hashString;
	}
	
	
	
	/**
	 * Reads SEARCH commands and sends adequate FOUNDs
	 * 
	 * @throws IOException
	 */
	void searchReply() throws IOException{

		// Joins multicast group and creates socket
		MulticastSocket mSocket = joinGroup(controlAddr);

		byte[] buf = new byte[BUFF];

		while(true) {
			DatagramPacket receivePacket = new DatagramPacket(buf,BUFF);
			mSocket.receive(receivePacket);
			String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
			
			StringTokenizer st = new StringTokenizer(received);

			if(st.nextToken().equalsIgnoreCase("SEARCH")){ // Only parses SEARCHs
				
				
				String receivedSearchID = st.nextToken();
				

				if (!receivedSearchID.equals(currentSearchID)){ // Compares to currentSearchID
					if(DEBUG) consolePrint("IN: "+ received);
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
							if(DEBUG) consolePrint("OUT: "+ answer);
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
	
		byte[] buf = new byte[BUFF];
			DatagramPacket receivePacket = new DatagramPacket(buf,BUFF);
		
		while(true){
			mSocket.receive(receivePacket);
			String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
			final StringTokenizer st = new StringTokenizer(received);
			
			if(st.nextToken().equalsIgnoreCase("GET")){ // Only parses GETs
				String fileID = st.nextToken();
				final UploadingFile file = new UploadingFile(fileID);
				
				// get chunk numbers
				String chunks = st.nextToken();
				if(DEBUG)consolePrint("chunks requested: " + chunks);
				 

				/*  Converts chunks to string
				 *  chunks format: a-b; a; a,b,c,d
				 */ 
				if (chunks.indexOf("-") != -1) {
					String[] firstLast = chunks.split("-");
					for (long i=Long.parseLong(firstLast[0]); i<=Long.parseLong(firstLast[1]); i++)
						file.chunksRequested.add(i);
				} 
				else if (chunks.indexOf(",") != -1) {
					StringTokenizer cToken = new StringTokenizer(chunks, ",");
					while(cToken.hasMoreTokens())
						file.chunksRequested.add(Long.parseLong(cToken.nextToken()));
				}
				else 
					file.chunksRequested.add(Long.parseLong(chunks));
				
				
				/*
				 *  If we are not already uploading this file, starts a new
				 *  thread to send the file
				 */
				if(!currentUploads.contains(file)){ 
					
					/*
					 *  Launch send thread for each new GET detected
					 */
					new Thread() {
						public void run() {

							// sends chunks requested
							try {
								sendChunks(file);
							} catch (IOException e) {
								
								e.printStackTrace();
							}
							
						}
					}.start();	
				}
				else{
					/*
					 *  Adds the requested chunks to the already uploading file
					 */
					int index = currentUploads.indexOf(file);
					UploadingFile existingFile = currentUploads.get(index);
					Iterator<Long> it = file.chunksRequested.iterator();
					Long chunkNumber = null;
					while(it.hasNext()){
						chunkNumber = it.next();
						if(!existingFile.chunksRequested.contains(chunkNumber)){ // Checks to see if chunkNumber's already there
							existingFile.chunksRequested.add(chunkNumber);
						}
					}
					
				}
			}
		}
	}
	
	
	/***
	 * Sends requested chunks through data port 
	 * 
	 * @param sha
	 * @param chunkNumbers
	 * @throws IOException
	 */
	void sendChunks(final UploadingFile file) throws IOException { // TODO testing
		
		fileStruct fReq= getFileByHash(file.fileID);
		Vector<Chunk> chunkVector= getChunks(fReq);
				
		// Joins multicast group and creates socket
		final MulticastSocket mSocket = joinGroup(dataAddr);
		DatagramPacket sendPacket = null;

		Random randGenerator = new Random();

		/*
		 *  Launch thread that verifies for repeated chunks
		 */
		Thread checkRepeated = new Thread()  {
			public void run() {
				
				byte[] buf = new byte[CHUNKSIZE+HEADERSIZE];
				while(!file.chunksRequested.isEmpty()) {
					// Joins multicast group and creates socket
					try {
						
						DatagramPacket cPacket = new DatagramPacket(buf,CHUNKSIZE+HEADERSIZE);
						mSocket.receive(cPacket);

						byte[] chunkData = cPacket.getData();

						// Gets the chunk SHA
						byte[] chunkSha = new byte[32];
						System.arraycopy(chunkData, 0, chunkSha, 0, 32);
						
						// Gets the chunk number
						byte[] chunkNumber = new byte[8];
						System.arraycopy(chunkData, 32, chunkNumber, 0, 8);
						
						long packetNumber = byteToLong(chunkNumber);
						
						// Converts the sha bytes to string so we can compare
						StringBuffer sb = new StringBuffer();
						for (int i = 0; i < 32; i++) {
							sb.append(Integer.toString((chunkSha[i] & 0xff) + 0x100, 16).substring(1));
						}
						String sha = sb.toString();
						
						if(sha.equals(file.fileID))
							if (file.chunksRequested.contains(packetNumber)) {
								file.chunksRequested.remove(packetNumber);
							}				

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		checkRepeated.start();
		
		/*
		 * Sends all the chunks that were requested
		 */
		while(!file.chunksRequested.isEmpty()) {
			long chunkChosen;
			int randChunk = randGenerator.nextInt(file.chunksRequested.size());
			
			// Sometimes vector changes size and we go out of bounds
			try{
				chunkChosen = file.chunksRequested.get(randChunk);
			} catch (ArrayIndexOutOfBoundsException e){
				if(DEBUG)consolePrint("DEBUG:vector changed size");
				continue;
			}
			byte[] chunk = chunkVector.get((int)chunkChosen).getBytes();
			
			sendPacket = new DatagramPacket(
					chunk, chunk.length,dataAddr);
			
			mSocket.send(sendPacket);
			
			//file.chunksRequested.remove(randChunk); // Not needed since thread that watches for repeted chunks takes care of this
			
			// To avoid network flood
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// All sent, removes the file from currentUploads
		currentUploads.remove(file);
		
			
	}
	
	/**
	 * sends a GET message to start receiving files
	 */
	void getFile(int choice){
		SearchResult sr = currentSearchResults.results.elementAt(choice);
		if(sr!= null){
			long chunks = (int) ((sr.filesize-1)/CHUNKSIZE)+1; // calculates total chunks( max chunk + 1)
			String getStr = null;
			
			consolePrint("Started download: " + sr.filename);
			DownloadingFile newFile = new DownloadingFile(chunks,sr.filename,sr.sha); // creates the structure to save the downloading file
			MulticastSocket dataSocket = null;
			MulticastSocket mSocket = null;
			DatagramPacket getPacket = null;

			try {
				dataSocket = joinGroup(dataAddr); // Joins the data group to receive the files;
				mSocket = joinGroup(controlAddr); // Joins the control group to send the get command;
				dataSocket.setSoTimeout(SOCKETTIMEOUT); // Makes the socket timeout 
				
			} catch (IOException e) {
				System.out.println("IOException");
				if (!DEBUG) System.exit(-1);
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
					if(((newFile.requestedChunks < 1) || (elapsedTime > 3000)) && !newFile.isDone()){
						getStr = "GET " + sr.sha + " "+ newFile.getSome(); // creates the message
						getPacket = new DatagramPacket(
								getStr.getBytes(), getStr.length(),controlAddr);
						mSocket.send(getPacket);
						if(DEBUG)consolePrint("OUT: "+ getStr);
					}
					
					try{
					dataSocket.receive(dataPacket);
					}catch (SocketTimeoutException e){
						System.out.println("Stalled: received no data.");
					}
					
					byte[] receivedData = dataPacket.getData();
					
					if(receivedData != null){ // only if we received data

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
							
							newFile.addChunk(byteToLong(cNumber), cData, cHashCheck );
							
							if (DEBUG)
								consolePrint("DEBUG: Received chunk "+ byteToLong(cNumber)+" | SHA: " + sha);
							else
								consolePrint("Remaining: " + (int)(( (float)(newFile.missingChunks.size())/(float) newFile.totalChunks)*100) + " %");
						}
					}
					


				} while(!newFile.isDone());
			} catch (IOException e) {
				System.out.println("IOException");
				if (!DEBUG) System.exit(-1);
				e.printStackTrace();
			}
			
			
			consolePrint("Download Completed: " + newFile.filename);
			try {
				newFile.writeToDisk(localDirectory);
			} catch (IOException e) {
				System.out.println("IOException");
				if (!DEBUG) System.exit(-1);
				e.printStackTrace();
			}
			
			
		}	
	}
	
	/**
	 * Outputs a String str to console
	 * @param str
	 */
	void consolePrint(final String str){
		Runnable doWorkRunnable = new Runnable() {
		    public void run() { 
				console.append(str + "\n");
				console.setCaretPosition(console.getDocument().getLength());
		    }
		};
		SwingUtilities.invokeLater(doWorkRunnable);

	}
	
}
