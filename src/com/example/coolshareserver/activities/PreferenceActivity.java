package com.example.coolshareserver.activities;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.coolshareserver.MultiSelectListPreference;
import com.example.coolshareserver.RepositoryUtils;
import com.example.coolshareserver2.R;
import com.example.xml.Apklst;
import com.example.xml.Repository;
import com.example.xml.Package;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.SortableFieldKeySorter;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.util.Log;
import android.widget.Toast;

public class PreferenceActivity extends android.preference.PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	public static final String KEY_SHARE_ALL = "pref_key_share_all";
	public static final String KEY_SHARE_SELECTED = "pref_key_share_selected";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {     
	    super.onCreate(savedInstanceState);   
	    // PreferenceFragment not supported in support library, hence using a deprecated method to keep backwards compatibility
	    addPreferencesFromResource(R.xml.preferences);
	    setShareSelected();
	    loadInstalledApps();
	}
	
	private void loadInstalledApps() {
		MultiSelectListPreference shareSelectedPref = (MultiSelectListPreference) findPreference(KEY_SHARE_SELECTED);
		ArrayList<String> appIdList = new ArrayList<String>();
		ArrayList<String> appNameList = new ArrayList<String>();
		
		final PackageManager pm = getPackageManager();
		//get a list of installed apps.
		List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
		for (ApplicationInfo packageInfo : packages) {
			if((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
				//Log.d("PM", "Installed package :" + packageInfo.packageName);
				appIdList.add(packageInfo.packageName);
				appNameList.add((packageInfo.loadLabel(pm)).toString());
			}
		}
		shareSelectedPref.setEntries(appNameList.toArray(new String[appIdList.size()]));
		shareSelectedPref.setEntryValues(appIdList.toArray(new String[appNameList.size()]));
		shareSelectedPref.refreshEntries();
	}

	@Override
	protected void onResume() {
	    super.onResume();
	    getPreferenceScreen().getSharedPreferences()
	            .registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
	    super.onPause();
	    getPreferenceScreen().getSharedPreferences()
	            .unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        CheckBoxPreference shareAllPref = (CheckBoxPreference) findPreference(KEY_SHARE_ALL);
        MultiSelectListPreference shareSelectedPref = (MultiSelectListPreference) findPreference(KEY_SHARE_SELECTED);
        
		if (key.equals(KEY_SHARE_ALL)) {
			setShareSelected();

			if(shareAllPref.isChecked()) {
				RepositoryUtils.generateRepository(this);
			}
		}

        if(key.equals(KEY_SHARE_SELECTED) || (key.equals(KEY_SHARE_ALL) && !shareAllPref.isChecked())) {
        	Set<CharSequence> packagesSet = new HashSet<CharSequence>();
        	Collections.addAll(packagesSet, shareSelectedPref.getCheckedValues());
        	RepositoryUtils.generateRepository(packagesSet, this);      	
        }
        
    }
    
    private void setShareSelected() {
        CheckBoxPreference shareAllPref = (CheckBoxPreference) findPreference(KEY_SHARE_ALL);
        MultiSelectListPreference shareSelectedPref = (MultiSelectListPreference) findPreference(KEY_SHARE_SELECTED);
        
        if(shareAllPref.isChecked()) {
        	shareSelectedPref.setEnabled(false);
        } else {
        	shareSelectedPref.setEnabled(true);
        }
    }
}
