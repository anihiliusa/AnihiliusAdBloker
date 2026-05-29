package com.anihiliusa.xtube;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(5, 5, 8));
        getWindow().setNavigationBarColor(Color.rgb(5, 5, 8));

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 44, 32, 44);
        root.setBackgroundColor(Color.rgb(5, 5, 8));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Xtube v1.2.0");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25f);
        root.addView(title);

        addInfo(root, "Browser engine: GeckoView");
        addSwitch(root, "Clean page layer", true);
        addSwitch(root, "Network rules layer", true);
        addSwitch(root, "Phone traffic layer", false);
        addSwitch(root, "Background helper", true);

        Button close = new Button(this);
        close.setText("Close");
        close.setOnClickListener(v -> finish());
        root.addView(close, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(scroll);
    }

    private void addInfo(LinearLayout root, String text) {
        TextView note = new TextView(this);
        note.setText(text);
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(14f);
        note.setPadding(0, 18, 0, 22);
        root.addView(note);
    }

    private void addSwitch(LinearLayout root, String label, boolean enabled) {
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setTextColor(Color.WHITE);
        sw.setTextSize(16f);
        sw.setChecked(enabled);
        sw.setPadding(0, 14, 0, 14);
        root.addView(sw, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}
