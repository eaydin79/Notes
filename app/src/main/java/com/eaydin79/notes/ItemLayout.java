package com.eaydin79.notes;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ItemLayout extends LinearLayout {

    private TextView txtItem;
    private boolean selected = false;
    private View view;
    private NoteItem noteItem;

    public ItemLayout(Context context) {
        super(context);
        init(context);
    }

    public ItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ItemLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = layoutInflater.inflate(R.layout.layout_item, this, false);
        txtItem = view.findViewById(R.id.txtItem);
    }

    public View getView() {
        return view;
    }

    public boolean isChecked() {
        return noteItem.isChecked();
    }

    public void setChecked(boolean checked) {
        noteItem.setChecked(checked);
        if (checked) {
            txtItem.setPaintFlags(txtItem.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            txtItem.setAlpha(0.6f);
        } else {
            txtItem.setPaintFlags(txtItem.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            txtItem.setAlpha(1f);
        }
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    public void select() {
        selected = true;
        view.setBackgroundResource(R.drawable.rounded_bg_selected);
    }

    public void deSelect() {
        selected = false;
        view.setBackgroundResource(R.drawable.ripple_background);
    }

    public void toggle() {
        if (selected) deSelect(); else select();
    }

    public void setItem(NoteItem noteItem) {
        this.noteItem = noteItem;
        txtItem.setText(noteItem.getText());
        setChecked(noteItem.isChecked());
    }

    public NoteItem getItem() {
        return noteItem;
    }
}
