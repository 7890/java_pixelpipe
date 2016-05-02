//tb/1604

///gcc -o imgframe_t imgframe_t.c -Wunknown-pragmas

#include <stdio.h>
#include <inttypes.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

static const char IMG_MAGIC[8]={'i','m','g','f','0','0','0','\0'};

//=============================================================================
//-Wunknown-pragmas
#pragma pack(push)
#pragma pack(1)
typedef struct
{
	char magic[8];			//8
	int pixel_data_size_bytes;	//+4 =12
	uint64_t frame_number;		//+8 =20
	int width;			//+4 =24
	int height;			//+4 =28
	int channel_count;		//+4 =32
	int bytes_per_channel;		//+4 =36
	int stream_number;		//+4 =40
	float fps;			//+4 =44
	uint64_t millis_since_epoch;	//+8 =52
}
imgframe_t;
#pragma pack(pop)

//=============================================================================
static void imgframe_set_dim(imgframe_t *img, int w, int h)
{
	img->width=w;
	img->height=h;
}

//=============================================================================
static imgframe_t *imgframe_new(int size, int w, int h, int channel_count, int bytes_per_channel, int stream_number, float fps)
{
	imgframe_t *img;
	img=(imgframe_t*)malloc(sizeof(imgframe_t));
	strncpy(img->magic, IMG_MAGIC, 8);
	img->pixel_data_size_bytes=size;
	img->frame_number=0;
	imgframe_set_dim(img,w,h);
	img->channel_count=channel_count;
	img->bytes_per_channel=bytes_per_channel;
	img->stream_number=stream_number;
	img->fps=fps;

	struct timeval tv;
	gettimeofday(&tv, NULL);
//	unsigned long long 
	img->millis_since_epoch=
		(unsigned long long)(tv.tv_sec) * 1000 +
		(unsigned long long)(tv.tv_usec) / 1000;

	printf("%" PRId64 "\n", img->millis_since_epoch);
	return img;
}

//=============================================================================
int main(int argc, char *args[])
{
	fprintf(stderr,"int size, int w, int h, int channel_count, int bytes_per_channel, int stream_number, float fps\n");
//	imgframe_t *img=imgframe_new(640*480*3, 640, 480, 3, 3, 0, 30);
	imgframe_t *img=imgframe_new(
		atoi(args[0])
		,atoi(args[1])
		,atoi(args[2])
		,atoi(args[3])
		,atoi(args[4])
		,atoi(args[5])
		,atoi(args[6])
	);

	img->frame_number=0;

	fprintf(stderr,"%s %d %" PRId64 " %d %d %d %d %d %f %" PRId64 " \n"
		,img->magic
		,img->pixel_data_size_bytes
		,img->frame_number
		,img->width
		,img->height
		,img->channel_count
		,img->bytes_per_channel
		,img->stream_number
		,img->fps
		,img->millis_since_epoch
	);

	unsigned char const *p = (void const*)img;
	size_t i=0;
	for (;i<sizeof(imgframe_t); i++)
	{
		fprintf(stdout,"%c", p[i]);
	}

	fprintf(stderr,"sizeof(imgframe_t): %zu\n",sizeof(imgframe_t));

	free(img);
}//end main()
