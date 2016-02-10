package ar.edu.itba.pdc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Timer;

import ar.edu.itba.pdc.defs.Defs;
import ar.edu.itba.pdc.model.attachment.AttachmentAccepterConfig;
import ar.edu.itba.pdc.model.attachment.AttachmentAccepterHTTP;
import ar.edu.itba.pdc.utils.LoggingManager;
import ar.edu.itba.pdc.utils.PoolClientConnectionManager;
import ar.edu.itba.pdc.utils.PoolServerConnectionManager;
import ar.edu.itba.pdc.utils.Statistics;

public class Proxy {
	
	public static void main(String[] args) throws IOException {

		if (args.length != 1) {
			System.out
					.println("Error in parameters. Must provide path to config.properties");
			return;
		}

		String url = args[0];
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(url + "/config.properties");
		} catch (FileNotFoundException e) {
			System.out.println("File config.properties not found");
			return;
		}

		if (!Defs.initialize(inputStream))
			return;
		
		inputStream.close();

		try {
			LoggingManager.initializeLoggers();
		} catch (FileNotFoundException e) {
			System.out.println("Incorrect Logs path.");
			return;
		}

		Selector selector = Selector.open();

		initializeAccepters(selector);

		startPoolManagers();

		Statistics.getInstance().initialize();

		TCPProtocol protocol = new ProxySelectorProtocol();
		while (true) {

			if (selector.select(Defs.TIMEOUT) == 0) {
				System.out.print(".");
				continue;
			}
			Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
			while (keyIter.hasNext()) {
				SelectionKey key = keyIter.next();

				if (key.isValid() && key.isAcceptable()) {
					protocol.handleAccept(key);
				}

				if (key.isValid() && key.isReadable()) {
					protocol.handleRead(key);
				}

				if (key.isValid() && key.isWritable()) {
					protocol.handleWrite(key);
				}

				if (key.isValid() && key.isConnectable()) {
					protocol.handleConnect(key);
				}

				keyIter.remove();
			}
		}
	}

	private static void startPoolManagers() {
		Timer timer = new Timer();
		timer.schedule(PoolClientConnectionManager.getInstance(),
				Defs.TIMER_CLIENT, Defs.TIMER_CLIENT);
		timer.schedule(PoolServerConnectionManager.getInstance(),
				Defs.TIMER_SERVER, Defs.TIMER_SERVER);

		// Timer t = new Timer();
		// t.schedule(PoolBufferManager.getInstance(),
		// Defs.TIMER_BUFFER_MANAGER, Defs.TIMER_BUFFER_MANAGER);
	}

	private static void initializeAccepters(Selector selector)
			throws IOException {

		ServerSocketChannel listnChannel = ServerSocketChannel.open();
		listnChannel.socket().bind(new InetSocketAddress(Defs.PROXY_PORT));
		listnChannel.configureBlocking(false);
		listnChannel.register(selector, SelectionKey.OP_ACCEPT,
				new AttachmentAccepterHTTP());

		ServerSocketChannel configChannel = ServerSocketChannel.open();
		configChannel.socket().bind(
				new InetSocketAddress(Defs.PROXY_CONFIG_PORT));
		configChannel.configureBlocking(false);
		configChannel.register(selector, SelectionKey.OP_ACCEPT,
				new AttachmentAccepterConfig());

	}
}
