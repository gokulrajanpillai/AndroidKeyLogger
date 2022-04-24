package com.gpow.androidkeylogger;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        updateText();
        Button refreshButton = (Button)findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateText();
            }
        });
    }

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
}
