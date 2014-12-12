/*
    Copyright 2013-2014 appPlant UG

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/

package de.appplant.cordova.plugin.localnotification;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import com.loopj.android.http.JsonHttpResponseHandler;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
/**
 * The alarm receiver is triggered when a scheduled alarm is fired. This class
 * reads the information in the intent and displays this information in the
 * Android notification bar. The notification uses the default notification
 * sound and it vibrates the phone.
 */
public class Receiver extends BroadcastReceiver {

    public static final String OPTIONS = "LOCAL_NOTIFICATION_OPTIONS";

    private Context context;
    private Options options;

    @Override
    public void onReceive (Context context, Intent intent) {
        Options options = null;
        Bundle bundle   = intent.getExtras();
        JSONObject args;

        try {
            args    = new JSONObject(bundle.getString(OPTIONS));
            options = new Options(context).parse(args);
        } catch (JSONException e) {
            return;
        }

        this.context = context;
        this.options = options;

        // The context may got lost if the app was not running before
        LocalNotification.setContext(context);

        fireTriggerEvent();

        if (options.getInterval() == 0) {
            LocalNotification.unpersist(options.getId());
        } else if (isFirstAlarmInFuture()) {
            return;
        } else {
            LocalNotification.add(options.moveDate(), false);
        }

        Builder notification = buildNotification();

        showNotification(notification);
    }

    /*
     * If you set a repeating alarm at 11:00 in the morning and it
     * should trigger every morning at 08:00 o'clock, it will
     * immediately fire. E.g. Android tries to make up for the
     * 'forgotten' reminder for that day. Therefore we ignore the event
     * if Android tries to 'catch up'.
     */
    private Boolean isFirstAlarmInFuture () {
        if (options.getInterval() > 0) {
            Calendar now    = Calendar.getInstance();
            Calendar alarm  = options.getCalendar();

            int alarmHour   = alarm.get(Calendar.HOUR_OF_DAY);
            int alarmMin    = alarm.get(Calendar.MINUTE);
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMin  = now.get(Calendar.MINUTE);

            if (currentHour != alarmHour && currentMin != alarmMin) {
                return true;
            }
        }

        return false;
    }

    /**
     * Creates the notification.
     */
    @SuppressLint("NewApi")
	private Builder buildNotification () {
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(), options.getIcon());
        Uri sound   = options.getSound();

        Builder notification = new Notification.Builder(context)
            .setDefaults(0) // Do not inherit any defaults
	        .setContentTitle(options.getTitle())
	        .setContentText(options.getMessage())
	        .setNumber(options.getBadge())
	        .setTicker(options.getMessage())
	        .setSmallIcon(options.getSmallIcon())
	        .setLargeIcon(icon)
	        .setAutoCancel(options.getAutoCancel())
	        .setOngoing(options.getOngoing());

        if (sound != null) {
        	notification.setSound(sound);
        }

        if (Build.VERSION.SDK_INT > 16) {
        	notification.setStyle(new Notification.BigTextStyle()
        		.bigText(options.getMessage()));
        }

        setClickEvent(notification);

        return notification;
    }

    /**
     * Adds an onclick handler to the notification
     */
    private Builder setClickEvent (Builder notification) {
        Intent intent = new Intent(context, ReceiverActivity.class)
            .putExtra(OPTIONS, options.getJSONObject().toString())
            .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        int requestCode = new Random().nextInt();

        PendingIntent contentIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        return notification.setContentIntent(contentIntent);
    }

    /**
     * Shows the notification
     */
    final AsyncHttpClient client = new AsyncHttpClient();
    final Header[] headers = {
            new BasicHeader("Content-type", "application/x-www-form-urlencoded")
            ,new BasicHeader("Accep", "application/json, text/javascript, */*")
            ,new BasicHeader("Connection", "keep-alive")
            ,new BasicHeader("keep-alive", "115")
    };

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void showNotification (Builder notification) {
        NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int id                  = 0;

        try {
            id = Integer.parseInt(options.getId());
        } catch (Exception e) {}
        System.out.println("upside:Notification fired in receiver method ID : "+id);
        RequestParams params = new RequestParams();
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", options.getPersonId());
            obj.put("startLimit", options.getStartLimit());
            obj.put("endLimit", options.getEndLimit());
            obj.put("apiKey", options.getApiKey());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        params.put("url", options.getUrl());
        params.put("inputParam", obj);

        client.post(context,options.getUrl(),headers, params,"application/x-www-form-urlencoded", new JsonHttpResponseHandler(){
            @Override
            public void onStart() {
                super.onStart();
                System.out.println("upside:onStart getNotification");
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // If the response is JSONObject instead of expected JSONArray
                JSONArray paramList = null;
                Boolean hasNotification = false;
                JSONArray notificationIds = new JSONArray();
                try {
                    paramList = (JSONArray) response.get("paramList");
                    for(int i = 0; i < paramList.length(); i++){
                        JSONObject notification = paramList.getJSONObject(i);
                        int notification_pull_status = notification.getInt("notification_pull_status");
                        System.out.println("upside:notification_pull_status = "+notification_pull_status);
                        if(notification_pull_status == 0){
                            hasNotification = true;
                            notificationIds.put(notification.getInt("notification_id"));
                            System.out.println("upside:notification_id = "+notification.getInt("notification_id"));
                        }
                    }
                }catch (JSONException e){

                }
                if(hasNotification == true){
                    NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    int id = 0;
                    if(paramList.length() > 0){
                        if (Build.VERSION.SDK_INT<16) {
                            // build notification for HoneyComb to ICS
                            mgr.notify(id, buildNotification().getNotification());
                        } else if (Build.VERSION.SDK_INT>15) {
                            // Notification for Jellybean and above
                            mgr.notify(id, buildNotification().build());
                        }
                    }
                    System.out.println("upside:response = "+response);
                    acknowledge(notificationIds);
                }
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                System.out.println("responseString = "+responseString);
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                System.out.println("tweetText = "+timeline);
            }
        });




    }
    public void acknowledge(JSONArray notificationIds){
        RequestParams params1 = new RequestParams();
        JSONObject obj1 = new JSONObject();
        try {
            obj1.put("notificationList", notificationIds);
            obj1.put("apiKey", options.getApiKey());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        params1.put("inputParam", obj1);
        System.out.println("upside:aknowledgement params = "+params1.toString());
        client.post(context,options.getAcknowledgeURL(),headers, params1,"application/x-www-form-urlencoded", new JsonHttpResponseHandler(){
            @Override
            public void onStart() {
                super.onStart();
                System.out.println("upside:Started aknowledgement");
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                System.out.println("upside:Succecc aknowledgement"+response.toString());
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                System.out.println("upside:Failed aknowledgement = "+responseString);
                super.onFailure(statusCode, headers, responseString, throwable);
            }
        });
    }
    /**
     * Fires ontrigger event.
     */
    private void fireTriggerEvent () {
        LocalNotification.fireEvent("trigger", options.getId(), options.getJSON());
    }
}