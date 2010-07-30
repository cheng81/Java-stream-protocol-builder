package dk.itu.frza.smartspb;

import java.io.InputStream;

public class InputStreamFiller implements BufferFiller {

	InputStream is;
	
	public InputStreamFiller(InputStream is) {
		this.is = is;
	}
	
	@Override
	public int fill(byte[] buf, int offset, int len) throws Exception {
		return is.read(buf,offset,len);
	}

}
