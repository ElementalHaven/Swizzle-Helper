import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

public class SwizzleHelper {
	private static class PixelMapping {
		// unnecessary
		//public int rgb;
		public int aOffset;
		public int bOffset;
	}
	// how much percentage must increase by to be printed to console
	private static int percentPrintResolution = 10;
	
	private static int bitCount(int val) {
		int count = 0;
		while((val >>= 1) > 0) ++count;
		return count;
	}
	
	// can barely be called a hash. simply a sum of the pixels,
	// but should give a better idea if all the pixels exist
	private static long calcHash(int[] px) {
		long hash = 0;
		for(int p : px) {
			hash += p & 0xFFFFFFFFl;
		}
		return hash;
	}
	
	public static void main(String[] args) {
		if(args.length < 2) {
			System.out.println("Usage: swizzlehelper <fileA> <fileB>");
			return;
		}
		
		File fileA = new File(args[0]);
		if(!fileA.isFile()) {
			System.err.println("fileA does not exist");
			return;
		}
		File fileB = new File(args[1]);
		if(!fileB.isFile()) {
			System.err.println("fileB does not exist");
			return;
		}
		
		BufferedImage imgA, imgB;
		try {
			imgA = ImageIO.read(fileA);
		} catch(IOException ioe) {
			System.err.println("Failed to read fileA as an image");
			return;
		}
		
		try {
			imgB = ImageIO.read(fileB);
		} catch(IOException ioe) {
			System.err.println("Failed to read fileB as an image");
			return;
		}
		
		if(imgA.getWidth() != imgB.getWidth() || imgA.getHeight() != imgB.getHeight()) {
			System.err.printf("Images are not the same size: %dx%d vs %dx%d\n",
					imgA.getWidth(), imgA.getHeight(), imgB.getWidth(), imgB.getHeight());
			return;
		}
		
		final int width = imgA.getWidth();
		final int height = imgB.getHeight();
		final int totalPixels = width * height;
		final int widthBits = bitCount(width);
		final int heightBits = bitCount(height);
		final int totalBits = widthBits + heightBits;
		if(width != 1 << widthBits) {
			System.err.println("Width is not a power of two");
			return;
		}
		if(height != 1 << heightBits) {
			System.err.println("Height is not a power of two");
			return;
		}

		int[] pxA = imgA.getRGB(0, 0, width, height, null, 0, width);
		int[] pxB = imgB.getRGB(0, 0, width, height, null, 0, width);
		long hashA = calcHash(pxA);
		long hashB = calcHash(pxB);
		if(hashA != hashB) {
			System.err.println("Images don't contain the same pixels");
			return;
		}

		// blank line for console formatting
		System.out.println();
		
		System.out.println("Size of images: " + width + "x" + height);
		System.out.println("Images use " + totalBits + " total bits for indexing");
		
		System.out.println("Scanning for unique pixels...");
		int percent = 0;
		List<PixelMapping> uniquePixels = new ArrayList<>();
		outer:
		for(int idxA = 0; idxA < totalPixels; idxA++) {
			int rgbA = pxA[idxA];
			int matchB = -1;
			
			int newPercent = (idxA + 1) * 100 / totalPixels;
			if(newPercent / percentPrintResolution > percent / percentPrintResolution) {
				percent = newPercent;
				System.out.print(percent + "%\r");
			}
			
			for(int idxB = 0; idxB < totalPixels; idxB++) {
				int rgbB = pxB[idxB];
				
				// pixels don't match
				if(rgbA != rgbB) continue;
				
				// non-unique pixel
				if(matchB != -1) continue outer;
				
				// first match
				matchB = idxB;
			}
			
			// no match found
			if(matchB == -1) {
				System.err.println("Pixel at index " + idxA + " of fileA does not exist in fileB");
				return;
			}
			
			PixelMapping mapping = new PixelMapping();
			// unnecessary
			//mapping.rgb = rgbA;
			mapping.aOffset = idxA;
			mapping.bOffset = matchB;
			uniquePixels.add(mapping);
		}
		
		if(uniquePixels.isEmpty()) {
			System.err.println("Every pixel in the image occurs more than once. " + 
					"Can not establish a mapping");
			return;
		}

		// blank line for console formatting
		System.out.println();
		
		for(int bitA = 0; bitA < totalBits; bitA++) {
			// bitmask tested against in indices of a to find a singular bit in b
			int mask = 1 << bitA;
			int result = -1;
			for(PixelMapping px : uniquePixels) {
				if((px.aOffset & mask) != 0) {
					result &= px.bOffset;
				}
			}
			
			if(result == 0) {
				System.out.println("Bit " + bitA + " of fileA has no matching bit in fileB");
				continue;
			}
			
			int bitB = bitCount(result);
			if(result != 1 << bitB) {
				System.out.println("Bit " + bitA + " has multiple possible results: 0b" +
						Integer.toBinaryString(result));
				continue;
			}
			
			System.out.println("Bit " + bitA + " -> Bit " + bitB);
		}
	}
}
