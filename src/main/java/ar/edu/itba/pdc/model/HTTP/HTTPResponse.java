package ar.edu.itba.pdc.model.HTTP;

public class HTTPResponse extends HTTPBase {

	private int statusCode;

	private String statusMessage;

	public HTTPResponse(String version, int statusCode, String statusMessage) {
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.version = version;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	@Override
	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append("Version -> " + version.toString() + "\n");
		ret.append("Code -> " + statusCode + "\n");
		ret.append("Message -> " + statusMessage + "\n");
		ret.append("\nHeaders -> \n");
		for (Entity entity : headers) {
			ret.append(entity.key + " ---- " + entity.value + "\n");
		}
		ret.append("\nBody -> \n");
		return ret.toString();
		// return "RESPONSE";
	}
}
