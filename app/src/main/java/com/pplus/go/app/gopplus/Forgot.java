package com.pplus.go.app.gopplus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
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


public class Forgot extends AppCompatActivity {

    Activity forgotActivity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        forgotActivity = this;
        APIRequest.setQueue(this);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    public void doSendCode(View view) {
        Utils.hideKeyboard(this);

        final String email = ((EditText) findViewById(R.id.emailField)).getText().toString();
        String fieldName = ((EditText)findViewById(R.id.emailField)).getHint().toString();
        String catchError = "";

        if (!RegexValidator.validateRequired(email)) {
            catchError +=  RegexValidator.replaceMessage(RegexValidator.message_required, fieldName) + "\n";
        }

        if (!RegexValidator.isEmail(email)) {
            catchError += RegexValidator.replaceMessage(RegexValidator.message_valid_email, fieldName) + "\n";
        }


        if (!catchError.isEmpty()) {
            Utils.showAlert(forgotActivity, catchError);
        } else {
            final ProgressDialog dialog = Utils.getProgressDialog(forgotActivity, "Espera un momento");
            dialog.show();

            APIRequest.SendRecoveryEmail(email, new RequestInterface() {
                @Override
                public void Success(JSONObject response) {
                    dialog.hide();
                    String message = getResources().getString(R.string.default_error);
                    boolean status = false;

                    try {
                        status = response.getBoolean("status");
                        message = response.getString("message");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (status == true) {
                        Intent intent = new Intent(forgotActivity, Restore.class);
                        intent.putExtra("email", email);
                        startActivityForResult(intent, Restore.CODE);
                    } else {
                        Utils.showAlert(forgotActivity, message);
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

                    Utils.showAlert(forgotActivity, message);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Restore.CODE) {
            if (resultCode == Restore.SUCCESS) {
                finish();
            }
        }
    }

}
