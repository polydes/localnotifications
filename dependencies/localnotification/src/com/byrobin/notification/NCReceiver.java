package com.byrobin.notification;

import android.app.Activity;
import android.app.Notification;
::if (ANDROID_TARGET_SDK_VERSION >= 26)::
import android.app.NotificationChannel;
::end::
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.R.dimen;
import android.view.Window;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import org.haxe.extension.Extension;

import com.byrobin.notification.Common;

public class NCReceiver extends BroadcastReceiver {

        static NotificationManager notificationManager;

	@Override
	public void onReceive(Context context, Intent intent) {

            Log.i(Common.TAG, "NCReceiver onReceive starts");

		if(context == null || intent == null) {
			Log.i(Common.TAG, "Received notification presentation broadcast with null context or intent");
			return;
		}
		String action = intent.getAction();
		if(action == null) {
			Log.i(Common.TAG, "Received notification presentation broadcast with null action");
			return;
		}
                if(action.equals("notification_cancelled"))
                {
                        // notification are cancelled by user.
                         Common.setApplicationIconBadgeNumber(context, 0);
                }else{
                    presentNotification(context, action); // Everything should be for presenting local device notifications
                }

	}
	
	private static void presentNotification(Context context, String action) {
                SharedPreferences prefs = context.getSharedPreferences(action, Context.MODE_PRIVATE);
		if(prefs == null) {
			Log.i(Common.TAG, "Failed to read notification preference data");
			return;
		}
		
		int slot = prefs.getInt(Common.SLOT_TAG, -1);
		if(slot == -1) {
			Log.i(Common.TAG, "Failed to read notification slot id");
			return;
		}
        
                String title = prefs.getString(Common.TITLE_TEXT_TAG, "");
                String message = prefs.getString(Common.MESSAGE_BODY_TEXT_TAG, "");
        
		//Common.erasePreference(context, slot);
		
                Common.setApplicationIconBadgeNumber(context, Common.getApplicationIconBadgeNumber(context) + 1);
        
		sendNotification(context, slot, title, message);
	}
	
	// Actually send the local notification to the device
        private static void sendNotification(Context context, int slot, String title, String message) {
            Log.i(Common.TAG, "Start sendNotification");

		Context applicationContext = context.getApplicationContext();
		if(applicationContext == null) {
			Log.i(Common.TAG, "Failed to get application context");
			return;
		}
		
		// Get small application icon
		int smallIconId = 0;
        try {
            PackageManager pm = context.getPackageManager();
            String pkg = "::APP_PACKAGE::";
            if(pm != null) {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                if(ai != null) {
                    smallIconId = ai.icon;
                }
            }
        } catch (NameNotFoundException e) {
            smallIconId = android.R.drawable.ic_dialog_info;
        }
		
		// Get large application icon
		int largeIconId = 0;
		try {
			PackageManager pm = context.getPackageManager();
			if(pm != null) {
				ApplicationInfo ai = pm.getApplicationInfo(Common.getPackageName(), 0);
				if(ai != null) {
					largeIconId = ai.icon;
				}
			}
		} catch (NameNotFoundException e) {
			Log.i(Common.TAG, "Failed to get application icon, falling back to default");
			largeIconId = android.R.drawable.ic_dialog_alert;
		}
		
		// Get large application icon
		Bitmap largeIcon = BitmapFactory.decodeResource(applicationContext.getResources(), largeIconId);
		
		// Scale it down if it's too big
		if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			int width = android.R.dimen.notification_large_icon_width > 0 ? android.R.dimen.notification_large_icon_width : 150;
			int height = android.R.dimen.notification_large_icon_height > 0 ? android.R.dimen.notification_large_icon_height : 150;
			if(largeIcon.getWidth() > width || largeIcon.getHeight() > height) {
				largeIcon = Bitmap.createScaledBitmap(largeIcon, width, height, false);
			}
		}
		
		// Launch or open application on notification tap
		Intent intent = null;
		try {
			PackageManager pm = context.getPackageManager();
			if(pm != null) {
				String packageName = context.getPackageName();
				intent = pm.getLaunchIntentForPackage(packageName);
				intent.addCategory(Intent.CATEGORY_LAUNCHER); // Should already be set, but just in case
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			}
		} catch (Exception e) {
			Log.i(Common.TAG, "Failed to get application launch intent");
		}
		
		if(intent == null) {
			Log.i(Common.TAG, "Falling back to empty intent");
			intent = new Intent();
		}

                Log.i(Common.TAG, "Build Notification " + title + " " + message);
                PendingIntent pendingIntent = PendingIntent.getActivity(applicationContext, slot, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationCompat.Builder builder = null;
                builder = new NotificationCompat.Builder(applicationContext);

                ::if (ANDROID_TARGET_SDK_VERSION >= 26)::
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setupNotificationChannel();
                     //builder = new NotificationCompat.Builder(applicationContext);
                    //builder = new NotificationCompat.Builder(applicationContext,"::APP_PACKAGE::");
                     builder.setChannelId("::APP_PACKAGE::");
                }
                ::end::

		builder.setAutoCancel(true);
		builder.setContentTitle(title);
		builder.setContentText(message);
		builder.setTicker("You have a new message from ::APP_TITLE::");
                builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

		if(largeIcon != null) {
			builder.setLargeIcon(largeIcon);
		}
		builder.setSmallIcon(smallIconId);
		builder.setContentIntent(pendingIntent);
		builder.setOngoing(false);
		builder.setWhen(System.currentTimeMillis());
		builder.setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS);
                builder.build();
		
                notificationManager = ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE));
		if(notificationManager != null) {
                        notificationManager.notify(slot, builder.getNotification());
		}

            Log.i(Common.TAG, "End sendNotification");
	}

        ::if (ANDROID_TARGET_SDK_VERSION >= 26)::
        private static void setupNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("::APP_PACKAGE::", "::APP_TITLE::", NotificationManager.IMPORTANCE_DEFAULT);
                channel.setShowBadge(true);
                channel.enableVibration(true);
                channel.enableLights(true);
                if(notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }else{
                    Log.i(Common.TAG, "notificationManager is NULL");
                }
            }
        }
        ::end::
}
