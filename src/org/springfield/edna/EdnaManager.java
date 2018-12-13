package org.springfield.edna;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.imageio.ImageWriteParam;
import javax.media.j3d.RotationInterpolator;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Rotation;
import org.springfield.edna.im.ProcessingImage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifInteropDirectory;
import com.drew.metadata.jpeg.JpegDirectory;

public class EdnaManager {
	
	private enum validactions { crop,scale, adjust, rotate, transparent, compress, original,recompress, creatememe,amazons3; }
	private static EdnaManager instance;
	private static HashMap<String, String> scriptcommands = null;
	private int counter = 0;
	private HashMap<String, String> urlParams = new HashMap<String, String>();
	
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
	
	public void sendImageBasedOnURL(String image,HttpServletRequest request,HttpServletResponse response) {
		String commands[] = null;
		this.parseUrlParameters(request);
		
		String script = request.getParameter("script");
		if (script!=null) {
			commands = applyScript(script);
		}
		
		if(commands==null) { //Apply default command = thumbnail
			commands = applyScript("thumbnail");
		}
		image = image.replace("%22","?");
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

		boolean download = false;
		int pos = inputimage.indexOf("/external/");
		if (pos==0) {
			if (inputimage.indexOf(".pdf")!=-1) {
				inputimage = inputimage.replace("(","[");
				inputimage = inputimage.replace(")","]");
				int pos2 = inputimage.indexOf("[");
				if (pos2!=-1) {
					String slidenumber = inputimage.substring(pos2+1);
					slidenumber = slidenumber.substring(0,slidenumber.indexOf("]"));
					inputimage = inputimage.substring(0,pos2);
					download = saveUrltoDisk(path+filename+".pdf","http://"+inputimage.substring(pos+10));
					if (download) {
						// now get the correct image from the PDF
						extractSlideFrom(path+filename+".pdf",path+filename,slidenumber);
					}
				}
			} else {
				//System.out.println("EDNA HTTPS CHECK="+inputimage.substring(pos+10));
				String spath = inputimage.substring(pos+10);
				if (spath.startsWith("s/")) {
					download = saveUrltoDisk(path+filename,"https://"+inputimage.substring(pos+12));		
				} else {
					download = saveUrltoDisk(path+filename,"http://"+inputimage.substring(pos+10));
				}
			}
			if (inputimage.indexOf(".svg")!=-1) {
				//System.out.println("SVG detected");
				// we should pass this untouched 
				File tmpimage = new File(path+filename); 
				String dirname = diskname.substring(0,diskname.lastIndexOf("/"));
				File dir = new File(dirname);
				if (!dir.exists()) {
					dir.mkdirs();
				}
				tmpimage.renameTo(new File(diskname));
				return;
			}
		} else {
			download = saveUrltoDisk(path+filename,"http://images1.noterik.com/"+inputimage);
			if (!download) { download = saveUrltoDisk(path+filename,"http://images2.noterik.com/"+inputimage); }
			if (!download) { download = saveUrltoDisk(path+filename,"http://images3.noterik.com/"+inputimage); }
		}
		
		if (download) {
		
			File tmpimage = new File(path+filename);
			int cameraorientation = 1;
			cameraorientation = readImageOrientation(tmpimage);


			//int cameraorientation = 1;
			
			ProcessingImage image = new ProcessingImage(tmpimage);
			
			// auto rotate first ?
			if (cameraorientation!=1) {

			    switch (cameraorientation) {
			    	case 3:
						processImageNew(image,"rotate","180");
			    		break;
			    	case 6:
						processImageNew(image,"rotate","90");
			    		break;
			    	case 8:
						processImageNew(image,"rotate","270");
			    		break;
				}
			}
			
		
			for (int i=0;i<commands.length;i++) { // needs minimal 2 commands ?
				String[] command = commands[i].split("=");
				String key = command[0];
				String value = command[1];
				processImageNew(image, key, value);
			}	
		
			String dirname = diskname.substring(0,diskname.lastIndexOf("/"));
			File dir = new File(dirname);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			
			image.writeToFile(diskname);
			
			//tmpimage.delete();
		}
	}
	
	private void processImageNew(ProcessingImage image,String key, String value) {
	    switch (validactions.valueOf(key)) {
	       case crop :
        	   doCrop(image,value);
	    	   break;
           case scale :
        	   doScale(image,value);
        	   break;
           case adjust :
        	   break;
           case rotate :
        	   doRotate(image,value);
        	   break;
           case transparent :
        	   break;
           case compress :
        	   doCompress(image,value);
        	   break;
           case recompress :
        	   doRecompress(image,value);
        	   break;
           case amazons3 :
        	   doAmazonS3(image,value);
        	   break;
           case original:
        	   break;    
           case creatememe:
        	   doCreateMeme(image,value);
        	   break;
       }

	}
	
	private void doScale(ProcessingImage image,String value) {
			String[] params = value.split("x");
			String width = params[0];
			String height = params[1];
			if (width.indexOf("!")==-1 && height.indexOf("!")==-1) { // so its a direct scale
				try {
					int newWidth = Integer.parseInt(width);
					int newHeight = Integer.parseInt(height);
					image.workingImage = org.imgscalr.Scalr.resize(image.workingImage,Scalr.Method.QUALITY,Scalr.Mode.FIT_EXACT,newWidth, newHeight,Scalr.OP_ANTIALIAS);
				} catch(Exception e) {
				
				}
			} else {
				width = width.substring(0,width.length()-1);
				height = height.substring(0,height.length()-1);
				int cw = image.workingImage.getWidth();
				int ch = image.workingImage.getHeight();
				try {
					int nw = Integer.parseInt(width);
					int nh = Integer.parseInt(height);
					// protect for silly scales if with or height is bigger then original we scale to that
					if (cw<nw) { // scale on with is silly
						if (ch>nh) { // height is bigger so scale on that
							image.workingImage = org.imgscalr.Scalr.resize(image.workingImage,Scalr.Method.QUALITY,Scalr.Mode.FIT_TO_HEIGHT,nw, nh,Scalr.OP_ANTIALIAS);
						} else {
							// we ignore scale both width and height is already smaller
						}
						
					} else {
						// ok so am i really the best option maybe height is worse ?
						int dw = cw - nw;
						int dh = ch - nh;
						if (dw>dh) { // nope with is more pixels over 
							image.workingImage = org.imgscalr.Scalr.resize(image.workingImage,Scalr.Method.QUALITY,Scalr.Mode.FIT_TO_WIDTH,nw, nh,Scalr.OP_ANTIALIAS);
						} else {
							image.workingImage = org.imgscalr.Scalr.resize(image.workingImage,Scalr.Method.QUALITY,Scalr.Mode.FIT_TO_HEIGHT,nw, nh,Scalr.OP_ANTIALIAS);
						}
					}
				} catch(Exception e) {
				
				}
			}
	}
	
	private void doRotate(ProcessingImage image, String value) {
		try {
			int rotation = Integer.parseInt(value);
			 AffineTransformOp[] xform = null;
			 Rotation rot = null;
			switch(rotation){
				case 90:
					rot = Rotation.CW_90;
					break;
				case 180:
					rot = Rotation.CW_180;
					break;
				case 270:
					rot = Rotation.CW_270;
					break;
			}
			 
			image.workingImage = org.imgscalr.Scalr.rotate(image.workingImage, rot,xform);
				
			
		} catch(Exception e) {
			
		}
	}
	
	private void doCrop(ProcessingImage image,String value) {
		String[] params = value.split("x");
		try {
			String cXco = params[0];
			String cYco = params[1];
			String cWidth = params[2];
			String cHeight = params[3];
			int x = Integer.parseInt(cXco);
			int y = Integer.parseInt(cYco);
			int w = Integer.parseInt(cWidth);
			int h = Integer.parseInt(cHeight);
			image.workingImage = org.imgscalr.Scalr.crop(image.workingImage, x, y, w, h);
		} catch(Exception e) {
			
		}
}
	
	private void doCompress(ProcessingImage image,String value) {
  	  try{
			float cVal = Float.parseFloat(value);
			if (cVal < 0 || cVal > 1) {
				System.out.println("edna: compress only valid between 0 and 1");
			} else {
				image.iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			    image.iwparam.setCompressionQuality(cVal);
			}
		}catch(Exception e){
		}
	}
	
	private void doRecompress(ProcessingImage image,String value) {
	  	  image.recompress = value;
	}
	
	private void parseUrlParameters(HttpServletRequest request){
		for(Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
		    String key = entry.getKey();
		    String[] value = entry.getValue();
		    String rs = "";
		    for(int i = 0; i < value.length; i++){
		    	rs = value[i];
		    }
		    urlParams.put(key, rs);
		}
	}
	
	private void doAmazonS3(ProcessingImage image,String value) {
	  	  image.amazons3 = value;
	}
	
	private void doCreateMeme(ProcessingImage image,String value){
		String memeText = urlParams.get("txt");
		int memeFontSize = Integer.parseInt(urlParams.get("fs"));
		if(memeFontSize <= 0){
			memeFontSize = 40;
		}
		String memeFontType = urlParams.get("ft");
		if(memeFontType == null || memeFontType == ""){
			memeFontType = "serif";
		}
		int position = Integer.parseInt(urlParams.get("pos"));
		if(position < 0 || position > 2){
			position = 1;
		}
		
		burnStringInToImage(image,memeText,memeFontType,memeFontSize, position);
	}
	
	private void burnStringInToImage(ProcessingImage image, String text, String fontType, int fontSize, int positon){
		int width = image.workingImage.getWidth();
		int height = image.workingImage.getHeight();
		Font font = new Font(fontType, Font.BOLD, fontSize);

		Graphics2D g2 = image.workingImage.createGraphics();
		
		g2.drawImage(image.workingImage, 0, 0, null);
		g2.setColor(Color.white);
		g2.setFont(font);
		FontMetrics fm = g2.getFontMetrics();
		int d = 0;
		switch(positon){
			case 0:
					d = image.workingImage.getHeight() - 40;
				break;
			case 1:
					d = image.workingImage.getHeight() / 2;
				break;
			case 2:
					d= image.workingImage.getHeight() / 4;
				break;
		}
		int x = (image.workingImage.getWidth() - fm.stringWidth(text)) / 2;
		int y = image.workingImage.getHeight() - d;
		g2.drawString(text, x, y);
		g2.dispose();
	}
	
	
	/** parse parameters from script (xml parsing) */
	private static HashMap<String, String> readCommandList() {
		String filename = "/springfield/edna/config/cmdlist.xml";
		HashMap<String, String> cmdList = new HashMap<String, String>();

		try {
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(true);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document doc = builder.parse(new File(filename));
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
	
	private String[] applyScript(String name) {
		String script = scriptcommands.get(name);
		if (script != null) {
				return script.split(",");
		}
		return null;
	}

	private String getOutputName(String filename, String[] commands) {
		String extension = "";
		String basedir = "/springfield/edna/outputimages";
		int pos = filename.lastIndexOf("/");
		String imagepath = filename.substring(0,pos);
		
		String cmdstring = filename.substring(pos+1);
		int pos2 = cmdstring.indexOf(".");
		
		if (pos2!=-1) {
			extension = cmdstring.substring(pos2);
			cmdstring = cmdstring.substring(0,pos2);
		}
		
		for (int i=0;i<commands.length;i++) {
			cmdstring+="-"+commands[i];
		}
		
		return basedir+imagepath+"/"+cmdstring+extension;
	}
	
	private void sendFile(File file,HttpServletResponse response) {
		try {
			response.setHeader("Cache-Control", "no-transform,public,max-age=86400,s-maxage=86400");
			response.setContentType(setCorrectExtention(file.getName()));
			response.setContentLength((int)file.length());
			FileInputStream in = new FileInputStream(file);    
			OutputStream out = response.getOutputStream();
			byte[] buf = new byte[10240];    
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
			    URL urlo = new URL(url);
			    HttpURLConnection oc = (HttpURLConnection) urlo.openConnection();
				oc.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");

				try {
				int code = 0;
				try {
					code = oc.getResponseCode();
				} catch(SSLException e2) {
					System.out.println("supress sec. exception");
				}
			    if (code==301) {
			    	String redirect = oc.getHeaderField("Location");
				    urlo = new URL(redirect);
				    oc = (HttpURLConnection) urlo.openConnection();
					oc.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");

			    }
				} catch(Exception e) {
					System.out.println("could not get return code doing a ignore");
				}
				in = new BufferedInputStream(oc.getInputStream());
				//oc.getResponseCode();
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
			e.printStackTrace();
			return false;
		}	
		return true;
	}
	
	private String setCorrectExtention(String name) {
		String ext = name.substring(name.lastIndexOf(".")+1);
		ext = ext.toLowerCase();
		if (ext.equals("jpg")) {
			return("image/jpeg");
		} else if (ext.equals("svg")) {
			return("image/svg+xml");
		}
		return("image/jpeg");
	}
	
	private boolean extractSlideFrom(String inputname,String outputname,String slidenumber) {
		String cmd = "/usr/bin/convert";
		cmd += " -density 400";
		cmd += " "+inputname+"["+slidenumber+"]";
		cmd += " jpg:"+outputname;

		try {
			Process child = Runtime.getRuntime().exec(cmd);
			InputStream is = child.getErrorStream();
			if (is != null) {
				BufferedReader br = new BufferedReader( new InputStreamReader(is) );
				String line;
				while ((line = br.readLine()) != null) {
					
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static int readImageOrientation(File imageFile)  {
	    try {	
	    	try {
	    	Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
	    	Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
	    	JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
	    	if (directory.containsTag(ExifInteropDirectory.TAG_EXPOSURE_PROGRAM)) {
	    		System.out.println("CR="+directory.getInt(ExifInteropDirectory.TAG_EXPOSURE_PROGRAM));
	    	}
	    	if (directory.containsTag(ExifInteropDirectory.TAG_CUSTOM_RENDERED)) {
	    		System.out.println("CR="+directory.getInt(ExifInteropDirectory.TAG_CUSTOM_RENDERED));
	    	}
	    	if (directory.containsTag(ExifInteropDirectory.TAG_SCENE_CAPTURE_TYPE)) {
	    		System.out.println("CCT="+directory.getInt(ExifInteropDirectory.TAG_SCENE_CAPTURE_TYPE));
	    	}
	        return  directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
	    	} catch(NoClassDefFoundError e2) {
	    		
	    	}
	    } catch (Exception e) {
	    	e.printStackTrace();
	    } 
	    return 1;
	}
	


	
}
