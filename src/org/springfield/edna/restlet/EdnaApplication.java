package org.springfield.edna.restlet;

import java.util.logging.Level;

import javax.servlet.ServletContext;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.springfield.edna.GlobalConfig;
import org.springfield.edna.homer.*;


/**
 * Application class
 */
public class EdnaApplication extends Application {
	private static LazyHomer lh = null; 
	
	public EdnaApplication() {
		super();
	}

	public EdnaApplication(Context cx) {
		super(cx);
	}

	/**
	 * Called on startup
	 */
	public void start() {
		System.out.println(GlobalConfig.SERVICE_NAME+": starting application");
		try {
			super.start();
		} catch (Exception e) {
			System.out.println(GlobalConfig.SERVICE_NAME+": error starting application");
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Called on shutdown
	 */
	public void stop() throws Exception {
		try {
			super.stop();
		} catch (Exception e) {
			System.out.println(GlobalConfig.SERVICE_NAME+": error stopping application");
			e.printStackTrace();
		}
		
		// destroy global config
		GlobalConfig.getInstance().destroy();
		//lh.destroy();
	}
	
	@Override
	public Restlet createRoot() {
		// get servlet context
		ServletContext servletContext = (ServletContext) getContext()
			.getAttributes()
			.get("org.restlet.ext.servlet.ServletContext");
		
		// turn logging off
		Context.getCurrentLogger().setLevel(Level.OFF);
		
		if(servletContext!=null) {
			// initialize global config
			GlobalConfig.getInstance().initialize(servletContext.getRealPath("/"));
		}
		
		//lh = new LazyHomer();
		//lh.init(servletContext.getRealPath("/"));
		
		return new EdnaRestlet(super.getContext());
	}
}
