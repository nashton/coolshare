package com.example.coolshareserver.activities;

import com.example.coolshareserver.BluetoothService;
import com.example.coolshareserver.RepositoryUtils;
import com.example.coolshareserver2.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ServerActivity extends Activity {

	private static final boolean D = true;
	private static final String TAG = "ServerActivity";
	
	protected static final int MESSAGE_CMD_REQUEST_INFO = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Set default preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		Button bStart = (Button) this.findViewById(R.id.buttonStartServer);
		Button bStop = (Button) this.findViewById(R.id.buttonStopServer);

		bStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startService(new Intent(ServerActivity.this, BluetoothService.class));
			}
		});
		
		bStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopService(new Intent(ServerActivity.this, BluetoothService.class));
			}
		});
		
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(Intent.ACTION_PACKAGE_ADDED);
    	filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
    	filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
    	filter.addDataScheme("package");
    	this.registerReceiver(mReceiver, filter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
    	this.unregisterReceiver(mReceiver);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.action_settings:
	    		startActivity(new Intent(ServerActivity.this, PreferenceActivity.class));
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

    //The BroadcastReceiver that listens for apps installed/replaced/removed broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();
    		Uri data = intent.getData();
    		String packageName = data.getSchemeSpecificPart();
    		
    		if (Intent.ACTION_PACKAGE_REMOVED.equals(action) || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
    			// Update repository if package is shared
    			if(RepositoryUtils.isAppShared(packageName, ServerActivity.this)) {
    				RepositoryUtils.generateRepository(ServerActivity.this);
    			}
    		}           
    		
    		if(Intent.ACTION_PACKAGE_ADDED.equals(action)) {
    			// Update repository if everything is shared
    			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ServerActivity.this);
    			boolean allSharedPref = pref.getBoolean(PreferenceActivity.KEY_SHARE_ALL, true);
    			if(allSharedPref) {
    				RepositoryUtils.generateRepository(ServerActivity.this);
    			}
    		}
    	}
    };
    
	
	// The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
        
            case MESSAGE_CMD_REQUEST_INFO:
            	if(D) Log.i(TAG, "MESSAGE_CMD_REQUEST_INFO");
                byte[] infoBuf = (byte[]) msg.obj;
                String infoXml = new String(infoBuf, 0, msg.arg1);
            	
                if(D) Log.i(TAG, "info.xml: " + infoXml);

            	break;
            }
            
        }
    };
}
