/* 
* DeveloperManualResource.java
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

import org.apache.log4j.Logger;
import org.restlet.data.MediaType;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.springfield.edna.GlobalConfig;
import org.springfield.edna.restlet.DeveloperManualResource;

public class DeveloperManualResource extends ServerResource{

	/** the TestResource's log4j Logger */
	private static Logger logger = Logger.getLogger(DeveloperManualResource.class);
	
	/**
	 * Called right after constructor of this resource (every request)
	 */
	public void doInit() {
		logger.info("doInit");
	}
	
	/**
	 * Test get
	 * 
	 * @param rep
	 * @return
	 */
	@Get
	public Representation getTest(Representation rep) {
		
		FileRepresentation rep1 = new FileRepresentation (GlobalConfig.getInstance().DEVELOPER_MANUAL, MediaType.TEXT_HTML);
		return rep1;
		
	}

	
	
}
