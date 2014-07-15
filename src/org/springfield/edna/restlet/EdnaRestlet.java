/* 
* EdnaRestlet.java
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
import org.restlet.Context;
import org.restlet.routing.Router;

/**
 * Class that contains the routing logic
 */
public class EdnaRestlet extends Router {
	public EdnaRestlet(Context cx) {
		super(cx);
		
		// routing mode
		this.setRoutingMode(Router.MODE_BEST_MATCH);
		
		//image manipulation resource
		this.attachDefault(EdnaResource.class);
		
		// logging
		this.attach("/logging",LoggingResource.class);
		
		//developer manual resource
		this.attach("/manual", DeveloperManualResource.class);
		
	}
}
