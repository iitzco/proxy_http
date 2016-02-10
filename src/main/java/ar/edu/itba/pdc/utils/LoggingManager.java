package ar.edu.itba.pdc.utils;

import java.io.FileNotFoundException;

import org.apache.log4j.Logger;

public class LoggingManager {

	private static Logger reportLogger;

	private static Logger errorLogger;

	private static Logger configLogger;

	public static void initializeLoggers() throws FileNotFoundException{
		reportLogger = Logger.getLogger("reportLogger");
		errorLogger = Logger.getLogger("errorLogger");
		configLogger = Logger.getLogger("configLogger");

	}

	private LoggingManager() {
		new AssertionError("Util class");
	}

	public static void logError(String msg) {
		errorLogger.error(msg);
	}

	public static void logReport(String msg) {
		reportLogger.info(msg);
	}

	public static void logConfig(String msg) {
		configLogger.info(msg);
	}

}
