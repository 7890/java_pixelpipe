//tb/1604

//=============================================================================
//=============================================================================
public class FPSThread extends Thread
{
	private float fps=30;
	private float sleep=(float)1000/30;
	private boolean done=false;
	PixelPipeGUI pixelpipe;

//=============================================================================
	public FPSThread(PixelPipeGUI pp)
	{
		this.pixelpipe=pp;
	}

//=============================================================================
	public void setFPS(float fps)
	{
		this.fps=fps;
		this.sleep=(float)1000/this.fps;
	}

//=============================================================================
	public float getFPS()
	{
		return fps;
	}

//=============================================================================
	public void run()
	{
		while(!done)
		{
			System.err.print(".");
			pixelpipe.next();
			try{Thread.sleep((int)sleep);}catch(Exception e){}
		}//end while true
	}//end run()
}//end class FPSThread
//EOF
