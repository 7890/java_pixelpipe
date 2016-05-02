import java.nio.*;

//tb/1604

//=============================================================================
//=============================================================================
class ImgFrameHeader
{
/*
//see imgframe_t.c

//-Wunknown-pragmas
#pragma pack(push)
#pragma pack(1)
typedef struct
{
        char magic[8];                  //8
        int pixel_data_size_bytes;      //+4 =12
        uint64_t frame_number;          //+8 =20
        int width;                      //+4 =24
        int height;                     //+4 =28
        int channel_count;              //+4 =32
        int bytes_per_channel;          //+4 =36
        int stream_number;              //+4 =40
        float fps;                      //+4 =44
        uint64_t millis_since_epoch;    //+8 =52
}
imgframe_t;
#pragma pack(pop)
*/

	static final int frameheader_size=52; //pragma packed
	byte[] frameheaderbuf=new byte[frameheader_size];

	byte magic[]=new byte[8];
	String magic_="";
	int frame_size=0;
	long frame_number=0;
	int w=0;
	int h=0;
	int channel_count=0;
	int bytes_per_channel=0;
	int stream_number=0;
	float fps=0;
	long millis_since_epoch=0;

//=============================================================================
	public boolean parse()
	{
		try
		{
			ByteBuffer bb=ByteBuffer.wrap(frameheaderbuf);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			bb.position(0);
			bb.get(magic);
			magic_=new String(magic,"UTF-8");
			if(!magic_.equals("imgf000\0"))
			{
				System.err.println("LOST SYNC  magic was: "+magic);
				///find next frame with magic ...

				try{Thread.sleep(1);}catch(Exception e){}
				///System.exit(1);
				return false;
			}

			frame_size=bb.getInt();
			frame_number=bb.getLong();
			w=bb.getInt();
			h=bb.getInt();
			channel_count=bb.getInt();
			bytes_per_channel=bb.getInt();
			stream_number=bb.getInt();
			fps=bb.getFloat();
			millis_since_epoch=bb.getLong();

			return true;
		}
		catch(Exception e){e.printStackTrace();}
		return false;
	}//end parse()
}//end class ImgFrameHeader
//EOF
