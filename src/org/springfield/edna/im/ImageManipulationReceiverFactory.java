package org.springfield.edna.im;

public class ImageManipulationReceiverFactory {
	public static final int JAVA2D = 1;
	
	public  IImageManipulationReceiver buildManipulationReceiver (int recType){
		switch (recType){
		case JAVA2D:
			return new ImageManipulationReceiverJava2D();
		}
		return null;
	}

}
