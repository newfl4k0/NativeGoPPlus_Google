package com.pplus.go.app.gopplus;

import android.app.Activity;
import android.app.ProgressDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import com.pplus.go.API.APIRequest;
import com.pplus.go.Utils.Utils;
import com.pplus.go.Utils.RegexValidator;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;


public class Restore extends AppCompatActivity {

    static final int CODE = 1;
    static final int SUCCESS = 1;
    String email;
    String code;
    String password;
    Activity restoreActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        /*setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);*/
        restoreActivity = this;
        email = getIntent().getStringExtra("email");
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    public void doChangePassword(View view) {
        Utils.hideKeyboard(this);

        String catchError = "";
        String codeFieldName = ((EditText) findViewById(R.id.codeField)).getHint().toString();
        String passwordFieldName = ((EditText) findViewById(R.id.passwordField)).getHint().toString();

        code = ((EditText) findViewById(R.id.codeField)).getText().toString();
        password = ((EditText) findViewById(R.id.passwordField)).getText().toString();


        if (!RegexValidator.validateRequired(code)) {
            catchError += RegexValidator.replaceMessage(RegexValidator.message_required, codeFieldName);
        }

        if (!RegexValidator.isPassword(password)) {
            catchError += RegexValidator.replaceMessage(RegexValidator.message_valid_password, passwordFieldName);
        }


        if (catchError.isEmpty()) {
            //API Request


            final ProgressDialog dialog = Utils.getProgressDialog(restoreActivity, "Espera un momento");
            dialog.show();

            APIRequest.ResetPassword(email, code, password, new RequestInterface() {
                @Override
                public void Success(JSONObject response) {
                    dialog.hide();
                    boolean status = false;
                    String message = getResources().getString(R.string.default_error);

                    try {
                        message = response.getString("message");
                        status = response.getBoolean("status");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (status) {
                        setResult(Restore.SUCCESS);
                        finish();
                    } else {
                        Utils.showAlert(restoreActivity, message);
                    }
                }

                @Override
                public void Error(JSONObject error) {
                    dialog.hide();
                    String message = getResources().getString(R.string.default_error);

                    try {
                        message = error.getString("message");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Utils.showAlert(restoreActivity, message);
                }
            });

        } else {
            Utils.showAlert(restoreActivity, catchError);
        }
    }
}
