package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.util.Util.shortDate;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.fnal.ppd.dd.util.PropertiesFile;

public class LoggerForDebugging {
	private Logger			logger;
	private static boolean	bypassLogger	= PropertiesFile.getBooleanProperty("BypassStandardLog", false);

	/*
	 * Note that the levels are in this order: 
	 * 
	 * OFF = Integer.MAX_VALUE
	 * 
	 * SEVERE;
	 * 
	 * WARNING;
	 * 
	 * INFO;
	 * 
	 * CONFIG;
	 * 
	 * FINE;
	 * 
	 * FINER;
	 * 
	 * FINEST;
	 * 
	 * ALL = 0
	 */

	public LoggerForDebugging(String name) {
		logger = Logger.getLogger(name);
	}

	private void p(String s) {
		System.out.println(shortDate() + " - " + s);
	}

	public void fine(String string) {
		if (bypassLogger && logger.isLoggable(Level.FINE))
			p(string);
		else
			logger.fine(string);
	}

	public void finer(String string) {
		if (bypassLogger && logger.isLoggable(Level.FINER))
			p(string);
		else
			logger.finer(string);
	}

	public void info(String string) {
		if (bypassLogger && logger.isLoggable(Level.INFO))
			p(string);
		else
			logger.info(string);
	}

	public void warning(String string) {
		if (bypassLogger && logger.isLoggable(Level.WARNING))
			p(string);
		else
			logger.warning(string);
	}

	public void config(String string) {
		if (bypassLogger && logger.isLoggable(Level.CONFIG))
			p(string);
		else
			logger.config(string);
	}

	public void severe(String string) {
		if (bypassLogger && logger.isLoggable(Level.SEVERE))
			p(string);
		else
			logger.severe(string);
	}

	public void addHandler(FileHandler fileTxt) {
		logger.addHandler(fileTxt);
	}

	public void setLevel(Level level) {
		logger.setLevel(level);
	}

	public Logger getLogger() {
		return logger;
	}

}
