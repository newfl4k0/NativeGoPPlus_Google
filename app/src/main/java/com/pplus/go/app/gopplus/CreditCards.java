package com.pplus.go.app.gopplus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.HashMap;

import com.pplus.go.API.APIRequest;
import com.pplus.go.Data.Database;
import com.pplus.go.Utils.Utils;
import com.pplus.go.Utils.RegexValidator;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;



public class CreditCards extends AppCompatActivity {

    private CardAdapter cardAdapter = new CardAdapter();
    private JSONArray cards = new JSONArray();
    private ListView list;

    private EditText number;
    private EditText month;
    private EditText year;
    private EditText security;
    private WebView webView;
    private WebViewClient webViewClient;
    private Activity creditCardActivity;
    private ProgressDialog dialog;
    private int prevCreditCardId;
    private boolean prevCreditCardDeleted;

    private boolean isWebViewHidden;
    private AdapterView.OnItemClickListener onItemClickListener;

    private static final String LCAT = "CREDITCARDS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creditcards);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        creditCardActivity = this;

        list = findViewById(R.id.list);
        list.setAdapter(cardAdapter);

        number = findViewById(R.id.numberText);
        month = findViewById(R.id.monthText);
        year = findViewById(R.id.yearText);
        security = findViewById(R.id.securityText);
        webView = findViewById(R.id.webview);

        prevCreditCardId = getIntent().getIntExtra("cardId", 0);
        prevCreditCardDeleted = false;

        Log.d(LCAT, "prevCreditCardId : " + String.valueOf(prevCreditCardId));
        Log.d(LCAT, "prevCreditCardDeleted : " + String.valueOf(prevCreditCardDeleted));

        final HashMap<String, String> extraHeaders = new HashMap<String, String>();
        extraHeaders.put("appid", Database.getAppId(creditCardActivity));
        extraHeaders.put("userid", Database.getEncryptedUserId(creditCardActivity));

        webViewClient = new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("card-service-end")) {
                    setWebviewVisible(false);
                    number.setText("");
                    month.setText("");
                    year.setText("");
                    security.setText("");
                    dialog.show();

                    new android.os.Handler().postDelayed(
                            () -> loadCards(),
                            1000 * 5);
                } else if (url.contains("card-service-error")) {
                    setWebviewVisible(false);

                    try {
                        Utils.showAlert(creditCardActivity, URLDecoder.decode(url.split("\\?e=")[1], "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                view.loadUrl(url, extraHeaders);
                return true;
            }
        };

        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setWebViewClient(webViewClient);
        isWebViewHidden = true;
        dialog = Utils.getProgressDialog(creditCardActivity, getResources().getString(R.string.defaultProgress));

        onItemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    dialog.hide();
                    dialog = null;
                    JSONObject card = cards.getJSONObject(i);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("cardObject", card.toString());
                    setResult(Destination.CREDITCARD_RESULT_SUCCESS, resultIntent);
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        loadCards();
        Utils.hideKeyboard(creditCardActivity);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isWebViewHidden) {
            if (prevCreditCardDeleted) {
                Intent resultIntent = new Intent();
                setResult(Destination.CREDITCARD_RESULT_FAIL, resultIntent);
                finish();
                return false;
            } else {
                super.onBackPressed();
                return super.onOptionsItemSelected(item);
            }
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        if (isWebViewHidden) {
            if (prevCreditCardDeleted) {
                Intent resultIntent = new Intent();
                setResult(Destination.CREDITCARD_RESULT_FAIL, resultIntent);
                finish();
            } else {
                super.onBackPressed();
            }
        }
    }

    public void doAddCreditCard(View view) {
        try {
            Utils.hideKeyboard(creditCardActivity);

            String cardText    = number.getText().toString();
            String monthText   = month.getText().toString();
            String yearText    = year.getText().toString();
            String cvvText     = security.getText().toString();
            String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
            String startYear   = currentYear.substring(0, 2);
            String error       = "";

            if (!RegexValidator.validateRequired(cardText)) {
                error += RegexValidator.replaceMessage(RegexValidator.message_required, "Número de Tarjeta");
            }

            if (cardText.length() < 16) {
                error += "\nNúmero de Tarjeta no válido";
            }

            if (!RegexValidator.validateRequired(monthText)) {
                error += "\n" + RegexValidator.replaceMessage(RegexValidator.message_required, "Mes");
            }

            if (!RegexValidator.isMonth(monthText)) {
                error += "\n" + RegexValidator.replaceMessage(RegexValidator.message_valid_month, "Mes");
            }

            if (!RegexValidator.validateRequired(yearText)) {
                error += "\n" + RegexValidator.replaceMessage(RegexValidator.message_required, "Año");
            }

            if (Integer.parseInt(startYear + yearText) < Integer.parseInt(currentYear)) {
                error += "\n" + "Verifica el año ingresado";
            }

            if (!RegexValidator.validateRequired(cvvText)) {
                error += "\n" + RegexValidator.replaceMessage(RegexValidator.message_required, "Código de seguridad");
            }

            if (!RegexValidator.isCVV(cvvText)) {
                error += "\n" + RegexValidator.replaceMessage(RegexValidator.message_valid_cvv, cvvText);
            }

            if (error.isEmpty()) {
                String card = Database.encrypt64(cardText, getResources().getString(R.string.secret));
                String exp  = Database.encrypt64( monthText + "" + yearText, getResources().getString(R.string.secret));
                String cvv  = Database.encrypt64(cvvText, getResources().getString(R.string.secret));
                String id   = Database.encrypt64(String.valueOf(Database.getUserClientId(this)), getResources().getString(R.string.secret));
                String url  = getResources().getString(R.string.apiPayment) + "card-service-start?y=" + id + "&i=" + card + "&f=" + exp + "&a=" + cvv;

                final HashMap<String, String> extraHeaders = new HashMap<String, String>();
                extraHeaders.put("appid", Database.getAppId(creditCardActivity));
                extraHeaders.put("userid", Database.getEncryptedUserId(creditCardActivity));

                setWebviewVisible(true);
                webView.loadUrl(url, extraHeaders);
            } else {
                Utils.showAlert(this, error);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setWebviewVisible(boolean visible) {
        if (visible) {
            isWebViewHidden = false;
            webView.bringToFront();
            webView.setVisibility(View.VISIBLE);
        } else {
            isWebViewHidden = true;
            webView.setVisibility(View.INVISIBLE);
            list.bringToFront();
            findViewById(R.id.addCardContainer).bringToFront();
        }
    }

    private void loadCards() {
        Utils.hideKeyboard(creditCardActivity);
        dialog.show();


        APIRequest.GetPaymentCards(Database.getUserClientId(creditCardActivity), new RequestInterface() {
            @Override
            public void Success(JSONObject response) {
                dialog.hide();

                try {
                    if (response.has("data")) {
                        cards = response.getJSONArray("data");
                        cardAdapter.notifyDataSetChanged();

                        if (cards.length() == 0) {
                            Utils.showAlert(creditCardActivity, "No encontramos tarjetas vinculadas a tu cuenta. Agrega una nueva tarjeta para solicitar tu vehículo seleccionado con GoPPlus");
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void Error(JSONObject error) {
                dialog.hide();

                try {
                    Utils.showAlert(creditCardActivity, error.getString("message"));

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    private class CardAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return cards.length();
        }

        @Override
        public Object getItem(int i) {
            try {
                return cards.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = getLayoutInflater().inflate(R.layout.row_card, null);

            try {
                final JSONObject card = cards.getJSONObject(i);
                final int position = i;

                ((TextView) view.findViewById(R.id.cardNumber)).append(card.getString("Numero"));

                ((ConstraintLayout) view.findViewById(R.id.numberContainer)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (onItemClickListener != null) {
                            onItemClickListener.onItemClick(null, view, position, -1);
                        }
                    }
                });


                ((Button) view.findViewById(R.id.deleteCard)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            dialog.show();

                            final int cardId = card.getInt("Id");

                            APIRequest.RemovePaymentCard(Database.getUserClientId(creditCardActivity), cardId, new RequestInterface() {
                                @Override
                                public void Success(JSONObject response) {
                                    try {
                                        if (prevCreditCardId == cardId) {
                                            prevCreditCardDeleted = true;
                                        }

                                        Log.d(LCAT, "prevCreditCardId : " + String.valueOf(prevCreditCardId));
                                        Log.d(LCAT, "prevCreditCardId : " + String.valueOf(prevCreditCardDeleted));

                                        dialog.hide();
                                        Utils.showAlert(creditCardActivity, response.getString("message"));
                                        loadCards();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                }

                                @Override
                                public void Error(JSONObject error) {
                                    try {
                                        dialog.hide();
                                        Utils.showAlert(creditCardActivity, error.getString("message"));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return view;
        }
    }
}
