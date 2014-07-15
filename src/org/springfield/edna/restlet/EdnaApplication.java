/* 
* EdnaApplication.java
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
