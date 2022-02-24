package com.rli.scripts.utils;

import org.apache.logging.log4j.Logger;

import com.rli.logging.LoggingUtils;

public class Utils {

	/*
	 * Sample custom logger, create your own as needed 
	 */
	public static Logger getMyCustomLogger() {
		return LoggingUtils.getUserLogger("$RLI_HOME/logs/user.log");
	}

}
