package ar.edu.itba.pdc.model.HTTP;

import ar.edu.itba.pdc.defs.Defs;
import ar.edu.itba.pdc.defs.Defs.Method;

public class HTTPRequest extends HTTPBase {

	private String resource;
	
	protected String host;

	private Method method;

	public HTTPRequest(Method method, String resource, String version) {
		this.resource = resource;
		this.method = method;
		this.version = version;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public Method getMethod() {
		return method;
	}

	@Override
	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append("Method -> " + method.toString() + "\n");
		ret.append("Resource -> " + resource + "\n");
		ret.append("Version -> " + version + "\n");
		ret.append("\nHeaders -> \n");
		for (Entity entity : headers) {
			ret.append(entity.key + " ---- " + entity.value + "\n");
		}
		ret.append("\nBody -> \n");
		return ret.toString();
//		return "REQUEST";
	}

	@Override
	public void addHeader(String key, String value) {
		super.addHeader(key, value);
		if (key.toUpperCase().equals(Defs.HOST))
			this.host = value;
	}
	
	public String getHost() {
		return host;
	}
	
	
	

}
