package ar.edu.itba.pdc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import ar.edu.itba.pdc.defs.Defs.ErrorType;
import ar.edu.itba.pdc.model.attachment.AttachmentAccepter;
import ar.edu.itba.pdc.model.attachment.AttachmentHTTP;
import ar.edu.itba.pdc.model.attachment.AttachmentHTTPReq;
import ar.edu.itba.pdc.model.attachment.AttachmentHTTPResp;
import ar.edu.itba.pdc.utils.ConnectionManager;
import ar.edu.itba.pdc.utils.LoggingManager;
import ar.edu.itba.pdc.utils.PoolServerConnectionManager;
import ar.edu.itba.pdc.utils.Statistics;

public class ProxySelectorProtocol implements TCPProtocol {

	public void handleAccept(SelectionKey key) throws IOException {
		AttachmentAccepter accepter = (AttachmentAccepter) key.attachment();
		accepter.handleIncomeClient(key);
	}

	public void handleRead(SelectionKey key) throws IOException {
		SocketChannel chan = (SocketChannel) key.channel();
		AttachmentHTTP attachment = (AttachmentHTTP) key.attachment();
		long bytesRead = -2;
		try {
			bytesRead = chan.read(attachment.getReadBuffer());
			if (bytesRead == -1) {
				attachment.endOfConnection(key);
			} else if (bytesRead > 0) {
				attachment.addReadStatistic(bytesRead);
				handleIncomeData(key);
			}
		} catch (IOException e) {
		}
	}

	public void handleWrite(SelectionKey key) throws IOException {
		AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
		ByteBuffer buf = attachmentHTTP.getWriteBufferWrapper().byteBuffer;
		buf.flip();
		SocketChannel chan = (SocketChannel) key.channel();
		try {
			long bytesWritten = chan.write(buf);
			attachmentHTTP.addWriteStatistic(bytesWritten);
			if (!buf.hasRemaining() && !attachmentHTTP.hasFullWrittenBuffer()) {
				attachmentHTTP.finishWriting(key);
			}
		} catch (IOException e) {
		}
		buf.compact();
	}

	public void handleConnect(SelectionKey key) throws IOException {
		SocketChannel srvrChan = (SocketChannel) key.channel();
		try {
			if (srvrChan.finishConnect() && srvrChan.isConnected()) {
				PoolServerConnectionManager.getInstance().removeServer(key);
				key.interestOps(SelectionKey.OP_WRITE);
				Statistics.getInstance().addConnection();
				((AttachmentHTTP) key.attachment()).setConnected(true);
			}
		} catch (Exception e) {
			AttachmentHTTPResp resp = (AttachmentHTTPResp) key.attachment();
			if (!PoolServerConnectionManager.getInstance()
					.continueTryingToConnect(key)) {
				PoolServerConnectionManager.getInstance().removeServer(key);
				SelectionKey keyReq = resp.getOpposite().keyFor(key.selector());
				((AttachmentHTTPReq) keyReq.attachment()).reportError(keyReq,
						ErrorType.UNREACHABLE_HOST, "Unreachable Host");
				resp.endOfConnection(key);
				LoggingManager.logError(" Unreachable Host - "
						+ ((AttachmentHTTPReq) keyReq.attachment()).httpRequest
								.getHost());
			} else {
				ConnectionManager.reopen(key);
			}
		}
	}

	public void handleIncomeData(SelectionKey key) throws IOException {

		AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
		attachmentHTTP.registerInTimer(key);
		ByteBuffer b = attachmentHTTP.getReadBuffer();
		b.flip();
		while (b.hasRemaining()) {
			int actual = b.position();
			try {
				attachmentHTTP.getState().process(key);
			} catch (RuntimeException e) {
				attachmentHTTP.handleError(key, ErrorType.INTERNAL_SERVER_ERROR,
						"Internal proxy error");
			}
			if (actual == b.position()) {
				break;
			}
		}
		b.compact();
	}
}