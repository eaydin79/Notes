package com.eaydin79.notes;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputFilter;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.Toolbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ChecklistNoteActivity extends Activity {

    private EditText editNoteName;
    private LinearLayout layoutItems;
    private NoteManager noteManager;
    private Note note;
    private Menu menu;
    private final List<ItemLayout> itemLayoutList = new ArrayList<>();
    private ItemLayout selectedLayout;
    private int selectedCount = 0;
    private int fileType;
    private boolean editable;
    private String noteFile;
    private Intent intentNote;
    private boolean scrollToBottom = false;
    private static final int REQUEST_EXPORT = 100;
    private static final int REQUEST_IMPORT = 101;

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

    /** Parses lines like "[x] text" / "[ ] text". Lines without the prefix become unchecked items. */
    private NoteItem parseLine(String line) {
        String trimmed = line.trim();
        boolean checked = false;
        String text = trimmed;
        if (trimmed.length() >= 3 && trimmed.charAt(0) == '[' && trimmed.charAt(2) == ']') {
            char mark = trimmed.charAt(1);
            if (mark == 'x' || mark == 'X') {
                checked = true;
                text = trimmed.substring(3).trim();
            } else if (mark == ' ') {
                text = trimmed.substring(3).trim();
            }
        }
        if (text.length() > NotePreferences.MAX_ITEM_CHAR_COUNT)
            text = text.substring(0, NotePreferences.MAX_ITEM_CHAR_COUNT);
        return text.isEmpty() ? null : new NoteItem(text, checked);
    }

    private String buildExportText() {
        StringBuilder sb = new StringBuilder();
        for (ItemLayout itemLayout : itemLayoutList) {
            NoteItem item = itemLayout.getItem();
            sb.append(item.isChecked() ? "[x] " : "[ ] ")
                    .append(item.getText())
                    .append('\n');
        }
        return sb.toString();
    }

    private void exportNote() {
        String name = editNoteName.getText().toString().trim();
        if (name.isEmpty()) name = getString(R.string.new_list);
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");

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
                try (OutputStream out = getContentResolver().openOutputStream(data.getData(), "wt")) {
                    if (out == null) throw new IOException("Cannot open output stream");
                    out.write(buildExportText().getBytes(StandardCharsets.UTF_8));
                    Toast.makeText(this, R.string.toast_note_exported, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(this, R.string.toast_export_failed, Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_IMPORT:
                Uri uri = data.getData();
                List<NoteItem> itemList = new ArrayList<>();
                boolean limitReached = false;

                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new IOException("Cannot open input stream");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        NoteItem item = parseLine(line);
                        if (item == null) continue;
                        if (itemList.size() >= NotePreferences.MAX_ITEM_COUNT) {
                            limitReached = true;
                            break;
                        }
                        itemList.add(item);
                    }
                } catch (IOException e) {
                    Toast.makeText(this, R.string.toast_import_failed, Toast.LENGTH_SHORT).show();
                    return;
                }

                saveNote(getFileDisplayName(uri), itemList);
                Toast.makeText(this, limitReached ? R.string.toast_import_limit : R.string.toast_note_imported, Toast.LENGTH_SHORT).show();
                break;
        }

    }

    private void addItemLayouts() {
        for (NoteItem noteItem: note.getNoteItemList()) addItemLayout(noteItem);
        setSelection(0);
    }

    private void addItemLayout(NoteItem noteItem) {
        ItemLayout itemLayout = new ItemLayout(this);
        itemLayoutList.add(itemLayout);
        layoutItems.addView(itemLayout.getView());
        itemLayout.setItem(noteItem);
        if (!editable) return;

        itemLayout.getView().setOnLongClickListener(v -> {
            itemLayout.toggle();
            checkSelection();
            return true;
        });
        itemLayout.getView().setOnClickListener(v -> {
            if (selectedCount>0) {
                itemLayout.toggle();
                checkSelection();
            } else {
                itemLayout.setChecked(!itemLayout.isChecked());
            }
        });
    }

    private void removeLayout() {
        itemLayoutList.removeIf(
                itemLayout -> {
                    if (itemLayout.isSelected()) {
                        layoutItems.removeView(itemLayout.getView());
                        return true;
                    }
                    return false;
                }
        );
        setSelection(0);
    }

    private void setSelection(int selectedCount) {
        this.selectedCount = selectedCount;
        if (editable) {
            menu.findItem(R.id.action_delete).setVisible(selectedCount > 0);
            menu.findItem(R.id.action_add_new).setVisible(selectedCount == 0);
            menu.findItem(R.id.action_edit).setVisible(selectedCount == 1);
            menu.findItem(R.id.action_select_none).setVisible(true).setEnabled(selectedCount > 0);
            menu.findItem(R.id.action_select_all).setVisible(true).setEnabled(selectedCount != itemLayoutList.size());
            menu.findItem(R.id.action_import).setVisible(true);
        }
        menu.findItem(R.id.action_backups)
                .setVisible(fileType != NoteManager.BACKUP_FILE)
                .setEnabled(noteManager.hasBackup(note.getId()));
    }

    private void selectAllLayout() {
        itemLayoutList.forEach(ItemLayout::select);
        setSelection(itemLayoutList.size());
    }

    private void selectNoneLayout() {
        itemLayoutList.forEach(ItemLayout::deSelect);
        setSelection(0);
    }

    private void checkSelection() {
        int selectedCount = 0;
        for (ItemLayout itemLayout: itemLayoutList)
            if (itemLayout.isSelected()) {
                selectedLayout = itemLayout;
                selectedCount++;
            }
        setSelection(selectedCount);
    }

    private void saveNote(String noteName, List<NoteItem> itemList) {
        if (noteName.length() > 128) noteName = noteName.substring(0, 128);
        note.setName(noteName.trim().isEmpty() ? getString(R.string.new_text) : noteName.trim());
        note.setItems(itemList);
        if (note.isModified()) {
            noteFile = noteManager.saveNote(note);
            intentNote.putExtra("noteFile", noteFile);
        }
    }

    private void addNewItem(String text) {
        addItemLayout(new NoteItem(text.isEmpty() ? getString(R.string.new_item) : text, false));
        scrollToBottom = true;
        Toast.makeText(this, R.string.toast_item_added, Toast.LENGTH_SHORT).show();
    }

    private void addNewClick() {
        if (itemLayoutList.size() >= NotePreferences.MAX_ITEM_COUNT) {
            Toast.makeText(this, R.string.max_item_count_msg, Toast.LENGTH_SHORT).show();
            return;
        }
        EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(NotePreferences.MAX_ITEM_CHAR_COUNT)});
        editText.requestFocus();
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setHint(R.string.new_item);

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.add_item)
                .setIcon(R.drawable.ic_action_add_new)
                .setView(editText)
                .setCancelable(true)
                .setPositiveButton(R.string.btn_ok, (dialogInterface, i) -> addNewItem(editText.getText().toString().trim()))
                .setNeutralButton(getString(R.string.btn_add_another), null)
                .setNegativeButton(R.string.btn_cancel, (dialogInterface, i) -> dialogInterface.cancel())
                .create();

        alertDialog.setOnShowListener(dialog -> {

            editText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                    return true;
                }
                return false;
            });

            android.widget.Button neutralBtn = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (neutralBtn != null) {
                neutralBtn.setEnabled(itemLayoutList.size() < NotePreferences.MAX_ITEM_COUNT-1);
                neutralBtn.setOnClickListener(v -> {
                    addNewItem(editText.getText().toString().trim());
                    editText.setText("");
                    neutralBtn.setEnabled(itemLayoutList.size() < NotePreferences.MAX_ITEM_COUNT-1);
                });
            }

        });

        Window window = alertDialog.getWindow();
        if (window != null) window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();

    }

    private void editItemClick() {
        EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.requestFocus();
        editText.setText(selectedLayout.getItem().getText());
        editText.setSelection(editText.getText().length(), editText.getText().length());

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.menu_edit_item)
                .setIcon(R.drawable.ic_action_edit)
                .setView(editText)
                .setCancelable(true)
                .setPositiveButton(R.string.btn_ok, (dialogInterface, i) -> {
                    String text = editText.getText().toString().trim();
                    selectedLayout.setItem(new NoteItem(text.isEmpty() ? getString(R.string.new_item) : text, selectedLayout.getItem().isChecked()));
                    selectedLayout.deSelect();
                    checkSelection();
                    Toast.makeText(this, R.string.toast_item_changed, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.btn_cancel, (dialogInterface, i) -> dialogInterface.cancel())
                .create();

        alertDialog.setOnShowListener(dialog -> editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        }));

        alertDialog.setOnCancelListener(dialog -> {
            selectedLayout.deSelect();
            checkSelection();
        });

        Window window = alertDialog.getWindow();
        if (window != null) window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();

    }

    private void setScrollListener() {
        ScrollView scrollItems = findViewById(R.id.scrollItems);
        LayoutTransition transition = layoutItems.getLayoutTransition();
        if (transition != null) {
            transition.addTransitionListener(new LayoutTransition.TransitionListener() {
                @Override
                public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
                    if (scrollToBottom && transitionType == LayoutTransition.APPEARING) {
                        scrollToBottom = false;
                        scrollItems.fullScroll(View.FOCUS_DOWN);
                    }
                }
                @Override public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) { }
            });

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

    private void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_checklist_note);
        menu = toolbar.getMenu();
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_add_new) addNewClick();
            if (item.getItemId() == R.id.action_edit) editItemClick();
            if (item.getItemId() == R.id.action_delete) removeLayout();
            if (item.getItemId() == R.id.action_select_all) selectAllLayout();
            if (item.getItemId() == R.id.action_select_none) selectNoneLayout();
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
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadNote();
        itemLayoutList.clear();
        layoutItems.removeAllViews();
        addItemLayouts();
    }

    @Override
    protected void onPause() {
        if (selectedCount>0) setSelection(0);
        if (editable) {
            List<NoteItem> itemList = new ArrayList<>();
            for (ItemLayout itemLayout: itemLayoutList) itemList.add(itemLayout.getItem());
            saveNote(editNoteName.getText().toString(), itemList);
        }
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
        setContentView(R.layout.activity_checklist_note);
        findViewById(R.id.txtBack).setOnClickListener(v -> finish());
        editNoteName = findViewById(R.id.editName);
        noteManager = new NoteManager(this);

        intentNote = getIntent();
        noteFile = intentNote.getStringExtra("noteFile");
        loadNote();
        if (note == null) note = new Note(getString(R.string.new_list), "", Note.TYPE_CHECKLIST);

        fileType = intentNote.getIntExtra("fileType", NoteManager.NOTE_FILE);
        editable = intentNote.getBooleanExtra("editable", true);
        setToolbar();
        editNoteName.setEnabled(editable);
        layoutItems = findViewById(R.id.layout_items);
        setScrollListener();

        noteFile = null; // prevent loading note again in onResume

    }



}