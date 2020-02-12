package com.pplus.go.app.gopplus.Interfaces;

import org.json.JSONObject;

public interface RequestInterface {
    void Success(JSONObject response);
    void Error(JSONObject error);
}
