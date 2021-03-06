/* 
* LazyHomer.java
* 
* Copyright (c) 2014 Noterik B.V.
* 
* This file is part of Edna, related to the Noterik Springfield project.
*
* Edna is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Edna is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Edna.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.springfield.edna.homer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.*;
import org.dom4j.*;
import org.springfield.mojo.interfaces.ServiceInterface;
import org.springfield.mojo.interfaces.ServiceManager;



public class LazyHomer implements MargeObserver {
	
	private static Logger LOG = Logger.getLogger(LazyHomer.class);

	/** Noterik package root */
	public static final String PACKAGE_ROOT = "com.noterik";
	private static enum loglevels { all,info,warn,debug,trace,error,fatal,off; }
	public static String myip = "unknown";
	private static int port = -1;
	static String group = "224.0.0.0";
	static int ttl = 1;
	static boolean noreply = true;
	static LazyMarge marge;
	static SmithersProperties selectedsmithers = null;
	private static String rootPath = null;
	//private static NelsonServer serv;
	private static Map<String, SmithersProperties> smithers = new HashMap<String, SmithersProperties>();
	private static Map<String, EdnaProperties> ednas = new HashMap<String, EdnaProperties>();
	private static Map<String, MountProperties> mounts = null;
	private static LazyHomer ins;
	private static boolean running = false;
	static String role = "production";
	private int retryCounter;

	
	/**
	 * Initializes the configuration
	 */
	public void init(String r) {
		rootPath = r;
		ins = this;
		retryCounter = 0;
		initConfig();
		initLogger();
		
		try{
			InetAddress mip=InetAddress.getLocalHost();
			myip = ""+mip.getHostAddress();
		}catch (Exception e){
			System.out.println("Exception ="+e.getMessage());
		}
		LOG.info("Edna init service name = edna on ipnumber = "+myip);
		System.out.println("Edna init service name = edna on ipnumber = "+myip+" on marge port "+port);
		marge = new LazyMarge();
		
		// lets watch for changes in the service nodes in smithers
		marge.addObserver("/domain/internal/service/edna/nodes/"+myip, ins);
		marge.addTimedObserver("/smithers/downcheck",6,this);
		new DiscoveryThread();	
	}
	
	public static void addSmithers(String ipnumber,String port,String mport,String role) {
		int oldsize = smithers.size();
		if (!(""+LazyHomer.getPort()).equals(mport)) {
			System.out.println("EXTREEM WARNING CLUSTER COLLISION ("+LazyHomer.getPort()+") "+ipnumber+":"+port+":"+mport);
			return;
		}
		
		if (!role.equals(getRole())) {
			System.out.println("Edna : Ignored this smithers ("+ipnumber+") its "+role+" and not "+getRole()+" like us");
			return;
		}
		
		SmithersProperties sp = smithers.get(ipnumber);
		if (sp==null) {
			sp = new SmithersProperties();
			smithers.put(ipnumber, sp);
			sp.setIpNumber(ipnumber);
			sp.setPort(port);
			sp.setAlive(true); // since talking its alive 
			noreply = false; // stop asking (minimum of 60 sec, delayed)
			LOG.info("Edna found smithers at = "+ipnumber+" port="+port+" multicast="+mport);
			System.out.println("Edna found smithers at = "+ipnumber+" port="+port+" multicast="+mport);
		} else {
			if (!sp.isAlive()) {
				sp.setAlive(true); // since talking its alive again !
				LOG.info("recovered smithers at = "+ipnumber);
			}
		}
		

		// so check if we are known 
		if (oldsize==0 && ins.checkKnown()) {
			
			// we are verified (has a name other than unknown) and status is on
			EdnaProperties mp = ednas.get(myip);
			setLogLevel(mp.getDefaultLogLevel());
			if (mp!=null && mp.getStatus().equals("on")) {
				if (!running) {
					running = true;
					LOG.info("This edna will be started (on startup)");
				}
			} else {
				if (running) {
					running = false;
				} else {
					LOG.info("This lou is not turned on, use smithers todo this for ip "+myip);
				}
			}

		}

	}
	
	public static EdnaProperties getMyEdnaProperties() {
		return ednas.get(myip);
	}
	
	public static int getMyEdnaPosition() {
		int i = 0;
		for(Iterator<EdnaProperties> iter = ednas.values().iterator(); iter.hasNext(); ) {
			EdnaProperties m = (EdnaProperties)iter.next();
			i++;
			if (m.getIpNumber().equals(myip)) return i;
		}
		return -1;
	}
	
	public static int getNumberOfEdnas() {
		return ednas.size();
	}
	
	
	public static MountProperties getMountProperties(String name) {
		if (mounts==null) readMounts();
		return mounts.get(name);
	}
	
	private static void readMounts() {
		mounts = new HashMap<String, MountProperties>();
		ServiceInterface smithers = ServiceManager.getService("smithers");
		if (smithers==null) {
			System.out.println("can't reach smithers");
			return;
		}
//		String mountslist = LazyHomer.sendRequest("GET","/domain/internal/service/edna/mounts",null,null);
		String mountslist = smithers.get("/domain/internal/service/edna/mounts",null,null);

		try { 
			Document result = DocumentHelper.parseText(mountslist);
			for(Iterator<Node> iter = result.getRootElement().nodeIterator(); iter.hasNext(); ) {
				Element child = (Element)iter.next();
				if (!child.getName().equals("properties")) {
					String name = child.attributeValue("id");
					String hostname = child.selectSingleNode("properties/hostname").getText();
					String path = child.selectSingleNode("properties/path").getText();
					String account = child.selectSingleNode("properties/account").getText();
					String password = child.selectSingleNode("properties/password").getText();
					String protocol = child.selectSingleNode("properties/protocol").getText();
					MountProperties mp = new MountProperties();
					mp.setHostname(hostname);
					mp.setPath(path);
					mp.setAccount(account);
					mp.setPassword(password);
					mp.setProtocol(protocol);
					mounts.put(name, mp);
					
					
				}
			}
		} catch (DocumentException e) {
			LOG.info("LazyHomer: "+e.getMessage());
		}
	}
	
	private Boolean checkKnown() {
		ServiceInterface smithers = ServiceManager.getService("smithers");
		if (smithers==null) {
			System.out.println("can't reach smithers");
			return false;
		}
		String xml = "<fsxml><properties><depth>1</depth></properties></fsxml>";
		//String nodes = LazyHomer.sendRequest("GET","/domain/internal/service/edna/nodes",xml,"text/xml");
		String nodes = smithers.get("/domain/internal/service/edna/nodes",xml,"text/xml");
		//System.out.println("NODES="+nodes);
		boolean iamok = false;

		try { 
			boolean foundmynode = false;
			
			Document result = DocumentHelper.parseText(nodes);
			for(Iterator<Node> iter = result.getRootElement().nodeIterator(); iter.hasNext(); ) {
				Element child = (Element)iter.next();
				if (!child.getName().equals("properties")) {
					String ipnumber = child.attributeValue("id");
					String status = child.selectSingleNode("properties/status").getText();
					String name = child.selectSingleNode("properties/name").getText();

					// lets put all in our edna list
					EdnaProperties mp = ednas.get(ipnumber);
					if (mp==null) {
						mp = new EdnaProperties();
						ednas.put(ipnumber, mp);

					}
					mp.setIpNumber(ipnumber);
					mp.setName(name);
					mp.setStatus(status);
					mp.setDefaultLogLevel(child.selectSingleNode("properties/defaultloglevel").getText());
					mp.setPreferedSmithers(child.selectSingleNode("properties/preferedsmithers").getText());
					mp.setImagePath(child.selectSingleNode("properties/imagepath").getText());

					if (ipnumber.equals(myip)) {
						foundmynode = true;
						retryCounter = 0;
						if (name.equals("unknown")) {
							System.out.println("This edna is not verified change its name, use smithers todo this for ip "+myip);
						} else {
							// so we have a name (verified) return true
							iamok = true;
						}
					}
				}	
			}
			if (!foundmynode) {
				if (retryCounter < 30) {
					//retry 30 times (= 5 min) to handle temp smithers downtime (eg daily restarts)
					retryCounter++;
				} else {
				LOG.info("LazyHomer : Creating my processing node "+LazyHomer.getSmithersUrl()  + "/domain/internal/service/edna/properties");
				String os = "unknown"; // we assume windows ?
				try{
					  os = System.getProperty("os.name");
				} catch (Exception e){
					System.out.println("LazyHomer : "+e.getMessage());
				}
				
				String newbody = "<fsxml>";
	        	newbody+="<nodes id=\""+myip+"\"><properties>";
	        	newbody+="<name>unknown</name>";
	        	newbody+="<status>off</status>";
	        	newbody+="<activesmithers>"+selectedsmithers.getIpNumber()+"</activesmithers>";
	        	newbody+="<lastseen>"+new Date().getTime()+"</lastseen>";
	        	newbody+="<preferedsmithers>"+myip+"</preferedsmithers>";
	        	if (isWindows()) {
	        		newbody+="<defaultloglevel>info</defaultloglevel>";
	        		newbody+="<imagepath>c:\\springfield\\tomcat\\webapps\\ROOT\\images</imagepath>";
	        		newbody+="<temporarydirectory>c:\\springfield\\edna\\temp</temporarydirectory>";
	        	} if (isMac()) {
	        		newbody+="<defaultloglevel>info</defaultloglevel>";
	        		newbody+="<imagepath>/springfield/tomcat/webapps/ROOT/images</imagepath>";
	        		newbody+="<temporarydirectory>/springfield/edna/temp</temporarydirectory>";
	        	} if (isUnix()) {
	        		newbody+="<defaultloglevel>info</defaultloglevel>";
	        		newbody+="<imagepath>/springfield/tomcat/webapps/ROOT/images</imagepath>";
	        		newbody+="<temporarydirectory>/springfield/edna/temp</temporarydirectory>";
	        	} else {
	        		newbody+="<defaultloglevel>info</defaultloglevel>";
	        		newbody+="<imagepath>/springfield/tomcat/webapps/ROOT/images</imagepath>";
	        		newbody+="<temporarydirectory>c:\\springfield\\edna\\temp</temporarydirectory>";

	        	}
	        	newbody+="</properties></nodes></fsxml>";	
				smithers.put("/domain/internal/service/edna/properties",newbody,"text/xml");
			}
			}
		} catch (Exception e) {
			LOG.info("LazyHomer exception doc");
			e.printStackTrace();
		}
		return iamok;
	}
	
	public static void setLastSeen() {
		ServiceInterface smithers = ServiceManager.getService("smithers");
		if (smithers==null) {
			System.out.println("can't reach smithers");
			return;
		}
		Long value = new Date().getTime();
		//LazyHomer.sendRequest("PUT", "/domain/internal/service/edna/nodes/"+myip+"/properties/lastseen", ""+value, "text/xml");
		smithers.put("/domain/internal/service/edna/nodes/"+myip+"/properties/lastseen", ""+value, "text/xml");

	}
	

	
	public static void send(String method, String uri) {
		try {
			MulticastSocket s = new MulticastSocket();
			String msg = myip+" "+method+" "+uri;
			byte[] buf = msg.getBytes();
			//System.out.println("EDNA SEND="+msg);
			DatagramPacket pack = new DatagramPacket(buf, buf.length,InetAddress.getByName(group), port);
			s.send(pack,(byte)ttl);
			s.close();
		} catch(Exception e) {
			System.out.println("LazyHomer error "+e.getMessage());
		}
	}
	
	public static Boolean up() {
		if (smithers==null) return false;
		return true;
	}
	
	public static String getSmithersUrl() {
		if (selectedsmithers==null) {
			for(Iterator<SmithersProperties> iter = smithers.values().iterator(); iter.hasNext(); ) {
				SmithersProperties s = (SmithersProperties)iter.next();
				if (s.isAlive()) {
					selectedsmithers = s;
				}
			}
		}
		return "http://"+selectedsmithers.getIpNumber()+":"+selectedsmithers.getPort()+"/smithers2";
	}
	
	public void remoteSignal(String from,String method,String url) {
		if (url.indexOf("/smithers/downcheck")!=-1) {
			for(Iterator<SmithersProperties> iter = smithers.values().iterator(); iter.hasNext(); ) {
				SmithersProperties sm = (SmithersProperties)iter.next();
				if (!sm.isAlive()) {
					LOG.info("One or more smithers down, try to recover it");
					LazyHomer.send("INFO","/domain/internal/service/getname");
				}
			}
		} else {
		// only one trigger is set for now so we know its for nodes :)
		if (ins.checkKnown()) {
			// we are verified (has a name other than unknown)		
			EdnaProperties mp = ednas.get(myip);
		}
		}
	}
	
	public static boolean isWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}
 
	public static boolean isMac() {
 		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("mac") >= 0);
 	}
 
	public static boolean isUnix() {
 		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);
 	}
	
	private void initConfig() {
		System.out.println("Bart: initializing configuration.");
		
		// properties
		Properties props = new Properties();
		
		// new loader to load from disk instead of war file
		String configfilename = "/springfield/homer/config.xml";
		if (isWindows()) {
			configfilename = "/springfield/homer/config.xml";
		}
		
		// load from file
		try {
			System.out.println("INFO: Loading config file from load : "+configfilename);
			File file = new File(configfilename);

			if (file.exists()) {
				props.loadFromXML(new BufferedInputStream(new FileInputStream(file)));
			} else { 
				System.out.println("FATAL: Could not load config "+configfilename);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// only get the marge communication port unless we are a smithers
		port = Integer.parseInt(props.getProperty("marge-port"));
	}

	
	/**
	 * get root path
	 */
	public static String getRootPath() {
		return rootPath;
	}
	
	private static void setLogLevel(String level) {
		Level logLevel = Level.INFO;
		Level oldlevel = LOG.getLogger(PACKAGE_ROOT).getLevel();
		switch (loglevels.valueOf(level)) {
			case all : logLevel = Level.ALL;break;
			case info : logLevel = Level.INFO;break;
			case warn : logLevel = Level.WARN;break;
			case debug : logLevel = Level.DEBUG;break;
			case trace : logLevel = Level.TRACE;break;
			case error: logLevel = Level.ERROR;break;
			case fatal: logLevel = Level.FATAL;break;
			case off: logLevel = Level.OFF;break;
		}
		if (logLevel.toInt()!=oldlevel.toInt()) {
			LOG.getLogger(PACKAGE_ROOT).setLevel(logLevel);
			LOG.info("logging level: " + logLevel);
		}
	}
	
 
	/**
	 * Initializes logger
	 */
    private void initLogger() {    	 
    	System.out.println("Initializing logging.");
    	
    	// get logging path
    	String logPath = LazyHomer.getRootPath().substring(0,LazyHomer.getRootPath().indexOf("webapps"));
		logPath += "logs/edna/edna.log";	
		

		
		try {
			// default layout
			Layout layout = new PatternLayout("%-5p: %d{yyyy-MM-dd HH:mm:ss} %c %x - %m%n");
			
			// rolling file appender
			DailyRollingFileAppender appender1 = new DailyRollingFileAppender(layout,logPath,"'.'yyyy-MM-dd");
			BasicConfigurator.configure(appender1);
			
			// console appender 
			ConsoleAppender appender2 = new ConsoleAppender(layout);
			BasicConfigurator.configure(appender2);
		}
		catch(IOException e) {
			System.out.println("EdnaServer got an exception while initializing the logger.");
			e.printStackTrace();
		}
		
		Level logLevel = Level.INFO;
		LOG.getRootLogger().setLevel(Level.OFF);
		LOG.getLogger(PACKAGE_ROOT).setLevel(logLevel);
		LOG.info("logging level: " + logLevel);
		
		LOG.info("Initializing logging done.");
    }

	
    /**
     * Shutdown
     */
	public static void destroy() {
		// destroy timer
		if (marge!=null) marge.destroy();
	}
	
	private class DiscoveryThread extends Thread {
	    DiscoveryThread() {
	      super("dthread");
	      start();
	    }

	    public void run() {
	     int counter = 0;
	      while (LazyHomer.noreply || counter<10) {
	    	if (counter>4 && LazyHomer.noreply) LOG.info("Still looking for smithers on multicast port "+port+" ("+LazyHomer.noreply+")");
	    	LazyHomer.send("INFO","/domain/internal/service/getname");
	        try {
	          sleep(500+(counter*100));
	          counter++;
	        } catch (InterruptedException e) {
	          throw new RuntimeException(e);
	        }
	      }
	      LOG.info("Stopped looking for new smithers");
	    }
	}
	
	public static int getPort() {
		return port;
	}

	public static String getRole() {
		return role;
	}


}
