package com.pplus.go.app.gopplus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.text.InputFilter;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import com.pplus.go.API.APIRequest;
import com.pplus.go.Data.Database;
import com.pplus.go.Utils.Utils;
import com.pplus.go.Utils.RegexValidator;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;

import java.util.Objects;


public class PromoCode extends AppCompatActivity {

    private Activity promoCodeActivity;
    private EditText codeText;
    private String code;
    private JSONObject codeObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promocode);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        codeText = (EditText) findViewById(R.id.codeText);
        promoCodeActivity = this;
        codeObject = new JSONObject();

        codeText.setFilters(new InputFilter[] {new InputFilter.AllCaps()});

        try {
            if (getIntent().hasExtra("codeObject")) {
                codeObject = new JSONObject( getIntent().getStringExtra("codeObject") );
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    public void doFindPromoCode(View view) {
        code = codeText.getText().toString();
        code = code.toUpperCase();

        //Force UpperCase
        codeText.setText(code);

        if (RegexValidator.validateRequired(code) == false) {
            Utils.showAlert(this,  RegexValidator.replaceMessage(RegexValidator.message_required, "Código de promoción") );
        } else {
            final ProgressDialog dialog = Utils.getProgressDialog(this, "Espere un momento");
            dialog.show();

            APIRequest.ValidateCode(code, Database.getUserClientId(this), new RequestInterface() {
                @Override
                public void Success(JSONObject response) {
                    dialog.hide();

                    try {
                        dialog.hide();

                        if (response.getBoolean("status") == true) {
                            codeObject = new JSONObject();
                            codeObject.put("code", code);

                            if (response.has("cid")) {
                                codeObject.put("typecode", "code");
                                codeObject.put("id", response.getString("cid"));
                            }

                            if (response.has("uid")) {
                                codeObject.put("typecode", "code");
                                codeObject.put("id", response.getString("uid"));
                            }

                            Intent validCodeIntent = new Intent();
                            validCodeIntent.putExtra("codeObject", codeObject.toString());
                            setResult(Destination.PROMOCODE_RESULT_SUCCESS, validCodeIntent);
                            finish();
                        } else {
                            codeObject = new JSONObject();
                            Utils.showAlert(promoCodeActivity, response.getString("message"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void Error(JSONObject error) {
                    try {
                        dialog.hide();
                        Utils.showAlert(promoCodeActivity, error.getString("message"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
