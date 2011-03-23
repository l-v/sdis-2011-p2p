import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class MulticastP2P {

	public static final int MAXID = 9999999;
	public static final int CHUNKSIZE = 1024;

	Random randomGenerator; // stores the Random Number Generator

	SearchResults currentSearchResults; // stores the results from the current search
	
	//MulticastSocket sockData; comented because it has to be createad every single thread for concurrent access
	//MulticastSocket sockControl;
	String currentSearchID; // Saves the current search id. We should not answer searches from ourselves.
	
	InetSocketAddress controlAddr; // IP and Port for control
	InetSocketAddress dataAddr; // IP and port for data
	
	private  Vector<fileStruct> fileArray;
	
	/*
	 * Stores the results from the current search
	 */
	private class SearchResults{
		
		Vector<SearchResult> results;
		SearchResults(){
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
			SearchResult sr = new SearchResult(sha,filesize,filename);
			
			int resultIndex = results.indexOf(sr);
			if(resultIndex != -1){ // if Object exits
				results.elementAt(resultIndex).addPeer();
				return results.elementAt(resultIndex).peers;
			}
			else{
				results.add(sr);
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
		/* The size of the chunks must be 1024 bytes plus protocol headers. 
		 * The chunk must be numbered from 0 to C 
		 * where C is given by int( (S-1)/1024) 
		 * where S is the file's size in bytes. 
		 * The control messages are always strings 
		 * while the data messages are binary packets.  */

		// TODO 
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
			System.out.println("File: " + fileName + "\n\tSize: " + fileSize);
			System.out.println("\tTotalChunks: " + totalChunks);
			System.out.println("\tHashValue (sha): " + sha);
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
			return sha + " | " + filename + " | " + filesize + " | " + peers;
		}
		
	}


	public MulticastP2P(){
		randomGenerator = new Random(); // Generates a random number sequence
		currentSearchResults = null; // only created when after a search
		fileArray = new Vector<fileStruct>();
	}


	/**
	 * @param args
	 * 
	 * args = <>
	 */
	
	//TODO -p Path -i IP -c CONTROLPORT -d DATAPORT
	
	public static void main(String[] args){

		final MulticastP2P p2p = new MulticastP2P();
		
		/* Check that native byte order is little_endian */
		/* 
		 * ByteBuffer.order(ByteOrder bo)
         *      Modifies this buffer's byte order.
		 */
		 // System.out.println((java.nio.ByteOrder.nativeOrder().toString()));
		 


		// Index files directory
		try {
			p2p.indexFiles("./files/", CHUNKSIZE); //TODO verificar se dir existe
	 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Sets udp group address
		p2p.controlAddr = new InetSocketAddress("224.0.2.10",8967); //TODO
		p2p.dataAddr = new InetSocketAddress("224.0.2.10",8966);
		
		
		// Lançar thread pesquisa
		new Thread() {
			public void run() {			
				try {
					for(;;){
						p2p.search();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
		
		// Lançar thread resposta
		new Thread() {
			public void run() {			
				try {
					p2p.searchReply();
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
						p2p.sendFile();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};		
		
		
		/* testes_temp*/
		/*
		try {
			p2p.indexFiles("/home/liliana/Documents", CHUNKSIZE);

			System.out.println(p2p.intToByte(12));
			System.out.println("\n"+p2p.byteToInt(p2p.intToByte(999999999)));
			
			for (int i = 0; i!=fileArray.size(); i++) {
				fileArray.get(i).printStruct();
			}
	
			 System.out.println("Presente? " + p2p.hasFile("scraps"));
			 System.out.println("Path: " + fileArray.get( p2p.hasFile("scraps") ).completePath + "\n");
			 
			 Vector<byte[]> chunkResult = getChunks(fileArray.get( p2p.hasFile("boletim.html")));
			 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		
		//Novo teste
		/*
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
	void search() throws IOException{

		currentSearchID = genSearchID();
		currentSearchResults = new SearchResults();
		
		Scanner in = new Scanner(System.in);
		
		System.out.println("Search:");
		String keywordList = in.nextLine();

		/* Create a new MulticastSocket so we can concurrently read and write
		 * from the multicast group.
		 */
		MulticastSocket mSocket = joinGroup(controlAddr);

		String searchString = "SEARCH" + " " + currentSearchID + " " 
		+ keywordList; // Generates search string.
		System.out.println(searchString);

		DatagramPacket searchPacket = null;

		// Creates the searchPacket and sends it.
		searchPacket = new DatagramPacket(
				searchString.getBytes(), searchString.length(),controlAddr);

		mSocket.send(searchPacket);

		// TODO While para ler e apresentar resultados
		
		// TODO While para receber e filtrar resultados
		boolean searchON = true;
		
		while (searchON){ //TODO!!!!!!!
			byte[] buf = new byte[512];
			DatagramPacket receivePacket = new DatagramPacket(buf,512);
			mSocket.receive(receivePacket);
			String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
			// Splitting the answer in tokens
			StringTokenizer st = new StringTokenizer(received);
			//System.out.println(received);
			
			if(st.nextToken().equalsIgnoreCase("FOUND")){ // Only parses FOUNDs
				if (st.nextToken().equals(currentSearchID)){ // Compares to currentSearchID
					String receivedSha = st.nextToken();
					long receivedSize = Long.parseLong(st.nextToken());
					
					// now to get the filename
					String receivedFilename = st.nextToken();
					while(st.hasMoreTokens()){
						receivedFilename = receivedFilename + " " + st.nextToken();
					}
					// Inserts the new search result in currentSearchResults.
					currentSearchResults.insertResult(receivedSha, receivedSize, receivedFilename);

					System.out.println(currentSearchResults.toString());
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
	private void indexFiles(String directory, int chunkSize) throws IOException
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
	private byte[] intToByte(int integer) {
        byte[] byteValue =  {
                (byte)(integer >>> 24),
                (byte)(integer >>> 16),
                (byte)(integer >>> 8),
                (byte)integer};
        
        return byteValue;
	}
	
	/***
	 * Converts byte[] values to integer. 
	 * 
	 * @param byteValue
	 * @return 
	 */
	private int byteToInt(byte[] byteValue) {
		int integerValue = (byteValue[0] << 24)
						+ ((byteValue[1] & 0xFF) << 16) 
						+ ((byteValue[2] & 0xFF) << 8) 
						+ (byteValue[3] & 0xFF);
		
		return integerValue;
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
		int chunkCounter = 0;  // TODO check if INT chunk counter is adequated

		while (bytesRead != fLength) {
			
			byte[] fChunk =  new byte[1024]; 
			long bytes = file.read(fChunk);  
			bytesRead += bytes; 
			
			
			/* add chunk header */
			byte[] fileID = new byte[256];
			byte[] chunkNumber = new byte[256];
			
			fileID = fileReq.sha.getBytes();
			chunkNumber = intToByte(chunkCounter); 
			
			//TODO find a way to 'concatenate' byte arrays efficiently
			
			
			
			chunkVector.add(fChunk);
			chunkCounter++;
		}
		
		if (chunkVector.size() != fileReq.totalChunks) {
			System.out.println("\nWarning: number of chunks generated is not the expected.");
		}
		/*
		System.out.println("File: " + fileReq.fileName);
		System.out.println("fLength: " + fLength);
		System.out.print("Bytes total: " + bytesRead + "\tnChunks: " + chunkVector.size());
		*/
		return chunkVector;
	}
	



	/***
	 * Checksum method
	 * 
	 * @param filePath
	 * @return String: contains the hashValue of the file
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public String SHACheckSum(String fileName) throws NoSuchAlgorithmException, IOException{

		MessageDigest md = MessageDigest.getInstance("SHA-256");	 
		FileInputStream fis = new FileInputStream(fileName);
		
		
		byte[] dataBytes = new byte[1024];

		int nread = 0;
		while ((nread = fis.read(dataBytes)) != -1) {
			md.update(dataBytes, 0, nread);
		};

		byte[] mdbytes = md.digest();



		//convert the byte to hex format
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < mdbytes.length; i++) {
			sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		} 

		/*
		System.out.println("Hex format : " + sb.toString());
		System.out.println("Hex format length : " + sb.toString().length());
		System.out.println("MD length : " + mdbytes.length);
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

			StringTokenizer st = new StringTokenizer(received);

			if(st.nextToken().equalsIgnoreCase("SEARCH")){ // Only parses SEARCHs

				String receivedSearchID = st.nextToken();
				System.out.println("RECEIVED: "+ received);

				if (!receivedSearchID.equals(currentSearchID)){ // Compares to currentSearchID

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

						}
					}
				}
			}
		}
	}
	
	
	void sendFile() throws IOException {
		
		// Joins multicast group and creates socket
		MulticastSocket mSocket = joinGroup(controlAddr);
	
		byte[] buf = new byte[512];
		DatagramPacket receivePacket = new DatagramPacket(buf,512);
		mSocket.receive(receivePacket);
		String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
	
		final StringTokenizer st = new StringTokenizer(received); // TODO in theory, no conflicts should emerge - still to test though.
		System.out.println("RECEIVED GET: "+ received);

		if(st.nextToken().equalsIgnoreCase("GET")){ // Only parses GETs

			
			// Launch send thread for each GET detected // TODO and test
			new Thread() {
				public void run() {

					String fileID = st.nextToken();

					
					// get chunk numbers
					String chunks = st.nextToken();
					Vector<Long> chunksReq = new Vector<Long>(); /* vector with chunk numbers requested */
					System.out.println("chunks requested: " + chunks);
					 

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
			};		
		}
	}
	
	
	/***
	 * Sends requested chunks through data port 
	 * 
	 * @param sha
	 * @param chunkNumbers
	 * @throws IOException
	 */
	void sendChunks(String sha, Vector<Long> chunkNumbers) throws IOException { // TODO testing
		
		fileStruct fReq= getFileByHash(sha);
		Vector<byte[]> chunkVector= getChunks(fReq);
		
		
		// Joins multicast group and creates socket
		MulticastSocket mSocket = joinGroup(dataAddr);
		DatagramPacket sendPacket = null;
		
		Random randGenerator = new Random();
		
		
		while(!chunkVector.isEmpty()) {
			
			int randChunk = randGenerator.nextInt(chunkVector.size()); //TODO test of int/long doesn't give problems
			byte[] chunk = chunkVector.get(randChunk);
			
			sendPacket = new DatagramPacket(
					chunk, chunk.length,dataAddr);

			mSocket.send(sendPacket);
			chunkVector.remove(randChunk);
		}
	
	}
	
}
