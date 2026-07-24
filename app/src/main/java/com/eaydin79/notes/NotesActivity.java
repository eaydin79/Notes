package com.eaydin79.notes;

import android.content.Intent;
import android.widget.Toast;
import android.widget.Toolbar;

import java.io.File;
import java.io.IOException;

// MainActivity
public class NotesActivity extends NoteListActivity {

    @Override
    protected File noteDirectory() {
        return new File(getFilesDir(),"notes");
    }

    @Override
    protected void onNoteLayoutClick(Note note) {
        Intent intent = new Intent(getApplicationContext(), note.getType() == Note.TYPE_TEXT ? TextNoteActivity.class: ChecklistNoteActivity.class);
        intent.putExtra("noteFile", noteDirectory().toString() + "/" + note.getId())
                .putExtra("fileType", NoteManager.NOTE_FILE)
                .putExtra("editable", true);
        startActivity(intent);
    }

    @Override
    protected void onRemoveNoteLayout(Note note, int command) throws IOException {
        noteManager.deleteNote(note);
    }

    @Override
    protected void onSelection(int selectedCount) {
        menu.findItem(R.id.action_delete).setVisible(selectedCount > 0);
        menu.findItem(R.id.action_add_new).setVisible(selectedCount == 0);
        menu.findItem(R.id.action_deleted_notes).setEnabled(noteManager.hasDeletedNotes());
    }

    private void addNewNote(int noteType) {
        if (noteLayoutList.size() >= NotePreferences.MAX_NOTE_COUNT) {
            Toast.makeText(this, getString(R.string.toast_max_note_count), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(getApplicationContext(), noteType == Note.TYPE_TEXT ? TextNoteActivity.class: ChecklistNoteActivity.class);
        intent.putExtra("noteFile", "")
                .putExtra("fileType", NoteManager.NOTE_FILE)
                .putExtra("editable", true);
        startActivity(intent);
    }

    @Override
    protected void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_notes);
        menu = toolbar.getMenu();
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_new_text) addNewNote(Note.TYPE_TEXT);
            if (item.getItemId() == R.id.action_new_checklist) addNewNote(Note.TYPE_CHECKLIST);
            if (item.getItemId() == R.id.action_delete) removeNoteLayout(COMMAND_DELETE);
            if (item.getItemId() == R.id.action_select_all) selectAllNoteLayout();
            if (item.getItemId() == R.id.action_select_none) selectNoneNoteLayout();
            if (item.getItemId() == R.id.action_deleted_notes) startActivity(new Intent(getApplicationContext(), DeletedNotesActivity.class));
            return false;
        });
    }

    @Override
    protected int layoutResId() {
        return R.layout.activity_notes;
    }

}
