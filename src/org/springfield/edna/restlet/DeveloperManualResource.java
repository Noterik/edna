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
