package ar.edu.itba.pdc.model.attachment;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import ar.edu.itba.pdc.utils.PoolClientConnectionManager;
import ar.edu.itba.pdc.utils.Statistics;

public class AttachmentAccepterConfig implements AttachmentAccepter{

	@Override
	public void handleIncomeClient(SelectionKey key) throws IOException{
		
		Statistics.getInstance().addAccess();
		
		if (PoolClientConnectionManager.getInstance().overflowConnections()){
			return ;
		}
		
		SocketChannel configChan = ((ServerSocketChannel) key.channel()).accept();
		configChan.configureBlocking(false);
		AttachmentHTTP attachmentHTTP = new AttachmentHTTPReqConfig();
		attachmentHTTP.setConnected(true);
		SelectionKey clientKey = configChan.register(key.selector(), SelectionKey.OP_READ, attachmentHTTP);
		PoolClientConnectionManager.getInstance().registerClient(clientKey);
		
	}

}
