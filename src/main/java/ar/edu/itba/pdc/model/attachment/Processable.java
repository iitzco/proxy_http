package ar.edu.itba.pdc.model.attachment;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface Processable {

	public void process(SelectionKey key) throws IOException;
}
