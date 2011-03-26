/**
 * Stores information about files on disk
 */
public class fileStruct {
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
}