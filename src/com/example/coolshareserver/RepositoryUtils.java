package com.example.coolshareserver;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.example.coolshareserver.activities.PreferenceActivity;
import com.example.xml.Apklst;
import com.example.xml.Package;
import com.example.xml.Repository;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.SortableFieldKeySorter;

public class RepositoryUtils {
	

	public static boolean isAppShared(String name, Context ctx) {
		// Check if it is shared
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		boolean allSharedPref = pref.getBoolean(PreferenceActivity.KEY_SHARE_ALL, true);
		String sharedPref = pref.getString(PreferenceActivity.KEY_SHARE_SELECTED, "");
		Set<String> sharedApps = new HashSet<String>();
		Collections.addAll(sharedApps, sharedPref.split(MultiSelectListPreference.DEFAULT_SEPARATOR));

		if(allSharedPref || sharedApps.contains(name)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Generate repository information from all installed applications
	 */
	public static void generateRepository(Context context) {
		generateRepository(null, context);
	}
	
	/**
	 * Generate repository information for selected apps to share
	 * 
	 * @param sharedPackages List of shared apk package names. If null everything is shared
	 */
	public static void generateRepository(final Set<CharSequence> sharedPackages, final Context ctx) {
		final PackageManager pm = ctx.getPackageManager();
		
		AsyncTask<Void, Void, Boolean> generateBackground = new AsyncTask<Void, Void, Boolean>() {
			private ProgressDialog progressDlg;
			
			@Override
			protected void onPreExecute() {
            	progressDlg = new ProgressDialog(ctx);
            	progressDlg.setMessage("Please wait while generating repository...");
            	progressDlg.setMax(100);
            	progressDlg.setCancelable(false);
            	progressDlg.show();				
			}
			
			@Override
			protected void onProgressUpdate(Void... progress) {
				progressDlg.incrementProgressBy(1);
			}
			
			@Override
			protected void onPostExecute(Boolean result) {
				progressDlg.dismiss();
				
				if(!result) {
					Toast.makeText(ctx, "An error occurred while trying to generate repository", Toast.LENGTH_LONG).show();
				}
			}
			
			@Override
			protected Boolean doInBackground(Void... params) {
				List<PackageInfo> packages = pm.getInstalledPackages(0);
				List<Package> pojoPackages = new ArrayList<Package>();
				progressDlg.setMax(packages.size());
				
				for (PackageInfo pInfo : packages) {
					ApplicationInfo aInfo = pInfo.applicationInfo;
					if((aInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && (sharedPackages == null || sharedPackages.contains(aInfo.packageName))) {
						Package pojoPackage = new Package();
						pojoPackage.apkid = aInfo.packageName;
						pojoPackage.path = aInfo.publicSourceDir;
						pojoPackage.name = (aInfo.loadLabel(pm)).toString();
						pojoPackage.minSdk = aInfo.targetSdkVersion;

						pojoPackage.vercode = pInfo.versionCode;
						pojoPackage.ver = pInfo.versionName;
						
						pojoPackage.sz = (int) ((new File(aInfo.publicSourceDir)).length()/1024);
						
						//pojoPackage.date = (new Date(pInfo.lastUpdateTime)).toString();
						
						pojoPackages.add(pojoPackage);
					}
					
					this.publishProgress();
				}

				// Generate info list in xml
				Apklst apklst = new Apklst();
				apklst.version = 5;
				apklst.repository = new Repository();
				apklst.repository.appscount = Integer.toString(pojoPackages.size());
				apklst.packages = pojoPackages;

				SortableFieldKeySorter sorter = new SortableFieldKeySorter();
				sorter.registerFieldOrder(Apklst.class, new String[] { "version", "repository", "packages" });
				XStream xstream = new XStream(new PureJavaReflectionProvider(new FieldDictionary(sorter)));
				xstream.processAnnotations(Apklst.class);
				xstream.processAnnotations(Package.class);
				xstream.processAnnotations(Repository.class);

				try {
					OutputStream infoOut = ctx.openFileOutput(BluetoothService.FILEPATH_INFO, Context.MODE_PRIVATE);
					String infoXml = xstream.toXML(apklst);
					Log.d("XML", infoXml);
					xstream.toXML(apklst, infoOut);
					infoOut.close();
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
				
				return true;
			}
			
		};
		
		generateBackground.execute();
	
	}
}
