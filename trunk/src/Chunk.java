
/**
 * Received chunk
 *
 */
public class Chunk {
	long number;
	byte[] data;
	byte[] hashCheck;
	
	public Chunk(long number, byte[] data, byte[] hashCheck) {
		this.number = number;
		this.data = data;
		this.hashCheck = hashCheck;
	}

}
