package com.gpow.androidkeylogger;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ActivityNotFoundException;
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
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
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
    private Button logsButton;
    private Button exportButton;
    private Button clearButton;

    private AdView mAdView;
    private AdRequest mAdRequest;
    private int REFRESH_RATE_IN_SECONDS = 5;
    private final Handler refreshHandler = new Handler();
    private final Runnable refreshRunnable = new RefreshRunnable();

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
        mAdView.resume();
        super.onRestart();
        checkForAccessibility();
        updateText();
    }

    @Override
    public void onPause() {
        // Pause the AdView.
        mAdView.pause();
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

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = findViewById(R.id.adView);
        mAdRequest = new AdRequest.Builder().build();
        mAdView.setAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();

                LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) swipeRefreshLayout.getLayoutParams();
                param.weight = 8.0f;
                swipeRefreshLayout.setLayoutParams(param);
                mAdView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);

                LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) swipeRefreshLayout.getLayoutParams();
                param.weight = 9.0f;
                swipeRefreshLayout.setLayoutParams(param);
                mAdView.setVisibility(View.GONE);

                refreshHandler.removeCallbacks(refreshRunnable);
                refreshHandler.postDelayed(refreshRunnable, REFRESH_RATE_IN_SECONDS * 1000);
            }
        });
        mAdView.loadAd(mAdRequest);
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

        // Setup Logs Button - opens the file manager to browse exported log files
        logsButton = (Button)findViewById(R.id.logsButton);
        logsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLogPath();
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

    private void openLogPath() {
        File logsDir = getExternalFilesDir(null);
        if (logsDir == null) {
            showToast("Log directory not available");
            return;
        }

        try {
            // Navigate to the logs directory in the system Files app.
            // getExternalFilesDir() always returns primary external storage,
            // so "primary" is the correct volume identifier here.
            String documentId = "primary:Android/data/" + getPackageName() + "/files";
            Uri dirUri = DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents", documentId);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(dirUri, DocumentsContract.Document.MIME_TYPE_DIR);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Fallback: open a generic file picker so the user can select a log file
            try {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("text/plain");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivity(Intent.createChooser(intent, "Select Log File"));
            } catch (ActivityNotFoundException ex) {
                showToast("No file manager found on this device");
            }
        }
    }


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

    private class RefreshRunnable implements Runnable {
        @Override
        public void run() {
            mAdView.loadAd(mAdRequest);
        }
    }
}
