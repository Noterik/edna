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
