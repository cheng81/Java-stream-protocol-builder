package dk.itu.frza.smartspb;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Smart buffer object. It acts as a stream, but it buffers the bytes.
 * Reads are performed when needed.<br/>
 * It supports 3 different types of reads:
 * <ul>
 * <li><code>byte read()</code> reads a single byte
 * <li><code>byte[] read(int len)</code> reads <code>len</code> bytes, performing additional reads from the underlying stream when necessary
 * <li><code>byte[] readFromMark(int lastBytesDiscard)</code> reads all the bytes since the last call to the <code>mark()</code> method.
 * Discards the <code>lastBytesDiscard</code> bytes</li>
 * </ul>
 * 
 * @author frza
 *
 */
public class Buffer {

	/**
	 * Temporary byte array used for marked reads
	 */
	byte[] temp = null;
	
	/**
	 * The byte array buffer
	 */
	byte[] buf;
	
	/**
	 * Current index
	 */
	int cur;
	
	/**
	 * How many bytes were read last time
	 */
	int lastRead;
	
	/**
	 * Identifies the start index when performing a <code>readFromMark</code> read
	 */
	int marked = -1;
	
	/**
	 * Protocol-dependent information. Concrete protocols should store here
	 * relevant information for each iteration.<br/>
	 * When an iteration ends, this map is cleared.
	 */
	Map<Integer,Object> extras = new HashMap<Integer,Object>();
	
	/**
	 * Filler object. Called when the current byte buffer is exhausted
	 */
	BufferFiller filler;
	
	/**
	 * Construct a new smart buffer with size <code>size</code> and with the given filler.
	 * @param size
	 * @param filler
	 */
	public Buffer(int size, BufferFiller filler) {
		buf = new byte[size];
		cur = -1;
		lastRead = -1;
		this.filler = filler;
	}
	
	private boolean hasMore() {
		return cur < lastRead;
	}
	private boolean hasAtLeastMore(int size) {
		return (cur+size) < lastRead;
	}
	private void maybeFill() throws Exception {
		if(!hasMore()) {
			System.out.println("fill required");
			fill();
		}
	}
	private void fill() throws Exception {
		if(marked > -1) {
			temp = append(temp, Arrays.copyOfRange(buf, marked, lastRead));
			marked = 0;
		}
		lastRead = filler.fill(buf);
		cur = 0;
	}
	private byte[] append(byte[] first, byte[] second) {
		if(first == null || first.length == 0) {
			return second;
		} else {
			byte[] out = Arrays.copyOf(first, first.length + second.length);
			System.arraycopy(second, 0, out, first.length, second.length);
			return out;
		}
	}
	
	/**
	 * Read a single byte
	 * @return
	 * @throws Exception
	 */
	public byte read() throws Exception {
		maybeFill();
		return buf[cur++];
	}
	
	/**
	 * Read <code>size</code> bytes
	 * @param size
	 * @return
	 * @throws Exception
	 */
	public byte[] read(int size) throws Exception {
		if(hasAtLeastMore(size)) {
			return Arrays.copyOfRange(buf, cur, (cur+size));
		}

		byte[] out = null;
		int toGo = size;
		do {
			if(hasAtLeastMore(toGo)) {
				out = append(out, Arrays.copyOfRange(buf, cur, (cur+toGo)));
				cur += toGo;
				break;
			} else {
				out = append(out, Arrays.copyOfRange(buf, cur, lastRead));
				toGo -= (lastRead-cur);
				fill();
			}
		} while(toGo != 0);
		return out;
	}
	
	/**
	 * Read all the bytes since the last call to the <code>mark()</code> method, except the <code>lastBytesDiscard</code> bytes.
	 * After this call, the mark index is reset.
	 * @param lastBytesDiscard
	 * @return
	 */
	public byte[] readFromMark(int lastBytesDiscard) {
		byte[] out = null;
		if(cur - lastBytesDiscard > 0) {
			out = append(temp, Arrays.copyOfRange(buf, marked, (cur-lastBytesDiscard)));
		} else if(cur - lastBytesDiscard == 0) {
			out = temp;
		} else {
			out = append(temp, Arrays.copyOfRange(buf, marked, cur));
			out = Arrays.copyOf(temp, temp.length - lastBytesDiscard);
		}
		temp = null;
		marked = -1;
		return out;
	}
	
	/**
	 * Start a marked-read from the current position
	 */
	public void mark() {
		marked = cur;
	}
	
	/**
	 * Get the protocol-specific object identified by the <code>id</code> parameter
	 * @param <T>
	 * @param id
	 * @return
	 */
	public <T> T get(int id) {
		//I know, I know, this is pretty *unsafe*. But damn, you know what are you doing, don't you??
		return (T)extras.get(id);
	}
	
	/**
	 * If the protocol-specific object identified by <code>id</code> parameter does not 
	 * exists yet, put the <code>def</value>. In all cases, return the current value.
	 * @param <T>
	 * @param id
	 * @param def
	 * @return
	 */
	public <T> T maybePut(int id, Object def) {
		if(!extras.containsKey(id)) {
			put(id,def);
		}
		return get(id);
	}
	
	/**
	 * Store the protocol-specific object <code>o</code> with the <code>id</code> key
	 * @param id
	 * @param o
	 */
	public void put(int id, Object o) {
		extras.put(id, o);
	}
	
	/**
	 * Remove all the protocol-specific objects
	 */
	public void resetExtras() {
		extras.clear();
	}
}
