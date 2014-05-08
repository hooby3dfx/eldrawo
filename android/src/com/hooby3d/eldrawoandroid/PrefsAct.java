package com.hooby3d.eldrawoandroid;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PrefsAct extends PreferenceActivity{
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.prefs);
    }  
}
