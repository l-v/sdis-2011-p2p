
/**
 * Received chunk
 *
 */
public class Chunk {
	long number;
	byte[] data;
	byte[] Sha;
	
	/**
	 * Constructor used when downloading
	 * @param number
	 * @param data
	 */
	public Chunk(long number, byte[] data) {
		this.number = number;
		this.data = data;
		this.Sha = null;
	}
	
	/**
	 * Constructor used when sending
	 * @param number
	 * @param data
	 * @param Sha
	 */
	public Chunk( long number, byte[] data, byte[] Sha) {
		this.number = number;
		this.data = data;
		this.Sha = Sha;
	}
	
	byte[] getBytes(){

		/* add chunk header */
		byte[] chunkNumber = new byte[8];
		byte[] reserved = new byte[24];

		// Generates chunk number
		System.arraycopy(MulticastP2P.longToByte(number), 0, chunkNumber, 0, MulticastP2P.longToByte(number).length);

		/* concatenate byte arrays with header */
		byte[] header = new byte[64];
		
		System.arraycopy(Sha, 0, header, 0, Sha.length);
		System.arraycopy(chunkNumber, 0, header, 32, chunkNumber.length);
		System.arraycopy(reserved, 0, header, 40, reserved.length);
		
		byte[] finalChunk = new byte[64 + MulticastP2P.CHUNKSIZE];
		System.arraycopy(header, 0, finalChunk, 0, 64);
		System.arraycopy(data, 0, finalChunk, 64, data.length);
		
		return finalChunk;
	}

}
