import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.*;


/**
 * Saves chunks and information about a file
 * that is being downloaded.
 *
 */
public class DownloadingFile {
	public final static long SOMECHUNKS = 30; // Maximum number of chunks we get at a time
	final long totalChunks;
	final String shaStr;
	String filename;
	long filesize;
	
	Vector<Chunk> chunks;
	Vector<Long> missingChunks;
	long requestedChunks; // Saves the size of the last batch of chunks requested.
	long timeLastAdded; // Time last added chunk;
	
	public DownloadingFile(long totalChunks, String filename, String shaStr, long filesize){
		
		chunks = new Vector<Chunk>();
		missingChunks = new Vector<Long>();
		
		this.totalChunks = totalChunks;
		this.shaStr = shaStr;
		this.filename = filename;
		this.filesize = filesize;
		
		for(long i = 0; i <totalChunks; i++){
			missingChunks.add(new Long(i));
			chunks.setSize((int)totalChunks); 
		}
		requestedChunks = 0;
		timeLastAdded = System.currentTimeMillis();
	};
	
	/***
	 * Writes the file to disk, with fileName)
	 * 
	 * @param fileName
	 * @param chunksData
	 * @throws IOException
	 */
	void writeToDisk(String directory) throws IOException {

		File f = new File(directory + "/" + filename);
		FileOutputStream outFile;
		
		if(f.exists()){
			outFile =  new FileOutputStream( directory + "/-Downloaded-" + filename );
		}
		else{
			outFile =  new FileOutputStream( directory + "/" + filename );
		}
		
		// appends all data bytes and writes to output file
		for (int i = 0 ; i < (totalChunks-1); i++) {
			outFile.write(chunks.get(i).data);
			System.out.println("Wrote to disk chunk " + i);
		}
		// Special case for last chunk, because data may not ocupy the full chunk.
		int remainder = (int) (filesize - ((totalChunks-1)*MulticastP2P.CHUNKSIZE));
		outFile.write(chunks.get((int) (totalChunks-1)).data, 0, remainder );
		
		outFile.close();
	};
	
	
	/**
	 * Adds a new chunk, updating information about the received and missing chunks . 
	 * 
	 * @param chunkNumber
	 * @param data
	 * @param hashCheck
	 * @return
	 */
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
	
	
	/**
	 * Checks whether the file has been completely downloaded or not.
	 * 
	 * @return
	 */
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
