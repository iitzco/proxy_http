package ar.edu.itba.pdc.utils;

import ar.edu.itba.pdc.defs.Defs.Method;

public class ConverterHTTP {

	private ConverterHTTP() {
		new AssertionError();
	}

	public static Method convertMethod(String buf) {
		if (buf.equals("GET")) {
			return Method.GET;
		} else if (buf.equals("POST")) {
			return Method.POST;
		} else if (buf.equals("HEAD")) {
			return Method.HEAD;
		} else if (buf.equals("CONNECT")) {
			return Method.CONNECT;
		} else {
			return Method.UNKNOWN;
		}
	}

	public static String convertVersion(String buf) {
		if (buf.toUpperCase().equals("HTTP/1.1")) {
			return "1.1";
		} else if (buf.toUpperCase().equals("HTTP/2.0")) {
			return "2.0";
		} else if (buf.toUpperCase().equals("HTTP/1.0")) {
			return "1.0";
		} else {
			return "Unknown";
		}
	}

	public static String convertValue(String buf) {
		if (buf.charAt(buf.length() - 1) == '\r')
			return buf.substring(0, buf.length() - 1);
		return buf;
	}
}
