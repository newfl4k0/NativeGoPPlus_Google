package com.pplus.go.app.gopplus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import org.json.JSONException;
import org.json.JSONObject;

import com.pplus.go.API.APIRequest;
import com.pplus.go.Data.Database;
import com.pplus.go.Utils.Utils;
import com.pplus.go.Utils.RegexValidator;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;

public class Rate extends AppCompatActivity {
    private static final String LCAT = "RATE";
    public static final String REQUEST_RATE = "data";
    public static final int REQUEST_CODE    = 4001;
    private JSONObject service;
    private EditText commentText;

    private RequestOptions imageOptions = new RequestOptions()
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate);
        final Activity activity = this;
        commentText = ( findViewById(R.id.commentsText));

        String extras = getIntent().getStringExtra(REQUEST_RATE);

        if (!extras.isEmpty()) {
            try {
                service = new JSONObject(extras);

                Glide.with(this).load(getResources().getString(R.string.apiDriver) + "images/?id=" + String.valueOf(service.getInt("conductor")) + ".jpg").apply(RequestOptions.circleCropTransform()).into(((ImageView) findViewById(R.id.driverImage)));
                ((TextView) findViewById(R.id.driverNameText)).setText(service.getString("nombre_conductor"));
                ((TextView) findViewById(R.id.priceText)).setText(String.format("$%.2f", service.getDouble("precio")));
                ((TextView) findViewById(R.id.dateText)).setText(service.getString("fecha"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void doSendRating(View view) {
        try {
            int id = service.getInt("id");
            int rate =  Math.round(((RatingBar) findViewById(R.id.ratingBar)).getRating());
            String comments = commentText.getText().toString();

            final Activity activity = this;
            final ProgressDialog dialog = Utils.getProgressDialog(activity, "Espere un momento");

            if (rate == 0) {
                Utils.showAlert(this, "Califica tu servicio");
            } else {
                if (!RegexValidator.validateRequired(comments)) {
                    Utils.showAlert(this, RegexValidator.replaceMessage(RegexValidator.message_required,"Observaciones"));
                } else if (!RegexValidator.isText(comments)) {
                    Utils.showAlert(this, RegexValidator.replaceMessage(RegexValidator.message_valid_text,"Observaciones"));
                } else if (comments.length() > 30) {
                    Utils.showAlert(this, RegexValidator.replaceMessage(RegexValidator.message_less_than,"Observaciones,30 caracteres"));
                } else if (comments.length() < 2) {
                    Utils.showAlert(this, RegexValidator.replaceMessage(RegexValidator.message_greater_than,"Observaciones,1 carácter"));
                }  else {
                    dialog.show();

                    APIRequest.SendRate(id, rate, comments, new RequestInterface() {
                        @Override
                        public void Success(JSONObject response) {
                            dialog.hide();
                            setResult(REQUEST_CODE, new Intent());
                            finish();
                        }

                        @Override
                        public void Error(JSONObject error) {
                            Log.e(LCAT, error.toString());
                            dialog.hide();
                            try {
                                Utils.showAlert(activity, error.getString("message"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
