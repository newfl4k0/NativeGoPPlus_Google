package com.pplus.go.app.gopplus;

import android.Manifest;
import android.annotation.SuppressLint;
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

import com.bumptech.glide.Glide;
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

    private static final String LCAT = "ONREQUEST";

    public Onrequest() {
        // Required empty public constructor
    }

    public static Onrequest newInstance() {
        Onrequest fragment = new Onrequest();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        locationManager = (LocationManager) getActivity().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        unselectedOption.placeholder(R.mipmap.vehicle);
        unselectedOption.error(R.mipmap.vehicle);

        selectedOption.placeholder(R.mipmap.vehiclered);
        selectedOption.error(R.mipmap.vehiclered);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        LinearLayout swipeContainer = view.findViewById(R.id.swipeContainer);

        scrollView = getView().findViewById(R.id.vehiclesContainer);
        swipeArrow = getView().findViewById(R.id.swipeArrow);
        openDestinationButton = getView().findViewById(R.id.openDestination);
        searchText = view.findViewById(R.id.searchText);
        timeText = view.findViewById(R.id.time);

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

        (getView().findViewById(R.id.openDestination)).setOnClickListener(view12 -> {
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
        mapView = (MapView) view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

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
        getUnratedServices();
    }

    @Override
    public void onPause() {
        Log.d(LCAT, "ON PAUSE");
        super.onPause();
        mapView.onPause();

        if (vehiclesTimer != null) {
            vehiclesTimer.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        finishedServicesTimer.removeCallbacksAndMessages(null);
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(this.getActivity());
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
                getNearestVehicles();
                pauseMapListener = false;
            }
        }

        if (requestCode == Destination.DESTINATION_REQUEST_CODE) {
            if (resultCode == Destination.DESTINATION_RESULT_SUCCESS) {
                Log.d("ONREQUEST", "service requested");
            }
        }

        if (requestCode == Rate.REQUEST_CODE) {
            rateViewOpen = false;
            getUnratedServicesWait();
        }

        super.onActivityResult(requestCode, resultCode, data);
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
                        new Runnable() {
                            public void run() {
                                addVehicles();
                            }
                        },
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
                        ImageView vehicleImage = (ImageView) linearLayout.findViewById(R.id.vehicleImage);

                        ((TextView) linearLayout.findViewById(R.id.vehicleType)).setText(vehicleType.getString("nombre"));
                        ((TextView) linearLayout.findViewById(R.id.startPrice)).setText("Mínima $" + vehicleType.getString("precio_minimo"));
                        ((TextView) linearLayout.findViewById(R.id.timePrice)).setText("Minuto $" + vehicleType.getString("precio_min"));
                        ((TextView) linearLayout.findViewById(R.id.kmPrice)).setText("Km $" + vehicleType.getString("precio_km"));

                        Glide.with(getActivity()).setDefaultRequestOptions(unselectedOption).load(getResources().getString(R.string.apiAdmin) + "images/Uploads/" + vehicleType.getString("unselected")).into(vehicleImage);

                        if (vehicleType.getInt("id") == vehicleSelected.getInt("id")) {
                          Glide.with(getActivity()).setDefaultRequestOptions(selectedOption).load(getResources().getString(R.string.apiAdmin) + "images/Uploads/" + vehicleSelected.getString("selected")).into(vehicleImage);
                        }


                        linearLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                try {
                                    int index = ((LinearLayout) view.getParent()).indexOfChild(view);
                                    int viewsCount = scrollView.getChildCount();

                                    for (int i = 0; i < viewsCount; i++) {
                                        LinearLayout c = (LinearLayout) scrollView.getChildAt(i);
                                        ImageView vehicleImage = c.findViewById(R.id.vehicleImage);

                                        if (i == index) {
                                            vehicleSelected = vehiclesByType.getJSONObject(i);
                                            Glide.with(getActivity()).setDefaultRequestOptions(selectedOption).load(getResources().getString(R.string.apiAdmin) + "images/Uploads/" + vehicleSelected.getString("selected")).into(vehicleImage);
                                        } else {
                                            Glide.with(getActivity()).setDefaultRequestOptions(unselectedOption).load(getResources().getString(R.string.apiAdmin) + "images/Uploads/" + vehicleSelected.getString("unselected")).into(vehicleImage);
                                        }
                                    }

                                    getNearestVehicles();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        scrollView.addView(linearLayout);
                    }

                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            swipeContainerY = getView().findViewById(R.id.swipeContainer).getY();
                            hideSwipeContainer();
                        }
                    });

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void showSwipeContainer() {
        vehiclesHidden = false;

        float moveY = Utils.convertDpToPixel(0, getContext());
        swipeArrow.setImageResource(R.drawable.ic_arrow_down);
        runSwipeContainer(moveY);
    }

    public void hideSwipeContainer() {
        vehiclesHidden = true;
        float moveY = Utils.convertDpToPixel(58, getContext());
        swipeArrow.setImageResource(R.drawable.ic_arrow_up);
        runSwipeContainer(moveY);
    }

    public void runSwipeContainer(float moveY){
        openDestinationButton.setEnabled(false);
        Objects.requireNonNull(getView()).findViewById(R.id.swipeContainer).animate().translationY(moveY).withEndAction(() -> {

            final Handler enableHandler = new Handler();

            enableHandler.postDelayed(
                    new Runnable() {
                        public void run() {
                            openDestinationButton.setEnabled(true);
                            enableHandler.removeCallbacksAndMessages(null);
                        }
                    }, 3000);

        });
    }

    public void searchByLocation(Location location) {
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
                            if (response.getBoolean("status") == true && response.has("data")) {
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
                                            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.mipmap.car);
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
                        if (response.getBoolean("status") == true) {
                            Intent intent = new Intent(getActivity(), Rate.class);
                            intent.putExtra(Rate.REQUEST_RATE, response.getJSONObject("data").toString());
                            startActivityForResult(intent, Rate.REQUEST_CODE);
                            rateViewOpen = true;
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
