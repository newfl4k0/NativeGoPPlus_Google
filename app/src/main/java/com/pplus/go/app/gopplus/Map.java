package com.pplus.go.app.gopplus;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.app.AlertDialog;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.firebase.iid.InstanceIdResult;
import com.pplus.go.API.APIRequest;
import com.pplus.go.Data.Database;
import com.pplus.go.Utils.Utils;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.FragmentTransaction;

import java.util.Objects;


public class Map extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, Onrequest.OnFragmentInteractionListener {
    NavigationView navigationView;
    Activity mapActivity;
    private boolean updateProfile = true;
    private final String LCAT = "MAP";

    private FragmentTransaction fragmentManager;
    private String fragmentType = "";
    private Onboard onboardInstance = null;
    private Onrequest onrequestInstance = null;
    private Handler updateProfileHandler;
    private Handler loopServiceHandler;
    private Handler loopSyncHandler;

    private boolean onboardReplaced;
    private boolean onrequestReplaced;

    private RequestOptions imageProfileOptions = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .error(R.mipmap.defaultprofile);


    private void loopService() {
        try {
        setupUserImage();
        APIRequest.GetActiveService(new RequestInterface() {
            @Override
            public void Success(JSONObject response) {
                try {
                    JSONObject serviceObject = response;

                    if (fragmentType != "Onboard") {
                        if (!onboardReplaced) {
                            onboardInstance = Onboard.newInstance();
                            fragmentType = "Onboard";
                            fragmentManager = getSupportFragmentManager().beginTransaction();
                            fragmentManager.replace(R.id.frameContainer, onboardInstance);
                            fragmentManager.commitAllowingStateLoss();
                            getSupportFragmentManager().executePendingTransactions();
                            onboardReplaced = true;
                            onrequestReplaced = false;
                        }
                    }

                    if (onboardReplaced && onboardInstance.isAdded()) {
                        onboardInstance.serviceData(serviceObject.getJSONObject("data"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (updateProfile) {
                    loopServiceHandler.postDelayed(
                            () -> loopService(), 1000 * 10);
                }
            }

            @Override
            public void Error(JSONObject error) {

                boolean continueOnrequest = true;

                try {
                    if (error.has("code")) {
                        if (error.getInt("code") == 503) {
                            continueOnrequest = false;
                            logout();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                if (continueOnrequest) {
                    if (fragmentType != "Onrequest") {
                        if (!onrequestReplaced) {
                            onrequestInstance = Onrequest.newInstance();
                            fragmentType = "Onrequest";
                            fragmentManager = getSupportFragmentManager().beginTransaction();
                            fragmentManager.replace(R.id.frameContainer, onrequestInstance);
                            fragmentManager.commitAllowingStateLoss();
                            getSupportFragmentManager().executePendingTransactions();
                            onboardReplaced = false;
                            onrequestReplaced = true;
                        }
                    }

                    if (!Objects.requireNonNull(getSupportActionBar()).isShowing()) {
                        getSupportActionBar().show();
                    }

                    if (updateProfile) {
                        loopServiceHandler.postDelayed(
                                new Runnable() {
                                    public void run() {
                                        loopService();
                                    }
                                }, 1000 * 10);
                    }
                }
            }
        });
    } catch (Exception e) {
        Log.e(LCAT, e.toString());
        loopServiceHandler.postDelayed(
                () -> loopService(), 1000 * 2);
    }
    }


    private void loopSync() {
        APIRequest.Sync(new RequestInterface() {
            @Override
            public void Success(JSONObject response) {
                try {
                    JSONArray settings = response.getJSONArray("settings");
                    JSONArray vehiclesByType = response.getJSONArray("vehiclesByType");

                    for (int i = 0; i<settings.length(); i++) {
                        JSONObject setting = settings.getJSONObject(i);
                        String key = setting.getString("k");
                        String value = setting.getString("v");
                        Database.Insert(mapActivity, key, value);
                    }

                    Database.Insert(mapActivity, "vehiclesByType", vehiclesByType.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (updateProfile) {
                    loopSyncHandler.postDelayed(new Runnable() {
                        public void run() {
                            loopSync();
                        }
                    }, 1000 * 60 * 5);
                }


            }

            @Override
            public void Error(JSONObject error) {
                if (updateProfile) {
                    loopSyncHandler.postDelayed(new Runnable() {
                        public void run() {
                            loopSync();
                        }
                    },1000 * 10);
                }
            }
        });
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Utils.showAlert(mapActivity, intent.getStringExtra("message"));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        onboardReplaced = false;
        onrequestReplaced = false;
        onboardInstance = Onboard.newInstance();
        onrequestInstance = Onrequest.newInstance();
        mapActivity = this;

        /**
         * Replace Toolbar
         */
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /**
         * Setup Navigation Drawer
         */
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        fragmentManager = getSupportFragmentManager().beginTransaction();
        fragmentManager.replace(R.id.frameContainer, Onrequest.newInstance());
        fragmentManager.commit();
        fragmentType = "Onrequest";

        if (Database.getUserId(this) == 0) {
            openLogin();
        } else {
            APIRequest.setQueue(this);
            updateProfileHandler = new Handler();
            loopServiceHandler = new Handler();
            loopSyncHandler = new Handler();

            updateProfile = true;
            setupUserProfile();
            loopService();
            loopSync();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();

        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver),
                new IntentFilter("MyData")
        );
    }


    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.option_map) {
            //Just close the navbar, map is the main activity
        } else if (id == R.id.option_history) {
            startActivity(new Intent(this, History.class));
        } else if (id == R.id.option_profile) {
            startActivity(new Intent(this, Profile.class));
        } else if (id == R.id.option_discount) {
            startActivity(new Intent(this, Discount.class));
        } else if (id == R.id.option_terms) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Utils.getString(Map.this, R.string.default_terms))));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onFragmentInteraction(JSONObject data) {

    }

    public void doLogout(View view) {
        String message = ((onboardReplaced) ? "Tienes un servicio en curso " : "") + "¿Seguro de cerrar la sesión?";
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getResources().getString(R.string.app_name));
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                logout();
            }
        });
        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.show();
    }

    private void logout() {
        APIRequest.cancelQueue();
        Database.Clear(mapActivity);
        updateProfile = false;
        onboardReplaced = false;
        onrequestReplaced = false;
        updateProfileHandler.removeCallbacksAndMessages(null);
        loopServiceHandler.removeCallbacksAndMessages(null);
        loopSyncHandler.removeCallbacksAndMessages(null);
        openLogin();
        finish();
    }

    private void openLogin() {
        startActivity(new Intent(this, Login.class));
        finish();
    }

    private void setupUserImage() {
        String user = Database.Select(this, getResources().getString(R.string.db_user));

        if (!user.isEmpty()) {
            try {
                JSONObject userObject = new JSONObject(user);
                String fbid = userObject.getString("fbid");
                ImageView imageProfile = (ImageView) navigationView.getHeaderView(0).findViewById(R.id.profileImage);

                String profileURL = "";

                if (!fbid.isEmpty()) {
                    profileURL = "https://graph.facebook.com/" + fbid + "/picture?type=normal&height=100&width=100";

                } else {
                    profileURL = getResources().getString(R.string.apiEndpoint) + "profile-image?id=" + String.valueOf(userObject.getInt("id"));
                }

                try {
                    Glide.with(getApplicationContext())
                            .load(profileURL)
                            .apply(imageProfileOptions)
                            .apply(RequestOptions.circleCropTransform())
                            .into(imageProfile);
                } catch (Exception e) {
                    e.printStackTrace();
                    Glide.with(getApplicationContext()).load(R.mipmap.defaultprofile).into(imageProfile);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupUserProfile() {
        String user = Database.Select(this, getResources().getString(R.string.db_user));

        if (!user.isEmpty()) {
            try {
                JSONObject userObject = new JSONObject(user);
                String name = userObject.getString("nombre");
                String fbid = userObject.getString("fbid");
                ((TextView) navigationView.getHeaderView(0).findViewById(R.id.userName)).setText(name);
                ImageView imageProfile = navigationView.getHeaderView(0).findViewById(R.id.profileImage);

                if (!fbid.isEmpty()) {
                    Glide.with(this).load("http://graph.facebook.com/" + fbid + "/picture?type=normal&height=100&width=100").apply(RequestOptions.circleCropTransform()).into(imageProfile);
                } else {
                    Glide.with(mapActivity).load(getResources().getString(R.string.apiEndpoint) + "profile-image?id=" + String.valueOf(userObject.getInt("id"))).apply(RequestOptions.circleCropTransform()).into(imageProfile);
                }
                FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener( mapActivity, instanceIdResult -> {
                    String token = instanceIdResult.getToken();
                    String lastToken = Database.Select(mapActivity, "fcm");
                    //String token = FirebaseInstanceId.getInstance().getToken();

                    if (!Objects.requireNonNull(token).isEmpty() && !lastToken.equals(token)) {
                        Database.Insert(mapActivity, "fcm", token);

                        APIRequest.PushNotification(token, new RequestInterface() {
                            @Override
                            public void Success(JSONObject response) {
                                Log.d(LCAT, response.toString());
                            }

                            @Override
                            public void Error(JSONObject error) {
                                Log.e(LCAT, error.toString());
                            }
                        });
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (updateProfile) {
                updateProfileHandler.postDelayed(
                        new Runnable() {
                            public void run() {
                                setupUserProfile();
                            }
                        },
                        1000 * 15);
            }
        }
    }
}
