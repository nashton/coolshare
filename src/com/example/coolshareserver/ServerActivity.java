package com.example.coolshareserver;

import com.example.coolshareserver2.R;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

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
