package com.eaydin79.notes;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class NoteItem extends JSONObject {

    public String getText() {
        try {
            return getString("text");
        } catch (JSONException e) {
            Log.e("NoteItem", "getText: " + e);
        }
        return "";
    }

    public void setText(String text) {
        try {
            put("text", text);
        } catch (JSONException e) {
            Log.e("NoteItem", "setText: " + e);
        }
    }

    public boolean isChecked() {
        try {
            return getBoolean("checked");
        } catch (JSONException e) {
            Log.e("NoteItem", "isChecked: " + e);
        }
        return false;
    }

    public void setChecked(boolean checked) {
        try {
            put("checked", checked);
        } catch (JSONException e) {
            Log.e("NoteItem", "setChecked: " + e);
        }
    }

    public NoteItem(String text, boolean checked) {
        super();
        setText(text);
        setChecked(checked);
    }

    public NoteItem(String json) throws JSONException {
        super(json);
    }

}
