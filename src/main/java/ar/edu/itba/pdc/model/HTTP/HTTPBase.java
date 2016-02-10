package ar.edu.itba.pdc.model.HTTP;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import ar.edu.itba.pdc.defs.Defs;
import ar.edu.itba.pdc.defs.Defs.ContentTypeBody;

public abstract class HTTPBase {

	protected List<Entity> headers = new LinkedList<Entity>();

	protected String version;

	protected Long bodyLength = null;

	protected String boundaryEnd = null;

	protected ContentTypeBody contentTypeBody = ContentTypeBody.NORMAL;

	protected Charset charset = Charset.forName(Defs.ISO_8859_1);


	public List<String> getHeader(String key) {
		List<String> ret = new LinkedList<String>();
		for (Entity each : headers) {
			if (each.key.equals(key.toUpperCase()))
				ret.add(each.value);
		}
		return ret;
	}

	public List<Entity> getHeaders() {
		return new LinkedList<HTTPBase.Entity>(headers);
	}

	public void addHeader(String key, String value) {
		headers.add(new Entity(key, value));
		if (key.toUpperCase().equals(Defs.CONTENT_LENGTH)) {
			bodyLength = Long.parseLong(value);
		} else if (key.toUpperCase().equals(Defs.TRANSFER_ENCODING)
				&& value.equals("chunked")) {
			contentTypeBody = ContentTypeBody.CHUNKED;
		} else if (key.toUpperCase().equals(Defs.CONTENT_TYPE)) {
			String[] s = value.split(";");
			String each;
			for (int i = 0; i < s.length; i++) {
				each = s[i];
				String eachTrim = each.trim();
				if (eachTrim.length() >= Defs.CHARSET_LEN && eachTrim.substring(0, Defs.CHARSET_LEN)
						.equals("charset=")) {
					charset = Charset.forName(each.split("=")[1]);
				} else if (each.trim().equals(Defs.MULTIPART_BYTERANGES)) {
					contentTypeBody = ContentTypeBody.MULTIPART_BYTERANGES;
				} else if (isMultipart()
						&& each.trim().substring(0, "boundary=".length())
								.equals("boundary=")) {
					boundaryEnd = "--" + each.split("=")[1] + "--";
				}
			}
		}
	}

	public static class Entity {
		public String key;
		public String value;

		public Entity(String key, String value) {
			this.key = key;
			this.value = value;
		}
	}

	public String getVersion() {
		return version;
	}

	public boolean isChunkedBody() {
		return contentTypeBody == ContentTypeBody.CHUNKED;
	}

	public Charset getCharset() {
		return charset;
	}

	public boolean isMultipart() {
		return contentTypeBody == ContentTypeBody.MULTIPART_BYTERANGES;
	}

	public String getBoundaryEnd() {
		return boundaryEnd;
	}

	public Long getBodyLength() {
		return bodyLength;
	}
}
