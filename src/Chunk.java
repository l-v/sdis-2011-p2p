
/**
 * Received chunk
 *
 */
public class Chunk {
	long number;
	byte[] data;
	byte[] hash;
	
	public Chunk(long number, byte[] data, byte[] hash) {
		this.number = number;
		this.data = data;
		this.hash = hash;
	}

}
