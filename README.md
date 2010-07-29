# Java Stream Protocol Builder

## What is it

This is a little project which is composed by a kind-of _smart buffer_ and a dead-easy _finite state machine_ framework. Basically, it makes (hopefully) very easy to create parser for network protocols (the example in the test code is a WebSocket implementation). The smart buffer acts like a stream, even if under the hood it, well, buffer the stream. So when you read a single byte, you are actually reading it from the buffer, not from the underlying stream.

The FSM framework is, as I said, pretty basic: you create a class which handle the protocol and annotate each method that corresponds to a _state_ of the FSM with a `@ProtocolState(n)` annotation. `n` identifies the state, and by convention the lowest identifier is the _start_ state. Each state-method has to return an `int`, which identifies the _next_ state to call, and they must accept a `dk.itu.frza.smartspb.Buffer` object (the buffer).

So, let's do a simple example:

	public class MyProtocol {
		
		@ProtocolState(0)
		public int initialMatch(Buffer buf) throws Exception {
			byte b = buf.read();
			if(b == 0) {
				return 1;
			} else if(b == 1){
				return 2;
			}
			return StreamProtocol.ERROR;
		}
		
		@ProtocolState(1)
		public int first(Buffer buf) throws Exception {
			byte[] msg = buf.read( 10 );
			buf.put(1,msg);
			return 2;
		}
		
		@ProtocolState(2)
		public int second(Buffer buf) throws Exception {
			byte delimiter = 0xf;
			byte b;
			buf.mark();
			while(delimiter != buf.read()){}
			byte[] msg = buf.readFromMark( 1 ); //get rid of the 1-byte length delimiter
			puf.put(2,msg);
			return END;
		}
	}
	
Here the `initialMatch` method is called first, as its identifier is 0. Based on the first byte read, it redirects to the `first`, `second` or an error state (which results in an exception).

The `first` state reads 10 bytes, store them in the extra area of the buffer and continue with the `second` state. This reads until a special byte is matched, store them and return the `END` state.

A complete application may looks like this:

	import dk.itu.frza.smartspb.*;
	public class Main implements Handler {
		public static void main(String[] args) throws Exception {
			InputStream is = /*obtain some input stream*/
			BufferFiller bf = new InputStreamFiller(is); //create a filler based on the InputStream
			Buffer buf = new Buffer(1024,bf); //the buffer will hold an array with length 1024, and
			//the the bf filler when needed
			StreamProtocol sp = new StreamProtocol();
			sp.setBuffer(buf);
			sp.setProtocol(new MyProtocol());
			sp.setHandler(new Main());
			
			sp.setup(); //analyze the protocol object
			sp.start(); //start consuming the stream
		}
		public void onProtocolEnd(Buffer buf) throws Exception {
			byte[] first = buf.get(1);
			byte[] second = buf.get(2);
			//well, do something with these
		}
	}