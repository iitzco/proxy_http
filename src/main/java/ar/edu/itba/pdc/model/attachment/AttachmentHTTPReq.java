package ar.edu.itba.pdc.model.attachment;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import ar.edu.itba.pdc.defs.Defs;
import ar.edu.itba.pdc.defs.Defs.ErrorType;
import ar.edu.itba.pdc.defs.Defs.Method;
import ar.edu.itba.pdc.model.HTTP.HTTPBase;
import ar.edu.itba.pdc.model.HTTP.HTTPRequest;
import ar.edu.itba.pdc.utils.BufferManager;
import ar.edu.itba.pdc.utils.ConnectionManager;
import ar.edu.itba.pdc.utils.ConverterHTTP;
import ar.edu.itba.pdc.utils.ErrorRespManager;
import ar.edu.itba.pdc.utils.LoggingManager;
import ar.edu.itba.pdc.utils.PoolClientConnectionManager;
import ar.edu.itba.pdc.utils.Statistics;

public class AttachmentHTTPReq extends AttachmentHTTP {

	protected SocketChannel serverChannel;

	protected boolean presentTextPlain = false;

	protected String acceptEncodingValue;

	public AttachmentHTTPReq() {
		super();
		state = States.READING_FIRST_LINE;
	}

	@Override
	public void setOpposite(SocketChannel channel) {
		this.serverChannel = channel;
	}

	@Override
	public SocketChannel getOpposite() {
		return serverChannel;
	}

	@Override
	public void finishWriting(SelectionKey key) throws IOException {
		endOfConnection(key);
	}

	@Override
	public void endOfConnection(SelectionKey key) throws IOException {
		if (serverChannel != null) {
			serverChannel.close();
		}
		Statistics.getInstance().removeConnection();
		key.channel().close();
	}

	@Override
	public void reportError(SelectionKey key, ErrorType e, String msg)
			throws IOException {
		String error = ErrorRespManager.getStringError(e, msg);
		BufferManager.resetBuffer(writeBufferWrapper.byteBuffer);
		BufferManager.addStringToBuffer(error, writeBufferWrapper);
		key.interestOps(SelectionKey.OP_WRITE);
		((AttachmentHTTP) (key.attachment())).fullWrittenBuffer = true;
		LoggingManager.logReport("RESP - " + e.toString());
	}

	@Override
	public Charset getCharset() {
		return httpRequest.getCharset();
	}

	@Override
	public void finishedReading(SelectionKey key) {
		((AttachmentHTTP) serverChannel.keyFor(key.selector()).attachment()).fullWrittenBuffer = true;
		PoolClientConnectionManager.getInstance().removeClient(key);
		Statistics.getInstance().addMethod(httpRequest.getMethod());
		if (httpRequest.getHost() != null) {
			LoggingManager.logReport("REQ - " + httpRequest.getMethod() + " "
					+ httpRequest.getHost());
		} else {
			LoggingManager.logError("REQ - " + httpRequest.getMethod() + " "
					+ "Unkown host");
		}
	}

	@Override
	public HTTPBase getHTTPObject() {
		return httpRequest;
	}

	@Override
	public boolean parseFirstLine(SelectionKey key) throws IOException {
		try {
			String firstLine = buffers[0].toString().replaceAll("\\s+", " ");
			String[] parts = firstLine.split(" ");
			if (parts.length != 3) {
				reportError(key, ErrorType.BAD_REQUEST, "Error in first line");
				return false;
			}
			String method = parts[0];
			String resource = parts[1];
			String version = parts[2];
			if (resource.startsWith("http://")) {
				resource = deleteHost(resource);
			}
			httpRequest = new HTTPRequest(ConverterHTTP.convertMethod(parts[0]
					.toString()), parts[1].toString(),
					ConverterHTTP.convertVersion(ConverterHTTP
							.convertValue(parts[2].toString())));
			buffers[0] = new StringBuffer();
			state = States.READING_HEADER;
			BufferManager.addStringToBuffer(method + " " + resource + " "
					+ version + '\r' + '\n', writeBufferWrapper);
		} catch (RuntimeException e) {
			LoggingManager.logError("Buffer reached max capacity");
			reportError(key, ErrorType.INTERNAL_SERVER_ERROR,"Internal proxy error");
			return false;
		}
		return true;

	}

	private String deleteHost(String resource) {
		StringBuilder builder = new StringBuilder();
		int count = 0;
		for (int i = 0; i < resource.length(); i++) {
			if (count < 3 && resource.charAt(i) == '/') {
				count++;
			}
			if (count == 3) {
				builder.append(resource.charAt(i));
			}
		}
		return builder.toString();
	}

	@Override
	public boolean parseHeader(SelectionKey key) throws IOException {
		try {
			String line = buffers[0].toString();
			if (line.equals("\n") || line.equals("\r\n")) {
				if (leetOn) {
					if (presentTextPlain
							|| httpRequest.getResource().endsWith(".txt")) {
						presentTextPlain = true;
						BufferManager.addStringToBuffer(
								"Accept-Encoding: identity\r\n",
								writeBufferWrapper);
					} else {
						if (acceptEncodingValue != null)
							BufferManager.addStringToBuffer("Accept-Encoding: "
									+ acceptEncodingValue + "\r\n",
									writeBufferWrapper);
					}
				}
				if (httpRequest.getHost() == null) {
					reportError(key, ErrorType.NO_HOST_SPECIFIED, "No Host specified");
					return false;
				} else {
					buffers[0] = new StringBuffer();
					BufferManager.addStringToBuffer("\r\n", writeBufferWrapper);
					if (httpRequest.getMethod() == Method.POST) {
						buffers[1] = new StringBuffer();
						if (httpRequest.isChunkedBody()) {
							state = States.READING_BODY_CHUNKED_FIRST_LINE;
						} else if (httpRequest.isMultipart()
								&& httpRequest.getBodyLength() == null) {
							state = States.READING_BODY_MULTIPART;
						} else {
							state = States.READING_BODY_NORMAL;
							leftLen = httpRequest.getBodyLength();
						}
					} else {
						state = States.IDLE;
						finishedReading(key);
					}
					return true;
				}
			} else {

				StringBuilder name = new StringBuilder();
				StringBuilder value = new StringBuilder();
				boolean foundColon = false;
				for (int i = 0; i < line.length(); i++) {
					char ch = line.charAt(i);
					if (ch != '\r' && ch != '\n') {
						if (ch == ':') {
							foundColon = true;
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
				if (leetOn) {
					if (name.toString().equalsIgnoreCase(Defs.ACCEPT_ENCODING)) {
						acceptEncodingValue = val;
					} else {
						if (name.toString().equalsIgnoreCase(Defs.ACCEPT)) {
							String[] split = val.split(",");
							for (String string : split) {
								String[] vecString = string.split(";");
								if (vecString[0].equalsIgnoreCase("text/plain")) {
									presentTextPlain = true;
								}
							}
						}
						BufferManager.addStringToBuffer(name + ":" + val
								+ "\r\n", writeBufferWrapper);

					}
				} else {
					BufferManager.addStringToBuffer(name + ":" + val + "\r\n",
							writeBufferWrapper);

				}

				httpRequest.addHeader(name.toString(), val);
				if (httpRequest.getHost() != null) {
					if (!ConnectionManager.isTryingToConnect(key)) {
						ConnectionManager.startConnectionWithServer(key);
					}
				}
				state = States.READING_HEADER;
				buffers[0] = new StringBuffer();
				return true;
			}
		} catch (RuntimeException e) {
			LoggingManager.logError("Buffer reached max capacity");
			reportError(key, ErrorType.BUFFER_OVERFLOW,"Internal Buffer Overflow");
			return false;
		}
	}

	@Override
	public void addReadStatistic(long amount) {
		Statistics.getInstance().addBytesRecFromClt(amount);
	}

	@Override
	public void addWriteStatistic(long amount) {
		Statistics.getInstance().addBytesSentToClt(amount);
	}

	@Override
	public void registerInTimer(SelectionKey key) {
		PoolClientConnectionManager.getInstance().registerClient(key);
	}

	@Override
	public int parseBody(SelectionKey key) throws IOException {
		try {
			BufferManager.addByteToBuffer(
					BufferManager.getByteFromBuffer(readBuffer),
					writeBufferWrapper);
			return 1;
		} catch (RuntimeException e) {
			reportError(key, ErrorType.INTERNAL_SERVER_ERROR, "Internal proxy error");
			return -1;
		}
	}

	@Override
	public void handleError(SelectionKey key, ErrorType error, String msg)
			throws IOException {
		reportError(key, error,msg);
	}
}
