package com.hooby3d.eldrawoandroid;

import java.util.ArrayList;

import st.mark.highscores.HighscoreBoard;
import st.mark.highscores.HighscoreItem;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class PostScoreView extends LinearLayout{
	
	private int score;
	private String name;
	private TextView scores;
	
	private static final String POST_SCORE_NAME_PREF = "postScoreNamePref";
	
	//have an activity instead??

	public PostScoreView(Context context, int score) {
		super(context);
		this.score = score;
		LayoutInflater.from(context).inflate(R.layout.high_score, this, true);
		
		final EditText t = (EditText) findViewById(R.id.score_name);
		final Button b = (Button) findViewById(R.id.score_post_button);
		
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getContext());
		t.setText(p.getString(POST_SCORE_NAME_PREF, ""));
		t.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
		            b.performClick();
		            return true;
		        }
		        return false;
			}
		});
		
		
		b.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(t.getText().toString().equals("")){
					Toast.makeText(getContext(), R.string.enter_your_name, Toast.LENGTH_SHORT).show();
				}else{
					//spinner?
					b.setEnabled(false);
					name = t.getText().toString();
					p.edit().putString(POST_SCORE_NAME_PREF, name).commit();
					PostScoreTask p = new PostScoreTask();
					p.execute();
				}
				
			}
		});
		
		scores = (TextView) findViewById(R.id.the_high_scores);
		//scores.setText("\n\n\n\n\n\n\n\n\n\n");
		
	}
	
	public static String padLeft(String s, int n) {
	    return String.format("%1$" + n + "s", s);  
	}
	
	private void update(HighscoreItem status, ArrayList<HighscoreItem> top){
		String s;
		if(status==null || top==null){
			s = getContext().getString(R.string.couldnt_connect);
		}else{
			int prank = status.getRank();
			s="";
			for(int i=0; i<10 && i<top.size(); i++){
				if(prank==top.get(i).getRank()){
					s+="*";
				}else{
					s+=" ";
				}
				s+= padLeft(Integer.toString(top.get(i).getRank()), 3) + "\t" + padLeft(top.get(i).getScore(), 10) + "\t" + padLeft(top.get(i).getText1(),12)+"\n";
			}
			
			if(prank>10){
				s += "*" + padLeft(Integer.toString(status.getRank()), 3) + "\t" + padLeft(status.getScore(), 10) + "\t" + padLeft(status.getText1(), 12);

			}
		}
		
		scores.setText(s);
	}

	
	
	private class PostScoreTask extends AsyncTask<Void, Void, Void>{
		HighscoreItem hsi;
		ArrayList<HighscoreItem> top;
		
		@Override
		protected Void doInBackground(Void... params) {
			//can this be obfuscated?
			HighscoreBoard hs = new HighscoreBoard("SECRET_HIGH_SCORE_API_KEY");
			try{
			
				hsi = hs.addNewScore(score, name, "", "", "", "");
				top = hs.getTop();
			}catch(Exception e){
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v){
			update(hsi, top);
			
		}
		
	}
	
	
}
