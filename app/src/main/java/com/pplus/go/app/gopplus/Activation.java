package com.pplus.go.app.gopplus;

import android.app.Activity;
import android.app.ProgressDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import com.pplus.go.API.APIRequest;
import com.pplus.go.Utils.Utils;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;

import java.util.Objects;

import app.GoPPlus.R;

public class Activation extends AppCompatActivity {

    static final int CODE = 2;
    static final int SUCCESS = 1;
    Activity activationActivity = this;

    int user_id = 0;

    public Activation() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        user_id = getIntent().getIntExtra("id", 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onBackPressed();
        return super.onOptionsItemSelected(item);
    }


    public void doResend() {
        doResend();
    }

    public void doResend(View view) {
        Utils.hideKeyboard(this);

        final ProgressDialog dialog = Utils.getProgressDialog(activationActivity, "Espere un momento");
        dialog.show();


        APIRequest.SendActivationCode(user_id, new RequestInterface() {
            @Override
            public void Success(JSONObject response) {
                dialog.hide();
                String message = "Enviado";

                try {
                    message = response.getString("message");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Utils.showAlert(activationActivity, message);

            }

            @Override
            public void Error(JSONObject error) {
                dialog.hide();
                String message = getString(R.string.default_error);

                try {
                    message = error.getString("message");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Utils.showAlert(activationActivity, message);
            }
        });

    }
}
