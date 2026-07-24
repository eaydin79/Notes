package com.eaydin79.notes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.EditText;


public class NoteEditText extends EditText {

    private Paint paintMsgText;
    private Paint paintMsgBg;
    private String maxCharMsgShort;
    private String maxCharMsgLong;
    private RectF rectMsg;
    private boolean showMaxCharMsg = false;
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

        maxCharMsgShort = context.getString(R.string.max_char_count_msg_short);
        maxCharMsgLong = context.getString(R.string.max_char_count_msg_long);
        rectMsg = new RectF();

        paintMsgText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMsgText.setColor(Color.RED);
        paintMsgText.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics()));
        paintMsgText.setStyle(Paint.Style.FILL_AND_STROKE);

        paintMsgBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMsgBg.setColor(0xCCCCCCCC);
        paintMsgBg.setStyle(Paint.Style.FILL);

        notePreferences = new NotePreferences(context);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, notePreferences.getTextSizeScale()*2 + 12);

        setFilters(new InputFilter[]{
                new NotifyingLengthFilter( NotePreferences.MAX_CHAR_COUNT, () -> {
                    showMaxCharMsg = true;
                    invalidate();
                    postDelayed(()->showMaxCharMsg = false, 2000);
                } )
        });

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (showMaxCharMsg) {
            float textWidth = paintMsgText.measureText(maxCharMsgLong);
            float textHeight = paintMsgText.getFontMetrics().bottom - paintMsgText.getFontMetrics().top;
            rectMsg.set((getWidth() - textWidth) / 2f - 12, getScrollY() + getHeight() * 0.8f, (getWidth() - textWidth) / 2f + textWidth + 12, getScrollY() + getHeight() * 0.8f + textHeight * 3);
            canvas.drawRoundRect(rectMsg, 8, 8, paintMsgBg);
            canvas.drawText(maxCharMsgShort, rectMsg.left + 12, rectMsg.top + textHeight * 1.2f, paintMsgText);
            canvas.drawText(maxCharMsgLong, rectMsg.left + 12, rectMsg.top + textHeight * 2.2f, paintMsgText);
        }
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
