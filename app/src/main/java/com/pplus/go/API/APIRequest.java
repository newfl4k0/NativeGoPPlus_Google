package com.pplus.go.API;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import com.pplus.go.Data.Database;
import com.pplus.go.Utils.Utils;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;
import com.pplus.go.app.gopplus.Interfaces.ResponseInterface;
import com.pplus.go.app.gopplus.R;

public class APIRequest {
    private static RequestQueue queue;
    private static HashMap<String, String> headers = new HashMap<>();
    @SuppressLint("StaticFieldLeak")
    private static Activity currentActivity;

    public static void setQueue(Activity activity){
        currentActivity = activity;
        queue = Volley.newRequestQueue(activity.getApplicationContext());
        headers.put("appid", Database.getAppId(activity));
        headers.put("userid", Database.getEncryptedUserId(currentActivity));
    }

    public static void cancelQueue(){
        queue.cancelAll(R.string.app_name);
    }

    private static void getRequest(String url, final ResponseInterface listener) {
        try {
            headers.put("appid", Database.getAppId(currentActivity));
            headers.put("userid", Database.getEncryptedUserId(currentActivity));
            Log.d("[GET]", url);
            Log.d("HEADERS", headers.toString());

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d("RESPONSE", response.toString());
                    listener.Success(response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("ERROR", error.toString());
                    listener.Error(error);
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    return headers;
                }
            };

            jsonObjectRequest.setTag(R.string.app_name);
            queue.add(jsonObjectRequest);
        } catch (Exception e) {
            e.printStackTrace();
            listener.Catch(e.getMessage());
        }
    }

    public static void postRequest(String url, JSONObject objectToSend, final ResponseInterface listener) {
        try {
            headers.put("appid", Database.getAppId(currentActivity));
            headers.put("userid", Database.getEncryptedUserId(currentActivity));
            Log.d("[POST]", url);
            Log.d("DATA", objectToSend.toString());
            Log.d("HEADERS", headers.toString());

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, objectToSend, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d("RESPONSE", response.toString());
                    listener.Success(response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("ERROR", error.toString());
                    listener.Error(error);
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    return headers;
                }
            };

            jsonObjectRequest.setTag(R.string.app_name);
            queue.add(jsonObjectRequest);
        } catch (Exception e) {
            listener.Catch(e.getMessage());
        }
    }

    /**
     * API Endpoints
     */
    public static void Sync( final RequestInterface listener ) {
        final JSONObject errorResponse = new JSONObject();

        try {
            errorResponse.put("status", false);
            errorResponse.put("message", R.string.default_error);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (Database.getUserId(currentActivity) != 0) {
            getRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "sync", new ResponseInterface() {
                @Override
                public void Success(JSONObject response) {
                    listener.Success(response);
                }

                @Override
                public void Error(VolleyError error) {
                    try {
                        errorResponse.put("code", error.networkResponse.statusCode);
                        errorResponse.put("message", error.getMessage());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    listener.Error(errorResponse);
                }

                @Override
                public void Catch(String error) {
                    try {
                        errorResponse.put("message", error);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    listener.Error(errorResponse);
                }
            });
        } else {
            listener.Error(errorResponse);
        }
    }

    public static void Login(String email, String password, final RequestInterface listener) {
        JSONObject objectToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();

        try {
            objectToSend.put("email", email);
            objectToSend.put("password",  Database.getEncryptedPassword(password));

            errorResponse.put("status", false);
            errorResponse.put("message", R.string.default_error);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        postRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "login", objectToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {
                try {
                    if (response.getBoolean("status")) {
                        listener.Success(response);
                    } else {
                        listener.Error(response);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.Error(errorResponse);
                }
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("code", error.networkResponse.statusCode);
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error){
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }

    public static void History(final RequestInterface listener) {
        int id =  Database.getUserId(currentActivity);
        final JSONObject errorResponse = new JSONObject();

        try {
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        getRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "history?id=" + String.valueOf(id), new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {

                if (response.has("data")) {
                    listener.Success(response);
                } else {
                    listener.Error(errorResponse);
                }
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("code", error.networkResponse.statusCode);
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }

    public static void Password(String password, String newPassword, final RequestInterface listener) {
        JSONObject dataToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();
        int id = Database.getUserId(currentActivity);

        try {
            password = Database.getEncryptedPassword(password);
            newPassword = Database.getEncryptedPassword(newPassword);

            dataToSend.put("id", id);
            dataToSend.put("password", password);
            dataToSend.put("newpassword", newPassword);

            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "password", dataToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {
                try {
                    if (response.has("status") && response.getBoolean("status") == true) {
                        listener.Success(response);
                    } else {
                        listener.Error(response);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.Error(errorResponse);
                }
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });

    }

    public static void Profile(String oldEmail, String name, String phone, String email, String birthDate, final RequestInterface listener) {
        JSONObject dataToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();
        int id = Database.getUserId(currentActivity);

        try {
            dataToSend.put("id", id);
            dataToSend.put("oldemail", oldEmail);
            dataToSend.put("name", name);
            dataToSend.put("phone", phone);
            dataToSend.put("email", email);
            dataToSend.put("birthDate", birthDate);

            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "updateprofile", dataToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {
                try {
                    if (response.has("status") && response.getBoolean("status") == true) {
                        Database.Insert(currentActivity, Utils.getString(currentActivity, R.string.db_user), response.toString());
                        listener.Success(response);
                    } else {
                        listener.Error(response);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.Error(errorResponse);
                }
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }

    public static void UploadProfileImage(String image,  final RequestInterface listener) {
        JSONObject dataToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();

        try {
            int id = Database.getUserId(currentActivity);
            dataToSend.put("id", id);
            dataToSend.put("theImage", image);

            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "upload-profile-image", dataToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {
                try {
                    if (response.has("status") && response.getBoolean("status") == true) {
                        listener.Success(response);
                    } else {
                        listener.Error(response);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.Error(errorResponse);
                }
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });

    }

    public static void ValidateCode(final String code, int clientUserId, final RequestInterface listener) {
        JSONObject dataToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();

        try {
            dataToSend.put("code", code);
            dataToSend.put("id_cl", clientUserId);

            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "validate-code", dataToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {

                try {
                    if (response.has("status") && response.getBoolean("status") == true) {
                        JSONObject codeObject = new JSONObject(response.toString());

                        codeObject.put("code", code);

                        if (response.has("cid")) {
                            codeObject.put("typecode", "code");
                            codeObject.put("id", response.get("cid"));
                        }

                        if (response.has("uid")) {
                            codeObject.put("typecode", "user_code");
                            codeObject.put("id", response.get("uid"));
                        }

                        listener.Success(codeObject);
                    } else {
                        listener.Error(response);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.Error(errorResponse);
                }

            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });

    }

    public static void GetPaymentCards(int clientUserId, final RequestInterface listener) {
        final JSONObject errorResponse = new JSONObject();

        try {
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        getRequest(Utils.getString(currentActivity, R.string.apiPayment) + "list-?id=" + String.valueOf(clientUserId), new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {
                try {
                    if (response.has("data")) {
                        listener.Success(response);
                    } else {
                        errorResponse.put("message", "No tienes tarjetas");
                        listener.Error(errorResponse);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.Error(errorResponse);
                }
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);

            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);

            }
        });

    }


    public static void RemovePaymentCard(int clientUserId, int cardId, final RequestInterface listener) {
        JSONObject dataToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();

        try {
            dataToSend.put("id", clientUserId);
            dataToSend.put("card_id", cardId);
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(Utils.getString(currentActivity, R.string.apiPayment) + "remove", dataToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {
                listener.Success(response);
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }


    public static void GetChat(int serviceId, int driverId, final RequestInterface listener) {
        final JSONObject errorResponse = new JSONObject();

        try {
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));

        } catch (JSONException e) {
            e.printStackTrace();
        }


        getRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "chat?id=" + String.valueOf(serviceId) + "&id_chofer=" + String.valueOf(driverId), new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {
                listener.Success(response);
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }

    public static void sendChat(int serviceId, int driverId, String message, final RequestInterface listener) {
        JSONObject dataToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();

        try {
            dataToSend.put("id", serviceId);
            dataToSend.put("id_chofer", driverId);
            dataToSend.put("mensaje", message);
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "chat-message", dataToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {
                listener.Success(response);
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }

    public static void GetVehicles(String latitude, String longitude, int type, final  RequestInterface listener) {
        final JSONObject errorResponse = new JSONObject();
        String url = "?latitude=" + latitude + "&longitude=" + longitude + "&type=" + String.valueOf(type);

        try {
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        getRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "vehicles" + url, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {
                listener.Success(response);
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }

    public static void GetActiveService(final RequestInterface listener) {
        int id = Database.getUserId(currentActivity);
        final JSONObject errorResponse = new JSONObject();

        try {
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (id != 0) {
            getRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "get-service?id=" + String.valueOf(id), new ResponseInterface() {
                @Override
                public void Success(JSONObject response) {
                    try {
                        if (response.has("data") && response.has("status") &&  response.getBoolean("status") == true) {
                            listener.Success(response);
                        } else {
                            errorResponse.put("message", "No existe servicio activo");
                            listener.Error(errorResponse);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        listener.Error(errorResponse);
                    }
                }

                @Override
                public void Error(VolleyError error) {
                    try {
                        errorResponse.put("code", error.networkResponse.statusCode);
                        errorResponse.put("message", error.getMessage());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    listener.Error(errorResponse);
                }

                @Override
                public void Catch(String error) {
                    try {
                        errorResponse.put("message", error);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    listener.Error(errorResponse);
                }
            });
        } else {
            listener.Error(errorResponse);
        }
    }

    public static void GetFinishedService(final RequestInterface listener) {
        int id = Database.getUserId(currentActivity);
        final JSONObject errorResponse = new JSONObject();

        try {
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (id != 0) {
            getRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "finished?id=" + String.valueOf(id), new ResponseInterface() {
                @Override
                public void Success(JSONObject response) {
                    try {
                        if (response.has("status") &&  response.getBoolean("status") == true) {
                            listener.Success(response);
                        } else {
                            errorResponse.put("message", "No existe servicio finalizado");
                            listener.Error(errorResponse);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        listener.Error(errorResponse);
                    }
                }

                @Override
                public void Error(VolleyError error) {
                    try {
                        errorResponse.put("code", error.networkResponse.statusCode);
                        errorResponse.put("message", error.getMessage());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    listener.Error(errorResponse);
                }

                @Override
                public void Catch(String error) {
                    try {
                        errorResponse.put("message", error);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    listener.Error(errorResponse);
                }
            });
        } else {
            listener.Error(errorResponse);
        }
    }

    public static void SendRate(int id, int rate, String observation, final RequestInterface listener) {
        JSONObject dataToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();

        try {
            dataToSend.put("id", id);
            dataToSend.put("rate", rate);
            dataToSend.put("obs", observation);
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "rate", dataToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {
                listener.Success(response);
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }

    public static void PushNotification(String fcm, final RequestInterface listener) {
        JSONObject dataToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();

        try {
            dataToSend.put("id", Database.getUserId(currentActivity));
            dataToSend.put("token", fcm);
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "token", dataToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {

                try {
                    if (response.has("status") && response.getBoolean("status") == true) {
                        listener.Success(response);
                    } else {
                        listener.Error(response);
                    }
                } catch (JSONException e){
                    listener.Error(errorResponse);
                }
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }

    public static void Activate(int id, String code, final RequestInterface listener) {
        JSONObject dataToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();

        try {
            dataToSend.put("id", id);
            dataToSend.put("token", code);
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "activate", dataToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {

                try {
                    if (response.has("status") && response.getBoolean("status") == true) {
                        listener.Success(response);
                    } else {
                        listener.Error(response);
                    }
                } catch (JSONException e){
                    listener.Error(errorResponse);
                }
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }

    public static void SendActivationCode(int id, final RequestInterface listener) {

        JSONObject dataToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();

        try {
            dataToSend.put("id", id);
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "resend-code", dataToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {

                try {
                    if (response.has("status") && response.getBoolean("status") == true) {
                        listener.Success(response);
                    } else {
                        listener.Error(response);
                    }
                } catch (JSONException e){
                    listener.Error(errorResponse);
                }
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }

    public static void ResetPassword(String email, String token, String password, final RequestInterface listener) {
        JSONObject dataToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();

        try {
            password = Database.getEncryptedPassword(password);

            dataToSend.put("email", email);
            dataToSend.put("token", token);
            dataToSend.put("pass", password);

            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "recover", dataToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {

                try {
                    if (response.has("status") && response.getBoolean("status") == true) {
                        listener.Success(response);
                    } else {
                        listener.Error(response);
                    }
                } catch (JSONException e){
                    listener.Error(errorResponse);
                }
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }

    public static void SendRecoveryEmail(String email, final RequestInterface listener) {
        JSONObject dataToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();

        try {
            dataToSend.put("email", email);
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "forgot", dataToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {

                try {
                    if (response.has("status") && response.getBoolean("status") == true) {
                        listener.Success(response);
                    } else {
                        listener.Error(response);
                    }
                } catch (JSONException e){
                    listener.Error(errorResponse);
                }
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }

    public static void Register(String name, String email, String phone, String password, String birthDate, String facebookId, final RequestInterface listener) {
        JSONObject dataToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();

        try {
            password = Database.getEncryptedPassword(password);
            dataToSend.put("birthDate", birthDate);
            dataToSend.put("name", name);
            dataToSend.put("phone", phone);
            dataToSend.put("email", email);
            dataToSend.put("password", password);
            dataToSend.put("fbid", facebookId);

            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "register", dataToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {

                Log.v("register", response.toString());

                try {
                    if (response.has("status") && response.getBoolean("status") == true) {
                        listener.Success(response);
                    } else {
                        listener.Error(response);
                    }
                } catch (JSONException e){
                    listener.Error(errorResponse);
                }
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }

    public static void SetAppId(int id, String appId,  final RequestInterface listener) {
        JSONObject dataToSend = new JSONObject();
        final JSONObject errorResponse = new JSONObject();

        try {

            dataToSend.put("id", id);
            dataToSend.put("appid", appId);

            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(Utils.getString(currentActivity, R.string.apiEndpoint) + "set-appid", dataToSend, new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {

                try {
                    if (response.has("status") && response.getBoolean("status") == true) {
                        listener.Success(response);
                    } else {
                        listener.Error(response);
                    }
                } catch (JSONException e){
                    listener.Error(errorResponse);
                }
            }

            @Override
            public void Error(VolleyError error) {
                try {
                    errorResponse.put("message", error.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                try {
                    errorResponse.put("message", error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.Error(errorResponse);
            }
        });
    }

    public static void DistanceMatrix(String origin, String destination, final RequestInterface listener) {
        final JSONObject errorResponse = new JSONObject();

        try {
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        getRequest("https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + origin + "&destinations=" + destination + "&key=" + Utils.getString(currentActivity, R.string.apiKey), new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {
                listener.Success(response);
            }

            @Override
            public void Error(VolleyError error) {
                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                listener.Error(errorResponse);
            }
        });
    }


    public static void DirectionsAPI(String origin, String destination, final RequestInterface listener) {
        final JSONObject errorResponse = new JSONObject();

        try {
            errorResponse.put("status", false);
            errorResponse.put("message", Utils.getString(currentActivity, R.string.default_error));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        getRequest("https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&key=" + Utils.getString(currentActivity, R.string.apiKey), new ResponseInterface() {
            @Override
            public void Success(JSONObject response) {
                listener.Success(response);
            }

            @Override
            public void Error(VolleyError error) {
                listener.Error(errorResponse);
            }

            @Override
            public void Catch(String error) {
                listener.Error(errorResponse);
            }
        });
    }

}
