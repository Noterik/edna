/* 
* EdnaContextListener.java
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
		
		LazyHomer lh = new LazyHomer();

		lh.init(servletContext.getRealPath("/"));
		
	}
	
	public void contextDestroyed(ServletContextEvent event) {
		System.out.println("Edna: context destroyed");
		lh.destroy();
	}

}
