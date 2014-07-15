/* 
* NamingConvention.java
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