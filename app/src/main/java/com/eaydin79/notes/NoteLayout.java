package com.eaydin79.notes;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NoteLayout extends LinearLayout {

    private TextView txtName;
    private TextView txtTime;
    private boolean selected = false;
    private Note note;
    private View view;

    public NoteLayout(Context context) {
        super(context);
        init(context);
    }

    public NoteLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NoteLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = layoutInflater.inflate(R.layout.layout_note, this, false);
        txtName = view.findViewById(R.id.txtName);
        txtTime = view.findViewById(R.id.txtTime);
    }

    public View getView() {
        return view;
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

    public void setNote(Note note) {
        this.note = note;
        txtName.setText(note.getName());
        txtTime.setText(note.getTimeStrFormatted());
    }

    public Note getNote() {
        return note;
    }


}

