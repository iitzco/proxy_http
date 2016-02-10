package ar.edu.itba.pdc.utils;

import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.TimerTask;

public class PoolServerConnectionManager extends TimerTask{

	private static PoolServerConnectionManager instance = null;

	private int round;

	private HashMap<SelectionKey, Integer> map;

	private PoolServerConnectionManager() {
		round = 0;
		map = new HashMap<>();
	}

	public static PoolServerConnectionManager getInstance() {
		if (instance == null)
			instance = new PoolServerConnectionManager();
		return instance;
	}

	@Override
	synchronized public void run() {
		round++;
	}

	synchronized public boolean continueTryingToConnect(SelectionKey key) {
		Integer i = map.get(key);
		if (i == null) {
			map.put(key, round);
			return true;
		}else if (round <= i+1){
			return true;
		}
		return false;
	}

	synchronized public void removeServer(SelectionKey key) {
		map.remove(key);
	}

	synchronized public void renewSuscription(SelectionKey key, SelectionKey newKey) {
		map.put(newKey, map.get(key));
		map.remove(key);
	}

	
}
