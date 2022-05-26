package com.gpow.androidkeylogger;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

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
    private Button refreshButton;
    private Button exportButton;
    private Button clearButton;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        updateText();
        setupDisclaimer();
        setupView();

        checkForAccessibility();

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        checkForAccessibility();
        updateText();
    }

    void checkForAccessibility() {

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

        // Setup Refresh Button
        refreshButton = (Button)findViewById(R.id.refreshButton);
//        setOnTouchEffect(refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateText();
                Toast.makeText(MainActivity.this, "Content refreshed!",
                        Toast.LENGTH_SHORT).show();
            }
        });

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
        Toast.makeText(MainActivity.this, "File exported to " + filepath,
                Toast.LENGTH_LONG).show();
    }


    private void clearText() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("KeyLogger", "");
        editor.apply();
        updateText();
        Toast.makeText(MainActivity.this, "Text cleared!",
                Toast.LENGTH_SHORT).show();
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
}
