package com.plugin.gcm;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

import com.google.android.gcm.GCMBaseIntentService;

@SuppressLint("NewApi")
public class GCMIntentService extends GCMBaseIntentService {

	private static final String TAG = "GCMIntentService";
	private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
	private SharedPreferences.OnSharedPreferenceChangeListener listener;
	public GCMIntentService() {
		super("GCMIntentService");
		listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                              public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                                if(key.split("_").length==7 && prefs.getString(key,"oiwJFIWJRGKWRJGkjhouhef").equals("")){
                                    editor.remove(key);
                                    editor.commit();
                                }
                              }
                            };
	}

	@Override
	public void onRegistered(Context context, String regId) {

		Log.v(TAG, "onRegistered: "+ regId);

		JSONObject json;

		try
		{
			json = new JSONObject().put("event", "registered");
			json.put("regid", regId);

			Log.v(TAG, "onRegistered: " + json.toString());

			// Send this JSON data to the JavaScript application above EVENT should be set to the msg type
			// In this case this is the registration ID
			PushPlugin.sendJavascript( json );

		}
		catch( JSONException e)
		{
			// No message to the user is sent, JSON failed
			Log.e(TAG, "onRegistered: JSON exception");
		}

	}

	@Override
	public void onUnregistered(Context context, String regId) {
		Log.d(TAG, "onUnregistered - regId: " + regId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.d(TAG, "onMessage - context: " + context);

		// Extract the payload from the message
		Bundle extras = intent.getExtras();
		if (extras != null)
		{
			// if we are in the foreground, just surface the payload, else post it to the statusbar
            if (PushPlugin.isInForeground()) {
				extras.putBoolean("foreground", true);
                PushPlugin.sendExtras(extras);
			}
			else {
				extras.putBoolean("foreground", false);

                // Send a notification if there is a message
                if (extras.getString("message") != null && extras.getString("message").length() != 0) {
                    createNotification(context, extras);
                }
            }
        }
	}

	public void createNotification(Context context, Bundle extras)
	{
	    int notId = 0;

        		 try{
                            notId = (int)(Double.parseDouble(extras.getString("notId"))% Integer.MAX_VALUE);
                        }
        		catch(NumberFormatException e) {
        			Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
        		}
        		catch(Exception e) {
        			Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
        		}

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String appName = getAppName(this);

		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);

		PendingIntent contentIntent = PendingIntent.getActivity(this, notId, notificationIntent, PendingIntent.FLAG_ONE_SHOT);

		int defaults = Notification.DEFAULT_ALL;

		if (extras.getString("defaults") != null) {
			try {
				defaults = Integer.parseInt(extras.getString("defaults"));
			} catch (NumberFormatException e) {}
		}

			NotificationCompat.Builder mBuilder =
			new NotificationCompat.Builder(context)
				.setDefaults(defaults)
				.setSmallIcon(context.getApplicationInfo().icon)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(extras.getString("title"))
				.setTicker(extras.getString("title"))
				.setContentIntent(contentIntent)
				.setAutoCancel(true);

		String message = extras.getString("message");
		if (message != null) {
			mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message)).setContentText(message);
		} else {
			mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText("<missing message content>")).setContentText("<missing message content>");
		}

		String msgcnt = extras.getString("msgcnt");
		if (msgcnt != null) {
			mBuilder.setNumber(Integer.parseInt(msgcnt));
		}

		mNotificationManager.notify((String) appName, notId, mBuilder.build());

		//Crear  shared preferences con información de la notificación recibida
        String eventName = extras.getString("eventName");
if(prefs==null){
                    prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    prefs.registerOnSharedPreferenceChangeListener(listener);
                     editor = prefs.edit();
                    }






        if(eventName.charAt(0)=='0'){
            editor.putString(eventName, eventName);
            editor.commit();
        }

	}
	
	private static String getAppName(Context context)
	{
		CharSequence appName = 
				context
					.getPackageManager()
					.getApplicationLabel(context.getApplicationInfo());
		
		return (String)appName;
	}
	
	@Override
	public void onError(Context context, String errorId) {
		Log.e(TAG, "onError - errorId: " + errorId);
	}

}
