package com.eaydin79.notes;

import android.content.Context;
import android.content.SharedPreferences;

public class NotePreferences {

    public static final int MAX_TRASH_COUNT = 1024;
    public static final int MAX_CHAR_COUNT = 1024;
    public static final int MAX_NOTE_COUNT = 1024;
    public static final int MAX_ITEM_CHAR_COUNT = 128;
    public static final int MAX_ITEM_COUNT = 128;
    public static final int MAX_BACKUP_COUNT = 128;

    private final SharedPreferences sharedPreferences;

    public NotePreferences(Context context) {
        sharedPreferences = context.getSharedPreferences("NotePreferences", Context.MODE_PRIVATE);
    }

    public void setTextSizeScale(int textSizeScale) {
        sharedPreferences.edit().putInt("textSizeScale", textSizeScale).apply();
    }

    public int getTextSizeScale() {
        return sharedPreferences.getInt("textSizeScale", 4);
    }

}
