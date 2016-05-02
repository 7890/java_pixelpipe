import java.awt.*;
import javax.swing.*;

//tb/1604

//=============================================================================
//=============================================================================
public class BufferFillPanel extends JPanel
{
	private float fill_level=0;

//=============================================================================
	public BufferFillPanel()
	{
		this.setOpaque(false);
	}

//=============================================================================
	public void setValue(float level)
	{
		this.fill_level=level;
	}

//=============================================================================
	@Override
	public Dimension getPreferredSize()
	{
///
/*
		if(bi!=null)
		{
			return new Dimension(bi.getWidth(),50);
		}
		else
		{}
*/
		return new Dimension(640,50);
	} 

//=============================================================================
	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D gd = (Graphics2D) g.create();
		gd.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,.4f)); //opacity
		gd.setColor(Color.GREEN);
		gd.fillRect(0,0,(int)(getWidth()*fill_level),50);
		gd.dispose();
	}
}//end class BufferFillPanel
//EOF
