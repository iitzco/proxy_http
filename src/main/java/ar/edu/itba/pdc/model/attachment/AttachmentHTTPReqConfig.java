package ar.edu.itba.pdc.model.attachment;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Base64;

import ar.edu.itba.pdc.defs.Defs;
import ar.edu.itba.pdc.defs.Defs.ErrorType;
import ar.edu.itba.pdc.defs.Defs.Method;
import ar.edu.itba.pdc.defs.Defs.ProvidedResponseType;
import ar.edu.itba.pdc.model.HTTP.HTTPRequest;
import ar.edu.itba.pdc.utils.BufferManager;
import ar.edu.itba.pdc.utils.ConverterHTTP;
import ar.edu.itba.pdc.utils.LoggingManager;
import ar.edu.itba.pdc.utils.Statistics;

public class AttachmentHTTPReqConfig extends AttachmentHTTPReq {

	private boolean passAuth = false;
	private boolean lenProvided = false;

	@Override
	public void finishedReading(SelectionKey key) {
		StringBuilder body = new StringBuilder();
		if (writeBufferWrapper.byteBuffer.position() != 2
				&& writeBufferWrapper.byteBuffer.position() != 3) {
			try {
				reportError(key, ErrorType.BAD_REQUEST,"Body not accepted");
			} catch (IOException e) {
			}
			return;
		}
		writeBufferWrapper.byteBuffer.flip();
		while (writeBufferWrapper.byteBuffer.hasRemaining()) {
			body.append(BufferManager
					.getCharacterFromBuffer(writeBufferWrapper.byteBuffer));
		}
		String b = body.toString();
		try {
			if (b.equals("ON")) {
				Defs.APPLY_L33T = true;
				LoggingManager.logConfig("leet ON");
				provideResponseForPost(ProvidedResponseType.LEET_ON, key);
			} else if (b.equals("OFF")) {
				Defs.APPLY_L33T = false;
				LoggingManager.logConfig("leet OFF");
				provideResponseForPost(ProvidedResponseType.LEET_OFF, key);
			} else {
				reportError(key, ErrorType.BAD_REQUEST, "Body not accepted");
			}
		} catch (IOException e) {
			return;
		}
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
			Method method = ConverterHTTP.convertMethod(parts[0]);
			String resource = parts[1];
			String version = ConverterHTTP.convertVersion(ConverterHTTP
					.convertValue(parts[2].toString()));

			if (!passFilterOfFirstLine(method, resource, version)) {
				reportError(key, ErrorType.BAD_REQUEST, "Error in first line");
				return false;
			}
			httpRequest = new HTTPRequest(method, resource, version);
			buffers[0] = new StringBuffer();
			state = States.READING_HEADER;
			return true;
		} catch (RuntimeException e) {
			LoggingManager.logError("Buffer reached max capacity");
			reportError(key, ErrorType.BUFFER_OVERFLOW, "Internal Buffer Overflow");
			return false;
		}
	}

	private boolean passFilterOfFirstLine(Method method, String resource,
			String version) {
		if (version == "Unknown")
			return false;
		if ((method == Method.GET && resource.equals("/"))
				|| ((method == Method.POST && resource
						.equalsIgnoreCase("/leet"))))
			return true;
		return false;
	}

	@Override
	public boolean parseHeader(SelectionKey key) throws IOException {
		try {
			String line = buffers[0].toString();
			if (line.equals("\n") || line.equals("\r\n")) {
				buffers[0] = new StringBuffer();
				if (httpRequest.getMethod() == Method.POST) {
					if (httpRequest.getHeaders().size() != 2 || !passAuth
							|| !lenProvided) {
						reportError(key, ErrorType.BAD_REQUEST, "Wrong Headers");
						return false;
					}
					buffers[1] = new StringBuffer();
					state = States.READING_BODY_NORMAL;
					leftLen = httpRequest.getBodyLength();
				} else if (httpRequest.getMethod() == Method.GET) {
					if (httpRequest.getHeaders().size() != 1 || !passAuth) {
						reportError(key, ErrorType.BAD_REQUEST, "Wrong Headers");
						return false;
					}
					provideStatistics(key);
				}
				return true;
			} else {
				return parseContentHeader(line, key);
			}
		} catch (RuntimeException e) {
			LoggingManager.logError("Buffer reached max capacity");
			reportError(key, ErrorType.BUFFER_OVERFLOW, "Internal Buffer Overflow");
			return false;
		}
	}

	private boolean parseContentHeader(String line, SelectionKey key)
			throws IOException {
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
		String n = name.toString();
		String val = value.toString().trim();
		if (n.equalsIgnoreCase("Authorization")) {
			String authorization = getAuthorization(val);
			try {
				String auth = new String(Base64.getDecoder().decode(
						authorization.getBytes()));
				if (!auth.equals(Defs.USER + ":" + Defs.PASS)) {
					LoggingManager.logConfig("Failed login with " + auth);
					reportError(key, ErrorType.AUTHENTICATION_FAILED, "Authentication failed");
					return false;
				} else {
					passAuth = true;
				}
			} catch (IllegalArgumentException e) {
				reportError(key, ErrorType.AUTHENTICATION_FAILED, "Authentication failed");
				return false;
			}
		} else if (n.equalsIgnoreCase("Content-length")) {
			lenProvided = true;
		} else {
			reportError(key, ErrorType.BAD_REQUEST, "Headers not acceptable");
			return false;
		}
		httpRequest.addHeader(n, val);
		state = States.READING_HEADER;
		buffers[0] = new StringBuffer();
		return true;
	}

	private String getAuthorization(String val) {
		StringBuilder base64 = new StringBuilder();
		boolean foundSpace = false;
		for (int i = 0; i < val.length(); i++) {
			char ch = val.charAt(i);
			if (ch == ' ') {
				foundSpace = true;
			} else {
				if (foundSpace) {
					base64.append(ch);
				}
			}
		}
		return base64.toString();
	}

	private void provideResponseForPost(
			ProvidedResponseType providedResponseType, SelectionKey key)
			throws IOException {
		BufferManager.resetBuffer(writeBufferWrapper.byteBuffer);
		BufferManager.addStringToBuffer(getStringResponse(providedResponseType), writeBufferWrapper);
		key.interestOps(SelectionKey.OP_WRITE);
		((AttachmentHTTP) (key.attachment())).fullWrittenBuffer = true;
	}

	private String getStringResponse(ProvidedResponseType providedResponseType){
		StringBuilder builder = new StringBuilder();
		builder.append("HTTP/1.1 200 ok\r\n");
		builder.append("Connection: close\r\n");
		builder.append("Content-type: text/plain\r\n");
		if (providedResponseType == ProvidedResponseType.LEET_ON){
			builder.append("Content-length:29\r\n\r\n");
			builder.append("Transformacion leet activada.");
		}else{
			builder.append("Content-length:32\r\n\r\n");
			builder.append("Transformacion leet desactivada.");
		}
		return builder.toString();
	}

	private void provideStatistics(SelectionKey key) {
		StringBuilder builder = new StringBuilder();
		builder.append("HTTP/1.1 200 ok\r\n");
		builder.append("Connection: close\r\n");
		builder.append("Content-type: application/json\r\n");
		String json = Statistics.getInstance().dataJSON();
		builder.append("Content-length: " + (json.length() + 2) + "\r\n");
		builder.append("\r\n" + json + "\r\n");
		BufferManager.resetBuffer(writeBufferWrapper.byteBuffer);
		BufferManager.addStringToBuffer(builder.toString(), writeBufferWrapper);
		key.interestOps(SelectionKey.OP_WRITE);
		((AttachmentHTTP) (key.attachment())).fullWrittenBuffer = true;
		LoggingManager.logConfig("Request for Statistics");
	}

}
