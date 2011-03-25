import java.io.IOException;
import java.io.FileOutputStream;
import java.util.*;


/**
 * Saves chunks and information about a file
 * that is being downloaded.
 *
 */
public class DownloadingFile {
	final long totalChunks;
	//final byte[] sha;
	//final String shaStr;
	String filename;
	
	Vector<Chunk> chunks;
	Vector<Long> missingChunks;
	
	public DownloadingFile(long totalChunks/*,byte[] sha*/, String filename){
		this.totalChunks = totalChunks;
		//this.shaStr = sha;
		this.filename = filename;
		
		// TODO crasha aqui por alguma razao
		for(long i = 0; i <totalChunks; i++){
			missingChunks.add(new Long(i));
			chunks.setSize((int)totalChunks); 
		}
		System.out.println("acabou teste");
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
		for (int i =0 ; i!=chunks.size(); i++) {
			outFile.write(chunks.get(i).data);
		} 
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
