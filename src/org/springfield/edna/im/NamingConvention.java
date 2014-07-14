package org.springfield.edna.im;


public class NamingConvention   {
	
	public static String convertName(ImageManipulationGlobal imr) {
		String path = imr.originalImage.getParent();
		String filename = imr.originalImage.getName();
		String filebase = filename.substring( 0, filename.lastIndexOf('.') );
		String extension = filename.substring( filename.lastIndexOf('.') );
		
		String defaultName = filebase;
		if (imr.doScale){
			defaultName += "-s" + imr.width + "-" + imr.height;
		}
		
		if (imr.doRotate){
			defaultName += "-r" + imr.angle;
		}
		
		if (imr.doTransparent){
			defaultName += "-t" + imr.pixels;
		}
		
		if (imr.doCrop){
			defaultName += "-cr" + imr.xPos + "-" + imr.yPos + "-"+ imr.width + "-" + imr.height;
		}
		
		if (imr.doCompress){
			defaultName += "-c"+ imr.cQuality;
		}
		
		if(imr.doAdjustLayout){
			defaultName += "-a" + imr.width + "-" + imr.height;
		}
		defaultName += extension;
		defaultName = path+"/"+defaultName;
		 		
		return defaultName;
	}
	

}