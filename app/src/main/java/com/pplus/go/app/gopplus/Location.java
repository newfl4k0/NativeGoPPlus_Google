package com.pplus.go.app.gopplus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.pplus.go.API.APIRequest;
import com.pplus.go.Utils.RegexValidator;
import com.pplus.go.Utils.Utils;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Location extends AppCompatActivity {
    static final int SUCCESS = 1;

    ListView list;
    EditText searchEditText;
    List<String> foundAddress = new ArrayList<String>();
    ArrayAdapter<String> adapter;
     List<Address> addresses = null;
     private LocationRequest mLocationRequest;
     private android.location.Location mLastLocation;
     private FusedLocationProviderClient mFusedLocationClient;
     private String city = "";
    JSONArray predictions = new JSONArray();
    ProgressDialog dialog;
    Activity locationActivity = this;



    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dialog = Utils.getProgressDialog(this, getResources().getString(R.string.defaultProgress));
        list = findViewById(R.id.list);
        searchEditText = findViewById(R.id.searchText);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, foundAddress);

        list.setOnItemClickListener((adapterView, view, i, l) -> {
            try {
                dialog.show();
                String placeId = predictions.getJSONObject(i).getString("place_id");

                APIRequest.PlaceAPI(placeId, new RequestInterface() {
                    @Override
                    public void Success(JSONObject response) {
                        dialog.hide();

                        try {
                            JSONObject location = response.getJSONObject("result").getJSONObject("geometry").getJSONObject("location");
                            Intent intent = new Intent();

                            intent.putExtra("address", response.getJSONObject("result").getString("name"));
                            intent.putExtra("latitude", location.getDouble("lat"));
                            intent.putExtra("longitude", location.getDouble("lng"));

                            setResult(SUCCESS, intent);
                            finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void Error(JSONObject error) {
                        Log.e("PLACE", error.toString());
                        dialog.hide();
                        Utils.showAlert(locationActivity,  "Algo ha pasado, intenta nuevamente.");
                    }
                });


            } catch (JSONException e) {
                e.printStackTrace();
                Utils.showAlert(locationActivity,  "Algo ha pasado, intenta nuevamente.");
                dialog.hide();
            }
        });

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, l -> {
            if (l != null) {
                Geocoder gc;
                gc = new Geocoder(this,Locale.getDefault());
                try {
                    addresses = gc.getFromLocation(l.getLatitude(),l.getLongitude(),1);
                    city = addresses.get(0).getLocality();
                    //city= "";
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        list.setAdapter(adapter);
        Utils.hideKeyboard(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Utils.hideKeyboard(this);
        super.onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
           super.onLocationResult(locationResult);
           mLastLocation = locationResult.getLastLocation();
            }
        };


    @SuppressLint("MissingPermission")
    public void searchLocation(View view) {

        String location = searchEditText.getText().toString();
        foundAddress.clear();

        if (RegexValidator.validateRequired(location) && location.length()<100 && RegexValidator.isText(location)) {


            mFusedLocationClient.getLastLocation();
            Geocoder geocoder = new Geocoder(this.getApplicationContext(), Locale.getDefault());

            location +=","+city;
            ProgressDialog dialog = Utils.getProgressDialog(this, getResources().getString(R.string.defaultProgress));
            dialog.show();

            try {
                addresses = geocoder.getFromLocationName(location, 10);
            } catch (IOException e) {
                e.printStackTrace();
            }

            dialog.hide();

            if (addresses != null && addresses.size() > 0) {
                for (Address address : addresses) {
                    String[] addressText = address.getAddressLine(0).split(",");
                    foundAddress.add(addressText[0] + addressText[1]);
                }

                adapter.notifyDataSetChanged();
            } else {
                Utils.showAlert(this, "No se encontraron resultados");
            }
        } else {
            String errorMessage  = RegexValidator.replaceMessage(RegexValidator.message_required, "Calle y número");
                   errorMessage += "\n" + RegexValidator.replaceMessage(RegexValidator.message_valid_text, "Calle y número");
                   errorMessage += "\n" + RegexValidator.replaceMessage(RegexValidator.message_less_than, "Calle y número,100");

            Utils.showAlert(this,  errorMessage);
        }

        Utils.hideKeyboard(this);
    }
}
