package org.springfield.edna;

import java.awt.AlphaComposite;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.springfield.edna.im.ImageManipulationGlobal;
import org.springfield.edna.im.ImageManipulationReceiverFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class EdnaManager {
	
	private enum validactions { crop,scale, adjust, rotate, transparent, compress, original; }
	private static EdnaManager instance;
	private static HashMap<String, String> scriptcommands = null;
	private int counter = 0;
	
	//ImageManipulationGlobal imr1 = new ImageManipulationGlobal();
	
	private EdnaManager() {
        System.out.println("Edna Manager started");
		if (scriptcommands == null) {
			scriptcommands = readCommandList();
		}
	}
	
    public static EdnaManager instance(){
    	if(instance==null) instance = new EdnaManager();
    	return instance;
    }
	
	public void sendImageBasedOnURL(String image,String params,HttpServletResponse response) {
		String commands[] = null;
		
		if (params!=null) {
			// read from uri
		}
		
		if(commands==null) { //Apply default command = thumbnail
			commands = applyThumbnailAction();
		}
				
		String diskname = getOutputName(image,commands);
		File file = new File(diskname);
		if (file.exists()) {
			// send from cache !
			sendFile(file,response);
		} else {
			// generate file
			generateImageOnDisk(image,diskname,commands);
			// now send from cache 
			file = new File(diskname);	
			if (file.exists()) {
				sendFile(file,response);
			}
		}
	}	
	
	
	private void generateImageOnDisk(String inputimage,String diskname,String[] commands) {
	
		String path = "/springfield/edna/tmpimages/";
		String filename = ""+(counter++); // simple counter to make sure filenames are new each time. files are deleted when done

		boolean download = saveUrltoDisk(path+filename,"http://images1.noterik.com/"+inputimage);
		if (!download) { download = saveUrltoDisk(path+filename,"http://images2.noterik.com/"+inputimage); }
		if (!download) { download = saveUrltoDisk(path+filename,"http://images3.noterik.com/"+inputimage); }
		
		if (download) {
		
			ImageManipulationGlobal imr = new ImageManipulationGlobal();
			
			File tmpimage = new File(path+filename); 
			imr.setInputImage(tmpimage);
		
			for (int i=0;i<commands.length;i++) {
				String[] command = commands[i].split("=");
				String key = command[0];
				String value = commands[1];
				processImage(imr, key, value);
			}	
		
			String dirname = diskname.substring(0,diskname.lastIndexOf("/"));
			File dir = new File(dirname);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			imr.setOutputImage(new File(diskname));
			ImageManipulationReceiverFactory imF = new ImageManipulationReceiverFactory();
			imF.buildManipulationReceiver(ImageManipulationReceiverFactory.JAVA2D).execute(imr);
			
			tmpimage.delete();
		}
	}
	
	private void processImage(ImageManipulationGlobal imr,String key, String value) {
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
	  				imr.crop(x, y, w, h);
	  			}catch(Exception e){
	  				//LOG.error("Exception encountered while cropping!");
	  			}
	        	  break;
	           case scale :
	        	   //System.out.println("DO SCALE"); 
	        	   try{
	   				String[] scaleSplit = value.split("x");
	   				String sWidth = scaleSplit[0];
	   				String sHeight = scaleSplit[1];
	   				int intWidth = Integer.parseInt(sWidth);
	   				int intHeight = Integer.parseInt(sHeight);
	   				imr.scale(intWidth, intHeight);
	   			}catch(Exception e){
	   				//LOG.error("Exception encountered while scaling!");
	   			}
	        	   break;
	        	   
	           case adjust :
		        	  //System.out.println("DO ADJUST"); 
		        	  try{
		  				String[] adjustSplit = value.split("x");
		  				String aWidth = adjustSplit[0];
		  				String aHeight = adjustSplit[1];
		  				int intWidth = Integer.parseInt(aWidth);
		  				int intHeight = Integer.parseInt(aHeight);
		  				imr.adjustLayout(intWidth, intHeight);
		  			}catch(Exception e){
		  				//LOG.error("Exception encountered adjusting!");
		  			}
		        	  break;
		        	  
	           case rotate :
		        	  //System.out.println("DO ROTATE"); 
		        	  try{
		  				int intAngle = Integer.parseInt(value);
		  				if(intAngle<0 && intAngle>360){
		  				//	error = true;
		  				//	LOG.error("Invalid value to rotate an image");
		  				}else{
		  					imr.rotate(intAngle);
		  				}
		  				
		  			}catch(Exception e){
		  				//LOG.error("Exception encountered while rotating!");
		  			}

		        	  break;
		        	  
	           case transparent :
		        	  //System.out.println("DO TRANSPARENT"); 
		        	  try{
		  				float val = Float.parseFloat(value);
		  				if (val > AlphaComposite.SRC_OVER || val < 0) {
		  					//error = true;
		  					//LOG.error("Invalid transparency value: value should be between 0 to 1");
		  				} else {
		  					imr.transparent(val);
		  				}
		  			}catch(Exception e){
		  				//LOG.error("Exception encountered while making an image transparent!");
		  			}
		        	  break;
		        	  
	           case compress :
		        	  //System.out.println("DO COMPRESS"); 
		        	  try{
		  				float cVal = Float.parseFloat(value);
		  				if (cVal < 0 || cVal > 1) {
		  					//error = true;
		  					//LOG.error("Invalid compression value: value should be between 0 to 1");
		  				} else {
		  					imr.compress(cVal);
		  				}
		  			}catch(Exception e){
		  				//LOG.error("Exception encountered compressing!");
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
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		Node nValue = nlList.item(0);
		return nValue.getNodeValue();
	}
	
	private String[] applyThumbnailAction() {
		String script = scriptcommands.get("thumbnail");
		if (script != null) {
				return script.split(",");
		}
		return null;
	}

	private String getOutputName(String filename, String[] commands) {

		String basedir = "/springfield/edna/outputimages";
		int pos = filename.lastIndexOf("/");
		String imagepath = filename.substring(0,pos);
		
		String cmdstring = filename.substring(pos+1);
		int pos2 = cmdstring.indexOf(".");
		String extension = cmdstring.substring(pos2);
		cmdstring = cmdstring.substring(0,pos2);
		
		for (int i=0;i<commands.length;i++) {
			cmdstring+="-"+commands[i];
		}
		
		//System.out.println("imagepath="+imagepath);
		//System.out.println("cmdstring="+cmdstring);
		//System.out.println("ext="+extension);
		//System.out.println("TOTAL="+ basedir+imagepath+"/"+cmdstring+extension);
		return basedir+imagepath+"/"+cmdstring+extension;
	}
	
	private void sendFile(File file,HttpServletResponse response) {
		try {
			response.setContentType("image/jpeg");
			response.setContentLength((int)file.length());
			FileInputStream in = new FileInputStream(file);    
			OutputStream out = response.getOutputStream();
			byte[] buf = new byte[1024];    
			int i = 0;    
		    while ((i = in.read(buf)) > 0) {   
				out.write(buf, 0, i);    
			}    
			out.flush();       
			out.close(); 
			in.close();
		
		} catch(Exception e) {
			e.printStackTrace();
		}
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
