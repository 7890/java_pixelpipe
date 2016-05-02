import java.awt.*;
import java.awt.image.*;
import java.awt.color.*;
import javax.imageio.*;

//tb/1604

//=============================================================================
//=============================================================================
public class ImgConv
{
//http://stackoverflow.com/questions/12154090/creating-8-bit-image-from-byte-array
//=============================================================================
	public static BufferedImage _8itGrayToBuffered(byte[] buffer, int width, int height)
	{
		try
		{
			//https://docs.oracle.com/javase/7/docs/api/java/awt/color/ColorSpace.html
			ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
			int[] nBits = { 8 };
			//https://docs.oracle.com/javase/7/docs/api/java/awt/image/ComponentColorModel.html
			ColorModel cm = new ComponentColorModel(cs, null, false, true,
				Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

			SampleModel sm = cm.createCompatibleSampleModel(width, height);
			DataBufferByte db = new DataBufferByte(buffer, width * height);
			WritableRaster raster = Raster.createWritableRaster(sm, db, null);
			BufferedImage result = new BufferedImage(cm, raster, false, null);
			return result;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

//=============================================================================
	public static BufferedImage _16bitGrayToBuffered(byte[] pixels, int width, int height)
	{
		try
		{
			short[] pixels_=new short[pixels.length/2];
			///http://stackoverflow.com/questions/736815/2-bytes-to-short-java
			for(int i=0;i<pixels.length;i+=2)
			{
				short val=(short)( ((pixels[i]&0xFF)<<8) | (pixels[i+1]&0xFF) );
				pixels_[i/2]=val;///hmm
			}

			BufferedImage image 
				= new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
			image.getRaster().setDataElements(0,0,width,height,pixels_);
			return image;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

//=============================================================================
	public static BufferedImage _8bitRGBToBuffered(byte[] pixels, int width, int height)
	{
		try
		{
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			image.getRaster().setDataElements(0,0,width,height,pixels);
			return image;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
}//end class ImgConf
//EOF
