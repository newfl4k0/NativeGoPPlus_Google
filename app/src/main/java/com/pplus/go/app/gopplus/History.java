package com.pplus.go.app.gopplus;

import android.app.Activity;
import android.app.ProgressDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.pplus.go.API.APIRequest;
import com.pplus.go.Utils.Utils;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;


public class History extends AppCompatActivity {

    private final HistoryAdapter historyAdapter = new HistoryAdapter();
    private JSONArray history = new JSONArray();
    private ListView list;
    private Activity historyActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        historyActivity = this;
        Toolbar toolbar = findViewById(R.id.toolbar);

        list = findViewById(R.id.list);
        list.setAdapter(historyAdapter);


        loadHistory();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                loadHistory();
                break;
            default:
                super.onBackPressed();
                break;
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_refresh, menu);
        return true;
    }

    private void loadHistory() {


        final ProgressDialog dialog = Utils.getProgressDialog(historyActivity, "Espera un momento");
        dialog.show();

        APIRequest.History(new RequestInterface() {
            @Override
            public void Success(JSONObject response) {
                dialog.hide();

                if (response.has("data")) {
                    try {
                        history = response.getJSONArray("data");
                        historyAdapter.notifyDataSetChanged();

                        if (history.length() == 0) {
                            Utils.showAlert(historyActivity, "Aún no tienes historial.");
                        }
                    }
                    catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    Utils.showAlert(historyActivity, "Aún no tienes historial.");
                }
            }

            @Override
            public void Error(JSONObject error) {
                dialog.hide();

                Utils.showAlert(historyActivity, "Aún no tienes historial.");
            }
        });
    }

    private class HistoryAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return history.length();
        }

        @Override
        public Object getItem(int i) {
            try {
                return history.getJSONObject(i);
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
            view = getLayoutInflater().inflate(R.layout.row_history, null);
            TextView status   = view.findViewById(R.id.statusText);
            ImageView mapView = view.findViewById(R.id.mapImage);
            TextView driver   = view.findViewById(R.id.driverText);
            TextView car      = view.findViewById(R.id.carText);
            TextView start    = view.findViewById(R.id.startText);
            TextView end      = view.findViewById(R.id.endText);
            TextView date     = view.findViewById(R.id.dateText);

            try {
                JSONObject item = history.getJSONObject(i);
                String points = "";

                start.setText(item.getString("origen"));
                end.setText(item.getString("destino"));
                status.setText(item.getString("estatus"));

                if (item.has("conductor")) {
                    driver.setText("Conductor: " + item.getString("conductor"));
                    car.setText("Vehículo: " + item.getString("marca") + " " + item.getString("modelo") + " " + item.getString("color") + " " + item.getString("placas"));
                }

                if (item.getString("estatus").equals("Cancelado")) {
                    if (item.has("fecha_rechazo")) {
                        date.setText(item.getString("fecha_rechazo"));
                    } else if (item.has("fecha_actualizacion")) {
                        date.setText(item.getString("fecha_actualizacion"));
                    }
                } else {
                    if (item.has("fecha_finalizacion")) {
                        date.setText(item.getString("fecha_finalizacion"));
                    } else if (item.has("fecha_actualizacion")) {
                        date.setText(item.getString("fecha_actualizacion"));
                    }
                }

                if (item.has("ruta") && item.getString("ruta").isEmpty() == false) {
                    String route = item.getString("ruta");
                    points = route.substring(0, route.length() - 1);
                } else {
                    points = String.valueOf(item.getDouble("lat_origen")) + "," + String.valueOf(item.getDouble("lng_origen"));
                }

                Glide.with(historyActivity).load(getString(R.string.staticmapsuri) + getResources().getString(R.string.apiKey) + "&sensor=false&path=" + points ).into(mapView);
            } catch (JSONException ex) {
                ex.printStackTrace();
            }

            return view;
        }
    }

}
