package com.control_center.intelligent.view.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MyInformationView extends FrameLayout {
    private final TextView textView;

    public MyInformationView(Context context) {
        this(context, null);
    }

    public MyInformationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyInformationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        textView = new TextView(context);
        addView(textView);
    }

    public void setLeftText(String value) { textView.setText(value == null ? "" : value); }
    public void setRightText(String value) { }
    public void setRightTextValue(String value) { }
    public void setRightTextColor(int color) { }
    public void setRightTextVisibility(int visibility) { }
}