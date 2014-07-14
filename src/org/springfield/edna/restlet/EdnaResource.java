package org.springfield.edna.restlet;

import java.awt.AlphaComposite;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;

import org.w3c.dom.*;

import javax.xml.xpath.*;
import javax.xml.parsers.*;

import java.net.URL;

import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.data.*;
import org.restlet.representation.*;
import org.restlet.resource.*;
import org.springfield.edna.GlobalConfig;
import org.springfield.edna.im.ImageManipulationGlobal;
import org.springfield.edna.im.ImageManipulationReceiverFactory;
import org.springfield.edna.restlet.EdnaResource;

public class EdnaResource extends ServerResource {
	Boolean error = false;
	private static int counter = 0;
	public enum validactions { crop,scale, adjust, rotate, transparent, compress, original; }

	/** the EdnaResource's log4j Logger */
	private static final Logger LOG = Logger.getLogger(EdnaResource.class);

	private static HashMap<String, String> scriptcommands = null;

	/**
	 * Called right after constructor of this resource (every request)
	 */
	@Override
	public void doInit() {
		//LOG.info("doInit");
		//read commands from script
		if (scriptcommands == null) {
			scriptcommands = readCommandList();
		}
	}

	// build manipulation parameter object
	ImageManipulationGlobal imr1 = new ImageManipulationGlobal();

	/**
	 * Main call
	 * 
	 * @param rep
	 * @return
	 */
	@Get
	public Representation performConvert(Representation rep) {
		//read parameters from hashmap and form into an array
		String[] commands = readParamsIntoCommands(rep);
		
		if(commands==null) { //Apply default command = thumbnail
			commands = applyThumbnailAction();
		}
		//get request to determine image path
		Request request = getRequest();
		Reference ref = request.getResourceRef();
		// determine image path
		String imPath = ref.getPath().substring("/edna/".length());

		// check if the image path is empty
		if (imPath.isEmpty()) {
			error = true;
			LOG.error("Missing image!");
		}
	
		
		/*
		File IMAGE_PATH = GlobalConfig.getInstance().IMAGE_PATH;
		if(!new File(IMAGE_PATH, imPath).exists()) {
			System.out.println("IMAGE_PATH does not exit: " + IMAGE_PATH.getAbsolutePath());
			IMAGE_PATH = new File("/mount/images2");
			if(!new File(IMAGE_PATH, imPath).exists()) {
				System.out.println("IMAGE_PATH does not exit: " + IMAGE_PATH.getAbsolutePath());
				IMAGE_PATH = new File("/mount/images3");
			}
		}
		*/
		
		// check check and call function to manipulate image
		rep = sendFromCache(commands);
		if (rep!=null) {
			return rep;
		}
		
		String path = "/springfield/edna/tmpimages/";
		String filename = ""+(counter++);
		
		System.out.println("TMP IMG = "+path+filename+" P="+imPath);
		
		boolean download = saveUrltoDisk(path+filename,"http://images1.noterik.com/"+imPath);
		if (!download) { download = saveUrltoDisk(path+filename,"http://images2.noterik.com/"+imPath); }
		if (!download) { download = saveUrltoDisk(path+filename,"http://images3.noterik.com/"+imPath); }
		
		if (!download) {
			System.out.println("SAVE FAILED EDNA CHECKED ALL 3 SERVERS !");
			// weird other code returns not and performs commands anyway !!!
		};
		
		
		//System.out.println("IMAGE_PATH: " + IMAGE_PATH.getAbsolutePath());
		// locate image
		File inImg = new File(path+filename);
		// check if image exists
		if (!inImg.exists()) {
			LOG.debug(inImg);
			error = true;
			LOG.error("No input image!");
		} else {
			// set parameters for input image
			imr1.setInputImage(inImg);
		}
		
		rep = performCommands(commands);
			
		return rep;	
	}
	
	/**determine parameters form and script and keep in array*/
	private String[] readParamsIntoCommands(Representation rep) {
		String result[] = new String[100];
		
		// get request and query form
		Request request = getRequest();
		Reference ref = request.getResourceRef();
		Form form = ref.getQueryAsForm();

		// read params, check each one if its a 'script' if so copy its list
		String key, value = null;
		Parameter param;
		int commandcount = 0;
		
		for (Iterator<Parameter> iter = form.iterator(); iter.hasNext();) {
			param = iter.next();
			key = param.getName();
			// is the key a script so in xmlcommands ????
			String script = scriptcommands.get(key);
			if (script != null) {
					// now we have a string of commands we need to copy to our
					// result
					String[] parts = script.split(",");
					for (int i = 0; i < parts.length; i++) {
						// need to copy results
						String commandstep = parts[i];
						String[] commandsplit = commandstep.split("=");
						String ckey = commandsplit[0];
						String cvalue = commandsplit[1];
						result[commandcount++] = ckey;
						result[commandcount++] = cvalue;
					}
			} else {
					value = param.getValue();
					result[commandcount++] = key;
					result[commandcount++] = value;
			}
		}
		if(commandcount==0) {
			return null;
		}
		// end result will be a total list of 'basic' commands
		return result;
	}
	
	private String[] applyThumbnailAction() {
		String result[] = new String[100];
		String script = scriptcommands.get("thumbnail");
		int commandcount = 0;
		if (script != null) {
				// now we have a string of commands we need to copy to our
				// result
				String[] parts = script.split(",");
				for (int i = 0; i < parts.length; i++) {
					// need to copy results
					String commandstep = parts[i];
					String[] commandsplit = commandstep.split("=");
					String ckey = commandsplit[0];
					String cvalue = commandsplit[1];
					result[commandcount++] = ckey;
					result[commandcount++] = cvalue;
				}
		}
		
		return result;
	}
	
	/** pass commands (key and values) to perform the image manipulation */
	private Representation performCommands(String[] commands) {
		boolean fromdisk = false;
		int count = 0;
		String key, value;
		//get request to determine verbose output
		Request request = getRequest();
		Reference ref = request.getResourceRef();
		Form form = ref.getQueryAsForm();

		// determine verbose output
		String verbose = form.getFirstValue("verbose", true, null);
		
		while (count < commands.length) {
			key = commands[count++];
			value = commands[count++];
			if (key != null && value != null) {
				processImage(key, value);
			}
		}
		String outputimagename = getoutputname(commands);
		// give output image
		File outImg = new File(GlobalConfig.getInstance().OUT_IMAGE_PATH,outputimagename);
		if (!fromdisk) {
			// save to storage
			imr1.setOutputImage(outImg);
			if (error == false) {
				ImageManipulationReceiverFactory imF = new ImageManipulationReceiverFactory();
				// invoke
				imF.buildManipulationReceiver(
						ImageManipulationReceiverFactory.JAVA2D).execute(imr1);
			}
		}
		if (verbose == null || !verbose.equals("true")) {
			if (!fromdisk) {
				// return recently processed image
				return new FileRepresentation(outImg, MediaType.IMAGE_JPEG);
			} else {
				// return already processed image from storage
				return new FileRepresentation(outputimagename,
						MediaType.IMAGE_JPEG);
			}
		}

		if (error) {
			// send status code
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new StringRepresentation("Invalid request to the image!");
		} else {
			return new StringRepresentation(" " + ref.getQuery());
		}
	}
	
	/** check if the image already exists in the cache, if it exits return it from here */
	private Representation sendFromCache(String[] commands) {
		boolean fromdisk = false;
		//get output image
		String outputimagename = getoutputname(commands);
		
		//get request to determine verbose output
		Request request = getRequest();
		Reference ref = request.getResourceRef();
		Form form = ref.getQueryAsForm();
		
		// determine verbose output
		String verbose = form.getFirstValue("verbose", true, null);
		File image = new File(GlobalConfig.getInstance().OUT_IMAGE_PATH,outputimagename);
		if (image.exists()) {
			// give output image
			imr1.setOutputImage(image);
			if (verbose == null || !verbose.equals("true")) {
				if (!fromdisk) {
					// return recently processed image
					return new FileRepresentation(image, MediaType.IMAGE_JPEG);
				} else {
					// return already processed image from storage
					return new FileRepresentation(outputimagename,
							MediaType.IMAGE_JPEG);
				}
			}

			if (error) {
				// send status code
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new StringRepresentation("Invalid request to the image!");
			} else {
				return new StringRepresentation(" " + ref.getQuery());
			}
		}
		return null;
	}

	/** function to provide output name of the image */
	private String getoutputname(String[] commands) {
		String output = null;
		Request request = getRequest();
		Reference ref = request.getResourceRef();
		String imPath = ref.getPath().substring("/edna/".length());
		String imagename = imPath.substring(0, imPath.lastIndexOf('.')); // without extension																			
		String extension = imPath.substring(imPath.lastIndexOf('.'));// with extension
		String directory = imagename.substring(0, imagename.lastIndexOf('/'));
		directory = GlobalConfig.getInstance().OUT_IMAGE_PATH + "/" + directory;
		//System.out.println("Try to create dir: " + directory);
		File outputDir = new File(directory);
		if(!outputDir.exists()) {
			boolean created = createDestDirectory(outputDir);
			if(!created) {
				System.out.println("Could not create output directory: " + directory);
				error = true;
				LOG.error("Could not save input image");
			}
		}																
		int count = 0;
		String key, value;
		while (count < commands.length) {
			key = commands[count++];
			value = commands[count++];
			if (key != null && value != null) {
				if (output==null) {
					output=key+"="+value;
				} else {
					output+="-"+key+"="+value;
				}
			}
		}
		output=imagename+"-"+output+extension;
		return output;
	}

	/** parse strings into integers and call method for image manipulation */
	private void processImage(String key, String value) {
	       switch (validactions.valueOf(key)) {
	           case crop :
	        	  try{
	  				String[] cropSplit = value.split("x");
	  				String cXco = cropSplit[0];
	  				String cYco = cropSplit[1];
	  				String cWidth = cropSplit[2];
	  				String cHeight = cropSplit[3];
	  				int x = Integer.parseInt(cXco);
	  				int y = Integer.parseInt(cYco);
	  				int w = Integer.parseInt(cWidth);
	  				int h = Integer.parseInt(cHeight);
	  				imr1.crop(x, y, w, h);
	  			}catch(Exception e){
	  				LOG.error("Exception encountered while cropping!");
	  			}
	        	  break;
	           case scale :
	        	   System.out.println("DO SCALE"); 
	        	   try{
	   				String[] scaleSplit = value.split("x");
	   				String sWidth = scaleSplit[0];
	   				String sHeight = scaleSplit[1];
	   				int intWidth = Integer.parseInt(sWidth);
	   				int intHeight = Integer.parseInt(sHeight);
	   				imr1.scale(intWidth, intHeight);
	   			}catch(Exception e){
	   				LOG.error("Exception encountered while scaling!");
	   			}
	        	   break;
	        	   
	           case adjust :
		        	  System.out.println("DO ADJUST"); 
		        	  try{
		  				String[] adjustSplit = value.split("x");
		  				String aWidth = adjustSplit[0];
		  				String aHeight = adjustSplit[1];
		  				int intWidth = Integer.parseInt(aWidth);
		  				int intHeight = Integer.parseInt(aHeight);
		  				imr1.adjustLayout(intWidth, intHeight);
		  			}catch(Exception e){
		  				LOG.error("Exception encountered adjusting!");
		  			}
		        	  break;
		        	  
	           case rotate :
		        	  System.out.println("DO ROTATE"); 
		        	  try{
		  				int intAngle = Integer.parseInt(value);
		  				if(intAngle<0 && intAngle>360){
		  					error = true;
		  					LOG.error("Invalid value to rotate an image");
		  				}else{
		  					imr1.rotate(intAngle);
		  				}
		  				
		  			}catch(Exception e){
		  				LOG.error("Exception encountered while rotating!");
		  			}

		        	  break;
		        	  
	           case transparent :
		        	  System.out.println("DO TRANSPARENT"); 
		        	  try{
		  				float val = Float.parseFloat(value);
		  				if (val > AlphaComposite.SRC_OVER || val < 0) {
		  					error = true;
		  					LOG.error("Invalid transparency value: value should be between 0 to 1");
		  				} else {
		  					imr1.transparent(val);
		  				}
		  			}catch(Exception e){
		  				LOG.error("Exception encountered while making an image transparent!");
		  			}
		        	  break;
		        	  
	           case compress :
		        	  System.out.println("DO COMPRESS"); 
		        	  try{
		  				float cVal = Float.parseFloat(value);
		  				if (cVal < 0 || cVal > 1) {
		  					error = true;
		  					LOG.error("Invalid compression value: value should be between 0 to 1");
		  				} else {
		  					imr1.compress(cVal);
		  				}
		  			}catch(Exception e){
		  				LOG.error("Exception encountered compressing!");
		  			}
		        	  break;
	           case original:
	        	   break;
		        	 
	       }
	}

	/** parse parameters from script (xml parsing) */
	private static HashMap<String, String> readCommandList() {
		String fullUrl = "http://bart1.noterik.com/bart/domain/webtv/service/edna/cmdlist";
		HashMap<String, String> cmdList = new HashMap<String, String>();

		try {
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(true);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document doc = builder.parse(new URL(fullUrl).openStream());
			doc.getDocumentElement().normalize();
			XPath xpath = XPathFactory.newInstance().newXPath();

			Object result = xpath.evaluate("//fsxml/cmdlist", doc,
					XPathConstants.NODESET);
			NodeList nodes = (NodeList) result;

			for (int i = 0; i < nodes.getLength(); i++) {
				Node cmdNode = (Node) xpath.evaluate("properties/name",
						nodes.item(i), XPathConstants.NODE);
				NodeList cmdSteps = (NodeList) xpath.evaluate("cmdstep/properties", nodes.item(i),
						XPathConstants.NODESET);
				String cmdline = "";
				for (int j = 0; j < cmdSteps.getLength(); j++) {
					Node nNode = cmdSteps.item(j);
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) nNode;
						if (!cmdline.equals(""))
							cmdline += ",";
						cmdline += getTagValue("key", eElement) + "="
								+ getTagValue("value", eElement);
					}
				}
				cmdList.put(cmdNode.getTextContent(), cmdline);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cmdList;
	}

	private static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0)
				.getChildNodes();
		Node nValue = nlList.item(0);
		return nValue.getNodeValue();
	}
	
	/** make directory for output image */
	private static boolean createDestDirectory(File dir) {
		boolean cd = false;
		try {
		 cd = dir.mkdirs();
		} catch (SecurityException e) {
		  LOG.error("Could not create directory "+dir.toString());
		}
		
		LOG.debug("Create directory " + dir.toString() + ": " + Boolean.toString(cd));
		
		return cd;
	}
	
	private boolean saveUrltoDisk(String filename,String url) {
		try {
			BufferedInputStream in = null;
			FileOutputStream fout = null;
			try {
				in = new BufferedInputStream(new URL(url).openStream());
				fout = new FileOutputStream(filename);

				final byte data[] = new byte[1024];
				int count;
				while ((count = in.read(data, 0, 1024)) != -1) {
					fout.write(data, 0, count);
				}
			} finally {
				if (in != null) {
					in.close();
				}
				if (fout != null) {
					fout.close();
				}
			}
		} catch(Exception e) {
			//e.printStackTrace();
			return false;
		}	
		return true;
	}


}
