package com.example.coolshareserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServiceStarter extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		/*if (intent.getAction() != null) {
			if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)
					|| intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
				context.startService(new Intent(context, CoolShareService.class));
			}
		}*/
	}
};