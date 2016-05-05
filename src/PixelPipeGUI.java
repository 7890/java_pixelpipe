import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.color.*;
import java.awt.event.*;
import javax.imageio.*;
import javax.swing.*;

//tb/1604

//GUI showing frames received via RBPixelPipe
//javac *.java && java PixelPipeGUI /dev/shm/`ls -1tr /dev/shm/ |tail -1`

//=============================================================================
//=============================================================================
public class PixelPipeGUI
{
	//this file is loaded if found in current directory
	private String propertiesFileUri="PixelPipeGUI.properties";

	//===configurable parameters (here: default values)
	public String 	window_title 			="PixelPipe";
	public boolean 	start_iconified 		=false;
	public boolean 	always_on_top 			=false;
	public boolean 	hide_window_decoration 		=false;
	public int 	initial_width 			=640;
	public int 	initial_height 			=480;
	public int 	initial_placement_x 		=0;
	public int 	initial_placement_y 		=0;
	public int 	fontsize_overlay 		=42;
	public int 	fontsize_values 		=20;
	public String 	snapshot_save_dir		="./";
	public int 	cols 				=3;
	public int 	rows 				=2;
	//===end configurable parameters

	private RBPixelPipe pp;
	private ImgFrameHeader img_header;

	private JFrame main_frame;

	private ImagePanel ip[];
	private BufferedImage bi;

	private BufferFillPanel panel_buffer_fill;

	private JPanel panel_glass;
	private JPanel panel_info_outer;
	private JPanel panel_info;

	private JTextField tf_frame_size;
	private JTextField tf_frame_number;
	private JTextField tf_resolution;
	private JTextField tf_channel_count;
	private JTextField tf_bytes_per_channel;
	private JTextField tf_stream_number;
	private JTextField tf_fps;
	private JTextField tf_millis_since_epoch;

	private JLabel l_paused;
	private boolean is_paused=false;

	private int ip_index=0;

//=============================================================================
	public static void main(String[] args) throws Exception
	{
		if(args.length<1)
		{
			e("syntax: <ringbuffer shared memory file>");
			e("i.e. /dev/shm/e91880b6-0b01-11e6-a139-74d435e313ae");
			System.exit(1);
		}
		PixelPipeGUI t=new PixelPipeGUI(args);
	}

//=============================================================================
	public PixelPipeGUI(String[] args) throws Exception
	{
		if(!loadProps(propertiesFileUri))
		{
			e("could not load properties");
		}

		DTime.setTimeZoneUTC();
		setupGUI();

		img_header=new ImgFrameHeader();
		try
		{
			e("attaching ringbuffer "+args[0]);
			pp=new RBPixelPipe(args[0]);
			e("starting pixelpipe read thread");
			pp.start();
		}
		catch(Exception e)
		{
			e("could not attach ringbuffer.");
			System.exit(1);
		}

		while(1==1)
		{
			if(pp.available())
			{
				img_header=pp.getFrameHeader();
				bi=pp.getFrame();
				ip[ip_index].setImage(bi);
				ip[ip_index].repaint();
				ip_index++;
				ip_index%=cols*rows;
				pp.next();
			}
			if(panel_glass.isVisible() || is_paused)
			{
				updateInfoPanel();
				panel_buffer_fill.setValue(pp.getBufferFillLevel());
				panel_buffer_fill.repaint();
				if(is_paused)
				{
					try{Thread.sleep(10);}catch(Exception e){}
				}
			}
			try{Thread.sleep(1);}catch(Exception e){}
		}
	}//end constructor PixelPipeGUI

//========================================================================
	public boolean loadProps(String configfile_uri)
	{
		propertiesFileUri=configfile_uri;
		return LProps.load(propertiesFileUri,this);
	}

//=============================================================================
	public void updateInfoPanel()
	{
		if(img_header==null){return;}
		tf_frame_size.setText(""+img_header.frame_size);
		tf_frame_number.setText(""+img_header.frame_number);
		tf_resolution.setText(img_header.w+" x "+img_header.h);
		tf_channel_count.setText(""+img_header.channel_count);
		tf_bytes_per_channel.setText(""+img_header.bytes_per_channel);
		tf_stream_number.setText(""+img_header.stream_number);
		tf_stream_number.setText(String.format("%.3f",img_header.fps));
		tf_millis_since_epoch.setText(""+DTime.dateTimeFromMillis(img_header.millis_since_epoch)+" UTC");
		panel_info.validate();
	}

//=============================================================================
	JTextField getValueTextField(String text)
	{
		String font_name=new JLabel().getFont().getName();
		JTextField tf=new JTextField(text);
		tf.setFont(new Font(font_name, Font.PLAIN, fontsize_values));
		tf.setOpaque(true);
		tf.setBackground(new Color(0,0,0,255));
		tf.setForeground(new Color(255,255,255,255));
		tf.setEditable(false);
		return tf;
	}

//=============================================================================
	private void setupGUI()
	{
		main_frame=new JFrame();
		main_frame.setLayout(new GridLayout(rows,cols));
		ip=new ImagePanel[cols*rows];
		for(int i=0;i<cols*rows;i++)
		{
			ip[i]=new ImagePanel();
			ip[i].setInitialDimension(new Dimension((int)(initial_width/cols),(int)(initial_height/rows)));
			main_frame.add(ip[i]);
		}
		main_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		main_frame.setSize(initial_width,initial_height);
		main_frame.setTitle(window_title);
		if(hide_window_decoration)
		{
			main_frame.setUndecorated(true);
			main_frame.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
		}
		panel_glass=(JPanel)main_frame.getGlassPane();
		panel_glass.setLayout(new BorderLayout());

		panel_info_outer = new JPanel();;
		panel_info_outer.setLayout(new BorderLayout());
		panel_info_outer.setOpaque(false);

		panel_info = new JPanel();;
		panel_info.setLayout(new WrapLayout(WrapLayout.CENTER));
		panel_info.setOpaque(false);

		tf_frame_size=getValueTextField("");
		tf_frame_number=getValueTextField("");
		tf_resolution=getValueTextField("");
		tf_channel_count=getValueTextField("");
		tf_bytes_per_channel=getValueTextField("");
		tf_stream_number=getValueTextField("");
		tf_millis_since_epoch=getValueTextField("");

		panel_info.add(tf_frame_size);
		panel_info.add(tf_frame_number);
		panel_info.add(tf_resolution);
		panel_info.add(tf_channel_count);
		panel_info.add(tf_bytes_per_channel);
		panel_info.add(tf_stream_number);
		panel_info.add(tf_millis_since_epoch);

		panel_info_outer.add(panel_info,BorderLayout.CENTER);
		panel_glass.add(panel_info_outer,BorderLayout.SOUTH);

		l_paused=new JLabel("PAUSED",SwingConstants.CENTER);
		l_paused.setVisible(false);
		l_paused.setOpaque(true);
		l_paused.setBackground(new Color(0,0,255,100));
		l_paused.setForeground(new Color(255,255,255,255));
		String font_name=new JLabel().getFont().getName();
		l_paused.setFont(new Font(font_name, Font.BOLD, fontsize_overlay));

		JPanel panel_glass_north=new JPanel();
		panel_glass_north.setOpaque(false);
		panel_glass_north.setLayout(new GridLayout(2,1)); //rows, cols

		panel_buffer_fill=new BufferFillPanel();

		panel_glass_north.add(panel_buffer_fill);
		panel_glass_north.add(l_paused);

		panel_glass.add(panel_glass_north,BorderLayout.NORTH);

		main_frame.setFocusable(true);

		addKeyListeners();

		main_frame.pack();
		main_frame.setLocation(initial_placement_x,initial_placement_y);
		if(always_on_top)
		{
			main_frame.setAlwaysOnTop(true);
		}
		if(start_iconified)
		{
			main_frame.setState(JFrame.ICONIFIED);
		}
		main_frame.show();
	}//end setupGUI()

//=============================================================================
	void addKeyListeners()
	{
		main_frame.addKeyListener(new java.awt.event.KeyAdapter()
		{
			@Override
			public void keyTyped(KeyEvent e) {
//				System.out.println("typed");
			}
			@Override
			public void keyPressed(KeyEvent e) {
//				System.out.println("pressed");
				if(e.getKeyChar()=='i')
				{
					System.out.println("toggle info");
					panel_glass.setVisible(!panel_glass.isVisible());
				}
				else if(e.getKeyChar()=='p')
				{
					System.out.println("toggle pause");
					is_paused=!is_paused;
					l_paused.setVisible(is_paused);
					pp.setPaused(is_paused);
				}
				else if(e.getKeyChar()=='.')
				{
					System.out.println("next frame");
					if(is_paused)
					{
						pp.next();
					}
				}
				else if(e.getKeyChar()=='d')
				{
					System.out.println("drop frames");
					pp.dropAll();
				}
				else if(e.getKeyChar()=='n')
				{
					System.out.println("normal size");
					if(img_header!=null)
					{
						main_frame.pack();
					}
				}
				else if(e.getKeyChar()=='+')
				{
					System.out.println("larger");
					Dimension dim=main_frame.getSize();
					dim.width*=1.25;
					dim.height*=1.25;
					main_frame.setSize(dim);
				}
				else if(e.getKeyChar()=='-')
				{
					System.out.println("smaller");
					Dimension dim=main_frame.getSize();
					dim.width*=0.75;
					dim.height*=0.75;
					main_frame.setSize(dim);
				}
				else if(e.getKeyChar()=='f')
				{
					System.out.println("toggle fullscreen (NOT IMPLEMENTED)");
				}
				else if(e.getKeyChar()=='s')
				{
					System.out.println("snapshot");
					String filename=snapshot_save_dir+"/snapshot_"+img_header.frame_number+".jpg";
					try{
						ImageIO.write(bi,"jpg",new File(filename));
						e("wrote "+filename);
					}catch(Exception ex){ex.printStackTrace();}
				}
				else if(e.getKeyChar()=='w')
				{
					System.out.println("toggle window decoration");
					main_frame.setVisible(false);
					main_frame.dispose();

					if(main_frame.isUndecorated())
					{
						main_frame.setUndecorated(false);
					}
					else
					{
						main_frame.setUndecorated(true);
					}
					Runnable showAgain = new Runnable()
					{
						public void run()
						{
							main_frame.pack();
							main_frame.validate();
							main_frame.setVisible(true);
						}
					};
					SwingUtilities.invokeLater(showAgain);
				}
				else if(e.getKeyCode()==KeyEvent.VK_ESCAPE)
				{
					System.out.println("escape");
					///help_is_on=false;
					panel_glass.setVisible(false);

				}
				else if(e.getKeyChar()=='q')
				{
					System.out.println("quit");
					///stuff here
					System.exit(0);
				}
			}//end keyPressed()
			@Override
			public void keyReleased(KeyEvent e) {
//				System.out.println("released");
			}
		});
	}//end addKeyListeners

//=============================================================================
	public static void p(String s)
	{
		System.out.println(s);
	}

//=============================================================================
	public static void e(String s)
	{
		System.err.println(s);
	}
}//end class PixelPipeGUI
//EOF
