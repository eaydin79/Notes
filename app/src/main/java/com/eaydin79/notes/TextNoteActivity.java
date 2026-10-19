package com.eaydin79.notes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Menu;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toolbar;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.StringRes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class TextNoteActivity extends Activity {

    private static final int REQUEST_EXPORT = 100;
    private static final int REQUEST_IMPORT = 101;
    private EditText editNoteName;
    private NoteEditText noteEditText;
    private NoteManager noteManager;
    private Note note;
    private boolean editable;
    private int fileType;
    private Menu menu;
    private String noteFile;
    private Intent intentNote;
    private SeekBar seekTextSize;
    private LinearLayout layoutTextSize;
    private LinearLayout layoutMsg;
    private TextView txtMsg;
    private boolean msgVisible = false;

    private String getFileDisplayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) return "";
            String name = cursor.getString(0);
            if (name == null) return "";
            int dot = name.lastIndexOf('.'); // strip the extension, e.g. "shopping.txt" -> "shopping"
            if (dot > 0) name = name.substring(0, dot);
            return name.trim();
        } catch (Exception ignored) {}
        return "";
    }

    private void importNote() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    private void exportNote() {
        String name = editNoteName.getText().toString().trim();
        if (name.isEmpty()) name = getString(R.string.new_text);
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_"); // keep the file name valid

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, name + ".txt");
        startActivityForResult(intent, REQUEST_EXPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        switch (requestCode) {
            case REQUEST_EXPORT:
                // "wt" = write + truncate, so overwriting an existing file doesn't leave old trailing text
                try (OutputStream out = getContentResolver().openOutputStream(data.getData(), "wt")) {
                    if (out == null) throw new IOException("Cannot open output stream");
                    out.write(noteEditText.getNoteData().getBytes(StandardCharsets.UTF_8));
                    showMessageLayout(R.string.toast_note_exported);
                } catch (IOException e) {
                    showMessageLayout(R.string.toast_export_failed);
                }
                break;
            case REQUEST_IMPORT:
                Uri uri = data.getData();
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new IOException("Cannot open input stream");
                    InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                    char[] buffer = new char[NotePreferences.MAX_CHAR_COUNT + 1]; // extra char to detect and show "limit reached" message.
                    int total = 0, read;
                    while (total < buffer.length && (read = reader.read(buffer, total, buffer.length - total)) != -1) total += read;
                    showMessageLayout(total > NotePreferences.MAX_CHAR_COUNT ? R.string.msg_max_char_count : R.string.msg_import);
                    saveNote(getFileDisplayName(uri), new String(buffer, 0, Math.min(total, NotePreferences.MAX_CHAR_COUNT)));
                } catch (IOException e) {
                    showMessageLayout(R.string.msg_import_fail);
                }
                break;
        }
    }

    private void saveNote(String noteName, String noteData) {
        if (noteName.length() > 128) noteName = noteName.substring(0, 128);
        note.setName(noteName.trim().isEmpty() ? getString(R.string.new_text) : noteName.trim());
        note.setData(noteData);
        if (note.isModified()) {
            noteFile = noteManager.saveNote(note);
            intentNote.putExtra("noteFile", noteFile);
        }
    }

    private void showHistory() {
        Intent intent = new Intent(getApplicationContext(), NoteHistoryActivity.class);
        intent.putExtra("noteId", note.getId())
                .putExtra("fileType", fileType);
        startActivity(intent);
        switch (fileType) {
            case NoteManager.NOTE_FILE: noteFile = getFilesDir() + "/notes/" + note.getId(); break;
            case NoteManager.BACKUP_FILE: noteFile = getFilesDir() + "/history/" + note.getId() + "/" + note.getBackupFileName();
            case NoteManager.TRASH_FILE: noteFile = getFilesDir() + "/trash/" + note.getId(); break;
        }
    }

    private void showMessageLayout(@StringRes int msg) {
        msgVisible = true;
        txtMsg.setText(msg);
        layoutMsg.setVisibility(LinearLayout.VISIBLE);
        layoutMsg.setAlpha(1f);
        layoutMsg.animate()
                .alpha(0f)
                .setStartDelay(1000)
                .setDuration(1000)
                .withEndAction( () -> {
                    layoutMsg.setVisibility(LinearLayout.GONE);
                    msgVisible = false;
                } )
                .start();
    }


    private void showTextSizeLayout() {
        layoutTextSize.setVisibility(LinearLayout.VISIBLE);
        layoutTextSize.setAlpha(1f);
        layoutTextSize.animate()
                .alpha(0f)
                .setStartDelay(3000)
                .setDuration(2000)
                .withEndAction( () -> layoutTextSize.setVisibility(LinearLayout.GONE) )
                .start();
    }

    private void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_text_note);
        menu = toolbar.getMenu();
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_text_size) showTextSizeLayout();
            if (item.getItemId() == R.id.action_backups) showHistory();
            if (item.getItemId() == R.id.action_export) exportNote();
            if (item.getItemId() == R.id.action_import) importNote();
            return false;
        });
    }

    private void loadNote() {
        if (noteFile == null || noteFile.isEmpty()) return;
        note = noteManager.getNote(new File(noteFile));
        if (note == null) return;
        editNoteName.setText(note.getName());
        noteEditText.setText(note.getData());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNote();
        menu.findItem(R.id.action_import).setVisible(editable);
        menu.findItem(R.id.action_backups)
            .setVisible(fileType != NoteManager.BACKUP_FILE)
            .setEnabled(noteManager.hasBackup(note.getId()));
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (!msgVisible) layoutMsg.setVisibility(LinearLayout.GONE);
        layoutTextSize.setVisibility(LinearLayout.GONE);
        seekTextSize.setProgress(noteEditText.getTextSizeScale());
    }

    @Override
    protected void onPause() {
        if (editable) saveNote(editNoteName.getText().toString(), noteEditText.getNoteData());
        super.onPause();
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_note);
        findViewById(R.id.txtBack).setOnClickListener(v -> finish());

        noteManager = new NoteManager(this);
        editNoteName = findViewById(R.id.editName);
        noteEditText = findViewById(R.id.noteEditText);
        seekTextSize = findViewById(R.id.seekTextSize);
        layoutTextSize = findViewById(R.id.layout_text_size);

        seekTextSize.setProgress(noteEditText.getTextSizeScale());
        seekTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                noteEditText.setTextSizeScale(progress);
                if (!seekBar.isShown()) return;
                layoutTextSize.animate().cancel();
                showTextSizeLayout();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        findViewById(R.id.txtIncreaseSize).setOnClickListener(v -> seekTextSize.incrementProgressBy(1));
        findViewById(R.id.txtDecreaseSize).setOnClickListener(v -> seekTextSize.incrementProgressBy(-1));

        intentNote = getIntent();
        noteFile = intentNote.getStringExtra("noteFile");
        loadNote();
        if (note == null) note = new Note(getString(R.string.new_text), "", Note.TYPE_TEXT);

        fileType = intentNote.getIntExtra("fileType", NoteManager.NOTE_FILE);
        editable = intentNote.getBooleanExtra("editable", true);
        setToolbar();
        editNoteName.setEnabled(editable);
        noteEditText.setEnabled(editable);

        layoutMsg = findViewById(R.id.layout_msg);
        txtMsg = findViewById(R.id.txtMsg);
        if (editable) noteEditText.setFilters(new InputFilter[] {new NotifyingLengthFilter(NotePreferences.MAX_CHAR_COUNT, () -> showMessageLayout(R.string.msg_max_char_count))});

        noteFile = null; // prevent loading the note again in onResume

    }

}