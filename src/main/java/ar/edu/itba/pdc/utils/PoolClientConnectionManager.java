package ar.edu.itba.pdc.utils;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimerTask;

import ar.edu.itba.pdc.defs.Defs;
import ar.edu.itba.pdc.model.attachment.AttachmentHTTP;

public class PoolClientConnectionManager extends TimerTask {

	private static PoolClientConnectionManager instance = null;

	private int round;

	private HashMap<SelectionKey, Integer> map;

	private PoolClientConnectionManager() {
		round = 0;
		map = new HashMap<>();
	}

	public static PoolClientConnectionManager getInstance() {
		if (instance == null)
			instance = new PoolClientConnectionManager();
		return instance;
	}

	@Override
	synchronized public void run() {
		List<SelectionKey> toRemove = new LinkedList<SelectionKey>();
		Set<Entry<SelectionKey, Integer>> set = map.entrySet();
		for (Entry<SelectionKey, Integer> each : set) {
			if (round - each.getValue() > 1) {
				
				toRemove.add(each.getKey());
			}
		}
		for (SelectionKey selectionKey : toRemove) {
			try {
				((AttachmentHTTP) selectionKey.attachment()).endOfConnection(selectionKey);
			} catch (IOException e) {
				selectionKey.cancel();
			}
			map.remove(selectionKey);
		}
		round++;
	}

	synchronized public void registerClient(SelectionKey key) {
		map.put(key, round);
	}

	synchronized public void removeClient(SelectionKey key) {
		map.remove(key);
	}

	synchronized public boolean overflowConnections() {
		return map.size() >= Defs.MAX_CLIENT_CONNECTIONS;
	}

}
