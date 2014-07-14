package org.springfield.edna;

import java.net.InetAddress;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.restlet.Context;
import org.springfield.edna.homer.LazyHomer;


public class EdnaContextListener implements ServletContextListener {

	private static LazyHomer lh = null; 
	
	public void contextInitialized(ServletContextEvent event) {
		System.out.println("Edna: context initialized");
		ServletContext servletContext = event.getServletContext();
		
		// turn logging off
	//	Context.getCurrentLogger().setLevel(Level.SEVERE);
	//	Logger.getLogger("").setLevel(Level.SEVERE);
		
		
		LazyHomer lh = new LazyHomer();

		lh.init(servletContext.getRealPath("/"));
		
	}
	
	public void contextDestroyed(ServletContextEvent event) {
		System.out.println("Edna: context destroyed");
		lh.destroy();
		
		// destroy global config
		// GlobalConfig.instance().destroy();
	}

}
