package com.pplus.go.Utils;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class SearchLocation {

    public static String searchByLocation(Activity activity, Location location) {
        Geocoder geocoder = new Geocoder(activity.getApplicationContext(), Locale.getDefault());
        List<Address> addresses = null;
        String locationAddress = "";

        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addresses == null || addresses.size()  == 0) {
            Utils.showAlert(activity, "No se encontraron resultados");
        } else {
            String[] addressLine = addresses.get(0).getAddressLine(0).split(",");

            boolean hasCityGeocode = true;//addresses.get(0).getAddressLine(0).toLowerCase().contains(getResources().getString(R.string.ciudad_geocode).toLowerCase());

            if (hasCityGeocode) {
                locationAddress = addressLine[0] + ", " + addressLine[1];
            }
        }

        return locationAddress;
    }
}
