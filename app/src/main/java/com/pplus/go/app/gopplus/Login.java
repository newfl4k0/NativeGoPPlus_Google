package com.pplus.go.app.gopplus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.pplus.go.API.APIRequest;
import com.pplus.go.Data.Database;
import com.pplus.go.Utils.RegexValidator;
import com.pplus.go.Utils.Utils;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;

public class Login extends AppCompatActivity {

    Activity loginActivity;
    String email;

    {
        email = "";
    }

    String password;

    {
        password = "";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Database.getUserId(this) != 0) {
            startActivity(new Intent(this, Map.class));
            finish();
        } else {
            loginActivity = this;
            APIRequest.setQueue(this);
            sync();
        }
    }

    private void sync() {
        APIRequest.PublicSettings(new RequestInterface() {
            @Override
            public void Success(JSONObject response) {
                try {
                    JSONArray settings = response.getJSONArray("settings");

                    for (int i = 0; i<settings.length(); i++) {
                        JSONObject setting = settings.getJSONObject(i);
                        String key = setting.getString("k");
                        String value = setting.getString("v");
                        Database.Insert(loginActivity, key, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void Error(JSONObject error) {
                Log.d("LOGIN", "ERROR");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        APIRequest.setQueue(loginActivity);

        if (requestCode == Signup.CODE) {
            if (resultCode == Signup.SUCCESS_SIGNUP_FACEBOOK) {
                email = data.getStringExtra("email");
                password = data.getStringExtra("password");
                loginRequest(email, password);
            } else if (resultCode == Signup.SUCCESS_SIGNUP_MAIL) {
                email = data.getStringExtra("email");
                password = data.getStringExtra("password");
                openActivation(data.getIntExtra("id", 0));
            }
        } else if (requestCode == Activation.CODE) {
            if (resultCode == Activation.SUCCESS) {
                if (!email.isEmpty() && !password.isEmpty()) {
                    loginRequest(email, password);
                }
            }
        }


        super.onActivityResult(requestCode, resultCode, data);
    }

    public void doLogin(View view) {
        Utils.hideKeyboard(this);

        EditText emailField = findViewById(R.id.emailField);
        EditText passwordField = findViewById(R.id.passwordField);
        String catchError = "";

        email = emailField.getText().toString();
        password = passwordField.getText().toString();

        if (!RegexValidator.validateRequired(email)) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_required, "Correo Electrónico");
        }

        if (!RegexValidator.isEmail(email) ){
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_valid_email, "Correo Electrónico");
        }

        if (!RegexValidator.validateRequired(password)) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_required, "Contraseña");
        }

        if (!RegexValidator.isPassword(password)){
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_valid_password, "Contraseña");
        }

        if (!catchError.isEmpty()) {
            Utils.showAlert(loginActivity, catchError);
        } else {
            loginRequest(email, password);
        }
    }

    public void openSignup(View view) {
        startActivityForResult(new Intent(this, Signup.class), Signup.CODE);
    }

    public void openForgot(View view) {
        startActivity(new Intent(this, Forgot.class));
    }

    private void openActivation(int id) {
        Intent activationIntent = new Intent(loginActivity, Activation.class);
        activationIntent.putExtra("id", id);
        startActivityForResult(activationIntent, Activation.CODE);
    }

    private void loginRequest(final String email, String password) {
        final ProgressDialog dialog = Utils.getProgressDialog(loginActivity, "Espera un momento");
        dialog.show();

        APIRequest.Login(email, password, new RequestInterface() {
            @Override
            public void Success(JSONObject response) {
                dialog.dismiss();

                try {
                    if (response.getBoolean("status")) {

                        if (response.getInt("esActivo") == 1) {

                            JSONObject user = new JSONObject();
                            user.put("id", response.getInt("id"));
                            user.put("nombre", response.getString("nombre"));
                            user.put("telefono", response.getString("telefono"));
                            user.put("afiliado", response.getInt("afiliado"));
                            user.put("fbid", response.getString("fbid"));
                            user.put("correo", email);
                            user.put("codigo", response.getString("codigo"));
                            user.put("cliente", response.getInt("cliente"));
                            user.put("fechac", response.getString("fechac"));

                            Database.Insert(loginActivity, Utils.getString(loginActivity, R.string.db_user), user.toString());
                            String appId = Database.getAppId(loginActivity);

                            if (appId == null) {
                                Utils.showAlert(loginActivity, "Algo ha salido mal");
                            } else {
                                APIRequest.SetAppId(response.getInt("id"), Database.getAppId(loginActivity), new RequestInterface() {
                                    @Override
                                    public void Success(JSONObject response) {
                                        startActivity(new Intent(loginActivity, Map.class));
                                        finish();
                                    }

                                    @Override
                                    public void Error(JSONObject error) {
                                        Utils.showAlert(loginActivity, "Algo ha salido mal");
                                    }
                                });
                            }
                        } else {
                            openActivation(response.getInt("id"));
                        }
                    } else {
                        Utils.showAlert(loginActivity, "Verifica tu correo y contraseña");
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void Error(JSONObject error) {
                dialog.dismiss();

                try {
                    Utils.showAlert(loginActivity, error.getString("message"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
