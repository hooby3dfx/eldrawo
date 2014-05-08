package com.hooby3d.eldrawoandroid;


import java.util.ArrayList;
import java.util.HashSet;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;

public class DrawUtil {
	
	public static void drawRectangle(Canvas c, Paint p, float downX, float downY, float dragX, float dragY){
		//left top right bottom
		//NOT x y w h
		float l = dragX-downX>0?downX:dragX;
		float t = dragY-downY>0?downY:dragY;
		
		c.drawRect(
				l, 
				t, 
				(dragX-downX>0?dragX-downX:downX-dragX) + l, 
				(dragY-downY>0?dragY-downY:downY-dragY) + t, 
						p);

	}
	
	public static void drawEllipse(Canvas c, Paint p, float downX, float downY, float dragX, float dragY){
		//left top right bottom
		float l = dragX-downX>0?downX:dragX;
		float t = dragY-downY>0?downY:dragY;
		RectF r = new RectF(l, t, (dragX-downX>0?dragX-downX:downX-dragX) + l, (dragY-downY>0?dragY-downY:downY-dragY) + t);
		
		c.drawOval(r, p);
	}
	
	public static void drawLine(Canvas c, Paint p, float downX, float downY, float dragX, float dragY){
		c.drawLine(downX, downY, dragX, dragY, p);
	}

	static int rr = 255;
	static int rg, rb = 0;
	static final int RSPD = 5; //5 or 15

	
	public static int nextBrightColor(){
		if(rr==255){
			
			if(rb>=RSPD)
				rb-=RSPD;
			else
				rg+=RSPD;
			if(rg==255){
				rr-=RSPD;
			}
		}else if(rg==255){
			
			if(rr>=RSPD)
				rr-=RSPD;
			else
				rb+=RSPD;
			if(rb==255){
				rg-=RSPD;
			}
		}else if(rb==255){
			
			if(rg>=RSPD)
				rg-=RSPD;
			else
				rr+=RSPD;
			if(rr==255){
				rb-=RSPD;
			}
		}
		return Color.rgb(rr, rg, rb);
	}

	public static void drawFour(Canvas c, Paint p, float downX, float downY, float dragX, float dragY){
		c.drawLine(downX, downY, dragX, dragY, p);
		//vert
		c.scale(1, -1);
		c.translate(0, c.getHeight()*-1);		
		c.drawLine(downX, downY, dragX, dragY, p);
		//horiz
		c.scale(-1, 1);
		c.translate(c.getWidth()*-1, 0);		
		c.drawLine(downX, downY, dragX, dragY, p);
		//vert
		c.scale(1, -1);
		c.translate(0, c.getHeight()*-1);		
		c.drawLine(downX, downY, dragX, dragY, p);	
		
		//horiz
		c.scale(-1, 1);
		c.translate(c.getWidth()*-1, 0);	
	}

	
	//TODO increase speed
	private static int[] right, left;
	
	public static void scroll(Bitmap current) {
		
		int SCROLL_SPEED = current.getWidth()/8;
		
		//copy rightmost area to swap
		if(right==null){
			right = new int[current.getWidth()*current.getHeight()];
		}
		current.getPixels(right, 0, current.getWidth(), current.getWidth()-SCROLL_SPEED-1, 0, SCROLL_SPEED, current.getHeight());
		
		//shift left to right
		if(left==null){
			left = new int[current.getWidth()*current.getHeight()];
		}
		current.getPixels(left, 0, current.getWidth(), 0, 0, current.getWidth()-SCROLL_SPEED, current.getHeight());
		current.setPixels(left, 0, current.getWidth(), SCROLL_SPEED, 0, current.getWidth()-SCROLL_SPEED, current.getHeight());
		
		//put right back to left
		current.setPixels(right, 0, current.getWidth(), 0, 0, SCROLL_SPEED, current.getHeight());
		
	}

	//TODO gotta speed this up
	public static void lameFill(Bitmap img, Paint paint, float downX, float downY) {
		final int WIDTH = img.getWidth();
		final int HEIGHT = img.getHeight();
		final int TARGET_IX = (int)downY * WIDTH + (int)downX;	
		int[] pixels = new int[WIDTH*HEIGHT];
		img.getPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT);
		final int BASE_COLOR = pixels[TARGET_IX];
		boolean noMore = false;
		
		final int D_COLOR = paint.getColor();
		
		if(D_COLOR==BASE_COLOR)
			return;
		
		ArrayList<Integer> lastPoints = new ArrayList<Integer>();	
		lastPoints.add(TARGET_IX);
		
		ArrayList<Integer> nextDots;

		while(!noMore){
			
			if(noMore)
				break;
			noMore = true;
			nextDots = new ArrayList<Integer>();

			for(int j : lastPoints){
				//FIXME too aggressive (seeps through diagonal pix)
				for(int p : getAdjacents(j, pixels, img.getWidth())){
					if(p>-1 && p<pixels.length && pixels[p]==BASE_COLOR){
						pixels[p] = D_COLOR;
						nextDots.add(p);
						noMore = false;
					}
				}
			}
			lastPoints = nextDots;
			
		}	
		
		img.setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT);
		
		
	}
	
	private static HashSet<Integer> used = new HashSet<Integer>();
	private static Integer randomColor(){
		Integer out;
		do{
			out = Color.rgb((int)(255*Math.random()),(int)(255*Math.random()),(int)(255*Math.random()));		 
		} while(!used.add(out));
		
		return out;
	}
	
	public static void sillyFill(Bitmap img, float downX, float downY){
		
		
		final int WIDTH = img.getWidth();
		final int HEIGHT = img.getHeight();
		final int TARGET_IX = (int)downY * WIDTH + (int)downX;
		
			
		int[] pixels = new int[WIDTH*HEIGHT];
		img.getPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT);
		
		final int BASE_COLOR = pixels[TARGET_IX];
		used.add(BASE_COLOR);

		boolean noMore = false;
		
		ArrayList<Integer> lastPoints = new ArrayList<Integer>();	
		lastPoints.add(TARGET_IX);
		
		ArrayList<Integer> nextDots;
		int nextColor;
		while(!noMore){
			
			if(noMore)
				break;
			noMore = true;
			nextDots = new ArrayList<Integer>();
			nextColor = randomColor();
			for(int j : lastPoints){
				
				for(int p : getAdjacents(j, pixels, img.getWidth())){
					if(p>-1 && p<pixels.length && pixels[p]==BASE_COLOR){
						pixels[p] = nextColor;
						nextDots.add(p);
						noMore = false;
					}
				}
			}
			lastPoints = nextDots;
			
		}	
		
		used.clear();
		img.setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT);
		
	}
	
	private static int[] getAdjacents(int index, int[] all, int WIDTH){
		int tl = index-WIDTH-1;
		int bl = index+WIDTH-1;
		//TODO check to avoid wrapping?
		int[] surrounds = { tl, tl+1, tl+2,
							index-1, index+1,
							bl, bl+1, bl+2 };
		return surrounds;
		
	}

	private static final int SPRAY_WIDTH = 30;
	public static void spray(Bitmap img, Paint paint, float x, float y) {
		int dragX = (int)x;
		int dragY = (int)y;
		int[] pix = new int[SPRAY_WIDTH*SPRAY_WIDTH];
		img.getPixels(pix, 0, SPRAY_WIDTH,
				dragX<0?0:dragX > img.getWidth()-SPRAY_WIDTH?img.getWidth()-SPRAY_WIDTH:dragX, 
						dragY<0?0:dragY > img.getHeight()-SPRAY_WIDTH?img.getHeight()-SPRAY_WIDTH:dragY, 
								SPRAY_WIDTH, SPRAY_WIDTH);
		for(int i=0; i<pix.length; i++){
			if(Math.random()<.1){
				pix[i] = paint.getColor();
			}
		}
		img.setPixels(pix, 0, SPRAY_WIDTH, dragX<0?0:dragX > img.getWidth()-SPRAY_WIDTH?img.getWidth()-SPRAY_WIDTH:dragX, 
				dragY<0?0:dragY > img.getHeight()-SPRAY_WIDTH?img.getHeight()-SPRAY_WIDTH:dragY, SPRAY_WIDTH, SPRAY_WIDTH);
		
	}

	public static void drawTree(Canvas c, Paint paint, float x, float y) {
		drawBranch(c, new PointF(x,y), new PointF(x,y-200), 1, paint);
		
	}

	private static void drawBranch(Canvas g, PointF a, PointF b, int d, Paint p) {
		g.drawLine(a.x, a.y, b.x, b.y, p);
		if(d>6)
			return;
		//draw branches and call drawBranches on them
		d+=1;
		float totalR = 0;
		final float lengthMulti = 1.5f;
		
		PointF straight = new PointF((b.x+(b.x-a.x)/lengthMulti), (b.y+(b.y-a.y)/lengthMulti));
		float rotate = (-1*180/6); 
		g.rotate(rotate, b.x, b.y);
		
		totalR+=rotate;
		for(int i=0; i<d; i++){
			drawBranch(g, b, straight, d, p);
			rotate = (180/3); 
			g.rotate(rotate/(d-1), b.x, b.y);
			totalR+=rotate/(d-1);
		}
		g.rotate(totalR*-1f, b.x, b.y);
		
	}

	private static int getInverseColor(int c){
		
		return Color.rgb(255-Color.red(c), 255-Color.green(c), 255-Color.blue(c));
	}
	
	
	public static void inverse(Bitmap src) {
		
		int[] pixels = new int[src.getWidth() * src.getHeight()];
		src.getPixels(pixels, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());
		for(int i=0; i<pixels.length; i++){
			pixels[i] = getInverseColor(pixels[i]);
		}
		
		src.setPixels(pixels, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());
		
		
	}
	
	public static void grayscale(Bitmap src) {
		
		int[] pixels = new int[src.getWidth() * src.getHeight()];
		src.getPixels(pixels, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());
		int r,g,b;
		int gray;
		for(int i=0; i<pixels.length; i++){
			int c = pixels[i];
			r = Color.red(c);
			g = Color.green(c);
			b = Color.blue(c);
			gray = (int)(r*.3+g*.59+b*.11);
			pixels[i] = Color.rgb(gray,gray,gray);
		}
		
		src.setPixels(pixels, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());
		
		
	}
	
	public static void horizontalFlip(Bitmap src, Canvas c){
		Matrix flipHorizontalMatrix = new Matrix();
		flipHorizontalMatrix.setScale(-1,1);
		flipHorizontalMatrix.postTranslate(src.getWidth(),0);
		c.drawBitmap(src.copy(Config.ARGB_8888, false), flipHorizontalMatrix, null);

	}
	
	public static void verticalFlip(Bitmap src, Canvas c){
		Matrix flipHorizontalMatrix = new Matrix();
		flipHorizontalMatrix.setScale(1,-1);
		flipHorizontalMatrix.postTranslate(0,src.getHeight());
		c.drawBitmap(src.copy(Config.ARGB_8888, false), flipHorizontalMatrix, null);

	}
	
	private static final int PIXEL_SIZE = 4;
	//TODO take care of edge pixels
	public static void pixelate(Bitmap src) {
		
		int width = src.getWidth();
		int height = src.getHeight();

		int[] pixels = new int[width * height];
		src.getPixels(pixels, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());

		int x,y;
		int r = 0, g = 0, b = 0;
		int[] pxx = new int[PIXEL_SIZE*PIXEL_SIZE];

		for(x = 0; x <= width-PIXEL_SIZE; x+=PIXEL_SIZE) {
			
			for(y=0; y <= height-PIXEL_SIZE; y+=PIXEL_SIZE ){
				int rgb;
				for(int i=0; i<PIXEL_SIZE; i++){
					for(int j=0; j<PIXEL_SIZE; j++){
						pxx[i*PIXEL_SIZE+j] = (y+i) * width + (x+j);
						rgb = pixels[pxx[i*PIXEL_SIZE+j]];
						//a += rgb.getAlpha();
						r += Color.red(rgb);
						g += Color.green(rgb);
						b += Color.blue(rgb);
					}
					
				}
				
				//a = a/(PIXEL_SIZE*PIXEL_SIZE);
				r = r/(PIXEL_SIZE*PIXEL_SIZE);
				g = g/(PIXEL_SIZE*PIXEL_SIZE);
				b = b/(PIXEL_SIZE*PIXEL_SIZE);
				rgb = Color.rgb(r, g, b);
				
				for(int i=0; i<PIXEL_SIZE*PIXEL_SIZE; i++){
					pixels[pxx[i]] = rgb;
				}
				r = g = b = 0;

			}
			
		}

		src.setPixels(pixels, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());
		
		

	}
	
	//try 2x2?
	private final static float[][] Bayers = {{15/16.f,  7/16.f,  13/16.f,   5/16.f},
								  {3/16.f,  11/16.f,   1/16.f,   9/16.f},
								  {12/16.f,  4/16.f,  14/16.f,   6/16.f},
								  { 0f,      8/16.f,    2/16.f,  10/16.f} };

	private final static int ddd = 128; 
	public static void orderedDither(Bitmap src) {

		int width = src.getWidth();
		int height = src.getHeight();

		// a buffer that stores the destination image pixels
		int[] pixels = new int[width * height];
	
		// get the pixels of the source image	
		src.getPixels(pixels, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());

		int x,y,pixnum;
		int a, r, g, b;
		int e;
		for(x = 0; x < width; x ++) {
			for(y=0; y<height; y++ ){
				pixnum = y*width + x;
				int rgb = pixels[pixnum];
				if(pixels[pixnum]==-1)
					continue;
				a = Color.alpha(rgb);
				r = Color.red(rgb);
				g = Color.green(rgb);
				b = Color.blue(rgb);

				e = (int)(ddd*Bayers[x%4][y%4]);
				r+=e;
				g+=e;
				b+=e;
				r = r - r%ddd;
				g = g - g%ddd;
				b = b - b%ddd;
				
//				if(r >= e){
//					r=255;
//					g=255;
//					b=255;
//				}
//				else {
//					r=0;
//					g=0;
//					b=0;
//				}


				pixels[pixnum] = Color.rgb(r>255?255:r, g>255?255:g, b>255?255:b);

			}
			
		}

		src.setPixels(pixels, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());
	
	}

}
