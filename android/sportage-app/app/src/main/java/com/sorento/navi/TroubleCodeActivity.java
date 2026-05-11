package com.sorento.navi;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

public class TroubleCodeActivity extends Activity {
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppPrefs.obdEnabled(this)) {
            finish();
            return;
        }
        UiUtils.enterImmersive(this);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AppPrefs.obdEnabled(this)) {
            finish();
            return;
        }
        UiUtils.enterImmersive(this);
        refresh();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) UiUtils.enterImmersive(this);
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xff03070c);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(96), dp(32), dp(96), dp(90));
        root.addView(body, new FrameLayout.LayoutParams(-1, -1));

        TextView title = new TextView(this);
        title.setText("КОДЫ ОШИБОК");
        title.setTextColor(0xffffffff);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        body.addView(title, new LinearLayout.LayoutParams(-1, dp(58)));

        ListView list = new ListView(this);
        list.setDividerHeight(dp(2));
        list.setBackgroundColor(0x66061520);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                text.setTextColor(0xffffffff);
                text.setTextSize(22);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setPadding(dp(18), dp(12), dp(18), dp(12));
                return view;
            }
        };
        list.setAdapter(adapter);
        body.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));

        Button back = button("BACK");
        back.setText("");
        back.setBackgroundResource(R.drawable.back_btn);
        back.setOnClickListener(v -> finish());
        FrameLayout.LayoutParams backLp = new FrameLayout.LayoutParams(dp(60), dp(60), Gravity.RIGHT | Gravity.TOP);
        backLp.setMargins(0, dp(18), dp(22), 0);
        root.addView(back, backLp);

        Button clear = button("CLEAR DTC");
        clear.setText("");
        clear.setBackgroundResource(R.drawable.btn_clear_dtc);
        clear.setOnClickListener(v -> {
            ObdMonitor.clearDtc(this);
            refresh();
        });
        FrameLayout.LayoutParams clearLp = new FrameLayout.LayoutParams(dp(100), dp(80), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        clearLp.setMargins(0, 0, 0, dp(10));
        root.addView(clear, clearLp);

        setContentView(root);
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(0xffffffff);
        b.setTextSize(16);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setBackgroundColor(0xaa0b4f82);
        return b;
    }

    private void refresh() {
        if (adapter == null) return;
        adapter.clear();
        VehicleDisplayState.Snapshot s = VehicleDisplayState.snapshot();
        if (s.dtcCodes == null || s.dtcCodes.isEmpty()) {
            adapter.add("Ошибок нет");
        } else {
            adapter.addAll(s.dtcCodes);
        }
        adapter.notifyDataSetChanged();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
