package ar.edu.itba.pdc.utils;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import ar.edu.itba.pdc.defs.Defs;

public class BufferManager {

	private BufferManager() {
		new AssertionError();
	}

	public static Character getCharacterFromBuffer(ByteBuffer buff) {
		byte b = buff.get();
		ByteBuffer inputBuffer = ByteBuffer.wrap(new byte[] { b });
		Charset iso88591charset = Charset.forName(Defs.ISO_8859_1);
		CharBuffer charBuffer = iso88591charset.decode(inputBuffer);
		return charBuffer.get();

	}

	public static CharBuffer getCharBufferFromBuffer(ByteBuffer buff,
			Charset charset) {
		CharsetDecoder decoder = charset.newDecoder();
		CharBuffer outBuffer = CharBuffer.allocate(buff.capacity());
		decoder.decode(buff, outBuffer, false);
		return outBuffer;
	}

	public static byte getByteFromBuffer(ByteBuffer buff) {
		return buff.get();
	}

	public static void addCharBufferToBuffer(CharBuffer charBuffer,
			ByteBufferWrapper buffWrapper, Charset charset) {
		CharsetEncoder encoder = charset.newEncoder();
		ByteBuffer aux = ByteBuffer.allocate(charBuffer.capacity() * 16);
		encoder.encode(charBuffer, aux, true);
		aux.flip();
//		encoder.flush(aux);
		put(buffWrapper, aux);
	}

	public static void addCharacterToBuffer(char c,
			ByteBufferWrapper buffWrapper) {
		CharBuffer charBuffer = CharBuffer.wrap(new char[] { c });
		Charset iso88591charset = Charset.forName(Defs.ISO_8859_1);
		ByteBuffer b = iso88591charset.encode(charBuffer);
		put(buffWrapper, b);
	}

	public static void addStringToBuffer(String s, ByteBufferWrapper buffWrapper) {
		CharBuffer charBuffer = CharBuffer.wrap(s.toCharArray());
		Charset iso88591charset = Charset.forName(Defs.ISO_8859_1);
		ByteBuffer b = iso88591charset.encode(charBuffer);
		put(buffWrapper, b);
	}

	public static void addCharacterToBuffer(char c,
			ByteBufferWrapper buffWrapper, Charset charset) {
		CharBuffer charBuffer = CharBuffer.wrap(new char[] { c });
		ByteBuffer b = charset.encode(charBuffer);
		put(buffWrapper, b);
	}

	public static void addStringToBuffer(String s,
			ByteBufferWrapper buffWrapper, Charset charset) {
		CharBuffer charBuffer = CharBuffer.wrap(s.toCharArray());
		ByteBuffer b = charset.encode(charBuffer);
		put(buffWrapper, b);
	}

	public static void addByteToBuffer(Byte b, ByteBufferWrapper buffWrapper) {
		put(buffWrapper, b);
	}

	public static void resetBuffer(ByteBuffer buff) {
		buff.clear();
	}

	private static void put(ByteBufferWrapper bufferWrapper, ByteBuffer toWrite) {
		if (bufferWrapper.byteBuffer.remaining() < toWrite.remaining()) {
			long doble = bufferWrapper.byteBuffer.capacity() * 2;
			while (doble - bufferWrapper.byteBuffer.remaining() < toWrite
					.remaining()) {
				doble *= 2;
			}
			if (doble >= Defs.MAX_IN_MEMORY_BUFFER_CAPACITY) {
				throw new BufferOverflowException();
			}
			ByteBuffer aux = ByteBuffer.allocate((int) doble);
			bufferWrapper.byteBuffer.flip();
			aux.put(bufferWrapper.byteBuffer);
			bufferWrapper.byteBuffer = aux;
		}
		bufferWrapper.byteBuffer.put(toWrite);
	}

	private static void put(ByteBufferWrapper bufferWrapper, Byte b) {
		if (bufferWrapper.byteBuffer.remaining() == 0) {
			if (bufferWrapper.byteBuffer.capacity() * 2 >= Defs.MAX_IN_MEMORY_BUFFER_CAPACITY) {
				throw new BufferOverflowException();
			}
			ByteBuffer aux = ByteBuffer.allocate(bufferWrapper.byteBuffer
					.capacity() * 2);
			bufferWrapper.byteBuffer.flip();
			aux.put(bufferWrapper.byteBuffer);
			bufferWrapper.byteBuffer = aux;
		}
		bufferWrapper.byteBuffer.put(b);
	}

}
