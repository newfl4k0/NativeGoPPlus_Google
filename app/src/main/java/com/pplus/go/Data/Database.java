package com.pplus.go.Data;

import android.app.Activity;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.common.util.Hex;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;

import com.pplus.go.Utils.Utils;
import app.GoPPlus.R;


public final class Database {

    private static boolean Debug = false;

    public static void Delete(Activity activity, String key) {

        if (Debug) {
            Log.d("Database", "Delete");
        }

        if (activity != null) {
            PreferenceManager.getDefaultSharedPreferences(activity).edit().remove(key).commit();
        }
    }

    public static void Insert(Activity activity, String key, String value) {
        if (Debug) {
            Log.d("Database", "Insert");
            Log.d("key", key);
            Log.d("value", value);
        }

        if (activity != null) {
            PreferenceManager.getDefaultSharedPreferences(activity).edit().putString(key, value).commit();
        }
    }

    public static void Insert(Activity activity, String key, Integer value) {
        if (Debug) {
            Log.d("Database", "Insert");
            Log.d("key", key);
            Log.d("value", String.valueOf(value));
        }

        if (activity != null) {
            PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt(key, value).commit();
        }
    }

    public static void Insert(Activity activity, String key, Boolean value) {
        if (Debug) {
            Log.d("Database", "Insert");
            Log.d("key", key);
            Log.d("value", String.valueOf(value));
        }

        if (activity != null) {
            PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean(key, value).commit();
        }
    }

    public static String Select(Activity activity, String key) {
        if (activity == null) {
            return "";
        }

        return PreferenceManager.getDefaultSharedPreferences(activity).getString(key, "");
    }

    public static Integer SelectInt(Activity activity, String key) {
        if (activity == null) {
            return -1;
        }
        return PreferenceManager.getDefaultSharedPreferences(activity).getInt(key, -1);
    }

    public static Boolean SelectBoolean(Activity activity, String key) {
        if (activity == null) {
            return false;
        }

        return PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(key, false);
    }

    public static void Clear(Activity activity) {
        if (activity != null) {

        }
        PreferenceManager.getDefaultSharedPreferences(activity).getAll().clear();
        PreferenceManager.getDefaultSharedPreferences(activity).edit().clear().commit();
    }

    public static final String getEncryptedPassword(String s) {
        String password = null;
        MessageDigest mdEnc;

        try {
            mdEnc = MessageDigest.getInstance("MD5");
            mdEnc.update(s.getBytes(), 0, s.length());
            s = new BigInteger(1, mdEnc.digest()).toString(16);

            while (s.length() < 32) {
                s = "0" + s;
            }

            password = s;
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }

        return password;
    }

    public static final String getEncrypt(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            byte[] data = digest.digest(plainText.getBytes());
            return Hex.bytesToStringUppercase(data);
        } catch (Exception ex) {
            Log.e("getEncrypt", "Error");
            ex.printStackTrace();
            return null;
        }
    }

    public static final String encrypt64(String plainText, String key) throws Exception {
        String complete = plainText + key;
        return Base64.encodeToString(complete.getBytes("UTF-8"), Base64.DEFAULT);
    }

    private static final String getRandomId() {
        char[] possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@*-".toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i <= 30; i++) {
            char c = possible[random.nextInt(possible.length)];
            stringBuilder.append(c);
        }

        return stringBuilder.toString();
    }

    public static final String getAppId(Activity activity) {
        if (activity == null) {
            return "";
        }

        String key = activity.getApplicationContext().getString(R.string.db_appid);
        String appId = Select(activity, key);

        if( appId.isEmpty() ){
            appId = getEncrypt(UUID.randomUUID().toString());

            if (appId != null) {
                Insert(activity, key, appId);
            }
        }

        return appId;
    }

    public static final int getUserId(Activity activity) {
        if (activity == null) {
            return 0;
        }

        int id = 0;

        try {
            String userObjectString = Select(activity, Utils.getString(activity, R.string.db_user));

            if (userObjectString.isEmpty() == false) {
                JSONObject userObject = new JSONObject(userObjectString);

                if (userObject.has("id")) {
                    id = userObject.getInt("id");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return id;
    }


    public static final int getUserClientId(Activity activity) {
        if (activity == null) {
            return 0;
        }

        int id = 0;

        try {
            String userObjectString = Select(activity, Utils.getString(activity, R.string.db_user));

            if (userObjectString.isEmpty() == false) {
                JSONObject userObject = new JSONObject(userObjectString);

                if (userObject.has("cliente")) {
                    id = userObject.getInt("cliente");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return id;
    }

    public static final int getUserClientAf(Activity activity) {
        if (activity == null) {
            return 0;
        }

        int id = 0;

        try {
            String userObjectString = Select(activity, Utils.getString(activity, R.string.db_user));

            if (userObjectString.isEmpty() == false) {
                JSONObject userObject = new JSONObject(userObjectString);

                if (userObject.has("afiliado")) {
                    id = userObject.getInt("afiliado");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return id;
    }

    public static final String getEncryptedUserId(Activity activity) {
        if (activity == null) {
            return "";
        }

        int id = getUserId(activity);

        if (id == 0) {
            return "";
        } else {
            return getEncrypt(String.valueOf(id));
        }
    }

}
