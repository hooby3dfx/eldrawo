package com.hooby3d.eldrawoandroid;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends SherlockActivity {
	
	/* roadmap
	 * 
	 * polish
	 * research -OK-
	 * jp local
	 * testing
	 * 
	 * help/splash?
	 * undo/redo
	 * get src from camera/other apps?
	 * 
	 */

	public enum Mode{

		RECTANGLE_MODE(R.drawable.rect, R.string.mode_rect),
		ELLIPSE_MODE(R.drawable.elps, R.string.mode_ellipse),
		PENCIL_MODE(R.drawable.pencil, R.string.mode_pencil),
		ERASER_MODE(R.drawable.eraser, R.string.mode_eraser), 
		LINE_MODE(R.drawable.lines, R.string.mode_line),
		RAINBOW_PEN_MODE(R.drawable.rainbow, R.string.mode_rainbow_pen),
		COOL_LINES_MODE(R.drawable.coollines, R.string.mode_cool_lines),
		DRAW_FOUR(R.drawable.drawfour, R.string.mode_draw_four),
		SCROLL_MODE(R.drawable.scroll, R.string.mode_scroll),
		FILL_MODE(R.drawable.fill, R.string.mode_fill),
		SPRAY_MODE(R.drawable.spray, R.string.mode_spray),
		TREE_MODE(R.drawable.tree, R.string.mode_tree),
		MYSTERY_FILL(R.drawable.fillb, R.string.mode_mystery_fill);
		//text
		//selections?
		
		private int ires, sres;
		private Mode(int iconres, int strres){
			ires = iconres;
			sres = strres; 
		}
		
	}
	
	public enum Filter{
		INVERSE(R.drawable.inverse,R.string.filter_inverse),
		GRAYSCALE(R.drawable.gray,R.string.filter_grayscale),
		HORIZONTAL_FLIP(R.drawable.horiz,R.string.filter_horizontal_flip),
		VERTICAL_FLIP(R.drawable.vert,R.string.filter_vertical_flip),
		PIXEL(R.drawable.pix,R.string.filter_pixel),
		DITHER(R.drawable.dither,R.string.filter_dither);
		
		private int ires, sres;
		private Filter(int iconres, int strres){
			ires = iconres;
			sres = strres; 
		}
	}
	
	private DrawingPanel panel;
	

	private static final String FB_ACCESS_TOKEN_PREF = "fbAccessTokenPref";
	private static final String FB_ACCESS_TOKEN_EXPIRES_PREF = "fbAccessTokenExpiresPref";
	
	final Facebook facebook = new Facebook("SECRET_FB_API_KEY");


	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MA", "onCreate");

        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayShowHomeEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        bar.setListNavigationCallbacks(new ArrayAdapter<Mode>(this, R.layout.just_text, Mode.values()){

        	@Override
        	public View getView (int position, View convertView, ViewGroup parent){
        		//use xml layout?
        		LinearLayout ll = new LinearLayout(this.getContext());
        		ll.setGravity(Gravity.CENTER_VERTICAL);
        		ImageView iv = new ImageView(getContext());
        		iv.setBackgroundResource(Mode.values()[position].ires);
        		
        		TextView tv = new TextView(getContext());
        		tv.setText(Mode.values()[position].sres);
        		tv.setTextSize(18f);
        		tv.setLines(1);
        		//tv.setEllipsize(where)
        		int pad = 10;
        		tv.setPadding(pad, 0, 0, 0);
        		
        		ll.addView(iv);
        		ll.addView(tv);
        		ll.setPadding(pad, pad, pad, pad);
        		
        		
        		return ll;
        	}
        	
        	@Override 
        	public View getDropDownView (int position, View convertView, ViewGroup parent){
        		return getView(position, convertView, parent);
        	}
        }, new OnNavigationListener() {
			@Override
			public boolean onNavigationItemSelected(int itemPosition, long itemId) {
				panel.setMode(Mode.values()[itemPosition]);
				return true;
			}
		});
        bar.setSelectedNavigationItem(Mode.PENCIL_MODE.ordinal());
        
        
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        
        panel = new DrawingPanel(this);
        setContentView(panel);
        
        
        panel.workingOpen();
        
        //StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        

    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	Log.d("MA", "onPause");
    }
    
    
    @Override
    public void onStop(){
    	super.onStop();
    	Log.d("MA", "onStop");

    	panel.workingSave();
    	
    	//TODO remember everything abt state:
    	//mode, brush size, fill, ...
    }
    
    @Override
    public void onNewIntent(Intent ni){
    	setIntent(ni);
    }
    
    @Override
    public void onStart(){
    	super.onStart();
    	Log.d("MA", "onStart");

    	Intent i = getIntent();
        Uri data;
        if(i!=null && (data=i.getData())!=null){
    		try {
    			//memory ok...?
				Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data);
		        //make sure to backup what they were working on
				panel.setImage(bitmap);
			} catch (FileNotFoundException e) {
				
				e.printStackTrace();
			} catch (IOException e) {
				
				e.printStackTrace();
			} catch (OutOfMemoryError e){
				e.printStackTrace();
			}
        }
        setIntent(null);
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	Log.d("MA", "onResume");
    	facebook.extendAccessTokenIfNeeded(this, null);
    }
    
//    @Override
//    public void onSaveInstanceState(Bundle out){
//    	super.onSaveInstanceState(out);
//    	Log.d("MA", "onSaveInstanceState");
//    	out.putParcelable(THE_DRAWING, panel.getDrawing());
//    }
//    
//    @Override
//    public void onRestoreInstanceState(Bundle out){
//    	super.onRestoreInstanceState(out);
//    	Log.d("MA", "onRestoreInstanceState");
//    	panel.setDrawing((Bitmap)out.getParcelable(THE_DRAWING));
//    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	Log.d("MA", "onDestroy");
    	
    	//TODO disconnect multiplayer
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_main, menu);
        

        MenuItem mi = menu.add(R.string.color_chooser);//menu.findItem(R.id.menu_color_chooser);
        mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mi.setIcon(R.drawable.chooser);
        mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				AmbilWarnaDialog dialog = new AmbilWarnaDialog(MainActivity.this, panel.getColor(), new OnAmbilWarnaListener() {
			        @Override
			        public void onOk(AmbilWarnaDialog dialog, int color) {
			                // color is the color selected by the user.
			        	panel.setColor(color);
			        }
			                
			        @Override
			        public void onCancel(AmbilWarnaDialog dialog) {
			                // cancel was selected by the user
			        }
				});
				dialog.show();
				return true;
			}
		});

        mi = menu.add(R.string.brush_size);
        mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mi.setIcon(R.drawable.brushsize);
        mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				AlertDialog.Builder builder;
				AlertDialog alertDialog;


				builder = new AlertDialog.Builder(MainActivity.this);
				
				SeekBar bar = new SeekBar(MainActivity.this);
				bar.setProgress(panel.getBrushWidth());
				bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						
					}
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						
					}
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress,
							boolean fromUser) {
						panel.setBrushWidth(progress);
						
					}
				});
				
				builder.setView(bar);
				alertDialog = builder.create();
				alertDialog.show();
				return true;
			}
		});
        
        mi = menu.findItem(R.id.menu_choose_filter);
        mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				//popup filter chooser
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle(R.string.filters);

				ListView filterList = new ListView(MainActivity.this);
				filterList.setAdapter(new ArrayAdapter<Filter>(MainActivity.this, R.layout.just_text, Filter.values()){
					@Override
					public View getView (int position, View convertView, ViewGroup parent){

						LinearLayout ll = new LinearLayout(this.getContext());
						ll.setGravity(Gravity.CENTER_VERTICAL);
						ImageView iv = new ImageView(getContext());
						iv.setBackgroundResource(Filter.values()[position].ires);

						TextView tv = new TextView(getContext());
						tv.setText(Filter.values()[position].sres);
						tv.setTextSize(18f);
						tv.setLines(1);
						//tv.setEllipsize(where)
						int pad = 10;
						tv.setPadding(pad, 0, 0, 0);

						ll.addView(iv);
						ll.addView(tv);
						ll.setPadding(pad, pad, pad, pad);


						return ll;
					}

				});
				
				builder.setView(filterList);

				final AlertDialog dialog = builder.create();
				
				filterList.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int pos, long arg3) {
						panel.setFilter(Filter.values()[pos]);
						dialog.dismiss();
					}
				});
				
				dialog.show();


				return true;
			}
        });
        
//        for(final Filter f : Filter.values()){
//        	if(f==Filter.NONE)
//        		continue;
//        	mi = menu.add(f.sres);
//        	mi.setIcon(f.ires);
//        	mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {
//				@Override
//				public boolean onMenuItemClick(MenuItem item) {
//					panel.setFilter(f);
//					return true;
//				}
//			});
//        }
        
        
        
        final MenuItem mi2 = menu.findItem(R.id.menu_fill_shapes);
        mi2.setChecked(true); 
        mi2.setOnMenuItemClickListener(new OnMenuItemClickListener() {			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				panel.toggleFill();
				mi2.setChecked(!mi2.isChecked());
				return true;
			}
		});
        
//        mi = menu.findItem(R.id.menu_post_score);
//        mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {			
//			@Override
//			public boolean onMenuItemClick(MenuItem item) {
//				panel.postScore();
//				return true;
//			}
//		});
        
        mi = menu.findItem(R.id.menu_multiplayer);
        mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				panel.multiplayerPopup();
				return true;
			}
		});
        
        mi = menu.findItem(R.id.menu_clear_drawing);
        mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				panel.clear();
				return true;
			}
		});
        
                
        mi = menu.findItem(R.id.menu_save);
        mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				panel.save();
				return true;
			}
		});
        
        mi = menu.findItem(R.id.menu_settings);
        mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent i = new Intent();
				i.setClass(MainActivity.this, PrefsAct.class);
				startActivity(i);
				return true;
			}
		});
        
        mi = menu.findItem(R.id.email_dev);
        mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				emailStuff();
				return true;
			}
		});
        
        mi = menu.findItem(R.id.menu_about);
        mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {
    		@Override
    		public boolean onMenuItemClick(MenuItem item) {
    			
    			AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
    			b.setTitle(R.string.about).setMessage(R.string.about_long)
//    			.setPositiveButton(R.string.donate, new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						// TODO Auto-generated method stub
//						
//					}
//				})
    			.setNeutralButton(R.string.email_developer, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						emailStuff();
						
					}
				})
    			.create().show();
    			
    			
    			return true;
    		}
    	});
        mi = menu.findItem(R.id.menu_fb_post);
        mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {
    		@Override
    		public boolean onMenuItemClick(MenuItem item) {
    			
    			final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
    			
    			String access_token = p.getString(FB_ACCESS_TOKEN_PREF, null);
    	        long expires = p.getLong(FB_ACCESS_TOKEN_EXPIRES_PREF, 0);
    	        if(access_token != null) {
    	            facebook.setAccessToken(access_token);
    	        }
    	        if(expires != 0) {
    	            facebook.setAccessExpires(expires);
    	        }
    			
    			if(!facebook.isSessionValid()) {
	    			facebook.authorize(MainActivity.this, new String[]{"publish_stream"}, 
	    					new DialogListener() {
	    	            @Override
	    	            public void onComplete(Bundle values) {
	    	            	SharedPreferences.Editor editor = p.edit();
	                        editor.putString(FB_ACCESS_TOKEN_PREF, facebook.getAccessToken());
	                        editor.putLong(FB_ACCESS_TOKEN_EXPIRES_PREF, facebook.getAccessExpires());
	                        editor.commit();
	                        
	                        FbShareTask t = new FbShareTask();
	            			t.execute();
	    	            }
	
	    	            @Override
	    	            public void onFacebookError(FacebookError error) {
	    	            	//error.printStackTrace();
	    	            }
	
	    	            @Override
	    	            public void onError(DialogError e) {
	    	            	//e.printStackTrace();
	    	            }
	
	    	            @Override
	    	            public void onCancel() {
	    	            	Log.d("MA FB", "fb onCancel");
	    	            }
	    	        });
    			}else{
        			//do post - ASYNC!
        			FbShareTask t = new FbShareTask();
        			t.execute();
    			}
    			
    			
    			
    			return true;
    		}
    	});
        
        
        
        
        
        return true;
        
        
    }
    
    private class FbShareTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			panel.share(facebook.getAccessToken());
			return null;
		}
    	
    }
    
    //TODO fb needs this?
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("MA", "onActivityResult");
        facebook.authorizeCallback(requestCode, resultCode, data);
    }
    
    
    
    //back is undo??
    //redo menu item?
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            panel.undo();
//        	return true;
//        }
        return super.onKeyDown(keyCode, event);
    }

    private void emailStuff(){
    	Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
	    emailIntent.setType("plain/text");
	    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{getResources().getString(R.string.dev_email)}); 
	    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.dev_email_subject));
	    
	    startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.email_developer)));
	    
    }
    
}
