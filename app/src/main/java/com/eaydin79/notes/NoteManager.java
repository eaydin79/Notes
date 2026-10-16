package com.eaydin79.notes;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class NoteManager {

    public static final int NOTE_FILE = 0;
    public static final int BACKUP_FILE = 1;
    public static final int TRASH_FILE = 2;
    private final File notesDir;
    private final File historyDir;
    private final File trashDir;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public NoteManager(Context context) {
        File filesDir = context.getFilesDir();
        notesDir = new File(filesDir, "notes");
        historyDir = new File(filesDir, "history");
        trashDir = new File(filesDir, "trash");
        notesDir.mkdir();
        historyDir.mkdir();
        trashDir.mkdir();
    }

    public String saveNote(Note note) {
        note.setTime(System.currentTimeMillis());
        File noteFile = new File(notesDir, note.getId());
        try {
            addBackup(note);
            FileOutputStream fileOutputStream = new FileOutputStream(noteFile);
            fileOutputStream.write(note.getBytes());
            fileOutputStream.close();
            note.save();
            return noteFile.toString();
        } catch (IOException e) {
            Log.e("NotePreferences", "saveNote: " + e);
        }
        return null;
    }

    public Note getNote(File noteFile) {
        try {
            FileInputStream fileInputStream = new FileInputStream(noteFile);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ( (len = fileInputStream.read(buffer)) != -1 ) byteArrayOutputStream.write(buffer, 0, len);
            fileInputStream.close();
            return new Note(byteArrayOutputStream.toString());
        } catch (JSONException | IOException e) {
            Log.e("NotePreferences", "getNote: " + e);
        }
        return null;
    }

    public void restoreBackupNote(Note note) throws IOException {
        Note savedNote = getNote(new File(notesDir, note.getId()));
        Files.move( new File(notesDir, note.getId()).toPath(), new File(historyDir, note.getId() + "/" + savedNote.getBackupFileName()).toPath() );
        Files.move( new File(historyDir, note.getId() + "/" + note.getBackupFileName()).toPath(), new File(notesDir, note.getId()).toPath() );
    }

    public void deleteNote(Note note) throws IOException {
        cleanupTrash();
        Files.move( new File(notesDir, note.getId()).toPath(), new File(trashDir, note.getId()).toPath() );
    }

    public void undeleteNote(Note note) throws IOException {
        Files.move( new File(trashDir, note.getId()).toPath(), new File(notesDir, note.getId()).toPath() );
    }

    public void permanentDeleteNote(Note note) throws IOException {
        Files.delete( new File( trashDir, note.getId() ).toPath() );
        deleteHistory(note.getId());
    }

    public void deleteBackup(Note note) throws IOException {
        Files.delete( new File( historyDir, note.getId() + "/" + note.getBackupFileName() ).toPath() );
    }

    private void cleanupTrash() throws IOException {
        final String[] exception = {""};
        Executors.newSingleThreadExecutor().execute( ()-> {
            try {
                ArrayList<Note> noteList = getNoteList(trashDir);
                if (noteList.size() < NotePreferences.MAX_TRASH_COUNT) return;
                Note oldestNote = noteList.get(noteList.size()-1);
                Files.delete(new File(trashDir, oldestNote.getId()).toPath());
                deleteHistory(oldestNote.getId());
            } catch (IOException e) {
                exception[0] = e.toString();
            }
        });
        if (!exception[0].isEmpty()) throw new IOException(exception[0]);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void addBackup(Note note) throws IOException {

        File backupDir = new File(historyDir, note.getId());
        File noteFile = new File(notesDir, note.getId());
        if (!noteFile.exists()) { backupDir.mkdirs(); return; }
        Files.copy( noteFile.toPath(), new File(backupDir, note.getBackupFileName()).toPath(), StandardCopyOption.REPLACE_EXISTING );

        // delete oldest backup
        final String[] exception = {""};
        Executors.newSingleThreadExecutor().execute( ()-> {
            try {
                ArrayList<Note> noteList = getNoteList(backupDir);
                if (noteList.size() < NotePreferences.MAX_BACKUP_COUNT) return;
                Note oldestNote = noteList.get(noteList.size()-1);
                Files.delete(new File(backupDir, oldestNote.getBackupFileName()).toPath());
            } catch (IOException e) {
                exception[0] = e.toString();
            }
        });
        if (!exception[0].isEmpty()) throw new IOException(exception[0]);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteHistory(String noteId) {
        File noteHistoryDir = new File(historyDir, noteId);
        File[] files = noteHistoryDir.listFiles();
        if (files == null) return;
        for (File file: files) file.delete();
        noteHistoryDir.delete();
    }

    public ArrayList<Note> getNoteList(File directory) {
        ArrayList<Note> noteList = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files == null) return noteList;
        for (File file: files) noteList.add(getNote(file));
        noteList.sort(Comparator.comparingLong(Note::getTime).reversed());
        return noteList;
    }

    public boolean hasDeletedNotes() {
        try (Stream<Path> entries = Files.list(trashDir.toPath())) {
            return entries.findFirst().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean hasBackup(String noteId) {
        try (Stream<Path> entries = Files.list(new File(historyDir, noteId).toPath())) {
            return entries.findFirst().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

}
