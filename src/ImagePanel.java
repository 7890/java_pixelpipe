import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

//tb/1604

//http://stackoverflow.com/questions/299495/how-to-add-an-image-to-a-jpanel
//=============================================================================
//=============================================================================
public class ImagePanel extends JPanel
{
	private BufferedImage image;
	private Dimension initial_dimension=new Dimension(640,480);

//=============================================================================
	public ImagePanel()
	{
		this.setOpaque(false);
	}

//=============================================================================
	public ImagePanel(BufferedImage image)
	{
		this.setOpaque(false);		
		this.image=image;
	}

//=============================================================================
	public void setImage(BufferedImage image)
	{
		this.image=image;
	}
//=============================================================================
	public void setInitialDimension(Dimension dim)
	{
		this.initial_dimension=dim;
	}

//=============================================================================
	@Override
	public Dimension getPreferredSize()
	{
		if(image!=null)
		{
			return new Dimension(image.getWidth(),image.getHeight());
		}
		else
		{
			return initial_dimension;
		}
	} 

//=============================================================================
	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		if(image!=null)
		{
			Graphics2D gd = (Graphics2D) g.create();
//			gd.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//			RenderingHints.VALUE_ANTIALIAS_ON);
			gd.drawImage(image, 0, 0, getWidth(), getHeight(), this);
/*
///
			if(help_is_on)
			{
				gd.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,.7f)); //opacity
				gd.setColor(Color.RED);
				gd.fillRect(10, 10, getWidth()-20, getHeight()-20);
				gd.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f)); //opacity
				gd.drawImage(help_overlay, 20, 20, getWidth()-40, getHeight()-40, this);
			}
*/
			gd.dispose();
			//g.drawImage(image, 0, 0, null); //see javadoc for more info on the parameters
		}
	}
}//end class ImagePanel
//EOF
