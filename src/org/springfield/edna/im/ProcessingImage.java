package org.springfield.edna.im;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class ProcessingImage {
	
	public BufferedImage workingImage = null;
	public ImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
	public String recompress = null;
	public String amazons3 = null;

	public ProcessingImage(File inputfile) {
		try {
			BufferedImage originalImage = ImageIO.read(inputfile);
			
			//OpenJDK, unless Oracle JDK, has an issue with files containing transparancy bits (gif, png)
			//for this reason we convert all images to 3 byte RGB, ditching the transparancy bit.
			// https://stackoverflow.com/questions/3432388/imageio-not-able-to-write-a-jpeg-file/17845696
			int width = originalImage.getWidth();
			int height = originalImage.getHeight();
			int[] rgb = originalImage.getRGB(0, 0, width, height, null, 0, width);
			
			workingImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			workingImage.setRGB(0, 0, width, height, rgb, 0, width);
		} catch (IOException e) {
			System.out.println("Could not read input image :"+inputfile.getAbsolutePath());
		}
	}
	
	public void writeToFile(String filename) {
		String tmpfilename = filename;
		
		if (recompress!=null) tmpfilename +="_temp"; // we need to recompress
		
		File dest = new File(tmpfilename);
		try {
			Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
			ImageWriter writer = iter.next(); 
			FileImageOutputStream output = new FileImageOutputStream(dest);
			writer.setOutput(output);
			IIOImage image = new IIOImage(workingImage, null, null);
			writer.write(null, image, iwparam);
			writer.dispose();
		} catch (Exception e) {
			System.out.println("Could not write to compressed output image "+filename);
			e.printStackTrace();
		}
		
		// ok so we now have it on filesystem, was recompressed asked ?
		if (recompress!=null) {
			doRecompress(tmpfilename,filename,recompress);
			dest.delete(); // delete the _temp file
		}
		
		// ok so we now have it on filesystem, was recompressed asked ?
		if (amazons3!=null) {
			doAmazonS3(tmpfilename,filename,recompress);
			//dest.delete(); // delete the _temp file
		}
		
	}
	
	private void doAmazonS3(String inputname,String outputname,String options) {
		String bucketname = "springfield-storage";
		String filename = amazons3+outputname.substring(outputname.indexOf("/external/")+9)+".jpg";
		
		AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).build();

		
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType("image/jpeg");
		
		File dest = new File(outputname);
		if (dest != null) {
			try {
			InputStream in = new FileInputStream(dest);
			PutObjectRequest or = new PutObjectRequest(bucketname,filename,in, metadata);
			s3Client.putObject(or);
			} catch(Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("DEST NOT FOUND="+inputname+" "+outputname);
		}
	}
	
	private void doRecompress(String inputname,String outputname,String options) {
		String cmd = "/springfield/edna/bin/jpeg-recompress";
		cmd += " -a";
		cmd += " -m "+options;
		cmd += " "+inputname;
		cmd += " "+outputname;

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
		}
	}	
}
