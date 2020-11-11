package com.pplus.go.app.gopplus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.Normalizer;
import java.util.Objects;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.pplus.go.API.APIRequest;
import com.pplus.go.Data.Database;
import com.pplus.go.Utils.Utils;
import com.pplus.go.Utils.PermissionUtils;
import com.pplus.go.Utils.SearchLocation;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;

public class Destination extends AppCompatActivity implements OnMapReadyCallback {

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
    public static final int METHODDECISSION_REQUEST_CODE     = 3008;
    public static final int EFECTIVO_RESULT_CODE            = 3009;


    public static final String DESTINATION_REQUEST_LOCATION = "location";
    public static final String DESTINATION_REQUEST_TYPE     = "type";
    public static final String DESTINATION_METHOD           = "EFECTIVO";

    private GoogleMap map;
    private JSONObject fromLocation;
    private JSONObject toLocation;
    private JSONObject fare;
    private JSONObject promoCode;
    private JSONObject creditCard;
    private final float zoom = 20.0f;
    private double estimatedDistance = 0;
    private double estimated;
    private LocationManager locationManager;

    private TextView fromLocationText;
    private TextView toLocationText;
    private TextView priceValueText;
    private TextView promoCodeText;
    private TextView creditCardText;
    private WebView webView;
    private WebViewClient webViewClient;
    private ConstraintLayout containerData;
    private Activity destinationActivity;

    private static final String LCAT = "DESTINATION";

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Utils.showAlert(destinationActivity, intent.getStringExtra("message"));
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_destination);

        destinationActivity = this;

        Log.d(LCAT, Objects.requireNonNull(getIntent().getStringExtra(DESTINATION_REQUEST_LOCATION)));
        Log.d(LCAT, Objects.requireNonNull(getIntent().getStringExtra(DESTINATION_REQUEST_TYPE)));

        SupportMapFragment mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        webView = (WebView) findViewById(R.id.webView);
        containerData = (ConstraintLayout) findViewById(R.id.containerData);

        webViewClient = new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                Log.d(LCAT, url);

                if (url.contains("preauth-service-end")) {
                    setResult(Destination.DESTINATION_RESULT_SUCCESS, new Intent());
                    finish();
                } else if (url.contains("preauth-service-error")) {
                    try {
                        webView.setVisibility(View.INVISIBLE);
                        containerData.bringToFront();
                        Utils.showAlert(destinationActivity, URLDecoder.decode(url.split("\\?e=")[1], "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                view.loadUrl(url);
                return true;
            }
        };


        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setWebViewClient(webViewClient);

        try {
            fromLocation = new JSONObject(Objects.requireNonNull(getIntent().getStringExtra(DESTINATION_REQUEST_LOCATION)));
            toLocation = new JSONObject(Objects.requireNonNull(getIntent().getStringExtra(DESTINATION_REQUEST_LOCATION)));
            fare = new JSONObject(Objects.requireNonNull(getIntent().getStringExtra(DESTINATION_REQUEST_TYPE)));
            Objects.requireNonNull(mapFragment).getMapAsync(this);

            fromLocationText = findViewById(R.id.fromLocationText);
            toLocationText = findViewById(R.id.toLocationText);
            priceValueText = findViewById(R.id.priceValue);
            promoCodeText = findViewById(R.id.paymentMethodValue);
            creditCardText = findViewById(R.id.codeMethodValue);

            fromLocationText.setText("Origen: " + fromLocation.getString("address"));
            toLocationText.setText("Destino: " + fromLocation.getString("address"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == METHODDECISSION_REQUEST_CODE) {
            if (resultCode == CREDITCARD_RESULT_SUCCESS) {
                if (data.hasExtra("cardObject")) {
                    try {
                        creditCard = new JSONObject(Objects.requireNonNull(data.getStringExtra("cardObject")));
                        creditCardText.setText("**** " + creditCard.getString("Numero"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (resultCode == CREDITCARD_RESULT_FAIL) {
                creditCard = null;
                creditCardText.setText(R.string.Ninguno);
            }
        }

        if (requestCode == METHODDECISSION_REQUEST_CODE) {
            if (resultCode == EFECTIVO_RESULT_CODE) {
                creditCard = null;
                creditCardText.setText(R.string.efectivo);
            }

            if (requestCode == PROMOCODE_REQUEST_CODE) {
                if (resultCode == PROMOCODE_RESULT_SUCCESS) {
                    if (data.hasExtra("codeObject")) {
                        try {
                            promoCode = new JSONObject(Objects.requireNonNull(data.getStringExtra("codeObject")));
                            promoCodeText.setText(promoCode.getString("code"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (requestCode == LOCATIOM_REQUEST_CODE) {
                if (resultCode == LOCATIOM_REQUEST_SUCESS) {
                    toLocationText.setText(data.getStringExtra("address"));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(data.getDoubleExtra("latitude", 0), data.getDoubleExtra("longitude", 0)), zoom));
                }
            }

            super.onActivityResult(requestCode, resultCode, data);
        }

    }
        protected void onStart () {
            super.onStart();

            LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver),
                    new IntentFilter("MyData")
            );
        }


    @Override
    protected void onPause() {
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }

        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        try {
            LatLng from = new LatLng(fromLocation.getDouble("latitude"), fromLocation.getDouble("longitude"));
            final Location location = new Location("");

            map.addMarker(new MarkerOptions()
                    .position(from)
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.pin)));

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(from, zoom));

            map.setOnCameraIdleListener(() -> {
                CameraPosition cameraPosition = map.getCameraPosition();
                location.setLatitude(cameraPosition.target.latitude);
                location.setLongitude(cameraPosition.target.longitude);

                String address = SearchLocation.searchByLocation(destinationActivity, location);

                if (!address.isEmpty()) {
                    toLocationText.setText("Destino: " + address);

                    try {
                        toLocation.put("address", address);
                        toLocation.put("latitude", cameraPosition.target.latitude);
                        toLocation.put("longitude", cameraPosition.target.longitude);
                        calculateFare();
                    } catch (JSONException e){
                        e.printStackTrace();
                    }

                } else {
                    toLocationText.setText("No tenemos cobertura en esta zona");
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void doCenterLocation(View view) {
        startLocationManager();
    }

    private void calculateFare() {
        try {
            String from = String.valueOf(fromLocation.getDouble("latitude")) + "," +  String.valueOf(fromLocation.getDouble("longitude"));
            String to = String.valueOf(toLocation.getDouble("latitude")) + "," +  String.valueOf(toLocation.getDouble("longitude"));

            APIRequest.DistanceMatrix(from, to, new RequestInterface() {
                @Override
                public void Success(JSONObject response) {
                    try {
                        if ( response.getString("status").equals("OK")) {
                            JSONArray rows =  response.getJSONArray("rows");

                            if (rows.length() > 0) {
                                JSONObject element = rows.getJSONObject(0);
                                JSONArray elements = element.getJSONArray("elements");

                                Log.d(LCAT, elements.getJSONObject(0).toString());

                                double minutes    = 0;
                                double kilometers = 0;
                                estimated  = 0;

                                if (elements.getJSONObject(0).has("duration")) {
                                    minutes = Math.abs(elements.getJSONObject(0).getJSONObject("duration").getDouble("value") / 60);
                                }

                                if (elements.getJSONObject(0).has("distance")) {
                                    estimatedDistance = elements.getJSONObject(0).getJSONObject("distance").getDouble("value");
                                    kilometers = estimatedDistance / 1000;
                                }

                                estimated = (fare.getDouble("precio_min") * minutes) + (fare.getDouble("precio_km") * kilometers) + fare.getDouble("precio_base");

                                if (estimated < fare.getDouble("precio_minimo")) {
                                    estimated = fare.getDouble("precio_minimo");
                                }

                                priceValueText.setText("$" + String.format("%.2f", estimated));
                            }
                        }
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }

                @Override
                public void Error(JSONObject error) {
                    Log.e("GOPPLUS", getResources().getString(R.string.default_error));
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationManager() {
        locationManager = (LocationManager) destinationActivity.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        if (PermissionUtils.hasPermission(destinationActivity, Manifest.permission.ACCESS_FINE_LOCATION) || PermissionUtils.hasPermission(destinationActivity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Utils.showAlert(destinationActivity, "Verifica los servicios de ubicación. Imposible obtener tu ubicación GPS");
            } else {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                }

                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                }
            }
        } else {
            PermissionUtils.requestPermissions(destinationActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationManager();
            } else {
                Utils.showAlert(destinationActivity, "Activa los servicios de ubicación para brindarte un mejor servicio.");
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (map != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), zoom));
                locationManager.removeUpdates(locationListener);
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}

        public void onProviderEnabled(String provider) {}

        public void onProviderDisabled(String provider) {}
    };

    public void doCalculateFare(View view) {
        calculateFare();
    }

    public void doRequest(View view) {
        try {
            Boolean efectivo = false;
            String error = "";
            String paymenturl = "";

            estimatedDistance /= 1000;

            if (toLocation.getString("address") == "") {
                error = "Selecciona una dirección destino válida\n";
            }

            if (toLocation.getDouble("latitude") == fromLocation.getDouble("latitude")) {
                error += "Selecciona una dirección destino diferente a la dirección origen\n";
            }

           /* if (estimatedDistance < 100)
                error += "Selecciona una dirección destino con más de 100mts de diferencia con la dirección origen\n";
*/
            if (creditCard == null) {
                if (creditCardText.getText().toString().toUpperCase().compareTo(DESTINATION_METHOD)==0) {
                    double preauthPrice = estimated * 2;
                    String preauthValue = Database.Select(destinationActivity, "PreautorizacionTarifa");

                    if (preauthValue.isEmpty() == false) {
                        preauthPrice = Double.parseDouble(Database.Select(destinationActivity, "PreautorizacionTarifa"));
                    }

                    String code = "";

                    if (promoCode != null) {
                        if (promoCode.has("code")) {
                            code = promoCode.getString("code");
                        }
                    }
                    paymenturl = getResources().getString(R.string.apiPayment) + "preauth-service-start";
                    paymenturl += "?card_id=155";
                    paymenturl += "&origen=" + Normalizer.normalize(fromLocation.getString("address"), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
                    paymenturl += "&destino=" + Normalizer.normalize(toLocation.getString("address"), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
                    paymenturl += "&lat_origen=" + String.valueOf(fromLocation.getDouble("latitude"));
                    paymenturl += "&lng_origen=" + String.valueOf(fromLocation.getDouble("longitude"));
                    paymenturl += "&lat_destino=" + String.valueOf(toLocation.getDouble("latitude"));
                    paymenturl += "&lng_destino=" + String.valueOf(toLocation.getDouble("longitude"));
                    paymenturl += "&usuario_id=" + String.valueOf(Database.getUserId(destinationActivity));
                    paymenturl += "&tipo_id=" + String.valueOf(fare.getInt("id"));
                    paymenturl += "&afiliado=" + String.valueOf(Database.getUserClientAf(destinationActivity));
                    paymenturl += "&codigo_desc=" + code;
                    paymenturl += "&cliente_id=" + String.valueOf(Database.getUserClientId(destinationActivity));
                    paymenturl += "&monto=" + String.valueOf(preauthPrice);
                    paymenturl += "&km=" + String.valueOf(estimatedDistance);
                    efectivo = true;
                }
                else {
                    error += "Selecciona un método de pago\n";
                }
            }
            else {
                double preauthPrice = estimated * 2;
                String preauthValue = Database.Select(destinationActivity, "PreautorizacionTarifa");

                if (!preauthValue.isEmpty()) {
                    preauthPrice = Double.parseDouble(Database.Select(destinationActivity, "PreautorizacionTarifa"));
                }

                String code = "";

                if (promoCode != null) {
                    if (promoCode.has("code")) {
                        code = promoCode.getString("code");
                    }
                }
                paymenturl = getResources().getString(R.string.apiPayment) + "preauth-service-start";
                paymenturl += "?card_id=" + String.valueOf(creditCard.getInt("Id"));
                paymenturl += "&origen=" + Normalizer.normalize(fromLocation.getString("address"), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
                paymenturl += "&destino=" + Normalizer.normalize(toLocation.getString("address"), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
                paymenturl += "&lat_origen=" + String.valueOf(fromLocation.getDouble("latitude"));
                paymenturl += "&lng_origen=" + String.valueOf(fromLocation.getDouble("longitude"));
                paymenturl += "&lat_destino=" + String.valueOf(toLocation.getDouble("latitude"));
                paymenturl += "&lng_destino=" + String.valueOf(toLocation.getDouble("longitude"));
                paymenturl += "&usuario_id=" + String.valueOf(Database.getUserId(destinationActivity));
                paymenturl += "&tipo_id=" + String.valueOf(fare.getInt("id"));
                paymenturl += "&afiliado=" + String.valueOf(Database.getUserClientAf(destinationActivity));
                paymenturl += "&codigo_desc=" + code;
                paymenturl += "&cliente_id=" + String.valueOf(Database.getUserClientId(destinationActivity));
                paymenturl += "&monto=" + String.valueOf(preauthPrice);
                paymenturl += "&km=" + String.valueOf(estimatedDistance);

            }

            if (error.isEmpty()) {

                Log.d(LCAT, paymenturl);
                webView.bringToFront();
                if (!efectivo) {
                    webView.setVisibility(View.VISIBLE);
                }
                webView.loadUrl(paymenturl);
            } else {
                Utils.showAlert(destinationActivity, error);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void doOpenCreditCards(View view) {
        try {
            Intent creditCardIntent = new Intent(this,
                    CreditCards.class);

            if (creditCard != null) {
                creditCardIntent.putExtra("cardId", creditCard.getInt("Id"));
            }

            startActivityForResult(creditCardIntent, CREDITCARD_REQUEST_CODE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void doOpenPromoCode(View view) {
        startActivityForResult(new Intent(this, PromoCode.class), PROMOCODE_REQUEST_CODE);
    }

    public void doOpenMethodDecision(View view) {
        startActivityForResult(new Intent(this, MethodDecission.class),METHODDECISSION_REQUEST_CODE);
    }


    public void doSearchLocation(View view) {
        Intent intent = new Intent(destinationActivity, com.pplus.go.app.gopplus.Location.class);
        startActivityForResult(intent, LOCATIOM_REQUEST_CODE);


    }
}
