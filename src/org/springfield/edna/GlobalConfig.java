package org.springfield.edna;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;


/**
 * Singleton configuration class
 *
 */
public class GlobalConfig {
	/** Singleton instance */
	private static GlobalConfig instance = new GlobalConfig();
	
	/** Default name of configuration file */
	private static String CONFIG_FILE = "config.xml";
	
	/** package root */
	public static final String PACKAGE_ROOT = "org.springfield.edna";
	
	/** service name */
	public static final String SERVICE_NAME = "edna";
	
	/** image path */
	public File IMAGE_PATH;
	
	/** output image path */ 
	public File OUT_IMAGE_PATH = new File("/mount/images2/ednaoutimages"); 
	
	/** develop manual path **/
	public static final String DEVELOPER_MANUAL_FILENAME = "manual.txt";
	public File DEVELOPER_MANUAL;
	
	
	/** running directory */
	private String baseDir;
	
	/**
	 * Sole constructor
	 */
	private GlobalConfig() {}
	
	/**
	 * Returns singleton instance
	 * 
	 * @return singleton instance
	 */
	public static GlobalConfig getInstance() {
		return instance;
	}
	
	/**
	 * Should be called upon startup
	 */
	public void initialize(String baseDir) {
		this.baseDir = baseDir;
		initConfig();
		initManual();
		initLogging();
	}

	/**
	 * Should be called upon teardown
	 */
	public void destroy() {
		
	}
	
	/**
	 * Initializes configuration
	 */
	private void initConfig() {
		System.out.println(SERVICE_NAME + ": initializing configuration.");
		
		// properties
		Properties props = new Properties();
		
		// load from file
		try {
			
			File file = new File(baseDir + "/conf/" + CONFIG_FILE);
			System.out.println(SERVICE_NAME + ": config path "+file);
			if (file.exists()) {
				props.loadFromXML(new BufferedInputStream(new FileInputStream(file)));
				IMAGE_PATH = new File(props.getProperty("image-path"));
				System.out.println(SERVICE_NAME + ": image path "+IMAGE_PATH);
			} else { 
				System.out.println(SERVICE_NAME + ": Could not load config");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(SERVICE_NAME + ": initializing configuration done.");
	}
	
	private void initManual() {
		DEVELOPER_MANUAL = new File(baseDir + "conf/" + DEVELOPER_MANUAL_FILENAME);
	}
	
	
	/**
	 * Initializes logging
	 */
	private void initLogging() {
		System.out.println(SERVICE_NAME + ": initializing logging.");
		
		// enable appenders
		String logPath = "";
		int index = baseDir.indexOf("webapps");
    	if(index!=-1) {
    		logPath += baseDir.substring(0,index);
    	}
		logPath += "logs/"+SERVICE_NAME+"/"+SERVICE_NAME+".log";	
		
		try {
			// default layout
			Layout layout = new PatternLayout("%-5p: %d{yyyy-MM-dd HH:mm:ss,SSS} %c %x - %m%n");
			
			// rolling file appender
			DailyRollingFileAppender appender1 = new DailyRollingFileAppender(layout,logPath,"'.'yyyy-MM-dd");
			BasicConfigurator.configure(appender1);
			
			// console appender 
			ConsoleAppender appender2 = new ConsoleAppender(layout);
			BasicConfigurator.configure(appender2);
		}
		catch(IOException e) {
			System.out.println(SERVICE_NAME + ": GlobalConfig got an exception while initializing the logging configuration");
			e.printStackTrace();
		}
		
		/*
		 *  turn off all logging, and enable ERROR logging for root package
		 *  use restlet.LoggingResource to enable specific logging
		 */
		Logger.getRootLogger().setLevel(Level.OFF);
		Logger.getLogger(PACKAGE_ROOT).setLevel(Level.ERROR);
		
		System.out.println(SERVICE_NAME + ": initializing logging done.");
	}
	
	
	
}