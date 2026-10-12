package com.eaydin79.notes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.Toolbar;

import java.io.File;
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

    private static final int REQUEST_EXPORT = 100;

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
        if (requestCode != REQUEST_EXPORT || resultCode != RESULT_OK || data == null || data.getData() == null) return;

        try (OutputStream out = getContentResolver().openOutputStream(data.getData(), "wt")) {
            if (out == null) throw new IOException("Cannot open output stream");
            out.write(buildExportText().getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, R.string.toast_note_exported, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, R.string.toast_export_failed, Toast.LENGTH_SHORT).show();
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

    private void saveNote() {
        note.setName(editNoteName.getText().toString().trim().isEmpty() ? getString(R.string.new_list) : editNoteName.getText().toString().trim());
        List<NoteItem> itemList = new ArrayList<>();
        for (ItemLayout itemLayout: itemLayoutList) itemList.add(itemLayout.getItem());
        note.setItems(itemList);
        if (note.isModified()) {
            noteFile = noteManager.saveNote(note);
            intentNote.putExtra("noteFile", noteFile);
        }
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
                .setPositiveButton(R.string.btn_ok, (dialogInterface, i) -> {
                    String text = editText.getText().toString().trim();
                    addItemLayout(new NoteItem(text.isEmpty() ? getString(R.string.new_item) : text, false));
                    Toast.makeText(this, R.string.toast_item_added, Toast.LENGTH_SHORT).show();
                })
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
                    String text = editText.getText().toString().trim();
                    String itemName = text.isEmpty() ? getString(R.string.new_item) : text;
                    addItemLayout(new NoteItem(itemName, false));
                    Toast.makeText(this, R.string.toast_item_added, Toast.LENGTH_SHORT).show();
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
                    selectedLayout.setItem(new NoteItem(text.isEmpty() ? getString(R.string.new_item) : text, false));
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
        if (editable) saveNote();
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

        noteFile = null; // prevent loading note again in onResume

    }

}