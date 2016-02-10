package ar.edu.itba.pdc.utils;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import ar.edu.itba.pdc.defs.Defs;
import ar.edu.itba.pdc.defs.Defs.Method;

public class Statistics {

	private static Statistics instance = null;

	@SerializedName("Bytes recieved from client")
	private long bytesRecFromClt = 0;

	@SerializedName("Bytes recieved from server")
	private long bytesRecFromSrv = 0;

	@SerializedName("Bytes sent to client")
	private long bytesSentToClt = 0;

	@SerializedName("Bytes sent to server")
	private long bytesSentToSrv = 0;

	@SerializedName("Total bytes through proxy")
	private long totalBytesThroughProxy = 0;

	@SerializedName("Total accesses")
	private long accesses = 0;

	@SerializedName("Actual connections")
	private long actualConnections = 0;

	@SerializedName("Requests by method")
	private Map<Method, Integer> map;

	@SerializedName("Response by version")
	private Map<String, Integer> versionQ;

	private Statistics() {
	}

	public static Statistics getInstance() {
		if (instance == null) {
			instance = new Statistics();
		}
		return instance;
	}

	public void initialize() {
		map = new HashMap<Defs.Method, Integer>();
		map.put(Method.GET, 0);
		map.put(Method.POST, 0);
		map.put(Method.HEAD, 0);
		map.put(Method.CONNECT, 0);
		map.put(Method.UNKNOWN, 0);

		versionQ = new HashMap<String, Integer>();
		versionQ.put("1.1", 0);
		versionQ.put("1.0", 0);

	}

	public void addBytesRecFromClt(long amount) {
		bytesRecFromClt += amount;
		totalBytesThroughProxy += amount;
	}

	public void addBytesRecFromSrv(long amount) {
		bytesRecFromSrv += amount;
		totalBytesThroughProxy += amount;
	}

	public void addBytesSentToClt(long amount) {
		bytesSentToClt += amount;
		totalBytesThroughProxy += amount;
	}

	public void addBytesSentToSrv(long amount) {
		bytesSentToSrv += amount;
		totalBytesThroughProxy += amount;
	}

	public void addMethod(Method m) {
		map.put(m, map.get(m) + 1);
	}

	public void addVersion(String version) {
		if (version.equals("1.0")) {
			versionQ.put("1.0", versionQ.get("1.0") + 1);
		} else {
			versionQ.put("1.1", versionQ.get("1.1") + 1);
		}
	}

	public void incrementAccesses() {
		accesses++;
	}

	public void addConnection() {
		actualConnections++;
	}

	public void removeConnection() {
		if (actualConnections <= 0)
			return;
		actualConnections--;
	}

	public void addAccess() {
		accesses++;
	}

	public String dataJSON() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(instance);
	}
}
