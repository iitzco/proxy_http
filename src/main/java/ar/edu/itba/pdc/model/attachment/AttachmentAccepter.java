package ar.edu.itba.pdc.model.attachment;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface AttachmentAccepter {

	public void handleIncomeClient(SelectionKey key) throws IOException;

}
