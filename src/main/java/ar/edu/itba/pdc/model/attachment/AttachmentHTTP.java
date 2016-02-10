package ar.edu.itba.pdc.model.attachment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import ar.edu.itba.pdc.defs.Defs;
import ar.edu.itba.pdc.defs.Defs.ErrorType;
import ar.edu.itba.pdc.model.HTTP.HTTPBase;
import ar.edu.itba.pdc.model.HTTP.HTTPRequest;
import ar.edu.itba.pdc.model.HTTP.HTTPResponse;
import ar.edu.itba.pdc.utils.BufferManager;
import ar.edu.itba.pdc.utils.ByteBufferWrapper;

public abstract class AttachmentHTTP {

	protected ByteBuffer readBuffer;

	protected ByteBufferWrapper writeBufferWrapper;

	public States state;

	public HTTPResponse httpResponse;

	public HTTPRequest httpRequest;

	protected StringBuffer[] buffers;

	protected Long leftLen = null;

	protected boolean isConnected;

	protected boolean fullWrittenBuffer;

	protected boolean leetOn;

	public AttachmentHTTP() {
		readBuffer = ByteBuffer.allocate(Defs.READ_BUFFER_CAPACITY);
		buffers = new StringBuffer[3];
		buffers[0] = new StringBuffer();
		writeBufferWrapper = new ByteBufferWrapper(
				ByteBuffer.allocate(Defs.WRITE_BUFFER_CAPACITY));
		isConnected = false;
		fullWrittenBuffer = false;
		leetOn = Defs.APPLY_L33T;
	}

	public void resetWriteBuffer() {
		writeBufferWrapper.byteBuffer = ByteBuffer
				.allocate(Defs.WRITE_BUFFER_CAPACITY);
	}

	public ByteBuffer getReadBuffer() {
		return readBuffer;
	}

	public States getState() {
		return state;
	}

	public abstract void addReadStatistic(long amount);

	public abstract void addWriteStatistic(long amount);

	public abstract void setOpposite(SocketChannel channel);

	public abstract SocketChannel getOpposite();

	public abstract void finishWriting(SelectionKey key) throws IOException;

	public abstract void endOfConnection(SelectionKey key) throws IOException;

	public abstract void finishedReading(SelectionKey key) throws IOException;

	public abstract HTTPBase getHTTPObject();

	public abstract void reportError(SelectionKey key, ErrorType e, String msg)
			throws IOException;

	public abstract Charset getCharset();

	public boolean isConnected() {
		return isConnected;
	}

	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}

	public boolean hasFullWrittenBuffer() {
		return !fullWrittenBuffer;
	}

	public StringBuffer[] getBuffers() {
		return buffers;
	}

	public abstract boolean parseFirstLine(SelectionKey key) throws IOException;

	public abstract boolean parseHeader(SelectionKey key) throws IOException;

	public abstract int parseBody(SelectionKey key) throws IOException;

	public abstract void handleError(SelectionKey key, ErrorType error,
			String msg) throws IOException;

	public boolean parseFirstLineOfChunk(SelectionKey key) throws IOException {
		try {
			String line = buffers[0].toString();
			buffers[1] = new StringBuffer();
			for (int i = 0; i < line.length(); i++) {
				char c = line.charAt(i);
				if (c == ';')
					break;
				else
					buffers[1].append(line.charAt(i));
			}
			leftLen = Long.parseLong(buffers[1].toString().trim(), 16);
			if (leftLen == 0) {
				buffers[1] = new StringBuffer();
				state = States.READING_BODY_CHUNKED_TRAILER;
			} else {
				state = States.READING_BODY_CHUNKED_BODY;
			}
			BufferManager.addStringToBuffer(line + '\r' + '\n',
					writeBufferWrapper, getCharset());
			buffers[0] = new StringBuffer();
			return true;
		} catch (RuntimeException e) {
			handleError(key, ErrorType.INTERNAL_SERVER_ERROR,
					"Internal proxy error");
			return false;
		}
	}

	public boolean isLeetOn() {
		return leetOn;
	}

	public abstract void registerInTimer(SelectionKey key);

	public boolean parseBodyMultipart(SelectionKey key) throws IOException {
		try {
			CharBuffer buffer = BufferManager.getCharBufferFromBuffer(
					readBuffer, getCharset());
			buffer.flip();
			while (buffer.hasRemaining()) {
				char c = buffer.get();
				BufferManager.addCharacterToBuffer(c, writeBufferWrapper,
						getCharset());
				buffers[0].append(c);
				if (isFinishedBoundary()) {
					finishedReading(key);
					state = States.IDLE;
				}
			}
			return true;
		} catch (RuntimeException e) {
			handleError(key, ErrorType.INTERNAL_SERVER_ERROR,
					"Internal proxy error");
			return false;
		}
	}

	private boolean isFinishedBoundary() {
		if (buffers[0].length() >= getHTTPObject().getBoundaryEnd().length()) {
			int len = getHTTPObject().getBoundaryEnd().length();
			String s = buffers[0].substring(buffers[0].length() - len);
			if (s.equals(getHTTPObject().getBoundaryEnd()))
				return true;
			buffers[0] = buffers[0].deleteCharAt(0);
		}
		return false;
	}

	public void setWriteBufferWrapper(ByteBufferWrapper writeBufferWrapper) {
		this.writeBufferWrapper = writeBufferWrapper;
	}

	public ByteBufferWrapper getWriteBufferWrapper() {
		return writeBufferWrapper;
	}
}
