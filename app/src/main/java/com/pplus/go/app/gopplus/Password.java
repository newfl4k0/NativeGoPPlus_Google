package com.pplus.go.app.gopplus;

import android.app.Activity;
import android.app.ProgressDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import com.pplus.go.API.APIRequest;
import com.pplus.go.Utils.Utils;
import com.pplus.go.Utils.RegexValidator;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;


public class Password extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        Toolbar toolbar = findViewById(R.id.toolbar);
        /*setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);*/
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onBackPressed();
        return super.onOptionsItemSelected(item);
    }



    public void doUpdate(View view) {
        Utils.hideKeyboard(this);

        String currentPassword = ((TextView) findViewById(R.id.passwordField)).getText().toString();
        String newPassword     = ((TextView) findViewById(R.id.newPasswordField)).getText().toString();
        String confirmPassword = ((TextView) findViewById(R.id.confirmField)).getText().toString();
        String catchError      = "";


        if (!RegexValidator.validateRequired(currentPassword) ) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_required, "Contraseña actual");
        }

        if (!RegexValidator.validateRequired(newPassword)) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_required, "Nueva contraseña");
        }

        if (!RegexValidator.validateRequired(confirmPassword)) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_required, "Confirmar nueva contraseña");
        }

        if (!RegexValidator.isPassword(currentPassword) ) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_valid_password, "Contraseña actual");
        }

        if (!RegexValidator.isPassword(newPassword)) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_valid_password, "Nueva contraseña");
        }

        if (!RegexValidator.isPassword(confirmPassword)) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_valid_password, "Confirmar nueva contraseña");
        }

        if (!newPassword.equals(confirmPassword)) {
            catchError += "\nNueva contraseña y confirmación no coinciden";
        }

        if (catchError.isEmpty()) {
            final Activity passwordActivity = this;
            final ProgressDialog dialog = Utils.getProgressDialog(passwordActivity, getResources().getString(R.string.defaultProgress));
            dialog.show();

            APIRequest.Password(currentPassword, newPassword, new RequestInterface() {
                @Override
                public void Success(JSONObject response) {
                    dialog.hide();
                    String message = getResources().getString(R.string.default_error);

                    try {
                        if (response.has("message")) {
                            message = response.getString("message");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Utils.showAlert(passwordActivity, message);
                }

                @Override
                public void Error(JSONObject error) {
                    dialog.hide();
                    Utils.showAlert(passwordActivity, getResources().getString(R.string.default_error));
                }
            });
        } else {
            Utils.showAlert(this, catchError);
        }

    }
}
