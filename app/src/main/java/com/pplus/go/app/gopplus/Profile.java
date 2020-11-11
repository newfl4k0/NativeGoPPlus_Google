package com.pplus.go.app.gopplus;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.pplus.go.API.APIRequest;
import com.pplus.go.Data.Database;
import com.pplus.go.Utils.Utils;
import com.pplus.go.Utils.RegexValidator;
import com.pplus.go.Utils.PermissionUtils;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;
import com.pplus.go.app.gopplus.R;

public class Profile extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_CAMERA_PERMISSION = 2;
    private boolean canReplaceImage = true;

    private Activity profileActivity;
    private Bitmap imageBitmap;
    private ImageView imageProfile;
    private EditText birthField;
    private EditText nameField;
    private EditText emailField;
    private EditText phoneField;

    private String birthDay;
    private String currentEmail;
    private JSONObject userObject;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        /*setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);*/

        profileActivity = this;

        imageProfile = (ImageView) findViewById(R.id.profileImage);
        birthField = (EditText) findViewById(R.id.birthField);
        nameField = (EditText) findViewById(R.id.nameField);
        emailField = (EditText) findViewById(R.id.emailField);
        phoneField = (EditText) findViewById(R.id.phoneField);

        setUserData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            final ProgressDialog dialog = Utils.getProgressDialog(profileActivity, getResources().getString(R.string.defaultProgress));
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");

            String imageBase64 = Utils.BitMapToString(imageBitmap);
            dialog.show();

            APIRequest.UploadProfileImage(imageBase64, new RequestInterface() {
                @Override
                public void Success(JSONObject response) {
                    try {
                        dialog.hide();
                        Utils.showAlert(profileActivity, response.getString("message"));
                        Glide.with(profileActivity).load(imageBitmap).apply(RequestOptions.circleCropTransform()).into(imageProfile);
                        Database.Insert(profileActivity, getResources().getString(R.string.db_imageprofile), Utils.BitMapToString(imageBitmap));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void Error(JSONObject error) {
                    try {
                        dialog.hide();
                        Utils.showAlert(profileActivity, error.getString("message"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            } else {
                Utils.showAlert(this, "Para actualizar tu foto de perfil activa los permisos para utilizar la cámara");
            }
        }
    }

    public void doUpdate(View view) {
        Utils.hideKeyboard(this);

        final String name = nameField.getText().toString();
        final String email = emailField.getText().toString();
        final String phone = phoneField.getText().toString();
        String catchError = "";


        if (!RegexValidator.validateRequired(birthDay)) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_required, "Cumpleaños");
        }

        if (!RegexValidator.validateRequired(name)) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_required, "Nombre");
        }

        if (!RegexValidator.isName(name)) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_name, "Nombre");
        }

        if (!RegexValidator.validateRequired(phone)) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_required, "Teléfono");
        }

        if (!RegexValidator.isNumeric(phone)) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_numeric, "Teléfono");
        }

        if (phone.length() != 10) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_exact_length, "Teléfono,10");
        }

        if (!RegexValidator.validateRequired(email)) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_required, "Correo Electrónico");
        }

        if (!RegexValidator.isEmail(email)) {
            catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_valid_email, "Correo Electrónico");
        }

        if (catchError.isEmpty()) {
            final ProgressDialog dialog = Utils.getProgressDialog(profileActivity, getResources().getString(R.string.defaultProgress));
            dialog.show();

            APIRequest.Profile(currentEmail, name, phone, email, birthDay, new RequestInterface() {
                @Override
                public void Success(JSONObject response) {
                    dialog.hide();

                    String message = getResources().getString(R.string.default_error);

                    try {
                        if (response.has("status") && response.getBoolean("status") == true) {
                            userObject.put("nombre", name);
                            userObject.put("correo", email);
                            userObject.put("telefono", phone);
                            userObject.put("fechac", birthDay);

                            Database.Insert(profileActivity, getResources().getString(R.string.db_user), userObject.toString());
                            message = response.getString("message");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Utils.showAlert(profileActivity, message);
                }

                @Override
                public void Error(JSONObject error) {
                    dialog.hide();
                    String message = getResources().getString(R.string.default_error);

                    try {
                        message = error.getString("message");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Utils.showAlert(profileActivity, message);
                }
            });

        } else {
            Utils.showAlert(profileActivity, catchError);
        }
    }

    public void doOpenUpdatePassword(View view) {
        Utils.hideKeyboard(this);
        startActivity(new Intent(this, Password.class));
    }

    public void doTakePicture(View view) {
        Utils.hideKeyboard(this);

        if (canReplaceImage) {
            if (PermissionUtils.hasPermission(this, Manifest.permission.CAMERA)) {
                takePicture();
            } else {
                PermissionUtils.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
        }
    }

    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void setStoredImage(String fbid, int id) {
        if (fbid.isEmpty()) {
            String imageStored = Database.Select(profileActivity, getResources().getString(R.string.db_imageprofile));

            if (!imageStored.isEmpty()) {
                Glide.with(profileActivity).load(Utils.StringToBitMap(imageStored)).apply(RequestOptions.circleCropTransform()).into(imageProfile);
            } else {
                Glide.with(profileActivity).load(getResources().getString(R.string.apiEndpoint) + "profile-image?id=" + String.valueOf(id)).apply(RequestOptions.circleCropTransform()).into(imageProfile);
            }
        } else {
            Glide.with(this).load("http://graph.facebook.com/" + fbid + "/picture?type=normal&height=100&width=100").apply(RequestOptions.circleCropTransform()).into(imageProfile);
            ((TextView) findViewById(R.id.updateImageText)).setText("");
            canReplaceImage = false;
        }
    }

    private void setUserData() {
        try {
            String user = Database.Select(this, getResources().getString(R.string.db_user));
            userObject = new JSONObject(user);
            birthDay = userObject.getString("fechac");
            currentEmail = userObject.getString("correo");

            birthField.setText(birthDay);
            nameField.setText(userObject.getString("nombre"));
            emailField.setText(currentEmail);
            phoneField.setText(userObject.getString("telefono"));

            if (userObject.has("fbid")) {
                setStoredImage(userObject.getString("fbid"), userObject.getInt("id"));
            } else {
                setStoredImage("", userObject.getInt("id"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void doOpenCalendar(View view) {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog.OnDateSetListener datePickerDialog = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                calendar.set(i, i1, i2);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
                birthDay = simpleDateFormat.format(calendar.getTime());
                birthField.setText(birthDay);
            }
        };

        DatePickerDialog pickerDialog = new DatePickerDialog(Profile.this,
                datePickerDialog,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.YEAR, -18);
        pickerDialog.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        pickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        calendar.add(Calendar.YEAR, -82);
        pickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());

        if (birthDay.isEmpty() == false) {
            String[] datePart = birthDay.split("-");
            pickerDialog.updateDate(Integer.valueOf(datePart[2]).intValue(), Integer.valueOf(datePart[1]).intValue() -1, Integer.valueOf(datePart[0]).intValue());
        }

        pickerDialog.show();
    }
}
