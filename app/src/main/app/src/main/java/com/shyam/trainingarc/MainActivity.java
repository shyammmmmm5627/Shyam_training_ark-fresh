
package com.shyam.trainingarc;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(24, 24, 24, 24);
        layout.setBackgroundColor(Color.rgb(15, 12, 25));

        TextView title = new TextView(this);
        title.setText("SHYAM TRAINING ARC");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);

        TextView message = new TextView(this);
        message.setText("DIAGNOSTIC TEST\n\nIf you can see this screen,\nthe basic Activity works.");
        message.setTextColor(Color.WHITE);
        message.setTextSize(18);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 32, 0, 0);

        layout.addView(title);
        layout.addView(message);

        setContentView(layout);
    }
}
