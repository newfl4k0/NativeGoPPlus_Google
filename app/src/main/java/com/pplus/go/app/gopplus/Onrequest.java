package com.pplus.go.app.gopplus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.TypedValue;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.pplus.go.API.APIRequest;
import com.pplus.go.Utils.Utils;
import com.pplus.go.Utils.PermissionUtils;
import com.pplus.go.Data.Database;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;

import static com.pplus.go.Utils.Utils.getProgressDialog;
import static com.pplus.go.app.gopplus.R.id.openDestination;
import static com.pplus.go.app.gopplus.R.id.swipeContainer;
import static com.pplus.go.app.gopplus.R.id.time;
import static com.pplus.go.app.gopplus.R.id.vehiclesContainer;
import static com.pplus.go.app.gopplus.R.mipmap.*;
import static com.pplus.go.app.gopplus.R.mipmap.vehicle;
import static com.pplus.go.app.gopplus.R.string.defaultProgress;

public class Onrequest extends Fragment implements OnMapReadyCallback {
    private OnFragmentInteractionListener mListener;
    private RequestOptions unselectedOption = new RequestOptions();
    private RequestOptions selectedOption = new RequestOptions();

    private boolean pauseMapListener = false;
    private boolean pauseScrollVehicleContainer = true;
    private boolean vehiclesHidden = false;
    private boolean rateViewOpen = false;

    private float zoom = 16.0f;
    private float swipeContainerY = 0;

    private JSONArray vehiclesByType;
    private LocationManager locationManager;
    private Location userLocation;

    private final static int LOCATION_REQUEST_CODE = 9999;
    private final static int CODE_LOCATION = 9998;

    private Button openDestinationButton;
    private GoogleMap gmap;
    private LinearLayout scrollView;
    private ImageView swipeArrow;
    private MapView mapView;
    private TextView searchText;
    private TextView timeText;
    private JSONObject vehicleSelected;
    private JSONObject locationSelected;

    private Handler vehiclesTimer;
    private Handler finishedServicesTimer;

    private ProgressDialog dialog;

    private RequestOptions ActiveOptions = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(vehiclered)
            .error(vehiclered);

    private RequestOptions InactiveOptions = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(vehicle)
            .error(vehicle);

    private static final String LCAT = "ONREQUEST";

    private FusedLocationProviderClient mFusedLocationClient;

    public Onrequest() { }

    public static Onrequest newInstance() {
        Onrequest fragment = new Onrequest();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("CheckResult")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        locationManager = (LocationManager) Objects.requireNonNull(getActivity()).getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        unselectedOption.placeholder(vehicle);
        unselectedOption.error(vehicle);

        selectedOption.placeholder(vehiclered);
        selectedOption.error(vehiclered);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        LinearLayout swipeContainer = view.findViewById(R.id.swipeContainer);

        scrollView = Objects.requireNonNull(getView()).findViewById(vehiclesContainer);
        swipeArrow = getView().findViewById(R.id.swipeArrow);
        openDestinationButton = getView().findViewById(openDestination);
        searchText = view.findViewById(R.id.searchText);
        timeText = view.findViewById(time);
        dialog = getProgressDialog(getActivity(), getResources().getString(defaultProgress));

        scrollView.bringToFront();
        addVehicles();


        swipeArrow.setOnClickListener(view13 -> {
            if (vehiclesHidden) {
                showSwipeContainer();
            } else {
                hideSwipeContainer();
            }
        });

        (view.findViewById(R.id.userLocation)).setOnClickListener(view14 -> {

            if (gmap!= null && userLocation!= null) {
                gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()), zoom));
            }

            startLocationManager();
        });

        searchText.setOnClickListener(view1 -> {
            Intent intent = new Intent(getActivity(), com.pplus.go.app.gopplus.Location.class);
            startActivityForResult(intent, CODE_LOCATION);
        });

        (getView().findViewById(openDestination)).setOnClickListener(view12 -> {
            if (vehicleSelected != null && locationSelected != null && vehiclesHidden) {
                Intent intent = new Intent(getActivity(), Destination.class);
                intent.putExtra(Destination.DESTINATION_REQUEST_LOCATION, locationSelected.toString());
                intent.putExtra(Destination.DESTINATION_REQUEST_TYPE, vehicleSelected.toString());
                startActivityForResult(intent, Destination.DESTINATION_REQUEST_CODE);
            }
        });

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onrequest, container, false);
        mapView = view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        return view;
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(Objects.requireNonNull(context));

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        locationManager.removeUpdates(locationListener);
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        Log.d(LCAT, "ON RESUME");
        super.onResume();
        mapView.onResume();

        vehiclesTimer = new Handler();
        finishedServicesTimer = new Handler();

        getNearestVehicles();
        getUnratedServicesWait();
    }

    @Override
    public void onPause() {
        Log.d(LCAT, "ON PAUSE");
        super.onPause();
        mapView.onPause();
        dialog.hide();

        if (vehiclesTimer != null) {
            vehiclesTimer.removeCallbacksAndMessages(null);
        }

        if (finishedServicesTimer != null) {
            finishedServicesTimer.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        finishedServicesTimer.removeCallbacksAndMessages(null);
        vehiclesTimer.removeCallbacksAndMessages(null);
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(Objects.requireNonNull(this.getActivity()));
        gmap = googleMap;
        gmap.moveCamera(CameraUpdateFactory.newLatLngZoom( new LatLng(21.122039, -101.667102), zoom));
        startLocationManager();

        gmap.setOnCameraIdleListener(() -> {

            if (!pauseMapListener) {
                CameraPosition cameraPosition = gmap.getCameraPosition();
                Location location = new Location("");
                location.setLatitude(cameraPosition.target.latitude);
                location.setLongitude(cameraPosition.target.longitude);
                searchByLocation(location);
            }
        });
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(JSONObject data);
    }

    @SuppressLint("MissingPermission")
    private void startLocationManager() {
        if (PermissionUtils.hasPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) || PermissionUtils.hasPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Utils.showAlert(getActivity(), "Verifica los servicios de ubicación. Imposible obtener tu ubicación GPS");
            } else {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                }

                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                }
            }
        } else {
            PermissionUtils.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationManager();
            } else {
                Utils.showAlert(getActivity(), "Activa los servicios de ubicación para brindarte un mejor servicio.");
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if( requestCode == CODE_LOCATION ){
            if (resultCode == com.pplus.go.app.gopplus.Location.SUCCESS) {
                pauseMapListener = true;
                searchText.setText(data.getStringExtra("address"));
                gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(data.getDoubleExtra("latitude", 0), data.getDoubleExtra("longitude", 0)), zoom));
                getNearestVehicles();new android.os.Handler().postDelayed(
                        () -> pauseMapListener = false,
                        1000 * 2);
            }
        }

        if (requestCode == Destination.DESTINATION_REQUEST_CODE) {
            if (resultCode == Destination.DESTINATION_RESULT_SUCCESS) {
                dialog.show();
                Log.d("ONREQUEST", "service requested");
            }
        }

        if (requestCode == Rate.REQUEST_CODE) {
            rateViewOpen = false;
            getUnratedServicesWait();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void showInactive(ImageView active, ImageView inactive) {
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());

        ViewGroup.LayoutParams inactiveParams = inactive.getLayoutParams();
        inactiveParams.height = height;
        inactive.setLayoutParams(inactiveParams);
        inactive.setVisibility(View.VISIBLE);

        ViewGroup.LayoutParams activeParams = active.getLayoutParams();
        activeParams.height = 0;
        active.setLayoutParams(activeParams);
        active.setVisibility(View.INVISIBLE);
    }

    public void showActive(ImageView active, ImageView inactive) {
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());

        ViewGroup.LayoutParams activeParams = active.getLayoutParams();
        activeParams.height = height;
        active.setLayoutParams(activeParams);
        active.setVisibility(View.VISIBLE);

        ViewGroup.LayoutParams inactiveParams = inactive.getLayoutParams();
        inactiveParams.height = 0;
        inactive.setLayoutParams(inactiveParams);
        inactive.setVisibility(View.INVISIBLE);
    }

    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            userLocation = location;
            searchByLocation(userLocation);

            if (gmap != null) {
                gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()), zoom));
                locationManager.removeUpdates(locationListener);
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}

        public void onProviderEnabled(String provider) {}

        public void onProviderDisabled(String provider) {}
    };

    public void addVehicles() {
        try {
            String vehiclesByTypeText = Database.Select(getActivity(), "vehiclesByType");

            if (vehiclesByTypeText.isEmpty()) {
                vehicleSelected = null;
                scrollView.removeAllViews();

                new Handler().postDelayed(
                        () -> addVehicles(),
                        1000 * 10);
            } else {
                vehiclesByType = new JSONArray(vehiclesByTypeText);
                scrollView.removeAllViews();

                if (vehiclesByType.length() == 0) {
                    vehicleSelected = null;
                } else {
                    pauseScrollVehicleContainer = false;

                    if (vehicleSelected == null) {
                        vehicleSelected = vehiclesByType.getJSONObject(0);
                    }

                    for (int i = 0; i< vehiclesByType.length(); i++) {
                        JSONObject vehicleType = vehiclesByType.getJSONObject(i);
                        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
                        LinearLayout linearLayout = (LinearLayout) layoutInflater.inflate(R.layout.layout_vehicle, null);
                        ImageView vehicleImage = linearLayout.findViewById(R.id.vehicleImage);
                        final ImageView inactiveVehicleImage = linearLayout.findViewById(R.id.inactiveVehicleImage);


                        ((TextView) linearLayout.findViewById(R.id.vehicleType)).setText(vehicleType.getString("nombre"));
                        ((TextView) linearLayout.findViewById(R.id.startPrice)).setText("Mínima $" + vehicleType.getString("precio_minimo"));
                        ((TextView) linearLayout.findViewById(R.id.timePrice)).setText("Minuto $" + vehicleType.getString("precio_min"));
                        ((TextView) linearLayout.findViewById(R.id.kmPrice)).setText("Km $" + vehicleType.getString("precio_km"));

                        //Glide.with(getActivity()).setDefaultRequestOptions(unselectedOption).load(getResources().getString(R.string.apiAdmin) + "images/Uploads/" + vehicleType.getString("unselected")).into(vehicleImage);
                        Glide.with(getActivity()).load(getResources().getString(R.string.apiAdmin) + "images/Uploads/" + vehicleType.getString("unselected")).apply(InactiveOptions).into(inactiveVehicleImage);
                        Glide.with(getActivity()).load(getResources().getString(R.string.apiAdmin) + "images/Uploads/" + vehicleType.getString("selected")).apply(ActiveOptions).into(vehicleImage);

                        if (vehicleType.getInt("id") != vehicleSelected.getInt("id")) {
                          //Glide.with(getActivity()).setDefaultRequestOptions(selectedOption).load(getResources().getString(R.string.apiAdmin) + "images/Uploads/" + vehicleSelected.getString("selected")).into(vehicleImage);
                            showInactive(vehicleImage, inactiveVehicleImage);

                        }


                        linearLayout.setOnClickListener(view -> {
                            try {
                                int index = ((LinearLayout) view.getParent()).indexOfChild(view);
                                int viewsCount = scrollView.getChildCount();

                                for (int i1 = 0; i1 < viewsCount; i1++) {
                                    LinearLayout c = (LinearLayout) scrollView.getChildAt(i1);
                                    ImageView vehicleimage = c.findViewById(R.id.vehicleImage);
                                    ImageView inactivevehicleImage = c.findViewById(R.id.inactiveVehicleImage);


                                    if (i1 == index) {
                                        vehicleSelected = vehiclesByType.getJSONObject(i1);
                                        showActive(vehicleImage, inactiveVehicleImage);
                                    } else {
                                        showInactive(vehicleimage, inactivevehicleImage);
                                    }
                                }

                                getNearestVehicles();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });

                        scrollView.addView(linearLayout);
                    }

                    scrollView.post(() -> {
                        swipeContainerY = Objects.requireNonNull(getView()).findViewById(swipeContainer).getY();
                        hideSwipeContainer();
                    });

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void showSwipeContainer() {
        vehiclesHidden = false;

        float moveY = Utils.convertDpToPixel(0, Objects.requireNonNull(getContext()));
        swipeArrow.setImageResource(R.drawable.ic_arrow_down);
        runSwipeContainer(moveY);
    }

    public void hideSwipeContainer() {
        vehiclesHidden = true;
        float moveY = Utils.convertDpToPixel(58, Objects.requireNonNull(getContext()));
        swipeArrow.setImageResource(R.drawable.ic_arrow_up);
        runSwipeContainer(moveY);
    }

    public void runSwipeContainer(float moveY){
        openDestinationButton.setEnabled(false);
        Objects.requireNonNull(getView()).findViewById(swipeContainer).animate().translationY(moveY).withEndAction(() -> {

            final Handler enableHandler = new Handler();

            enableHandler.postDelayed(
                    () -> {
                        openDestinationButton.setEnabled(true);
                        enableHandler.removeCallbacksAndMessages(null);
                    }, 3000);

        });
    }

    public void searchByLocation(Location location) {
        if (location == null) {
            Utils.showAlert(getActivity(), "Verifica los permisos de GPS");
            return;
        }

        if (!APIRequest.getInternetConnection()){
            Utils.showAlert(getActivity(), "Verifica tu conexión a internet");
            return;
        }

        if (getActivity() != null) {

            Geocoder geocoder = new Geocoder(getActivity().getApplicationContext(), Locale.getDefault());
            List<Address> addresses = null;

            locationSelected = null;

            try {
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (addresses == null || addresses.size()  == 0) {
                Utils.showAlert(getActivity(), "No se encontraron resultados");
            } else {
                Log.d("addresses", addresses.toString());

                String[] addressLine = addresses.get(0).getAddressLine(0).split(",");
                searchText.setText( addressLine[0] + ", " + addressLine[1] );
                boolean hasCityGeocode = true;//addresses.get(0).getAddressLine(0).toLowerCase().contains(getResources().getString(R.string.ciudad_geocode).toLowerCase());

                if (!hasCityGeocode) {
                    timeText.setText("No tenemos cobertura en esta zona");
                } else {
                    try {
                        locationSelected = new JSONObject();
                        locationSelected.put("address", searchText.getText().toString());
                        locationSelected.put("latitude", location.getLatitude());
                        locationSelected.put("longitude", location.getLongitude());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    timeText.setText("Calculando tiempo de espera");
                    getNearestVehicles();
                }
            }

        }

    }

    private void getNearestVehiclesWait() {
        vehiclesTimer.postDelayed(
                () -> {
                    getNearestVehicles();
                    vehiclesTimer.removeCallbacksAndMessages(null);
                },
                1000 * 30);
    }

    private void getNearestVehicles() {
        if (gmap != null ) {
            gmap.clear();
        }

        try {
            if (vehicleSelected != null && locationSelected!=null) {
                final String origin = String.valueOf(locationSelected.getDouble("latitude")) + "," + String.valueOf(locationSelected.getDouble("longitude"));

                APIRequest.GetVehicles(String.valueOf(locationSelected.getDouble("latitude")), String.valueOf(locationSelected.getDouble("longitude")), vehicleSelected.getInt("id"), new RequestInterface() {
                    @Override
                    public void Success(JSONObject response) {
                        try {
                            if (response.getBoolean("status") && response.has("data")) {
                                JSONArray connectedVehicles = response.getJSONArray("data");

                                if (connectedVehicles.length() > 0) {
                                    String destination = connectedVehicles.getJSONObject(0).getDouble("latitude") + "," + connectedVehicles.getJSONObject(0).getDouble("longitude");

                                    APIRequest.DistanceMatrix(origin, destination, new RequestInterface() {
                                        @Override
                                        public void Success(JSONObject response) {
                                            try {
                                                if ( response.getString("status").equals("OK")) {
                                                    JSONArray rows =  response.getJSONArray("rows");

                                                    if (rows.length() > 0) {
                                                        JSONObject element = rows.getJSONObject(0);
                                                        JSONArray elements = element.getJSONArray("elements");

                                                        if (elements.getJSONObject(0).has("duration")) {
                                                            JSONObject duration = elements.getJSONObject(0).getJSONObject("duration");
                                                            timeText.setText(duration.getString("text"));
                                                        }
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

                                    //Add Marker
                                    for (int i = 0; i < connectedVehicles.length(); i++) {
                                        try {
                                            JSONObject connectedVehicle = connectedVehicles.getJSONObject(i);
                                            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(car);
                                            gmap.addMarker(new MarkerOptions().position(new LatLng(connectedVehicle.getDouble("latitude"), connectedVehicle.getDouble("longitude"))).icon(icon));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } else {
                                    timeText.setText("No hay vehículos conectados");
                                }
                            } else {
                                timeText.setText("No hay vehículos conectados");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        getNearestVehiclesWait();
                    }

                    @Override
                    public void Error(JSONObject error) {
                        Log.e("GOPPLUS", getActivity().getResources().getString(R.string.default_error));
                        getNearestVehiclesWait();
                    }
                });
            }

        } catch (JSONException e) {
            e.printStackTrace();
            getNearestVehiclesWait();
        }
    }

    private void getUnratedServicesWait() {
        finishedServicesTimer.postDelayed(
                () -> {
                    getUnratedServices();
                    finishedServicesTimer.removeCallbacksAndMessages(null);
                },
                1000 * 30);
    }

    private void getUnratedServices() {

        if (!rateViewOpen) {
            APIRequest.GetFinishedService(new RequestInterface() {
                @Override
                public void Success(JSONObject response) {
                    try {
                        if (response.getBoolean("status")) {

                            Activity act = getActivity();

                            if (act != null) {
                                Intent intent = new Intent(act, Rate.class);
                                intent.putExtra(Rate.REQUEST_RATE, response.getJSONObject("data").toString());
                                startActivityForResult(intent, Rate.REQUEST_CODE);
                                rateViewOpen = true;
                            } else {
                                Log.d(LCAT, "Is null");
                                getUnratedServicesWait();
                            }
                        } else {
                            getUnratedServicesWait();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        getUnratedServicesWait();
                    }
                }

                @Override
                public void Error(JSONObject error) {
                    Log.e(LCAT, error.toString());
                    getUnratedServicesWait();
                }
            });
        } else {
            return;
        }


    }

}
