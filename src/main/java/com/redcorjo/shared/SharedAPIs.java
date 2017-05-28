package com.redcorjo.shared;

import org.json.JSONException;
import org.json.JSONObject;
/**
 * Created by jordiredondo on 28/05/2017.
 */
public class SharedAPIs {

    public static boolean checkJson(String value){
        try {
            org.json.JSONObject myobject = new org.json.JSONObject(value);
            return true;
        } catch (JSONException e){
            return false;
        }
    }
}

