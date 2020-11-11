package com.pplus.go.app.gopplus;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import com.pplus.go.API.APIRequest;
import com.pplus.go.Utils.Utils;
import com.pplus.go.Utils.RegexValidator;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;



public class Signup extends AppCompatActivity {

    static final int CODE = 1;
    static final int SUCCESS_SIGNUP_FACEBOOK = 1;
    static final int SUCCESS_SIGNUP_MAIL = 2;

    CallbackManager callbackManager;
    LoginButton facebookButton;
    Button facebookCustomButton;
    String facebookId = "";
    EditText birthField;
    EditText nameField;
    EditText phoneField;
    EditText emailField;
    EditText passwordField;
    String birthDay = "";
    Boolean hasAccepted = false;
    Activity signupActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        signupActivity = this;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        /*setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);*/

        APIRequest.setQueue(this);

        callbackManager = CallbackManager.Factory.create();

        birthField = findViewById(R.id.birthField);
        nameField = findViewById(R.id.nameField);
        phoneField = findViewById(R.id.phoneField);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);


        facebookButton = (LoginButton) findViewById(R.id.fbLoginButton);
        facebookCustomButton = (Button) findViewById(R.id.facebookButton);

        facebookButton.setReadPermissions(Arrays.asList("public_profile", "email", "user_birthday"));
        facebookButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

                GraphRequest graphRequest = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {
                            if (object.has("id")) {
                                facebookId = object.getString("id");

                                if (object.has("birthday")) {
                                    String[] birthdayPart = object.getString("birthday").split("/");
                                    birthDay = birthdayPart[1] + "-" + birthdayPart[0] + "-" + birthdayPart[2];
                                    birthField.setText(birthDay);
                                }

                                nameField.setText(object.getString("name"));
                                emailField.setText(object.getString("email"));
                                nameField.setEnabled(false);
                                nameField.setFocusable(false);
                                emailField.setEnabled(false);
                                emailField.setFocusable(false);
                            }
                        } catch (JSONException exception) {
                            exception.printStackTrace();
                            nameField.setEnabled(true);
                            nameField.setFocusable(true);
                            emailField.setEnabled(true);
                            emailField.setFocusable(true);
                        }


                        LoginManager.getInstance().logOut();
                    }
                });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,birthday");
                graphRequest.setParameters(parameters);
                graphRequest.executeAsync();
            }

            @Override
            public void onCancel() {
                Log.d("Facebook", "Facebook Login canceled");
            }

            @Override
            public void onError(FacebookException error) {
                Log.e("Facebook", error.toString());
            }
        });

        Utils.hideKeyboard(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onBackPressed();
        return super.onOptionsItemSelected(item);
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

        DatePickerDialog pickerDialog = new DatePickerDialog(Signup.this,
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

    public void doOpenTermsAndConditions(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Utils.getString(Signup.this, R.string.default_terms))));
    }

    public void doAccept(View view) {
        if (!hasAccepted) {
            findViewById(R.id.acceptButton).setBackgroundColor(getResources().getColor(R.color.red));
        } else {
            findViewById(R.id.acceptButton).setBackgroundColor(getResources().getColor(R.color.gray));
        }

        hasAccepted = !hasAccepted;
    }

    public void doSignup(View view) {
        Utils.hideKeyboard(this);

        if (!hasAccepted) {
            Utils.showAlert(this, "Acepta los términos y condiciones");
        } else {

            String catchError = "";
            String name = nameField.getText().toString();
            String phone = phoneField.getText().toString();
            final String email = emailField.getText().toString();
            final String password = passwordField.getText().toString();


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
                catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_exact_length, "Teléfono");
            }

            if (!RegexValidator.validateRequired(email)) {
                catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_required, "Correo Electrónico");
            }

            if (!RegexValidator.isEmail(email)) {
                catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_valid_email, "Correo Electrónico");
            }

            if (!RegexValidator.validateRequired(password)) {
                catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_required, "Contraseña");
            }

            if (!RegexValidator.isPassword(password)) {
                catchError += "\n" + RegexValidator.replaceMessage(RegexValidator.message_valid_password, "Contraseña");
            }


            if (catchError.isEmpty()) {
                final ProgressDialog dialog = Utils.getProgressDialog(signupActivity, "Espera un momento");
                dialog.show();

                APIRequest.Register(name, email, phone, password, birthDay, facebookId, new RequestInterface() {
                    @Override
                    public void Success(JSONObject response) {
                        dialog.hide();

                        try {
                            if (response.getBoolean("status") == true) {
                                String token = response.getString("token");
                                int id = response.getInt("id");
                                int clientId = response.getInt("cl");

                                Intent result = new Intent();
                                result.putExtra("token", token);
                                result.putExtra("id", id);
                                result.putExtra("client_id", clientId);
                                result.putExtra("email", email);
                                result.putExtra("password", password);

                                if (facebookId.isEmpty()) {
                                    setResult(Signup.SUCCESS_SIGNUP_MAIL, result);
                                } else {
                                    setResult(Signup.SUCCESS_SIGNUP_FACEBOOK, result);
                                }

                                finish();
                            } else {
                                Utils.showAlert(signupActivity, response.getString("message"));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Utils.showAlert(signupActivity, "Algo ha pasado, intenta nuevamente");
                        }
                    }

                    @Override
                    public void Error(JSONObject error) {
                        dialog.hide();

                        String message = "Algo ha pasado, intenta nuevamente";

                        try {
                            message = error.getString("message");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Utils.showAlert(signupActivity, message);
                    }
                });
            } else {
                Utils.showAlert(signupActivity, catchError);
            }
        }
    }

    public void doFacebookLogin(View view) {
        if (view == facebookCustomButton) {
            facebookButton.performClick();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
