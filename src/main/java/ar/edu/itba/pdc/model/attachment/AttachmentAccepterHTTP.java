package ar.edu.itba.pdc.model.attachment;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import ar.edu.itba.pdc.utils.PoolClientConnectionManager;
import ar.edu.itba.pdc.utils.Statistics;

public class AttachmentAccepterHTTP implements AttachmentAccepter {

	@Override
	public void handleIncomeClient(SelectionKey key) throws IOException {
		
		Statistics.getInstance().addAccess();
		
		if (PoolClientConnectionManager.getInstance().overflowConnections()){
			return ;
		}
		
		SocketChannel clntChan = ((ServerSocketChannel) key.channel()).accept();
		clntChan.configureBlocking(false);
		AttachmentHTTPReq attachmentHTTP = new AttachmentHTTPReq();
		attachmentHTTP.setConnected(true);
		SelectionKey clientKey = clntChan.register(key.selector(), SelectionKey.OP_READ, attachmentHTTP);
		Statistics.getInstance().addConnection();
		PoolClientConnectionManager.getInstance().registerClient(clientKey);
	}

}
