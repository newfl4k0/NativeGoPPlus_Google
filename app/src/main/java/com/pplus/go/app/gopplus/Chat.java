package com.pplus.go.app.gopplus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.pplus.go.API.APIRequest;
import com.pplus.go.Utils.Utils;
import com.pplus.go.Utils.RegexValidator;
import com.pplus.go.app.gopplus.Interfaces.RequestInterface;
import app.GoPPlus.R;

public class Chat extends AppCompatActivity {

    private Activity chatActivity;
    private Handler chatTimer;
    private ChatAdapter chatAdapter = new ChatAdapter();
    private ListView list;
    private JSONArray messages;
    private EditText messageText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        /*setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);*/

        chatActivity = this;
        messages = new JSONArray();
        chatTimer = new Handler();
        list = findViewById(R.id.list);
        list.setAdapter(chatAdapter);

        messageText = ((EditText) findViewById(R.id.messageText));

        getChatLoop();
    }

    private void getChatLoop() {
        chatTimer.removeCallbacksAndMessages(null);
        getChat();

        chatTimer.postDelayed(
                new Runnable() {
                    public void run() {
                        getChatLoop();
                    }
                },
                1000 * 10);
    }

    @Override
    protected void onResume() {
        Utils.hideKeyboard(chatActivity);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        chatTimer.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    public void getChat() {
        int serviceId  = getIntent().getExtras().getInt("id");
        int driverId   = getIntent().getExtras().getInt("driverId");

        APIRequest.GetChat(serviceId, driverId, new RequestInterface() {
            @Override
            public void Success(JSONObject response) {
                try {
                    messages = response.getJSONArray("data");
                    chatAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void Error(JSONObject error) {
                finish();
            }
        });
    }

    public void doSendMessage(View view) {
        Utils.hideKeyboard(chatActivity);
        int serviceId  = getIntent().getExtras().getInt("id");
        int driverId   = getIntent().getExtras().getInt("driverId");
        String message = messageText.getText().toString();

        if (RegexValidator.validateRequired(message) == false) {
            Utils.showAlert(chatActivity, RegexValidator.replaceMessage( RegexValidator.message_required, "Mensaje"));
        } else if (RegexValidator.isText(message) == false) {
            Utils.showAlert(chatActivity, RegexValidator.replaceMessage( RegexValidator.message_valid_text, "Mensaje"));
        } else {
            final ProgressDialog dialog = Utils.getProgressDialog(chatActivity, getResources().getString(R.string.defaultProgress));
            dialog.show();

            APIRequest.sendChat(serviceId, driverId, message, new RequestInterface() {
                @Override
                public void Success(JSONObject response) {
                    dialog.hide();
                    messageText.setText("");
                }

                @Override
                public void Error(JSONObject error) {
                    dialog.hide();

                    try {
                        Utils.showAlert(chatActivity, error.getString("message"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private class ChatAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return messages.length();
        }

        @Override
        public Object getItem(int i) {
            try {
                return messages.getJSONObject(i);
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
            try {
                JSONObject message = messages.getJSONObject(i);

                if (message.getInt("es_conductor") == 1) {
                    view = getLayoutInflater().inflate(R.layout.row_chatdriver, null);
                } else {
                    view = getLayoutInflater().inflate(R.layout.row_chatclient, null);
                }

                ((TextView) view.findViewById(R.id.messageText)).setText(message.getString("mensaje"));
                ((TextView) view.findViewById(R.id.dateText)).setText(message.getString("fecha"));
                Glide.with(chatActivity).load(message.getString("img")).apply(RequestOptions.circleCropTransform()).into(((ImageView) view.findViewById(R.id.chatImage)));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return view;
        }
    }


}
