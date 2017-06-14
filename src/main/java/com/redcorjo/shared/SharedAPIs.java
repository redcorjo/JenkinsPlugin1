package com.redcorjo.shared;

import org.json.JSONException;
import org.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by jordiredondo on 28/05/2017.
 */
public class SharedAPIs {

    //Logger log;

    public SharedAPIs(){
        System.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level","INFO");
        //log = LogManager.getLogger(SharedAPIs.class.getName());
    }

    public static boolean checkJson(String value){
        try {
            JSONObject myobject = new org.json.JSONObject(value);
            return true;
        } catch (JSONException e){
            return false;
        }
    }

    public void logger(String message){
        logger("INFO", message);
    }

    public void logger(String severity, String message){
        //log.info("log4j2 : " + message);
        System.out.println("SYSOUT : [" + severity + "] "  + message);
    }
}

