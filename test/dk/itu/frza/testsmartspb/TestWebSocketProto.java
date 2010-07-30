package dk.itu.frza.testsmartspb;

import java.util.Arrays;

import junit.framework.TestCase;
import dk.itu.frza.smartspb.Buffer;
import dk.itu.frza.smartspb.BufferFiller;
import dk.itu.frza.smartspb.Handler;
import dk.itu.frza.smartspb.StreamProtocol;

public class TestWebSocketProto extends TestCase {
	private byte[] append(byte[] first, byte[] second) {
		if(first.length == 0) {
			return second;
		} else {
			byte[] out = Arrays.copyOf(first, first.length + second.length);
			System.arraycopy(second, 0, out, first.length, second.length);
			return out;
		}
	}
	
	BufferFiller filler = new BufferFiller() {
		byte[] tmp = null;
		int curIdx = 0;
//		int call = 0;
		private void init() throws Exception {
			byte start = (byte)0x80;
			byte end = (byte)0xFF;
			byte[] binary = "some random binary data".getBytes("UTF-8");
			byte[] msg = "The message!àì".getBytes("UTF-8");
			int blen = binary.length;
			int l_bytes = (blen>16384)?3:(blen>128)?2:1;
			byte[] blen_buf = new byte[l_bytes];
			int idx = 0;
			switch (l_bytes)
            {
                case 3:
                    blen_buf[idx] = ((byte)(0x80|(blen>>14))); idx++;
                case 2:
                    blen_buf[idx] = ((byte)(0x80|(0x7f&(blen>>7)))); idx++;
                case 1:
                    blen_buf[idx] = ((byte)(0x7f&blen));
            }

			tmp = append(new byte[]{start},blen_buf);
			tmp = append(tmp,new byte[]{start});
			tmp = append(tmp,binary);
			tmp = append(tmp,msg);
			tmp = append(tmp,new byte[]{end});
		}
		public int fill(byte[] buf,int offset, int len) throws Exception {
			System.out.println("fill called!");
			if(tmp == null) {
				init();
			}
			
			if(len > 8) {
				len = 8; //force division while reading binary
			}
			
			if( (curIdx+len) > tmp.length ) {
				len = tmp.length - curIdx;
			}
			System.out.println(buf.length+" "+offset+" "+len+" "+curIdx);

			System.arraycopy(tmp, curIdx, buf, offset, len);
			curIdx += len;
			
			return len;
		};
	};
	
	public void testws1() throws Exception{
		final StreamProtocol proto = new StreamProtocol();
		proto.setProtocol(new WebSocketProtocol());
		proto.setBuffer(new Buffer(8,filler));
		proto.setHandler(new Handler(){
			@Override
			public void onProtocolEnd(Buffer buf) throws Exception {
				String bin = new String( (byte[])buf.get(WebSocketProtocol.BINARY_DATA), "UTF-8" ); //oh well, I know it's a string
				String msg = new String( (byte[])buf.get(WebSocketProtocol.MESSAGE_DATA), "UTF-8" );
				System.out.println("binary: "+bin);
				System.out.println("received through websocket: " + msg);
				proto.terminate(); //got to close
			}
		});
		proto.setup();
		proto.start();
	}
}
