package com.eaydin79.notes;

import android.content.Intent;
import android.widget.Toast;
import android.widget.Toolbar;

import java.io.File;
import java.io.IOException;

public class DeletedNotesActivity extends NoteListActivity {

    @Override
    protected File noteDirectory() {
        return new File(getFilesDir(), "trash");
    }

    @Override
    protected void onNoteLayoutClick(Note note) {
        Intent intent = new Intent(getApplicationContext(), note.getType() == Note.TYPE_TEXT ? TextNoteActivity.class: ChecklistNoteActivity.class);
        intent.putExtra("noteFile", noteDirectory().toString() + "/" + note.getId())
              .putExtra("fileType", NoteManager.TRASH_FILE)
              .putExtra("editable", false);
        startActivity(intent);
    }

    @Override
    protected void onRemoveNoteLayout(Note note, int command) throws IOException {
        switch (command) {
            case COMMAND_RESTORE: noteManager.undeleteNote(note); break;
            case COMMAND_DELETE: noteManager.permanentDeleteNote(note); break;
        }
    }

    @Override
    protected void onRemoveLayoutFinish(int removedCount, int command) {
        super.onRemoveLayoutFinish(removedCount, command);
        switch (command) {
            case COMMAND_RESTORE: Toast.makeText(this, removedCount > 1 ? R.string.toast_restore_multi : R.string.toast_restore_single, Toast.LENGTH_SHORT).show(); break;
            case COMMAND_DELETE: Toast.makeText(this, removedCount > 1 ? R.string.toast_delete_multi : R.string.toast_delete_single, Toast.LENGTH_SHORT).show(); break;
        }
    }

    @Override
    protected void onSelection(int selectedCount) {
        menu.findItem(R.id.action_restore).setVisible(selectedCount > 0);
        menu.findItem(R.id.action_delete).setVisible(selectedCount > 0);
    }

    @Override
    protected void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_deleted_notes);
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
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected int layoutResId() {
        return R.layout.activity_deleted_notes;
    }

}
