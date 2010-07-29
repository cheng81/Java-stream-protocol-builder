package dk.itu.frza.testsmartspb;

import dk.itu.frza.smartspb.Buffer;
import dk.itu.frza.smartspb.ProtocolState;
import dk.itu.frza.smartspb.StreamProtocol;

/**
 * Parse websocket messages. Specs: {@link http://www.whatwg.org/specs/web-socket-protocol/}
 * @author frza
 *
 */
public class WebSocketProtocol {
	/**
	 * extra protocol info for binary-frame length
	 */
	public static final int BINARY_LEN = 0;
	/**
	 * extra protocol info for binary-frame
	 */
	public static final int BINARY_DATA = 1;
	/**
	 * extra protocol info for utf-8 message
	 */
	public static final int MESSAGE_DATA = 2;

	/**
	 * Initial state, match either a binary-frame length or a message
	 * @param buf
	 * @return
	 * @throws Exception
	 */
	@ProtocolState(0)
	public int matchStartToken(Buffer buf) throws Exception {
		byte b = buf.read();
		if((b & 0x80) == 0x80) {
			// got some binary, go to 1
			return 1;
		} else {
			// got message, got to 3
			return 3;
		}
	}
	
	/**
	 * Read a binary-frame length
	 * @param buf
	 * @return
	 * @throws Exception
	 */
	@ProtocolState(1)
	public int readBinaryLength(Buffer buf) throws Exception {
		byte b;
		int t,len;
		len = 0;
		
		do {
			b = buf.read();
			if( (b&0x80) == 0x80 ) {
				//end of length, store the len info and go to 2
				buf.put(BINARY_LEN, len);
				return 2;
			} else {
				//update the len
				t = b & 0x7f;
				len = t + (len*128);
			}
		}while(true);
	}
	
	/**
	 * Read the binary data
	 * @param buf
	 * @return
	 * @throws Exception
	 */
	@ProtocolState(2)
	public int readBinary(Buffer buf) throws Exception {
		byte[] binaryFrame = buf.read( buf.<Integer>get(BINARY_LEN) );
		buf.put(BINARY_DATA, binaryFrame);
		return 3;
	}
	
	/**
	 * Read the message data. Return END state.
	 * @param buf
	 * @return
	 * @throws Exception
	 */
	@ProtocolState(3)
	public int readMessage(Buffer buf) throws Exception {
		buf.mark();
		byte b;
		do {
			b = buf.read();
			if( (b & 0xff) == 0xff) {
				//got end of message, bulk copy, store the byte array and return END
				byte[] message = buf.readFromMark(1);
				buf.put(MESSAGE_DATA, message);
				return StreamProtocol.END;
			}
		} while(true);
	}
}
