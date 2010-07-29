package dk.itu.frza.smartspb;

/**
 * Buffer object must be constructed with an instance of a filler.<br/>
 * A filler has the only purpose of put some bytes in an byte array
 * and return the number of bytes filled.
 * @author frza
 *
 */
public interface BufferFiller {
	
	/**
	 * Put some bytes in the buf array. Returns the number of bytes wrote.
	 * @param buf
	 * @return
	 * @throws Exception
	 */
	int fill(byte[] buf) throws Exception;
}
