package org.pocketworkstation.pckeyboard;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.view.Gravity;
import android.graphics.Typeface;

/**
 * Debug log viewer for Hangul composition.
 * Accessible from keyboard settings.
 */
public class HangulDebugActivity extends Activity {
    private TextView mLogView;
    private Handler mHandler;
    private boolean mAutoScroll = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);

        // Toolbar
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        Button toggleBtn = new Button(this);
        toggleBtn.setText(HangulDebugLog.isEnabled() ? "로깅 끄기" : "로깅 켜기");
        toggleBtn.setOnClickListener(v -> {
            boolean next = !HangulDebugLog.isEnabled();
            HangulDebugLog.setEnabled(next);
            toggleBtn.setText(next ? "로깅 끄기" : "로깅 켜기");
        });
        toolbar.addView(toggleBtn);

        Button clearBtn = new Button(this);
        clearBtn.setText("지우기");
        clearBtn.setOnClickListener(v -> {
            HangulDebugLog.clear();
            refreshLog();
        });
        toolbar.addView(clearBtn);

        Button refreshBtn = new Button(this);
        refreshBtn.setText("새로고침");
        refreshBtn.setOnClickListener(v -> refreshLog());
        toolbar.addView(refreshBtn);

        root.addView(toolbar);

        // Status
        TextView status = new TextView(this);
        status.setText("항목: " + HangulDebugLog.size() + " / 최대 200");
        status.setPadding(8, 8, 8, 8);
        root.addView(status);

        // Log view
        ScrollView scroll = new ScrollView(this);
        mLogView = new TextView(this);
        mLogView.setTypeface(Typeface.MONOSPACE);
        mLogView.setTextSize(11);
        mLogView.setPadding(8, 8, 8, 8);
        scroll.addView(mLogView);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1));

        setContentView(root);
        setTitle("디버그 로그");

        refreshLog();

        // Auto-refresh every 2 seconds
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    refreshLog();
                    status.setText("항목: " + HangulDebugLog.size() + " / 최대 200");
                    mHandler.postDelayed(this, 2000);
                }
            }
        }, 2000);
    }

    private void refreshLog() {
        String text = HangulDebugLog.getText();
        if (text.isEmpty()) {
            mLogView.setText("(로그 없음)\n\n로깅을 켜고 한글을 입력하면 여기에 표시됩니다.");
        } else {
            mLogView.setText(text);
        }
    }
}
