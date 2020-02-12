package com.pplus.go.app.gopplus.Interfaces;

import com.android.volley.VolleyError;

import org.json.JSONObject;

public interface ResponseInterface {
    void Success(JSONObject response);
    void Error(VolleyError error);
    void Catch(String error);
}
