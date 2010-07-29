package dk.itu.frza.smartspb;

/**
 * A StreamProtocol object must be configured with an Handler.<br/>
 * Each time a final state is reached, the <code>onProtocolEnd</code> method
 * is called.
 * @author frza
 *
 */
public interface Handler {
	
	/**
	 * Handle the finished message. Probably, the important pieces of information
	 * are stored in the <code>extras</code> map of the buffer object.
	 * @param buf
	 * @throws Exception
	 */
	void onProtocolEnd(Buffer buf) throws Exception;
}
