package edu.stanford.thingengine.engine.service;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by gcampagn on 7/13/16.
 */
public class AppInfo {
    public final String uniqueId;
    public final String name;
    public final String description;
    public final String icon;
    public final boolean isRunning;
    public final boolean isEnabled;
    public final String error;

    public AppInfo(JSONObject object) throws JSONException {
        uniqueId = object.getString("uniqueId");
        name = object.getString("name");
        description = object.getString("description");
        Object icon = object.get("icon");
        if (icon == JSONObject.NULL)
            this.icon = null;
        else
            this.icon = (String)icon;
        isRunning = object.getBoolean("isRunning");
        isEnabled = object.getBoolean("isEnabled");
        error = object.getString("error");
    }

    // for convenience of ArrayAdapter
    @Override
    public String toString() {
        return description;
    }
}
