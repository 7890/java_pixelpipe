/*
http://stackoverflow.com/questions/1605332/java-nio-filechannel-versus-fileoutputstream-performance-usefulness
http://code.hammerpig.com/how-to-read-really-large-files-in-java.html
http://docs.oracle.com/javase/6/docs/api/java/nio/channels/FileChannel.html
http://docs.oracle.com/javase/6/docs/api/java/nio/ByteBuffer.html
http://docs.oracle.com/javase/6/docs/api/java/nio/MappedByteBuffer.html
http://docs.oracle.com/javase/6/docs/api/java/io/RandomAccessFile.html
http://stackoverflow.com/questions/4576388/changing-a-specific-byte-in-a-file
http://docs.oracle.com/javase/6/docs/api/java/nio/FloatBuffer.html
*/

//try to read ringbuffer in shared memory created by rb.h
//see https://github.com/7890/csnip/tree/master/rb
//incomplete raw test, partially reflect rb.h

//tb/130418/150117/1604

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

//=============================================================================
//=============================================================================
public class RB
{
	public static final byte RB_MAGIC[]={'r','i','n','g','b','u','f','\0'};
	public static final String RB_MAGIC_STRING=new String(RB_MAGIC);
	public static final float RB_VERSION=0.23f;
	public static final String bar_string="============================================================";

	//binary layout of C struct ( #pragma pack(1) to keep natural alignment )
	public static final int BOFF_version		=8;//RB_MAGIC.length         //@   8: float(4)
	public static final int BOFF_size		=BOFF_version		+4;  //@  12: long (8)
	public static final int BOFF_read_index		=BOFF_size		+8;  //@  20: long (8)
	public static final int BOFF_write_index	=BOFF_read_index	+8;  //@  28: long (8)
	public static final int BOFF_last_was_write	=BOFF_write_index	+8;  //@  36: int  (4)
	public static final int BOFF_memory_locked	=BOFF_last_was_write	+4;  //@  40: int  (4)
	public static final int BOFF_in_shared_memory	=BOFF_memory_locked	+4;  //@  44: int  (4)

	public static final int BOFF_memory_lockable	=BOFF_in_shared_memory	+4;  //@  48: int  (4)
	public static final int BOFF_memory_shareable	=BOFF_memory_lockable	+4;  //@  52: int  (4)
	public static final int BOFF_mutex_lockable	=BOFF_memory_shareable	+4;  //@  56: int  (4)

	public static final int BOFF_unlink_requested	=BOFF_mutex_lockable	+4;  //@  60: int  (4)
	public static final int BOFF_no_more_input_data	=BOFF_unlink_requested	+4;  //@  64: int  (4)
	public static final int BOFF_sample_rate	=BOFF_no_more_input_data+4;  //@  68: int  (4)
	public static final int BOFF_channel_count	=BOFF_sample_rate	+4;  //@  72: int  (4)
	public static final int BOFF_bytes_per_sample	=BOFF_channel_count	+4;  //@  76: int  (4)

	public static final int BOFF_total_bytes_read	=BOFF_bytes_per_sample	+4;  //@  80: long (8)
	public static final int BOFF_total_bytes_write	=BOFF_total_bytes_read	+8;  //@  88: long (8)
	public static final int BOFF_total_bytes_peek	=BOFF_total_bytes_write	+8;  //@  96: long (8)
	public static final int BOFF_total_underflows	=BOFF_total_bytes_peek	+8;  //@ 104: long (8)
	public static final int BOFF_total_overflows	=BOFF_total_underflows	+8;  //@ 112: long (8)

	public static final int BOFF_shm_handle		=BOFF_total_overflows	+8;  //@ 120: char [256]
	public static final int BOFF_human_name		=BOFF_shm_handle	+256;//@ 376: char [256] (end 632 + 84)

	//84 bytes mutex locks (optional, padded if not available, RB_DISABLE_RW_MUTEX)
	//buffer data starts after the header
	//sizeof(rb_t): 716
	private int header_length=716;

	//direct memory mapped bytebuffer, accessing c struct via RandomAccess file
	private MappedByteBuffer mbb;

	//to convert bytes to string
	private String charset="US-ASCII";

	//temporary byte holders
	private byte[] magic_=new byte[8];
	private byte[] shm_handle_=new byte[256];
	private byte[] human_name_=new byte[256];

//=============================================================================
	public boolean magic() throws Exception
	{
		mbb.rewind();
		mbb.get(magic_);

		if(new String(magic_,charset).equals(RB_MAGIC_STRING))
		{
			return true;
		}
		return false;
	}

//=============================================================================
	public float version() throws Exception
	{
		return mbb.getFloat(BOFF_version);
	}

	public long size(){return mbb.getLong(BOFF_size);}
	//size can't be set

	public long read_index(){return mbb.getLong(BOFF_read_index);}
	private void read_index(long newindex){mbb.putLong(BOFF_read_index,newindex);}

	public long write_index(){return mbb.getLong(BOFF_write_index);}
	private void write_index(long newindex){mbb.putLong(BOFF_write_index,newindex);}

	public int last_was_write(){return mbb.getInt(BOFF_last_was_write);}
	private void last_was_write(int was_write){mbb.putInt(BOFF_last_was_write,was_write);}

	public int is_memory_locked(){return mbb.getInt(BOFF_memory_locked);}
	public int is_in_shared_memory(){return mbb.getInt(BOFF_in_shared_memory);}

	public int is_memory_lockable(){return mbb.getInt(BOFF_memory_lockable);}
	public int is_memory_shareable(){return mbb.getInt(BOFF_memory_shareable);}
	public int is_mutex_lockable(){return mbb.getInt(BOFF_mutex_lockable);}

	public int is_unlink_requested(){return mbb.getInt(BOFF_unlink_requested);}
	public int no_more_input_data(){return mbb.getInt(BOFF_no_more_input_data);}
	public int sample_rate(){return mbb.getInt(BOFF_sample_rate);}
	public int channel_count(){return mbb.getInt(BOFF_channel_count);}
	public int bytes_per_sample(){return mbb.getInt(BOFF_bytes_per_sample);}

	public String shm_handle() throws Exception
	{
		mbb.position(BOFF_shm_handle);
		mbb.get(shm_handle_);
		return new String(shm_handle_,charset);
	}

	public String human_name() throws Exception
	{
		mbb.position(BOFF_human_name);
		mbb.get(human_name_);
		return new String(human_name_,charset);
	}

	public long total_bytes_read(){return mbb.getLong(BOFF_total_bytes_read);}
	private void total_bytes_read(long total){mbb.putLong(BOFF_total_bytes_read,total);}

	public long total_bytes_write(){return mbb.getLong(BOFF_total_bytes_write);}
	private void total_bytes_write(long total){mbb.putLong(BOFF_total_bytes_write,total);}

	public long total_bytes_peek(){return mbb.getLong(BOFF_total_bytes_peek);}
	private void total_bytes_peek(long total){mbb.putLong(BOFF_total_bytes_peek,total);}

	public long total_underflows(){return mbb.getLong(BOFF_total_underflows);}
	private void total_underflows(long total){mbb.putLong(BOFF_total_underflows,total);}

	public long total_overflows(){return mbb.getLong(BOFF_total_overflows);}
	private void total_overflows(long total){mbb.putLong(BOFF_total_overflows,total);}

//=============================================================================
	public RB(){}

//=============================================================================
	public long open_shared(String shm_handle) throws Exception
	{
		///more error checking needed
		File f=new File(shm_handle);
		if(!f.exists() || !f.canWrite())
		{
			throw new Exception("ringbuffer "+shm_handle+" not found or not writable.");
		}
		/*
		"rws" Open for reading and writing, as with "rw", and also require that every 
		update to the file's content or metadata be written synchronously to the 
		underlying storage device. 
		*/
		RandomAccessFile in = new RandomAccessFile(f, "rws");
		FileChannel fc = in.getChannel();

		p("filechannel size bytes: "+fc.size()+" file size bytes "+f.length());

		//map whole file
		mbb=fc.map(FileChannel.MapMode.READ_WRITE,0,f.length());
		mbb.order(ByteOrder.LITTLE_ENDIAN);
		return fc.size();
	}

//=============================================================================
	public long can_read()
	{
		long r=read_index();
		long w=write_index();
		if(r==w)
		{
			if(last_was_write()==1) {return size();}
			else {return 0;}
		}
		else if(r<w) {return w-r;}
		else {return w+size()-r;} //r>w
	}

//=============================================================================
	public long can_read_frames()
	{
		return (long)Math.floor((double)can_read()/channel_count()/bytes_per_sample());
	}

//=============================================================================
	public long can_write()
	{
		long r=read_index();
		long w=write_index();
		if(r==w)
		{
			if(last_was_write()==1) {return 0;}
			else {return size();}
		}
		else if(r<w) {return size()-w+r;}
		else {return r-w;} //r>w
	}

//=============================================================================
	public long can_write_frames()
	{
		return (long)Math.floor((double)can_write()/channel_count()/bytes_per_sample());
	}

//=============================================================================
	public long generic_read(byte[] destination, long count, int over)
	{
		if(count==0) {return 0;}

		long can_read_count;
		long do_read_count;

		if(over==1)
		{
			can_read_count=can_read();
			//limit to whole buffer
			do_read_count=Math.min(size(),count);
		}
		else
		{
			can_read_count=can_read();
			if(can_read_count<1) {return 0;}
			do_read_count=count > can_read_count ? can_read_count : count;
		}

		long r=read_index();
		long linear_end=r+do_read_count;
		long copy_count_1;
		long copy_count_2;

		if(linear_end>size())
		{
			copy_count_1=size()-r;
			copy_count_2=linear_end-size();
		}
		else
		{
			copy_count_1=do_read_count;
			copy_count_2=0;
		}

		//memcpy(destination, &( ((char*)buf_ptr(rb)) [rb->read_index] ), copy_count_1);
		//position at current read_index
		mbb.position((int)(header_length+r));
		//get(byte[] dst, int offset, int length)
		mbb.get(destination, 0, (int)copy_count_1);

		if(copy_count_2<1)
		{
			read_index( (r+copy_count_1) % size() );
		}
		else
		{
			//memcpy(destination+copy_count_1, &( ((char*)buf_ptr(rb)) [0]), copy_count_2);
			//position to buffer data 0 (right after header)
			mbb.position(header_length);
			mbb.get(destination, (int)copy_count_1, (int)copy_count_2);
			read_index( copy_count_2 % size() );
		}

		//if write index was overpassed, move up to read index
		if(over==1 && can_read_count<do_read_count)
		{
			write_index( read_index() );
		}

		last_was_write(0);

		total_bytes_read(total_bytes_read()+do_read_count);

		if(do_read_count<count){total_underflows(total_underflows()+1);}

		return do_read_count;
	}//end generic_read

//=============================================================================
	public long peek(byte[] destination, long count)
	{
		return peek_at(destination,count,0);
	}

//=============================================================================
	public long peek_at(byte[] destination, long count, long offset)
	{
		if(count==0) {return 0;}

		long can_read_count=can_read();

		//can not read more than offset, no chance to read from there
		if(can_read_count<=offset)
		{
			total_underflows(total_underflows()+1);
			return 0;
		}
		//limit read count respecting offset
		long do_read_count=count>can_read_count-offset ? can_read_count-offset : count;
		//adding the offset, knowing it could be beyond buffer end
		long tmp_read_index=read_index()+offset;
		//including all: current read index + offset + limited read count
		long linear_end=tmp_read_index+do_read_count;
		long copy_count_1;
		long copy_count_2;

		//beyond
		if(linear_end>size())
		{
			//still beyond
			if(tmp_read_index>=size())
			{
				//all in rolled over
				tmp_read_index%=size();
				copy_count_1=do_read_count;
				copy_count_2=0;
			}
			//segmented
			else
			{
				copy_count_1=size()-tmp_read_index;
				copy_count_2=linear_end-size()-offset;
			}
		}
		else
		//if not beyond the buffer end
		{
			copy_count_1=do_read_count;
			copy_count_2=0;
		}
		mbb.position((int)(header_length+tmp_read_index));
		mbb.get(destination, 0, (int)copy_count_1);

		if(copy_count_2>0)
		{
			mbb.position(header_length);
			mbb.get(destination, (int)copy_count_1, (int)copy_count_2);
		}

		total_bytes_peek(total_bytes_peek()+do_read_count);

		if(do_read_count<count){total_underflows(total_underflows()+1);}

		return do_read_count;
	}//end peek_at

//=============================================================================
	public long generic_advance_read_index(long count, int over)
	{
		if(count==0) {return 0;}
		long can_read_count;
		long do_advance_count;

		if(over==1)
		{
			can_read_count=can_read();
			//limit to whole buffer
			do_advance_count=Math.min(size(),count);
		}
		else
		{
			can_read_count=can_read();
			if(can_read_count<1)
			{
				total_underflows(total_underflows()+1);
				return 0;
			}
			do_advance_count=count > can_read_count ? can_read_count : count;
		}

		long r=read_index();
		long linear_end=r+do_advance_count;
		long tmp_read_index=linear_end>size() ? linear_end-size() : r+do_advance_count;

		read_index( tmp_read_index%=size() );

		//if write index was overpassed, move up to read index
		if(over==1 && can_read_count<do_advance_count)
		{
			write_index( read_index() );
		}

		last_was_write(0);

		total_bytes_read(total_bytes_read()+do_advance_count);

		if(do_advance_count<count){total_underflows(total_underflows()+1);}

		return do_advance_count;
	}//generic_advance_read_index()

//=============================================================================
	public void free() throws Exception
	{
		///close things here...
		//mbb..
		//fc.close();
	}

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
}//end class RB
//EOF
