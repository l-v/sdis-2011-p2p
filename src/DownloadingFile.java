import java.io.IOException;
import java.io.FileOutputStream;
import java.util.*;


/**
 * Saves chunks and information about a file
 * that is being downloaded.
 *
 */
public class DownloadingFile {
	public final static long SOMECHUNKS = 10; // Maximum number of chunks we get at a time
	final long totalChunks;
	//final byte[] sha;
	final String shaStr;
	String filename;
	
	Vector<Chunk> chunks;
	Vector<Long> missingChunks;
	long requestedChunks; // Saves the size of the last batch of chunks requested.
	long timeLastAdded; // Time last added chunk;
	
	public DownloadingFile(long totalChunks/*,byte[] sha*/, String filename, String shaStr){
		
		chunks = new Vector<Chunk>();
		missingChunks = new Vector<Long>();
		
		this.totalChunks = totalChunks;
		this.shaStr = shaStr;
		this.filename = filename;
		
		for(long i = 0; i <totalChunks; i++){
			missingChunks.add(new Long(i));
			chunks.setSize((int)totalChunks); 
		}
		requestedChunks = 0;
		timeLastAdded = 0;
	};
	
	/***
	 * Recebe o nome do ficheiro a escrever e a lista de data chunks ordenados (sem header)
	 * 
	 * @param fileName
	 * @param chunksData
	 * @throws IOException
	 */
	void writeToDisk() throws IOException {

		FileOutputStream outFile =  new FileOutputStream( filename );  
		
		// appends all data bytes and writes to output file
		for (int i =0 ; i < totalChunks; i++) {
			outFile.write(chunks.get(i).data);
			System.out.println("Wrote to disk chunk " + i);
		}
		outFile.close();
	};
	
	Chunk getChunk(long chunkNumber){
		// TODO
		return null;
	};
	
	boolean addChunk(long chunkNumber, byte[] data, byte[] hashCheck){
		if (hasChunk(chunkNumber))
			return false;
		else{
			chunks.set((int) chunkNumber,new Chunk((int) chunkNumber, data, hashCheck));
			missingChunks.removeElement(new Long(chunkNumber));
			requestedChunks--;
			timeLastAdded = System.currentTimeMillis();
			return true;
		}
	};
	
	boolean isDone(){
		if (missingChunks.isEmpty())
			return true;
		else
			return false;
	};
	
	/**
	 * Prints the complete missing chunks
	 * @deprecated outputs too long of a string.
	 */
	String missingStr(){
		String chunksStr = null;
		Iterator<Long> it = missingChunks.iterator();
		if(it.hasNext())
			chunksStr = it.next().toString(); // adds the first element
		while(it.hasNext()){
			chunksStr = chunksStr + "," + it.next().toString(); // adds the rest
		}
		return chunksStr;
	}
	
	/**
	 * Creates a string with the numbers of 
	 * some chunks from the missingChunks pool
	 * @return 
	 */
	String getSome(){
		String chunksStr = null;
		Iterator<Long> it = missingChunks.iterator();
		if(it.hasNext()){
			chunksStr = it.next().toString(); // adds the first element
			requestedChunks = 1;
		}
		long counter = SOMECHUNKS;
		while(it.hasNext() && counter > 0){
			chunksStr = chunksStr + "," + it.next().toString(); // adds the rest
			counter--;
			requestedChunks++;
		}
		return chunksStr;
		
	}
	
	
	/**
	 * Checks if we already downloaded a chunk with chunkNumber
	 * @param chunkNumber the chunk number to check
	 * @return true if chunk exists
	 *
	 */
	boolean hasChunk(long chunkNumber){
		Long number = new Long(chunkNumber);
		if (!missingChunks.contains(number))
			return true;
		else
			return false;
	};
	

}
