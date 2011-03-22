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
		
		new Thread() {
			public void run() {			
				try {
					while(true) {
						p2p.searchReply();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
		
		
		
		// TODO Lançar thread resposta
		
		// TODO Lançar thread de pesquisa
		
		
		/* testes_temp*/
		/*
		try {
			p2p.indexFiles("/home/liliana/Documents", CHUNKSIZE);

			
			
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
	 * Divide um ficheiro em chunks.
	 * 
	 * @param fileReq: ficheiro pedido
	 * @return Vector<byte>: vector com os varios chunks do ficheiro.
	 * @throws IOException
	 */
	private static Vector<byte[]> getChunks(fileStruct fileReq) throws IOException {
		
		Vector<byte[]> chunkVector = new Vector<byte[]>();
	
		FileInputStream file = new FileInputStream(fileReq.completePath);
		long fLength = fileReq.fileSize;
		long bytesRead = 0;

		while (bytesRead != fLength) {
			
			byte[] fChunk =  new byte[1024]; 
			long bytes = file.read(fChunk); 
			bytesRead += bytes; 
			
			chunkVector.add(fChunk);
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
