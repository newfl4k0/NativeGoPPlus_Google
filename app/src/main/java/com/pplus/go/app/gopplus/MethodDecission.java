package com.pplus.go.app.gopplus;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.text.TextPaint;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;


public class MethodDecission extends AppCompatActivity {

    public static final int DESTINATION_REQUEST_CODE        = 3001;
    public static final int DESTINATION_RESULT_SUCCESS      = 3002;
    public static final int CREDITCARD_REQUEST_CODE         = 3003;
    public static final int CREDITCARD_RESULT_SUCCESS       = 3004;
    public static final int CREDITCARD_RESULT_FAIL          = 3008;
    public static final int PROMOCODE_REQUEST_CODE          = 3005;
    public static final int PROMOCODE_RESULT_SUCCESS        = 3006;
    public static final int LOCATIOM_REQUEST_CODE           = 3007;
    public static final int LOCATIOM_REQUEST_SUCESS         = 1;
    private final static int LOCATION_REQUEST_CODE          = 9999;
    public static final int METHODDECISSION_RESULT_CODE     = 3008;
    public static final int EFECTIVO_RESULT_CODE            = 3009;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;
    private SwitchCompat switchMethod;
    private ConstraintLayout cs1;
    private AppCompatButton btnOK;
    private AppCompatButton btnCancel;
    private Activity mdactivity;

    private JSONObject creditCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_method_decission);
        switchMethod = (SwitchCompat) this.findViewById(R.id.switchMethod);
        mdactivity = this;
    }



    public void doMethodDecission(View view) {
            if (switchMethod.isChecked()) {
                try {
                    Intent creditCardIntent = new Intent(this, CreditCards.class);

                    if (creditCard != null) {
                        creditCardIntent.putExtra("cardId", creditCard.getInt("Id"));
                    }

                    startActivityForResult(creditCardIntent, CREDITCARD_REQUEST_CODE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else {
                Intent ins = new Intent();
                setResult(EFECTIVO_RESULT_CODE,ins);
                finish();
            }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CREDITCARD_REQUEST_CODE) {
            if (resultCode == CREDITCARD_RESULT_SUCCESS) {
                if (data.hasExtra("cardObject")) {
                    try {
                        creditCard = new JSONObject(Objects.requireNonNull(data.getStringExtra("cardObject")));
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("cardObject", creditCard.toString());
                        setResult(Destination.CREDITCARD_RESULT_SUCCESS, resultIntent);
                        finish();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (resultCode == CREDITCARD_RESULT_FAIL) {
                Intent resultIntent = new Intent();
                setResult(Destination.CREDITCARD_RESULT_FAIL, resultIntent);
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void doCancel(View view){
        finish();
    }


}