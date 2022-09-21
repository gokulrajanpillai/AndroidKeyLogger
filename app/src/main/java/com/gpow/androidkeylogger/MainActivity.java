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
import android.widget.FrameLayout;
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
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.integration.IntegrationHelper;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.BannerListener;

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
    private Button exportButton;
    private Button clearButton;

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
        setupIronSourceAdView();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        checkForAccessibility();
        updateText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IronSource.onResume(this);
    }

    @Override
    public void onPause() {
        // Pause the AdView.
        super.onPause();
        IronSource.onResume(this);
    }

    @Override
    public void onDestroy() {
        // Destroy the AdView.
        super.onDestroy();
    }


    /* Setup AdView */
    private void setupIronSourceAdView() {
        /**
         *Ad Units should be in the type of IronSource.Ad_Unit.AdUnitName, example
         */
        IronSource.init(this, "168b70cc5", IronSource.AD_UNIT.OFFERWALL, IronSource.AD_UNIT.INTERSTITIAL, IronSource.AD_UNIT.REWARDED_VIDEO, IronSource.AD_UNIT.BANNER);
        IntegrationHelper.validateIntegration(this);
        final LinearLayout bannerContainer = findViewById(R.id.bannerContainer);
        IronSourceBannerLayout banner = IronSource.createBanner(this, ISBannerSize.BANNER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bannerContainer.addView(banner, 0, layoutParams);
        banner.setBannerListener(new BannerListener() {
            @Override
            public void onBannerAdLoaded() {
            // Called after a banner ad has been successfully loaded
            }
            @Override
            public void onBannerAdLoadFailed(IronSourceError error) {
            // Called after a banner has attempted to load an ad but failed.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bannerContainer.removeAllViews();
                    }
                });
            }
            @Override
            public void onBannerAdClicked() {
            // Called after a banner has been clicked.
            }
            @Override
            public void onBannerAdScreenPresented() {
            // Called when a banner is about to present a full screen content.
            }
            @Override
            public void onBannerAdScreenDismissed() {
            // Called after a full screen content has been dismissed
            }
            @Override
            public void onBannerAdLeftApplication() {
            // Called when a user would be taken out of the application context.
            }
        });
        IronSource.loadBanner(banner);
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


    private void updateText() {
        TextView textView = findViewById(R.id.logs);
        textView.setText("Logs: " + loadContents());
    }


    private String loadContents() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String logs = preferences.getString("KeyLogger", "No logs found");
        return logs;
    }

}
