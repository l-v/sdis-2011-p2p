import java.util.*;


/**
 * Saves chunks and information about a file
 * that is being downloaded.
 *
 */
public class DownloadingFile {
	final long totalChunks;
	final byte[] sha;
	String filename;
	
	Vector<Chunk> chunks;
	Vector<Long> missingChunks;
	
	public DownloadingFile(long totalChunks,byte[] sha, String filename){
		this.totalChunks = totalChunks;
		this.sha = sha;
		this.filename = filename;
		
		for(long i = 0; i <totalChunks; i++){
			missingChunks.add(new Long(i));
			chunks.setSize((int) totalChunks);
		}
	};
	
	void writeToDisk(){
		//TODO
	};
	
	Chunk getChunk(long chunkNumber){
		// TODO
		return null;
	};
	
	boolean addChunk(long chunkNumber, byte[] data, byte[] hash){
		if (hasChunk(chunkNumber))
			return false;
		else{
			chunks.add((int) chunkNumber,new Chunk((int) chunkNumber, data, hash));
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
