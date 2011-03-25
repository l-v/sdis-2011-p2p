import java.util.Vector;


public class UploadingFile {
	Vector<Long> chunksRequested;
	String fileID;
	
	public UploadingFile(String fileID){
		this.fileID = fileID;
		chunksRequested = new Vector<Long>();
		

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileID == null) ? 0 : fileID.hashCode());
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
		UploadingFile other = (UploadingFile) obj;
		if (fileID == null) {
			if (other.fileID != null)
				return false;
		} else if (!fileID.equals(other.fileID))
			return false;
		return true;
	}
	
	
}
