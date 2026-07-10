package com.control_center.intelligent.view.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class CellInformationLayout extends LinearLayout {
    public CellInformationLayout(Context context) { this(context, null); }
    public CellInformationLayout(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public CellInformationLayout(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }
    public void setCellInfo(String title, String voltage, String status) { }
    public void setData(String title, String voltage, String status) { }
    public void setCellStatue(int status) { }
    public void setCellVoltage(String value) { }
    public void setCellCurrent(String value) { }
}