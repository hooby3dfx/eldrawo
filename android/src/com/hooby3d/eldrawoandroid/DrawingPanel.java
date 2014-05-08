package com.hooby3d.eldrawoandroid;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hooby3d.eldrawoandroid.DrawMplayerIRC.DrawMsg;
import com.hooby3d.eldrawoandroid.DrawMplayerIRC.MplayerClient;
import com.hooby3d.eldrawoandroid.DrawMplayerIRC.RoomListener;
import com.hooby3d.eldrawoandroid.MainActivity.Filter;
import com.hooby3d.eldrawoandroid.MainActivity.Mode;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

public class DrawingPanel extends SurfaceView implements 
			SurfaceHolder.Callback, SharedPreferences.OnSharedPreferenceChangeListener, MplayerClient{ //SensorEventListener
	
	private PointF down = new PointF(-1, -1);
	private PointF drag = new PointF(-1, -1);
	
	private Mode mode = Mode.PENCIL_MODE; //default
	private Filter filter = null;
	
	private boolean capt = false;
	private boolean scroll = false;
	
	private Bitmap current;
	private Canvas currentG;
	
	private Paint paint = new Paint();
	
	private Map<Mode, Integer> soundMap = new HashMap<Mode, Integer>();
	
	private MediaPlayer mp;
	
	private boolean playSounds;
	private boolean showScores;
	private static final String PLAY_SOUNDS_PREF = "soundsOnPref";
	private static final String SHOW_SCORES_PREF = "scoresOnPref";
	private static final String LAST_SCORE_PREF = "scorePref";
	private static final String SAVED_FILE_INDEX_PREF = "savedFilePref";

	
	private int score = 0;
	
	
//	private SensorManager mSensorManager;
//	private Sensor mSensor;

	public DrawingPanel(Context context) {
		super(context);
		Log.d("DP", "constructor");
		
		getHolder().addCallback(this);
		
		//dont need map, each mode should store mp3 res...
		soundMap.put(Mode.RECTANGLE_MODE, R.raw.shapesloop);
		soundMap.put(Mode.ELLIPSE_MODE, R.raw.shapesloop);
		soundMap.put(Mode.PENCIL_MODE, R.raw.pencilmodeloop);
		soundMap.put(Mode.LINE_MODE, R.raw.linemodeloop);
		soundMap.put(Mode.RAINBOW_PEN_MODE, R.raw.rainbowloop);
		soundMap.put(Mode.COOL_LINES_MODE, R.raw.coollinesmode);
		soundMap.put(Mode.DRAW_FOUR, R.raw.drawfour);
		soundMap.put(Mode.SCROLL_MODE, R.raw.scrollloop1);
		soundMap.put(Mode.SPRAY_MODE, R.raw.sprayloop);
		soundMap.put(Mode.ERASER_MODE, R.raw.eraserloop);
		
		
		paint.setStrokeWidth(3);
		paint.setStrokeJoin(Join.ROUND);
		paint.setStrokeCap(Cap.ROUND);
		
		netPaint.setStrokeJoin(Join.ROUND);
		netPaint.setStrokeCap(Cap.ROUND);

		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getContext());
		p.registerOnSharedPreferenceChangeListener(this);
		
		playSounds = p.getBoolean(PLAY_SOUNDS_PREF, true);
		showScores = p.getBoolean(SHOW_SCORES_PREF, false);
		score = p.getInt(LAST_SCORE_PREF, 0);
		//mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction()==MotionEvent.ACTION_DOWN){
			
			down.x = event.getX();
			down.y = event.getY();
			
			//Log.d("DP", "got down!"+down.x+", "+down.y);
			Integer sres;
			if(mp==null && playSounds && (sres = soundMap.get(mode))!=null){
				
				mp = MediaPlayer.create(this.getContext(), sres);
				mp.setLooping(true);
			
			}
			if(mp!=null){
				mp.start();
			}
			if(mode==Mode.SCROLL_MODE){
				toggleScroll();
			}
		}else if(event.getAction()==MotionEvent.ACTION_UP){
			
			drag.x = event.getX(event.getPointerCount()-1);
			drag.y = event.getY(event.getPointerCount()-1);
			capt = true;
			//Log.d("DP", "got up!"+drag.x+", "+drag.y);
			myDraw();
			if(mp!=null){
				mp.pause();
			}
			if(mode==Mode.SCROLL_MODE){
				toggleScroll();
			}
		}else if(event.getAction()==MotionEvent.ACTION_MOVE){
			if(mode==Mode.SCROLL_MODE){
				return true;
			}
			
			drag.x = event.getX(); event.getX(event.getPointerCount()-1);
			drag.y = event.getY(event.getPointerCount()-1);
			//Log.d("DP", "got drag!"+drag.x+", "+drag.y);
			myDraw();
		}
		
		return true;
	}
	
	private void myDraw(){
		SurfaceHolder h = getHolder();
		Canvas c = h.lockCanvas();
		if(c==null)
			return;
		allDrawingLogic(c, 
				swap, currentG, current, 
				paint, swapColor, capt, mode, drag, down, filter
				);
		
		
		h.unlockCanvasAndPost(c);
		
		if(drawmplayer!=null){
			DrawMsg msg = drawmplayer.new DrawMsg();
			msg.capt = capt;
			msg.downx = down.x;
			msg.downy = down.y;
			msg.dragx = drag.x;
			msg.dragy = drag.y;
			msg.filter = filter;
			msg.mode = mode;
			msg.color = paint.getColor();
			msg.size = (int) paint.getStrokeWidth();
			
			drawmplayer.send(msg);
		}
		
		if(	mode == Mode.PENCIL_MODE
				|| mode == Mode.ERASER_MODE
				|| mode == Mode.RAINBOW_PEN_MODE
				|| mode == Mode.DRAW_FOUR
				|| mode == Mode.SCROLL_MODE
				|| mode == Mode.SPRAY_MODE){
			down.x = drag.x;
			down.y = drag.y;
		}
		
		filter = null;
		
		if(capt){
			score();
			down.x=-1;
			down.y=-1;
			capt = false;				
			calls=0;
			
		}
	}
	
	private Paint netPaint = new Paint();
	private void netDraw(DrawMsg msg){
		SurfaceHolder h = getHolder();
		Canvas c = h.lockCanvas();
		if(c==null){
			Log.d("DP", "null canvas");
			return;
		}

		netPaint.setStrokeWidth(msg.size);
		netPaint.setColor(msg.color);
		allDrawingLogic(c, 
				swap, currentG, current, 
				netPaint, swapColor, msg.capt, msg.mode, new PointF(msg.downx, msg.downy), new PointF(msg.dragx, msg.dragy), msg.filter
				);
		Log.d("DP", "netDraw drew: "+msg.seq);
		
		h.unlockCanvasAndPost(c);
	}
	
	private Canvas swap;
	
	private Handler hr = new Handler();
	private Runnable dr = new Runnable() {
		
		@Override
		public void run() {
			myDraw();
		}
	};
	
	private int calls = 0;
	private int swapColor;
	
	@Override
	public void onDraw(Canvas c){
		allDrawingLogic(c, 
				swap, currentG, current, 
				paint, swapColor, capt, mode, drag, down, filter
				);
	}
	
	public void allDrawingLogic(Canvas c, Canvas swap, Canvas currentG, Bitmap current, 
			Paint paint, int swapColor,  
			boolean capt, Mode mode, PointF drag, PointF down, Filter filter
			){
		//Log.d("DP", "drawing!");
		 
		if(capt 
				|| mode == Mode.PENCIL_MODE
				|| mode == Mode.ERASER_MODE
				|| mode == Mode.RAINBOW_PEN_MODE
				|| mode == Mode.COOL_LINES_MODE
				|| mode == Mode.DRAW_FOUR
				|| mode == Mode.SCROLL_MODE
				|| mode == Mode.SPRAY_MODE){
			swap = c;
			c = currentG;
			
		}else{
			c.drawBitmap(current, 0, 0, null);
		}

		if(drag.x!=-1 && down.x!=-1){
			calls++;
			switch(mode){
			case RECTANGLE_MODE:
				DrawUtil.drawRectangle(c, paint, down.x, down.y, drag.x, drag.y);
				break;
			case ELLIPSE_MODE:
				DrawUtil.drawEllipse(c, paint, down.x, down.y, drag.x, drag.y);
				break;
			case RAINBOW_PEN_MODE:
				swapColor = paint.getColor();
				paint.setColor(DrawUtil.nextBrightColor());
				DrawUtil.drawLine(c, paint, down.x, down.y, drag.x, drag.y);
				//cool rainbow line? awesome if w/o next 2 lines..
//				down.x = drag.x;
//				down.y = drag.y;
				paint.setColor(swapColor);
				break;
			case PENCIL_MODE:
				DrawUtil.drawLine(c, paint, down.x, down.y, drag.x, drag.y);
//				down.x = drag.x;
//				down.y = drag.y;
				break;
			case ERASER_MODE:
				swapColor = paint.getColor();
				paint.setColor(Color.WHITE);
				DrawUtil.drawLine(c, paint, down.x, down.y, drag.x, drag.y);
//				down.x = drag.x;
//				down.y = drag.y;
				paint.setColor(swapColor);
				break;
			case COOL_LINES_MODE:
			case LINE_MODE:
				DrawUtil.drawLine(c, paint, down.x, down.y, drag.x, drag.y);
				break;
			case DRAW_FOUR:
				DrawUtil.drawFour(c, paint, down.x, down.y, drag.x, drag.y);
//				down.x = drag.x;
//				down.y = drag.y;
				break;
			case SCROLL_MODE:
				if(scroll){
					DrawUtil.scroll(current);
					//rely on move events?
					//fling?
					//gyroscope?!?!

					hr.postDelayed(dr, 33); //30 fps?
				}
				break;
			case FILL_MODE:
				DrawUtil.lameFill(current, paint, down.x, down.y);
				break;
			case MYSTERY_FILL:
				DrawUtil.sillyFill(current, down.x, down.y);
				break;
			case SPRAY_MODE:
				DrawUtil.spray(current, paint, drag.x, drag.y);
//				down.x = drag.x;
//				down.y = drag.y;
				break;
			case TREE_MODE:
				DrawUtil.drawTree(c, paint, down.x, down.y);
				break;
			}
		}
		
		if(capt
				|| mode == Mode.PENCIL_MODE
				|| mode == Mode.ERASER_MODE
				|| mode == Mode.RAINBOW_PEN_MODE
				|| mode == Mode.COOL_LINES_MODE
				|| mode == Mode.DRAW_FOUR
				|| mode == Mode.SCROLL_MODE
				|| mode == Mode.SPRAY_MODE){
			
			c = swap;
			
			Bitmap filterSwap = current;
			if(filter!=null){
				switch(filter){
				case INVERSE:
					DrawUtil.inverse(filterSwap);
					break;
				case GRAYSCALE:
					DrawUtil.grayscale(filterSwap);
					break;
				case HORIZONTAL_FLIP:
					DrawUtil.horizontalFlip(filterSwap, currentG);
					break;
				case VERTICAL_FLIP:
					DrawUtil.verticalFlip(filterSwap, currentG);
					break;
				case PIXEL:
					DrawUtil.pixelate(filterSwap);
					break;
				case DITHER:
					DrawUtil.orderedDither(filterSwap);
					break;
				}
			}

			

			
			c.drawBitmap(current, 0, 0, null);
			
		}
	}
	
	

	
	private void score(){
		int p = 0;
		switch(mode){
		case TREE_MODE:
			p=100; break;			
		case RECTANGLE_MODE:
		case ELLIPSE_MODE:
			p=4*distance(down, drag); break;			
		case RAINBOW_PEN_MODE:
			p=calls*8; break;
		case PENCIL_MODE:
			p=calls*2; break;
		case COOL_LINES_MODE:
			p=calls*7; break;
		case DRAW_FOUR:
			p=calls*9; break;
		case FILL_MODE:
			p=250; break;
		case MYSTERY_FILL:
			p=1000; break;
		case SCROLL_MODE:
			p=calls*5; break;
		case LINE_MODE:
			p=50; break;
		case SPRAY_MODE:
			p=75; break;
		//eraser - score?
		}
		
		score+=p;
		if(showScores)
			Toast.makeText(getContext(), Integer.toString(score), Toast.LENGTH_SHORT).show();
	}
	
	private static int distance(PointF a, PointF b){
		float q = a.x-b.x;
		float p = a.y-b.y;
		q=q*q;
		p=p*p;
		return (int)FloatMath.sqrt(q+p);
	}
	
	
	public void setMode(Mode m){
//		if(mode==Mode.SCROLL_MODE){
//			mSensorManager.unregisterListener(this);
//		}
		mode = m;
		if(mp!=null){
			mp.stop();
			mp.release();
			mp = null;
		}
//		if(m==Mode.SCROLL_MODE){
//			
//			mSensorManager.registerListener(this,
//	                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//	                SensorManager.SENSOR_DELAY_GAME);
//			
//		}
	}


	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d("DP", "surfaceChanged");
		myDraw();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("DP", "surfaceCreated");

		if(current==null){
			
			current = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.ARGB_8888);
			currentG = new Canvas(current);
			currentG.drawColor(Color.WHITE);
			
			
			//draw splash?
			
			myDraw();
		}
		
		if(simage!=null){
			currentG.drawBitmap(simage, 0, 0, null);
			simage = null;
		}

		
	}
	
	private void setDrawing(Bitmap parcelable) {
		Log.d("DP", "setDrawing");
		current = parcelable;
		currentG = new Canvas(current);
		myDraw();
	}
	
	private Bitmap getDrawing(){
		return current.copy(Bitmap.Config.ARGB_8888, true);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d("DP", "surfaceDestroyed");
	}

	public void setColor(int color) {
		paint.setColor(color);
		
	}
	
	public int getColor(){
		return paint.getColor();
	}
	
	public void toggleFill(){
		if(paint.getStyle()==Style.FILL){
			paint.setStyle(Style.STROKE);
		}else{
			paint.setStyle(Style.FILL);
		}
		
	}
	
	public void setBrushWidth(int w){
		paint.setStrokeWidth(w);
	}
	
	public int getBrushWidth(){
		return (int) paint.getStrokeWidth();
	}

	public boolean save() {
		//TODO async task
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getContext());
		
		int i = p.getInt(SAVED_FILE_INDEX_PREF, 0);
		
		File dir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		if(dir==null)
			return false;
		boolean status = false;
		//TODO randomize this more
		File f = new File(dir.toString(), "eld_"+i+".png");
		try {
		       FileOutputStream out = new FileOutputStream(f);
		       current.compress(Bitmap.CompressFormat.PNG, 100, out);
		       p.edit().putInt(SAVED_FILE_INDEX_PREF, i+1).commit();
		       status = true;
		       MediaScannerConnection.scanFile(getContext(), new String[]{f.getAbsolutePath()}, new String[]{"image/png"}, null);
		} catch (FileNotFoundException e) {
		       e.printStackTrace();
		}
		return status;
	}
	
	public void workingSave(){
				
		String path = getContext().getFilesDir().toString();
		File f = new File(path, "work.png");
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getContext());
		try {
		       FileOutputStream out = new FileOutputStream(f);
		       current.compress(Bitmap.CompressFormat.PNG, 100, out);
		       //FIXME compression issues? want bmp...
		       p.edit().putInt(LAST_SCORE_PREF, score).commit();
		} catch (Exception e) {
		       e.printStackTrace();
		}
	}
	
	public void workingOpen(){
		String path = getContext().getFilesDir().toString();
		File f = new File(path, "work.png");
		try {
			FileInputStream in = new FileInputStream(f);
			current = BitmapFactory.decodeStream(in);
			current = getDrawing();
		    setDrawing(current);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private Bitmap simage;
	public void setImage(final Bitmap in){
		Log.d("DP", "setImage");
		AlertDialog.Builder build = new Builder(getContext());
		build.setCancelable(false);

		build.setPositiveButton(R.string.load_continue, new AlertDialog.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Bitmap b = in;
				int safeW = DrawingPanel.this.getWidth();
				if(b.getWidth()>safeW){
					int scaleH = safeW * b.getHeight()/b.getWidth();
					b = Bitmap.createScaledBitmap(b, safeW, scaleH, false);
				}
				int safeH = DrawingPanel.this.getHeight();
				if(b.getHeight()>safeH){
					int scaleW = safeH * b.getWidth()/b.getHeight();
					b = Bitmap.createScaledBitmap(b, scaleW, safeH, false);
				}
				
				if(currentG!=null){
					currentG.drawBitmap(b, 0, 0, null);
					myDraw();
				}
				else{
					simage = b;
				}
			}
		});
		
		if(!save()){
			build.setMessage(R.string.load_status_failed);
			
			build.setNegativeButton(R.string.load_keep, new AlertDialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//dont do anything
				}
			});
			
		}else{
			build.setMessage(R.string.load_status_ok);
			
		}
		Dialog d = build.create();
		d.setCanceledOnTouchOutside(false);
		d.show();
		
		
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equals(PLAY_SOUNDS_PREF)){
			playSounds = sharedPreferences.getBoolean(key, true);
			if(mp!=null){
				mp.stop();
				mp.release();
				mp = null;
			}
		}else if(key.equals(SHOW_SCORES_PREF)){
			showScores = sharedPreferences.getBoolean(key, false);
		}
		
		
	}

	public void clear() {
		currentG.drawColor(Color.WHITE);
		myDraw();
		score=0;
	}

//	@Override
//	public void onAccuracyChanged(Sensor sensor, int accuracy) {
//		
//	}
//
//	@Override
//	public void onSensorChanged(SensorEvent event) {
//		for(int i=0; i<event.values.length; i++){
//			Log.d("DP SENS", i + ", "+event.values[i]);
//		}
//		
//	}
	
	private void toggleScroll(){
		if(mode==Mode.SCROLL_MODE){
			scroll = !scroll;
			if(!scroll){
				capt = true;
			}
			myDraw();
		}
	}

	public void postScore() {
		AlertDialog.Builder b = new Builder(getContext());
		b.setTitle(Integer.toString(score));
		b.setView(new PostScoreView(getContext(), score));
		b.create().show();
		
	}

	public void share(String token) {
		String path = getContext().getFilesDir().toString();
		File f = new File(path, "share.png");
		try{
			FileOutputStream out = new FileOutputStream(f);
		    current.compress(Bitmap.CompressFormat.PNG, 100, out);
		    
			DrawNet.post(f, token, getContext().getString(R.string.made_with_eld)+" "+score);
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}
		
	}

	public void setFilter(Filter f) {
		filter = f;
		capt = true;
		myDraw();
		
	}

	private DrawMplayerIRC drawmplayer;
	public void multiplayerPopup(){
		if(drawmplayer==null){
			drawmplayer = new DrawMplayerIRC(this, "somenick");
		}
		
		
		AlertDialog.Builder b = new Builder(getContext());
		b.setTitle(R.string.rooms);
		final RoomView roomList = new RoomView(getContext());
		
		final ArrayAdapter<String> roomAdapt = new ArrayAdapter<String>(getContext(), R.layout.just_text);
		roomList.setAdapter(roomAdapt);
		drawmplayer.setRoomAdapter(new RoomListener() {
			@Override
			public void add(final String room) {
				roomList.post(new Runnable() {		
					@Override
					public void run() {
						roomAdapt.add(room);
						
					}
				});
			}
		});
		
		b.setPositiveButton(R.string.new_room, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				AlertDialog.Builder alert = new AlertDialog.Builder(DrawingPanel.this.getContext());

//				alert.setTitle("Title");
//				alert.setMessage("Message");

				// Set an EditText view to get user input 
				final EditText input = new EditText(DrawingPanel.this.getContext());
				alert.setView(input);

				alert.setPositiveButton(R.string.join, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						drawmplayer.joinRoom(value);
					}
				});

				alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

				alert.show();
				
			}
		});
		b.setView(roomList);
		final AlertDialog dialog = b.create();
		dialog.show();
		roomList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				String theName = (String) roomList.getItemAtPosition(arg2);
				drawmplayer.joinRoom(theName);
				dialog.dismiss();
			}
		});
		
		if(drawmplayer.isConnected()){
			drawmplayer.getList();
		}else{
			new MpConnectTask().execute();
		}

	}
	
	private class MpConnectTask extends AsyncTask<Void, Void, Boolean>{

		@Override
		protected Boolean doInBackground(Void... params) {
			
			return drawmplayer.mplayerConnect();
		}
		
	}

	@Override
	public void onMsgRcv(DrawMsg msg) {
		netDraw(msg);
		
	}

}
