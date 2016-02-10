package ar.edu.itba.pdc.defs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Defs {

	// STRINGS

	public static final String HOST = "HOST";

	public static final String CONTENT_LENGTH = "CONTENT-LENGTH";

	public static final String TRANSFER_ENCODING = "TRANSFER-ENCODING";

	public static final String MULTIPART_BYTERANGES = "multipart/byteranges";

	public static final String CONTENT_TYPE = "CONTENT-TYPE";

	public static final String UTF_8 = "UTF-8";

	public static final String ISO_8859_1 = "ISO-8859-1";

	public static final String CONNECTION = "CONNECTION";

	public static final String ACCEPT = "ACCEPT";

	public static final String ACCEPT_CHARSET = "ACCEPT-CHARSET";

	public static final String ACCEPT_ENCODING = "ACCEPT-ENCODING";

	public static Integer READ_BUFFER_CAPACITY;

	public static Integer WRITE_BUFFER_CAPACITY;

	public static Integer MAX_IN_MEMORY_BUFFER_CAPACITY;

	// ENUM AND CONSTANT

	public enum Method {
		GET, HEAD, POST, UNKNOWN, CONNECT
	};

	public enum Phase {
		REQ, RESP
	};

	public enum ErrorType {
		BAD_REQUEST, UNKNOWN_HOST, NO_HOST_SPECIFIED, INTERNAL_SERVER_ERROR, BUFFER_OVERFLOW, AUTHENTICATION_FAILED, CANT_LEET, UNAUTHORIZED, UNREACHABLE_HOST, REQUEST_TIMEOUT
	};

	public enum ContentEncodingType {
		GZIP, IDENTITY, OTHER, NOT_PROVIDED
	};

	public enum ContentTypeBody {
		MULTIPART_BYTERANGES, CHUNKED, NORMAL
	};

	public enum ProvidedResponseType {
		LEET_ON, LEET_OFF, STATS
	};

	public static final int NONE = 0;

	public static final int CHARSET_LEN = 8;

	public static Integer TIMEOUT = 3000;

	// CONFIGURATION

	public static Integer HTTP_PORT;

	public static Integer PROXY_PORT;

	public static Integer PROXY_CONFIG_PORT;

	public static String USER;

	public static String PASS;

	public static Long TIMER_CLIENT;

	public static Long TIMER_SERVER;

	public static Boolean APPLY_L33T;

	public static Integer MAX_CLIENT_CONNECTIONS;

	public static boolean initialize(InputStream inputStream) {

		Properties props = new Properties();
		try {
			props.load(inputStream);
			System.setProperty("logfile.path",
					props.getProperty("config.path.logs"));
		} catch (IOException e) {
			System.out.println("Error loading config.properties");
			return false;
		}
		if ((APPLY_L33T = ParseProperties.parseBooleanConfigProperty(props,
				"config.apply.l33t")) == null)
			return false;
		if ((READ_BUFFER_CAPACITY = ParseProperties.parseIntegerConfigProperty(
				props, "config.capacity.buffer.read", 5, 10000000)) == null)
			return false;
		if ((WRITE_BUFFER_CAPACITY = ParseProperties
				.parseIntegerConfigProperty(props,
						"config.capacity.buffer.write", 1, 10000000)) == null)
			return false;
		if ((MAX_IN_MEMORY_BUFFER_CAPACITY = ParseProperties
				.parseIntegerConfigProperty(props,
						"config.capacity.buffer.memory.max",
						Math.max(WRITE_BUFFER_CAPACITY, READ_BUFFER_CAPACITY),
						100000000)) == null)
			return false;
		if ((HTTP_PORT = ParseProperties.parseIntegerConfigProperty(props,
				"config.http.port", 1, 65535)) == null)
			return false;
		if ((PROXY_PORT = ParseProperties.parseIntegerConfigProperty(props,
				"config.proxy.port", 1024, 65535)) == null)
			return false;
		if ((PROXY_CONFIG_PORT = ParseProperties.parseIntegerConfigProperty(
				props, "config.proxy.config.port", 1024, 65535)) == null)
			return false;
		if (!ParseProperties.checkPorts(PROXY_PORT, "config.proxy.port",
				PROXY_CONFIG_PORT, "config.proxy.config.port"))
			return false;
		if ((USER = ParseProperties.parseStringConfigProperty(props,
				"config.auth.user")) == null)
			return false;
		if ((PASS = ParseProperties.parseStringConfigProperty(props,
				"config.auth.pass")) == null)
			return false;
		if ((TIMER_CLIENT = ParseProperties.parseLongConfigProperty(props,
				"config.time.limit.client", (long) 1, (long) 3600000)) == null)
			return false;
		if ((TIMER_SERVER = ParseProperties.parseLongConfigProperty(props,
				"config.time.limit.server", (long) 1, (long) 3600000)) == null)
			return false;
		if ((MAX_CLIENT_CONNECTIONS = ParseProperties
				.parseIntegerConfigProperty(props,
						"config.limit.client.connections.max", 1, 1000000)) == null)
			return false;
		return true;
	}

}