package ar.edu.itba.pdc.utils;

import ar.edu.itba.pdc.defs.Defs.ErrorType;

public class ErrorRespManager {

	public static String getStringError(ErrorType errorType, String mssg) {
		StringBuilder builder = new StringBuilder();
		String html = getStringHtml(errorType, mssg);
		builder.append("HTTP/1.1 " + getNumber(errorType) + " " + mssg + "\r\n");
		builder.append("Content-Type: text/html\r\n");
		builder.append("Connection:close\r\n");
		builder.append("Content-length:" + html.length() + "\r\n\r\n");
		builder.append(html);
		return builder.toString();
	}

	private static String getNumber(ErrorType errorType) {
		switch (errorType) {
		case UNAUTHORIZED:
			return "401";
		case REQUEST_TIMEOUT:
			return "408";
		case AUTHENTICATION_FAILED:
			return "401";
		case BAD_REQUEST:
			return "400";
		case BUFFER_OVERFLOW:
			return "500";
		case CANT_LEET:
			return "415";
		case INTERNAL_SERVER_ERROR:
			return "500";
		case NO_HOST_SPECIFIED:
			return "400";
		case UNKNOWN_HOST:
			return "400";
		case UNREACHABLE_HOST:
			return "504";
		}
		return null;
	}

	private static String getStringHtml(ErrorType errorType, String mssg) {
		StringBuilder builder = new StringBuilder();
		builder.append("<!DOCTYPE html><html><title>" + getNumber(errorType)
				+ " " + mssg + "</title><body><h1>" + getNumber(errorType)
				+ " " + mssg + "</h1></body></html>");
		return builder.toString();
	}
}
