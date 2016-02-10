package ar.edu.itba.pdc.defs;

import java.util.Properties;

import ar.edu.itba.pdc.utils.LoggingManager;

public class ParseProperties {

	public static Integer parseIntegerConfigProperty(Properties props,
			String propName, Integer minVal, Integer maxVal) {
		Integer propValue;
		try {
			propValue = Integer.parseInt(props.getProperty(propName));
		} catch (NumberFormatException e) {
			System.out.println("CONFIG - Property " + propName
					+ " must be a numeric value.");
			LoggingManager.logError("CONFIG - Property " + propName
					+ " must be a numeric value.");
			return null;
		}
		if ((minVal != null && propValue < minVal)
				|| (maxVal != null && propValue > maxVal)) {
			System.out.println("CONFIG - Property " + propName
					+ " must be between " + minVal + " and " + maxVal + ".");
			LoggingManager.logError("CONFIG - Property " + propName
					+ " must be between " + minVal + " and " + maxVal + ".");
			return null;
		}
		return propValue;
	}

	public static Long parseLongConfigProperty(Properties props,
			String propName, Long minVal, Long maxVal) {
		Long propValue;
		try {
			propValue = Long.parseLong(props.getProperty(propName));
		} catch (NumberFormatException e) {
			System.out.println("CONFIG - Property " + propName
					+ " must be a numeric value.");
			LoggingManager.logError("CONFIG - Property " + propName
					+ " must be a numeric value.");
			return null;
		}
		if ((minVal != null && propValue < minVal)
				|| (maxVal != null && propValue > maxVal)) {
			System.out.println("CONFIG - Property " + propName
					+ " must be between " + minVal + " and " + maxVal + ".");
			LoggingManager.logError("CONFIG - Property " + propName
					+ " must be between " + minVal + " and " + maxVal + ".");
			return null;
		}
		return propValue;
	}
	
	public static boolean checkPorts(Integer p1, String p1Name, Integer p2, String p2Name) {
		if(p1.equals(p2)) {
			LoggingManager.logError("CONFIG - "+p1Name+" MUST BE in a different port than "+p2Name);
			return false;
		}
		return true;
	}

	public static Boolean parseBooleanConfigProperty(Properties props,
			String propName) {
		String aux = props.getProperty(propName);
		if (aux.equalsIgnoreCase("true"))
			return true;
		if (aux.equalsIgnoreCase("false"))
			return false;
		System.out.println("CONFIG - Property " + propName
				+ " MUST BE a boolean value.");
		LoggingManager.logError("CONFIG - Property " + propName
				+ " MUST BE a boolean value.");
		return null;
	}

	public static String parseStringConfigProperty(Properties props,
			String propName) {
		String propValue = props.getProperty(propName);
		if (propValue == null || propValue.length() == 0) {
			System.out.println("CONFIG - Property " + propName
					+ " missing value.");
			LoggingManager.logError("CONFIG - Property " + propName
					+ " missing value.");
			return null;
		}
		return propValue;
	}

}