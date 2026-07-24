package com.eaydin79.notes;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.Toolbar;

import java.io.File;
import java.io.IOException;

public class NoteHistoryActivity extends NoteListActivity{

    private String noteId;
    private int fileType;

    @Override
    protected File noteDirectory() {
        return new File(getFilesDir(), "history/" + noteId);
    }

    @Override
    protected void onNoteLayoutClick(Note note) {
        Intent intent = new Intent(getApplicationContext(), note.getType() == Note.TYPE_TEXT ? TextNoteActivity.class: ChecklistNoteActivity.class);
        intent.putExtra("noteFile", noteDirectory().toString() + "/" + note.getTime())
                .putExtra("fileType", NoteManager.BACKUP_FILE)
                .putExtra("editable", false);
        startActivity(intent);
    }

    @Override
    protected void onRemoveNoteLayout(Note note, int command) throws IOException {
        switch (command) {
            case COMMAND_RESTORE: noteManager.restoreBackupNote(note); break;
            case COMMAND_DELETE: noteManager.deleteBackup(note); break;
        }
    }

    @Override
    protected void onRemoveLayoutFinish(int removedCount, int command) {
        super.onRemoveLayoutFinish(removedCount, command);
        switch (command) {
            case COMMAND_RESTORE: Toast.makeText(this, R.string.toast_backup_restore, Toast.LENGTH_SHORT).show(); finish(); break;
            case COMMAND_DELETE: Toast.makeText(this, removedCount > 1 ? R.string.toast_backup_delete_multi : R.string.toast_backup_delete_single, Toast.LENGTH_SHORT).show(); break;
        }
    }

    @Override
    protected void onSelection(int selectedCount) {
        menu.findItem(R.id.action_restore).setVisible( selectedCount == 1 && fileType == NoteManager.NOTE_FILE );
        menu.findItem(R.id.action_delete).setVisible(selectedCount > 0);
    }

    @Override
    protected void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_note_history);
        menu = toolbar.getMenu();
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_select_all) selectAllNoteLayout();
            if (item.getItemId() == R.id.action_select_none) selectNoneNoteLayout();
            if (item.getItemId() == R.id.action_restore) removeNoteLayout(COMMAND_RESTORE);
            if (item.getItemId() == R.id.action_delete) new ConfirmDialog(this, ()-> removeNoteLayout(COMMAND_DELETE));
            return false;
        });
    }

    @Override
    protected int layoutResId() {
        return R.layout.activity_note_history;
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        noteId = intent.getStringExtra("noteId");
        if (noteId == null || noteId.isEmpty()) finish();
        fileType = intent.getIntExtra("fileType", NoteManager.NOTE_FILE);
    }

}
