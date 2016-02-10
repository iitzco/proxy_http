package ar.edu.itba.pdc.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

import ar.edu.itba.pdc.defs.Defs;
import ar.edu.itba.pdc.defs.Defs.ErrorType;
import ar.edu.itba.pdc.model.attachment.AttachmentHTTP;
import ar.edu.itba.pdc.model.attachment.AttachmentHTTPReq;
import ar.edu.itba.pdc.model.attachment.AttachmentHTTPResp;
import ar.edu.itba.pdc.model.attachment.States;

public class ConnectionManager {

	private ConnectionManager() {
		new AssertionError();
	}

	public static boolean isTryingToConnect(SelectionKey key) {
		AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
		SocketChannel channel = attachmentHTTP.getOpposite();
		if (channel == null)
			return false;
		return channel.keyFor(key.selector()) != null;
	}

	public static boolean isServerConnected(SelectionKey key) {
		AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
		SocketChannel channel = attachmentHTTP.getOpposite();
		if (channel == null)
			return false;
		return ((AttachmentHTTP) channel.keyFor(key.selector()).attachment())
				.isConnected();
	}

	private static void connectToServer(SelectionKey key) throws IOException {
		AttachmentHTTP attachmentReq = (AttachmentHTTP) key.attachment();
		AttachmentHTTP attachmentResp = new AttachmentHTTPResp();
		SocketChannel srvrChan = SocketChannel.open();
		srvrChan.configureBlocking(false);

		attachmentReq.setOpposite(srvrChan);
		attachmentResp.setOpposite((SocketChannel) key.channel());
		attachmentResp.httpRequest = attachmentReq.httpRequest;

		// Seteo lo que tengo que escribir en el servidor
		attachmentResp.setWriteBufferWrapper(attachmentReq
				.getWriteBufferWrapper());

		srvrChan.register(key.selector(), SelectionKey.OP_CONNECT,
				attachmentResp);

		try {
			srvrChan.connect(new InetSocketAddress(((AttachmentHTTP) key
					.attachment()).httpRequest.getHost(), Defs.HTTP_PORT));
		} catch (UnresolvedAddressException e) {
			srvrChan.close();
			attachmentReq.reportError(key, ErrorType.UNKNOWN_HOST,
					"Unresolved Host");
			attachmentResp.state = States.IDLE;
		}
	}

	public static void startConnectionWithServer(SelectionKey key)
			throws IOException {
		AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
		if (attachmentHTTP.httpRequest.getHost() == null) {
			attachmentHTTP.reportError(key, ErrorType.UNKNOWN_HOST,
					"Unknown Host");
			attachmentHTTP.state = States.IDLE;
		} else {
			connectToServer(key);
		}
	}

	public static void reopen(SelectionKey key) throws IOException {
		SocketChannel channel = ((AttachmentHTTP) key.attachment())
				.getOpposite();
		AttachmentHTTPReq attachmentReq = (AttachmentHTTPReq) channel.keyFor(
				key.selector()).attachment();
		AttachmentHTTPResp attachmentResp = (AttachmentHTTPResp) key
				.attachment();

		SocketChannel srvrChan = SocketChannel.open();
		srvrChan.configureBlocking(false);

		attachmentReq.setOpposite(srvrChan);
		attachmentResp.setOpposite(channel);
		attachmentResp.httpRequest = attachmentReq.httpRequest;

		attachmentResp.setWriteBufferWrapper(attachmentReq
				.getWriteBufferWrapper());

		SelectionKey newKey = srvrChan.register(key.selector(),
				SelectionKey.OP_CONNECT, attachmentResp);

		try {
			srvrChan.connect(new InetSocketAddress(((AttachmentHTTP) key
					.attachment()).httpRequest.getHost(), Defs.HTTP_PORT));
		} catch (UnresolvedAddressException e) {
			srvrChan.close();
			attachmentReq.reportError(channel.keyFor(key.selector()),
					ErrorType.UNKNOWN_HOST, "Unresolved Host");
			attachmentResp.state = States.IDLE;
		}
		PoolServerConnectionManager.getInstance().renewSuscription(key, newKey);
	}
}
