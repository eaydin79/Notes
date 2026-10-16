package com.eaydin79.notes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.EditText;

public class NoteEditText extends EditText {

    private NotePreferences notePreferences;

    public NoteEditText(Context context) {
        super(context);
        init(context);
    }

    public NoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NoteEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context) {

        notePreferences = new NotePreferences(context);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, notePreferences.getTextSizeScale()*2 + 12);
    }

    public void setTextSizeScale(int textSizeScale) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12+textSizeScale*2);
        notePreferences.setTextSizeScale(textSizeScale);
    }

    public int getTextSizeScale() {
        return notePreferences.getTextSizeScale();
    }

    public String getNoteData() {
        return getText().toString();
    }

}
