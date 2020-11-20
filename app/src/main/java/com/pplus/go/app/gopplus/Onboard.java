package com.pplus.go.app.gopplus;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.pplus.go.API.APIRequest;
import com.pplus.go.Utils.Utils;
import com.pplus.go.app.gopplus.Interfaces.AlertInterface;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;


public class Onboard extends Fragment implements OnMapReadyCallback {

    private final static String LCAT = "ONBOARD";

    private ConstraintLayout waitContainer;
    private ConstraintLayout dataContainer;
    private WebView paymentWebView;
    private TextView waitTextView;

    private MapView mapView;
    private GoogleMap map;
    private boolean isWaitingDriver;
    private boolean isDialogShown;
    private boolean centeredMap;
    private WebViewClient webViewClient;
    private JSONObject serviceData;
    private ProgressDialog cancelProgressDialog;
    private String cancelUrl;
    private String driverImageUrl;

    private ImageView driverImage;
    private TextView driverRating;
    private TextView driverNameText;
    private TextView carText;
    private TextView extracarText;
    private TextView kmText;
    private TextView serviceStatusText;
    private String lastStartPoint = "";
    private String lastEndPoint = "";
    private Button cancelButton;
    private Button chatButton;

    private Marker fromMarker;
    private Marker toMarker;
    private Marker carMarker;
    private Polyline routeLine;
    private float zoom = 16.0f;
    private boolean notificateArrival;


    private Intent chatIntent;

    public Onboard() {
    }

    public static Onboard newInstance() {
        Onboard fragment = new Onboard();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(LCAT, "onCreate");

        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mapView != null) {
            mapView.onPause();
        }

        centeredMap = false;
    }

    @Override
    public void onDestroy() {
        if (mapView != null) {
            mapView.onDestroy();
        }

        centeredMap = false;
        super.onDestroy();
    }

    @Override
    public void onResume() {
        if (mapView != null) {
            mapView.onResume();
        }

        centeredMap = false;
        super.onResume();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        isWaitingDriver = true;
        isDialogShown = false;
        centeredMap = false;
        notificateArrival = false;
        fromMarker = null;
        toMarker = null;
        carMarker = null;

        dataContainer     = (ConstraintLayout) view.findViewById(R.id.dataContainer);
        waitContainer     = (ConstraintLayout) view.findViewById(R.id.waitContainer);
        waitTextView      = (TextView) view.findViewById(R.id.waitTextView);
        paymentWebView    = (WebView) view.findViewById(R.id.webView);
        driverImage       = (ImageView) view.findViewById(R.id.driverImage);
        driverRating      = (TextView) view.findViewById(R.id.driverRating);
        driverNameText    = (TextView) view.findViewById(R.id.driverNameText);
        carText           = (TextView) view.findViewById(R.id.carText);
        extracarText      = (TextView) view.findViewById(R.id.extracarText);
        kmText            = (TextView) view.findViewById(R.id.kmText);
        serviceStatusText = (TextView) view.findViewById(R.id.serviceStatusText);
        cancelButton      = (Button) view.findViewById(R.id.cancelServiceButton);
        chatButton        = (Button) view.findViewById(R.id.chatButton);


        webViewClient = new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("postauth-service-end")) {
                    waitTextView.setText("Espere un momento");
                } else if (url.contains("postauth-service-error")) {
                    try {
                        cancelProgressDialog.hide();
                        Utils.showAlert(getActivity(), URLDecoder.decode(url.split("\\?e=")[1], "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                view.loadUrl(url);
                return true;
            }
        };

        paymentWebView.getSettings().setLoadsImagesAutomatically(true);
        paymentWebView.getSettings().setJavaScriptEnabled(true);
        paymentWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        paymentWebView.setWebViewClient(webViewClient);

        cancelProgressDialog = Utils.getProgressDialog(getActivity(), "Cancelando, espere un momento");
        waitTextView.setText(getActivity().getResources().getString(R.string.defaultProgress));
        cancelUrl = getActivity().getResources().getString(R.string.apiPayment) + "postauth-service-start?act=CANCEL&id=";
        driverImageUrl = getActivity().getResources().getString(R.string.apiDriver) + "images/?id=";

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.showCancelServiceDialog(getActivity(),
                        "Su unidad se encuentra en camino ¿Seguro de cancelar el servicio?", new AlertInterface() {
                    @Override
                    public void Accept() {
                    }

                    @Override
                    public void Cancel() {
                        try {
                            cancelProgressDialog.show();
                            paymentWebView.loadUrl(cancelUrl + String.valueOf(serviceData.getInt("id")));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    chatIntent = new Intent(getActivity(), Chat.class);
                    chatIntent.putExtra("id", serviceData.getInt("idd"));
                    chatIntent.putExtra("driverId", serviceData.getInt("id_conductor"));
                    startActivity(chatIntent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        view.findViewById(R.id.centerLocation).setOnClickListener(view1 -> {

            try {
                if (serviceData != null && map != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(serviceData.getDouble("lat"), serviceData.getDouble("lng")), 16.0f));
                }
            } catch (JSONException e){
                e.printStackTrace();
            }
        });

        waitContainer.bringToFront();

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboard, container, false);
        mapView = view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        cancelProgressDialog.hide();
        super.onDetach();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();

        super.onActivityCreated(savedInstanceState);
    }

    public void serviceData(JSONObject service) {
        Log.d(LCAT, "serviceData");

        try {
            serviceData = service;

            if (service != null) {
                int driverId = service.getInt("id_conductor");
                int estatusId = service.getInt("estatus_reserva");

                if (driverId == 0 ) {
                    waitTextView.setText("Buscando la unidad más cercana");
                    waitContainer.bringToFront();
                    isWaitingDriver = true;
                    runCancelDialog();
                } else if (map == null || (estatusId != 4 && estatusId != 5)) {
                    waitTextView.setText(getActivity().getResources().getString(R.string.defaultProgress));
                    waitContainer.bringToFront();
                } else {
                    if (((AppCompatActivity) getActivity()).getSupportActionBar().isShowing() == false) {
                        ((AppCompatActivity) getActivity()).getSupportActionBar().show();
                    }

                    dataContainer.bringToFront();
                    isWaitingDriver = false;
                    setupServiceDataUI();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setupServiceDataUI() {
        try {
            Glide.with(Objects.requireNonNull(getActivity())).load(driverImageUrl + String.valueOf(serviceData.getInt("id_conductor")) + ".jpg").apply(RequestOptions.circleCropTransform()).into(driverImage);
            driverRating.setText(String.valueOf(serviceData.getInt("calificacion")));
            driverNameText.setText(serviceData.getString("nombre"));
            carText.setText(serviceData.getString("marca") + " "  + serviceData.getString("modelo") + " " + serviceData.getString("color"));
            extracarText.setText(serviceData.getString("permiso")+ " "  + serviceData.getString("placas"));
            kmText.setText(String.valueOf(serviceData.getDouble("km")) + " km");
            serviceStatusText.setText(serviceData.getString("estatus_reserva_nombre"));

            String startPoint = String.valueOf(serviceData.getDouble("lat")) + "," + String.valueOf(serviceData.getDouble("lng"));
            String endPoint   = serviceData.getInt("estatus_reserva") == 4 ? String.valueOf(serviceData.getDouble("lat_origen")) + "," + String.valueOf(serviceData.getDouble("lng_origen")) : String.valueOf(serviceData.getDouble("lat_destino")) + "," + String.valueOf(serviceData.getDouble("lng_destino"));

            if (!centeredMap) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(serviceData.getDouble("lat"), serviceData.getDouble("lng")), 20.0f));
                centeredMap = true;
            }

            if (fromMarker == null) {
                fromMarker = map.addMarker(new MarkerOptions().position(new LatLng(serviceData.getDouble("lat_origen"), serviceData.getDouble("lng_origen"))).icon(BitmapDescriptorFactory.fromResource(R.mipmap.pin)).title(serviceData.getString("origen")));
            }

            if (toMarker == null) {
                toMarker = map.addMarker(new MarkerOptions().position(new LatLng(serviceData.getDouble("lat_destino"), serviceData.getDouble("lng_destino"))).icon(BitmapDescriptorFactory.fromResource(R.mipmap.flag)).title(serviceData.getString("destino")));
            }

            if (carMarker != null) {
                carMarker.setPosition(new LatLng(serviceData.getDouble("lat"), serviceData.getDouble("lng")));
            } else {
                carMarker = map.addMarker(new MarkerOptions().position(new LatLng(serviceData.getDouble("lat"), serviceData.getDouble("lng"))).icon(BitmapDescriptorFactory.fromResource(R.mipmap.car)).title("Espere un momento"));
            }

            if (serviceData.getInt("estatus_reserva") == 4) {
                cancelButton.setEnabled(true);
                chatButton.setEnabled(true);
                cancelButton.setVisibility(View.VISIBLE);
                chatButton.setVisibility(View.VISIBLE);

                if (!notificateArrival && !serviceData.getString("fecha_domicilio").isEmpty()) {
                    Utils.showAlert(getActivity(), "Tu conductor ha llegado");
                    MediaPlayer.create(getActivity().getApplicationContext(), R.raw.sound).start();
                    notificateArrival = true;
                }
            } else {
                cancelButton.setEnabled(false);
                chatButton.setEnabled(false);
                cancelButton.setVisibility(View.INVISIBLE);
                chatButton.setVisibility(View.INVISIBLE);
            }

            if (lastStartPoint.equals(startPoint) == false || lastEndPoint.equals(endPoint) == false) {
                lastStartPoint = startPoint;
                lastEndPoint = endPoint;

                APIRequest.DirectionsAPI(startPoint, endPoint, new RequestInterface() {
                    @Override
                    public void Success(JSONObject response) {
                        try {
                            if (response.getString("status").equals("OK")) {
                                if (routeLine != null) {
                                    routeLine.remove();
                                }

                                routeLine = map.addPolyline(new PolylineOptions().addAll(decodePoly(response.getJSONArray("routes").getJSONObject(0).getJSONObject("overview_polyline").getString("points"))).color(Color.BLUE));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void Error(JSONObject error) {
                        Log.e(LCAT, error.toString());
                    }
                });

                APIRequest.DistanceMatrix(startPoint, endPoint, new RequestInterface() {
                    @Override
                    public void Success(JSONObject response) {
                        try {
                            if (response.getString("status").equals("OK")) {
                                String duration = response.getJSONArray("rows").getJSONObject(0).getJSONArray("elements").getJSONObject(0).getJSONObject("duration").getString("text");
                                Log.d(LCAT, duration);
                                carMarker.setTitle(duration);
                            }
                        } catch (JSONException ex) {
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public void Error(JSONObject error) {
                        Log.e(LCAT, error.toString());
                    }
                });
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }


    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private void runCancelDialog() {
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        if( isWaitingDriver && !isDialogShown ){
                            isDialogShown = true;

                            Utils.showCancelServiceDialog(getActivity(), "No se encontraron unidades cercanas. ¿Desea seguir esperando?", new AlertInterface() {
                                @Override
                                public void Accept() {
                                    isDialogShown = false;
                                }

                                @Override
                                public void Cancel() {
                                    try {
                                        waitTextView.setText("Cancelando, espere un momento");
                                        paymentWebView.loadUrl(cancelUrl + String.valueOf(serviceData.getInt("id")));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                },
                1000 * 20);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(getActivity());
        map = googleMap;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom( new LatLng(21.122039, -101.667102), zoom));
    }
}
