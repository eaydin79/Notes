package com.eaydin79.notes;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class NoteListActivity extends Activity {

    public static final int COMMAND_DELETE = 0;
    public static final int COMMAND_RESTORE = 1;
    protected Menu menu;
    private LinearLayout layoutNotes;
    protected final List<NoteLayout> noteLayoutList = new ArrayList<>();
    protected NoteManager noteManager;
    private int selectedCount = 0;

    protected abstract File noteDirectory();

    private void addNoteLayouts() {
        for (Note note: noteManager.getNoteList(noteDirectory())) addNoteLayout(note);
        setSelection(0);
    }

    protected abstract void onNoteLayoutClick(Note note);

    private void addNoteLayout(Note note) {
        NoteLayout noteLayout = new NoteLayout(this);
        noteLayoutList.add(noteLayout);
        layoutNotes.addView(noteLayout.getView());
        noteLayout.setNote(note);
        noteLayout.getView().setOnLongClickListener(v -> {
            noteLayout.toggle();
            checkSelection();
            return true;
        });
        noteLayout.getView().setOnClickListener(v -> {
            if (selectedCount>0) {
                noteLayout.toggle();
                checkSelection();
            } else onNoteLayoutClick(note);
        });
    }

    protected abstract void onRemoveNoteLayout(Note note, int command) throws IOException;

    protected void onRemoveLayoutFinish(int removedCount, int command) {}

    protected void removeNoteLayout(int command) {
        final String[] exception = {""};
        int count = noteLayoutList.size();
        noteLayoutList.removeIf(
                noteLayout -> {
                    if (noteLayout.isSelected()) {
                        try {
                            onRemoveNoteLayout(noteLayout.getNote(), command);
                            layoutNotes.removeView(noteLayout.getView());
                            return true;
                        } catch (Exception e) {
                            exception[0] = e.toString();
                            return false;
                        }
                    }
                    return false;
                }
        );
        setSelection(0);
        if (!exception[0].isEmpty()) Toast.makeText(this, exception[0], Toast.LENGTH_LONG).show();
        if (noteLayoutList.size() < count) onRemoveLayoutFinish(count - noteLayoutList.size(), command);
    }

    private void checkSelection() {
        int selectedCount = 0;
        for (NoteLayout noteLayout: noteLayoutList)
            if (noteLayout.isSelected()) selectedCount++;
        setSelection(selectedCount);
    }

    protected abstract void onSelection(int selectedCount);

    private void setSelection(int selectedCount) {
        this.selectedCount = selectedCount;
        menu.findItem(R.id.action_select_none).setEnabled(selectedCount > 0);
        menu.findItem(R.id.action_select_all).setEnabled(selectedCount != noteLayoutList.size());
        onSelection(selectedCount);
    }

    protected void selectAllNoteLayout() {
        noteLayoutList.forEach(NoteLayout::select);
        setSelection(noteLayoutList.size());
    }

    protected void selectNoneNoteLayout() {
        noteLayoutList.forEach(NoteLayout::deSelect);
        setSelection(0);
    }

    protected abstract void setToolbar();

    @Override
    protected void onResume() {
        super.onResume();
        noteLayoutList.clear();
        layoutNotes.removeAllViews();
        addNoteLayouts();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (selectedCount>0) setSelection(0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layoutResId());
        noteManager = new NoteManager(this);
        layoutNotes = findViewById(R.id.layout_notes);
        setToolbar();
        TextView txtBack = findViewById(R.id.txtBack);
        if (txtBack != null) txtBack.setOnClickListener(v -> finish());
    }

    protected abstract int layoutResId();

}
