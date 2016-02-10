package ar.edu.itba.pdc.model.attachment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import ar.edu.itba.pdc.defs.Defs;
import ar.edu.itba.pdc.defs.Defs.ContentEncodingType;
import ar.edu.itba.pdc.defs.Defs.ErrorType;
import ar.edu.itba.pdc.defs.Defs.Method;
import ar.edu.itba.pdc.model.HTTP.HTTPBase;
import ar.edu.itba.pdc.model.HTTP.HTTPBase.Entity;
import ar.edu.itba.pdc.model.HTTP.HTTPResponse;
import ar.edu.itba.pdc.utils.BufferManager;
import ar.edu.itba.pdc.utils.ConverterHTTP;
import ar.edu.itba.pdc.utils.ErrorRespManager;
import ar.edu.itba.pdc.utils.LoggingManager;
import ar.edu.itba.pdc.utils.Statistics;

public class AttachmentHTTPResp extends AttachmentHTTP {

	protected SocketChannel clientChannel;

	protected ContentEncodingType contentEncoding = ContentEncodingType.NOT_PROVIDED;

	protected boolean readyToSend = false;

	protected boolean textPlain = true;

	public AttachmentHTTPResp() {
		super();
		state = States.READING_FIRST_LINE;
	}

	@Override
	public void setOpposite(SocketChannel channel) {
		this.clientChannel = channel;
	}

	@Override
	public SocketChannel getOpposite() {
		return clientChannel;
	}

	@Override
	public void finishWriting(SelectionKey key) throws IOException {
		resetWriteBuffer();
		key.interestOps(SelectionKey.OP_READ);
		SelectionKey clientKey = clientChannel.keyFor(key.selector());
		clientKey.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
		((AttachmentHTTP) clientKey.attachment())
				.setWriteBufferWrapper(writeBufferWrapper);
	}

	@Override
	public void endOfConnection(SelectionKey key) throws IOException {
		AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
		SocketChannel channel = attachmentHTTP.getOpposite();
		((AttachmentHTTP) channel.keyFor(key.selector()).attachment()).fullWrittenBuffer = true;
		key.channel().close();
		Statistics.getInstance().removeConnection();
		if (httpResponse != null)
			LoggingManager.logReport("RESP - " + httpRequest.getHost() + " "
					+ httpResponse.getStatusCode());
	}

	@Override
	public void reportError(SelectionKey key, ErrorType e,String msg) throws IOException {
		String error = ErrorRespManager.getStringError(e, msg);
		BufferManager.addStringToBuffer(error, writeBufferWrapper);
		((AttachmentHTTP) (key.attachment())).fullWrittenBuffer = true;
		endOfConnection(key);
	}

	@Override
	public Charset getCharset() {
		return httpResponse.getCharset();
	}

	@Override
	public void finishedReading(SelectionKey key) throws IOException {
		Statistics.getInstance().addVersion(httpResponse.getVersion());
		endOfConnection(key);
	}

	@Override
	public HTTPBase getHTTPObject() {
		return httpResponse;
	}

	@Override
	public boolean parseFirstLine(SelectionKey key) throws IOException {
		try {
			String firstLine = buffers[0].toString();
			String[] parts = firstLine.split(" ");
			String version = ConverterHTTP.convertVersion(parts[0].toString());
			int i = 1;
			while (parts[i].equals("")) {
				i++;
			}
			int code;
			try {
				code = Integer.parseInt(parts[i++].toString());
			} catch (Exception e) {
				reportError(key, ErrorType.BAD_REQUEST, "Error in first line");
				return false;
			}
			while (parts[i].equals("")) {
				i++;
			}
			StringBuilder builder = new StringBuilder();
			for (int j = i; j < parts.length; j++) {
				builder.append(parts[j].equals("") ? " " : parts[j]);
			}
			httpResponse = new HTTPResponse(version, code, builder.toString());
			state = States.READING_HEADER;
			buffers[0] = new StringBuffer();
			if (!leetOn)
				BufferManager
						.addStringToBuffer("HTTP/" + version + " " + code + " "
								+ builder.toString() + "\r\n",
								writeBufferWrapper);
			return true;
		} catch (RuntimeException e) {
			LoggingManager.logError("Buffer reached max capacity");
			reportError(key, ErrorType.BUFFER_OVERFLOW, "Internal Buffer Overflow");
			return false;
		}
	}

	@Override
	public boolean parseHeader(SelectionKey key) throws IOException {
		try {
			String line = buffers[0].toString();
			if (line.equals("\n") || line.equals("\r\n")) {
				buffers[0] = new StringBuffer();

				if (leetOn) {
					if (contentEncoding == ContentEncodingType.NOT_PROVIDED) {
						fillHeaders(key);
					}
					if (!readyToSend) {
						reportError(key, ErrorType.CANT_LEET, "Can't make transformation");
						return false;
					}
				}

				BufferManager.addStringToBuffer("Connection: close\r\n\r\n",
						writeBufferWrapper);
				if (httpRequest.getMethod() == Method.HEAD
						|| httpResponse.getStatusCode() / 100 == 1
						|| httpResponse.getStatusCode() == 204
						|| httpResponse.getStatusCode() == 304) {
					finishedReading(key);
				} else {
					buffers[1] = new StringBuffer();
					if (httpResponse.isChunkedBody()) {
						state = States.READING_BODY_CHUNKED_FIRST_LINE;
					} else if (httpResponse.isMultipart()
							&& httpResponse.getBodyLength() == null) {
						state = States.READING_BODY_MULTIPART;
					} else {
						state = States.READING_BODY_NORMAL;
						leftLen = httpResponse.getBodyLength();
					}
				}
			} else {
				StringBuilder name = new StringBuilder();
				StringBuilder value = new StringBuilder();
				boolean foundColon = false;
				for (int i = 0; i < line.length(); i++) {
					char ch = line.charAt(i);
					if (ch != '\r' && ch != '\n') {
						if (ch == ':') {
							if (!foundColon)
								foundColon = true;
							else
								value.append(ch);
						} else {
							if (!foundColon) {
								name.append(ch);
							} else {
								value.append(ch);
							}
						}
					}
				}
				String val = value.toString().trim();
				if (!name.toString().toUpperCase().equals(Defs.CONNECTION)) {
					httpResponse.addHeader(name.toString(), val);
					if (!leetOn) {
						BufferManager.addStringToBuffer(name + ":" + val
								+ "\r\n", writeBufferWrapper);
					} else {
						if (readyToSend) {
							BufferManager.addStringToBuffer(name + ":" + val
									+ "\r\n", writeBufferWrapper);
						} else {
							if (name.toString().equalsIgnoreCase(
									"CONTENT-ENCODING")) {
								if (val.equalsIgnoreCase("GZIP")) {
									contentEncoding = ContentEncodingType.GZIP;
								} else if (val.equalsIgnoreCase("IDENTITY")) {
									contentEncoding = ContentEncodingType.IDENTITY;
									fillHeaders(key);
								} else {
									contentEncoding = ContentEncodingType.OTHER;
								}
							} else if (name.toString().equalsIgnoreCase(
									"CONTENT-TYPE")) {
								if (!val.split(";")[0]
										.equalsIgnoreCase("TEXT/PLAIN")) {
									fillHeaders(key);
									textPlain = false;
								}
							} else if (name.toString().equalsIgnoreCase(
									"TRANSFER-ENCODING")
									&& val.contains("gzip")) {
								contentEncoding = ContentEncodingType.GZIP;
							}
						}
					}
				}
				buffers[0] = new StringBuffer();
				state = States.READING_HEADER;
			}
			return true;
		} catch (RuntimeException e) {
			LoggingManager.logError("Buffer reached max capacity");
			reportError(key, ErrorType.BUFFER_OVERFLOW, "Internal Buffer Overflow");
			return false;
		}
	}

	private void fillHeaders(SelectionKey key) {
		if (readyToSend)
			return;
		readyToSend = true;
		BufferManager.addStringToBuffer(
				"HTTP/" + httpResponse.getVersion() + " "
						+ httpResponse.getStatusCode() + " "
						+ httpResponse.getStatusMessage() + "\r\n",
				writeBufferWrapper);
		for (Entity each : httpResponse.getHeaders()) {
			BufferManager.addStringToBuffer(each.key + ":" + each.value
					+ "\r\n", writeBufferWrapper);
		}

	}

	@Override
	public void addReadStatistic(long amount) {
		Statistics.getInstance().addBytesRecFromSrv(amount);
	}

	@Override
	public void addWriteStatistic(long amount) {
		Statistics.getInstance().addBytesSentToSrv(amount);
	}

	@Override
	public void registerInTimer(SelectionKey key) {
	}

	@Override
	public int parseBody(SelectionKey key) throws IOException {
		try {
			if (!leetOn || !textPlain) {
				BufferManager.addByteToBuffer(
						BufferManager.getByteFromBuffer(readBuffer),
						writeBufferWrapper);
				return 1;
			} else {
				ByteBuffer bf = null;
				if (leftLen!=null && readBuffer.remaining() > leftLen) {
					byte[] arrB = new byte[(int) (long) leftLen];
					for (int i = 0; i < leftLen; i++) {
						arrB[i] = readBuffer.get();
					}
					bf = ByteBuffer.wrap(arrB);
				} else {
					bf = readBuffer;
				}
				int pos = writeBufferWrapper.byteBuffer.position();
				CharBuffer buffer = BufferManager.getCharBufferFromBuffer(bf,
						getCharset());
				buffer.flip();
				char[] arr = new char[buffer.remaining()];
				for (int i = 0; buffer.hasRemaining(); i++) {
					arr[i] = buffer.get();
				}
				for (int i = 0; i < arr.length; i++) {
					arr[i] = applyLeet(arr[i]);
				}
				BufferManager.addCharBufferToBuffer(CharBuffer.wrap(arr),
						writeBufferWrapper, getCharset());
				return writeBufferWrapper.byteBuffer.position() - pos;
			}
		} catch (RuntimeException e) {
			LoggingManager.logError("Buffer reached max capacity");
			finishedReading(key);
			return -1;
		}

	}

	private char applyLeet(char c) {
		switch (c) {
		case 'a':
			return '4';
		case 'e':
			return '3';
		case 'i':
			return '1';
		case 'o':
			return '0';
		case 'c':
			return '<';
		default:
			return c;
		}
	}

	@Override
	public void handleError(SelectionKey key, ErrorType error, String msg)
			throws IOException {
		finishedReading(key);
	}
}
