package com.eaydin79.notes;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Note {

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_CHECKLIST = 1;
    private final JSONObject jsonObject;
    private String initialName;
    private long initialTime;
    private String initialData;
    private JSONArray initialItems;

    public String getId() {
        try {
            return jsonObject.getString("id");
        } catch (JSONException e) {
            Log.e("Note", "getId: " + e);
        }
        return "";
    }

    private void setId(String id) {
        try {
            jsonObject.put("id", id);
        } catch (JSONException e) {
            Log.e("Note", "setId: " + e);
        }
    }

    public int getType() {
        try {
            return jsonObject.getInt("noteType");
        } catch (JSONException e) {
            Log.e("Note", "getType: " + e);
        }
        return 0;
    }

    public void setType(int noteType) {
        try {
            jsonObject.put("noteType", noteType);
        } catch (JSONException e) {
            Log.e("Note", "setType: " + e);
        }
    }

    public String getName() {
        try {
            return jsonObject.getString("name");
        } catch (JSONException e) {
            Log.e("Note", "getName: " + e);
        }
        return "";
    }

    public void setName(String name) {
        try {
            jsonObject.put("name", name);
        } catch (JSONException e) {
            Log.e("Note", "setName: " + e);
        }
    }

    public String getTimeStrFormatted() {
        return DateFormat.getDateTimeInstance().format(new Date(getTime()));
    }

    /**
     * Retrieves a backup file name using the {@code initialTime}
     * @return {@code String.valueOf(initialTime);}
     **/
    public String getBackupFileName() {
        return String.valueOf(initialTime);
    }

    public long getTime() {
        try {
            return jsonObject.getLong("time");
        } catch (JSONException e) {
            Log.e("Note", "getTime: " + e);
        }
        return System.currentTimeMillis();
    }

    public void setTime(long time) {
        try {
            jsonObject.put("time", time);
        } catch (JSONException e) {
            Log.e("Note", "setTime: " + e);
        }
    }

    public String getData() {
        try {
            return jsonObject.getString("data");
        } catch (JSONException e) {
            Log.e("Note", "getData: " + e);
        }
        return "";
    }

    public void setData(String data) {
        try {
            jsonObject.put("data", data);
        } catch (JSONException e) {
            Log.e("Note", "setData: " + e);
        }
    }

    private JSONArray getItemArray() {
        try {
            return jsonObject.getJSONArray("items");
        } catch (JSONException e) {
            Log.e("Note", "getItemArray: " + e);
        }
        return new JSONArray();
    }

    private void setItemArray(JSONArray jsonArray) {
        try {
            jsonObject.put("items", jsonArray);
        } catch (JSONException e) {
            Log.e("Note", "setItemArray: " + e);
        }

    }

    public ArrayList<NoteItem> getNoteItemList() {
        ArrayList<NoteItem> itemList = new ArrayList<>();
        try {
            JSONArray jsonArray = getItemArray();
            for (int i=0; i< jsonArray.length(); i++)
                itemList.add(new NoteItem(jsonArray.getJSONObject(i).toString()));
        } catch (JSONException e) {
            Log.e("Note", "getNoteItemList: " + e);
        }
        return itemList;
    }

    public void setItems(List<NoteItem> itemList) {
        JSONArray jsonArray = new JSONArray();
        for (NoteItem noteItem: itemList) jsonArray.put(noteItem);
        setItemArray(jsonArray);
    }

    public boolean isModified() {
        return !(initialName.equals(getName()) && initialData.equals(getData()) && initialItems.toString().equals(getItemArray().toString()));
    }

    public void save() {
        initialTime = getTime();
        initialName = getName();
        initialData = getData();
        initialItems = getItemArray();
    }

    public byte[] getBytes() {
        return jsonObject.toString().getBytes();
    }

    public Note(String name, String data, int noteType) {
        initialName = name;
        initialTime = System.currentTimeMillis();
        initialData = data;
        initialItems = new JSONArray();
        jsonObject = new JSONObject();
        setId(UUID.randomUUID().toString() + "-" + System.currentTimeMillis()); // better way to avoid duplication, also stores the creation time
        setName(name);
        setType(noteType);
        setTime(initialTime);
        setData(data);
        setItemArray(initialItems);
    }

    public Note(String json) throws JSONException {
        jsonObject = new JSONObject(json);
        initialName = getName();
        initialTime = getTime();
        initialData = getData();
        initialItems = getItemArray();
    }

}
