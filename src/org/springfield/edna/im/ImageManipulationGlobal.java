package org.springfield.edna.im;

import java.io.File;

public class ImageManipulationGlobal {
	public int width;
	public int height;
	public float pixels;
	public int angle;
	public int type;
	public int xPos;
	public int yPos;
	public float cQuality; //compression quality for the image
	public File originalImage;
	public File  newImage;
	public boolean doScale;
	public boolean doRotate;
	public boolean doTransparent;
	public boolean doCrop;
	public boolean doAdjustLayout;
	public boolean doCompress;
	public boolean doBW; //Black and white image
	
	
	public void setInputImage (File imgSrc){
		originalImage = imgSrc;
	}
	
	public void setOutputImage (File destImg){
			newImage = destImg;
	}
	
	
	public void scale(int wid, int hei){
		width = wid;
		height = hei;
		doScale = true;
	}
	
	public void rotate(int ang){
		angle = ang;
		doRotate = true;
	}
	
	public void transparent(float pix){
		pixels = pix;
		doTransparent = true;
	}
	
	public void crop (int x, int y, int wid, int hei){
		xPos = x;
		yPos = y;
		width = wid;
		height = hei;
		doCrop = true;
	}
	
	public void adjustLayout (int w, int h){
		width = w;
		height = h;
		doAdjustLayout = true;
	}
	
	public void compress (float quality){
		cQuality = quality;
		doCompress = true;
	}
	
	public void blackwhite(){
		doBW = true; 
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ImageManipulationGlobal [width=" + width + ", height=" + height
				+ ", pixels=" + pixels + ", angle=" + angle + ", type=" + type
				+ ", xPos=" + xPos + ", yPos=" + yPos + ", cQuality="
				+ cQuality + ", originalImage=" + originalImage + ", newImage="
				+ newImage + ", doScale=" + doScale + ", doRotate=" + doRotate
				+ ", doTransparent=" + doTransparent + ", doCrop=" + doCrop
				+ ", doAdjustLayout=" + doAdjustLayout + ", doCompress="
				+ doCompress + "]";
	}
	
}
