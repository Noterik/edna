package org.springfield.edna.im;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;

import org.apache.log4j.Logger;


public class ImageManipulationReceiverJava2D implements IImageManipulationReceiver {
	/** the ImageManipulationReceiverJava2D's log4j logger */
	private static final Logger LOG = Logger.getLogger(ImageManipulationReceiverJava2D.class);
	
	private BufferedImage workingImage = null;
	private ImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
	
	public boolean execute (ImageManipulationGlobal imG){
		LOG.debug("Executing Java2D with "+imG);
		
		if (imG.originalImage == null){
			LOG.error("no input image");
			return false;
		}else{
			readImage(imG.originalImage);
		}
		
		if (imG.doScale){
			scaleImage(imG.width, imG.height);
		}
		
		if (imG.doRotate){
			rotateImage( imG.angle);
		}
		
		if (imG.doTransparent){
			makeTransparent (imG.pixels);
		}
		
		if (imG.doCrop){
			cropImage (imG.xPos, imG.yPos, imG.width, imG.height);
		}
		
		if (imG.doCompress){
			compressImage (imG.cQuality);
		}
		
		if (imG.doAdjustLayout){
			adjustLayout (imG.width, imG.height);
		}
		
		if (imG.doBW){
			blackwhite ();
		}
		
		if (imG.newImage == null){
			LOG.error("no output image");
			return false;
		}else{
				if(imG.doCompress) {
				writeCompressedImage (imG.newImage);
			} else {
				writeImage (imG.newImage);
			}
			
		}
		
		return true;
		
	}
	

	//method for getImage
	private void readImage (File im1){
		LOG.debug("Trying to read image "+im1);
		try {
			workingImage = ImageIO.read(im1);
		} catch (IOException e) {
			LOG.error("Could not read input image",e);
		}
	}
	
	//method for scaling an image
	private void scaleImage(int w, int h) {
		LOG.debug("Scaling image "+w+"x"+h);
		
		int imgWidth = workingImage.getWidth(); //get width of working image
		int imgHeight = workingImage.getHeight();//get height of working image
		
		double scaleRatio = (double)w/(double)h;
		double imageRatio = (double)imgWidth/(double)imgHeight;
		
		int type = workingImage.getType();
		BufferedImage rescaledImage = new BufferedImage (w, h, type);
		
		
		if(scaleRatio<imageRatio) {
			  int newHeight1 = (int) (w/imageRatio);
			  Graphics2D graphics = rescaledImage.createGraphics();
			  graphics.drawImage(workingImage, 0, (h-newHeight1)/2, w, newHeight1,null);
			  graphics.dispose();
			 
		   } else {
			  int newWidth1 = (int) (h*imageRatio);
			  Graphics2D graphics = rescaledImage.createGraphics();
			  graphics.drawImage(workingImage, (w-newWidth1)/2, 0, newWidth1, h,null);
			  graphics.dispose();
		   }
		workingImage = rescaledImage;
	}
	
	//method for rotating an image
	private void rotateImage(int angle){
		LOG.debug("Rotating image with angle "+angle);
		int w = workingImage.getWidth();
		int h = workingImage.getHeight();
		int type = workingImage.getType();
		BufferedImage newImage = new BufferedImage (w, h, type );
		Graphics2D g = newImage.createGraphics();
		g.rotate(Math.toRadians(angle), w/2, h/2);
		g.drawImage(workingImage, null, 0, 0);
		g.dispose();
		workingImage = newImage;
	}
	
	//making image transparent
	private void makeTransparent (float transperancy){
		LOG.debug("Making image transparent with "+transperancy);
		BufferedImage newImage = new BufferedImage (workingImage.getWidth(), workingImage.getHeight(),BufferedImage.TRANSLUCENT);
		Graphics2D g = newImage.createGraphics();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transperancy));
		g.drawImage (workingImage, null, 0, 0);
		g.dispose();
		workingImage = newImage;
	}
	
	//method for cropping image
	private void cropImage (int x1, int y1, int w, int h){
		LOG.debug("Cropping image to x:"+x1+", y:"+y1+", width:"+w+", h:"+h);
		int type = workingImage.getType();
		BufferedImage croppedImage = new BufferedImage (w, h, type);
		Graphics2D g = croppedImage.createGraphics();
		g.drawImage(croppedImage, x1, y1, w, h, null);
		g.dispose();
		workingImage = croppedImage;
	}
	
	
	//method to compress image
	private void compressImage (float q){
		LOG.debug("Compressing image with quality "+q);
		iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	    iwparam.setCompressionQuality(q);
	}
	
	private void adjustLayout (int w, int h){
		LOG.debug("Adjust the layout of an image "+w+"x"+h);
		
		int imageWidth = workingImage.getWidth(); //get width of working image
		int imageHeight = workingImage.getHeight();//get height of working image
		
		double adjustRatio = (double)w/(double)h;
		double imageRatio = (double)imageWidth/(double)imageHeight;
		
		int type = workingImage.getType();
		BufferedImage adjustedImage = new BufferedImage (w, h, type);
		
		
		if(adjustRatio<imageRatio) {
			  int newH = (int) (w/imageRatio);
			  Graphics2D graphics = adjustedImage.createGraphics();
			  graphics.drawImage(workingImage, 0, (h-newH)/2, w, newH,null);
			  graphics.dispose();
			 
		   } else {
			  int newW = (int) (h*imageRatio);
			  Graphics2D graphics = adjustedImage.createGraphics();
			  graphics.drawImage(workingImage, (w-newW)/2, 0, newW, h,null);
			  graphics.dispose();
		   }
		workingImage = adjustedImage;
	}
	
	private void blackwhite(){
		LOG.debug("Make image black and white");
		int imageWidth = workingImage.getWidth(); //get width of working image
		int imageHeight = workingImage.getHeight();//get height of working image
		BufferedImage im =
			  new BufferedImage(imageWidth,imageHeight,BufferedImage.TYPE_BYTE_BINARY);
		Graphics2D graphics = im.createGraphics();  
		graphics.drawImage(workingImage,0,0,null); 
		workingImage = im;

	}
	
	private void writeImage (File dest) {
		LOG.debug("Trying to write image "+dest);
		try {
			ImageIO.write(workingImage, "jpg", dest);	
		} catch(Exception e) {
			LOG.error("Could not write to output image",e);
		}
	}
	
	//method for writing compressed images
	private void writeCompressedImage (File dest) {
		LOG.debug("Trying to write compressed image "+dest);
		try {
			Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
			ImageWriter writer = iter.next(); 
			FileImageOutputStream output = new FileImageOutputStream(dest);
			writer.setOutput(output);
			IIOImage image = new IIOImage(workingImage, null, null);
			writer.write(null, image, iwparam);
			writer.dispose();
		} catch (IOException e) {
			LOG.error("Could not write to compressed output image",e);
		}
		
	}
	
}


