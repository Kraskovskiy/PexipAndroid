package com.kab.pixipforandroid;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.pexip.pexkit.Conference;
import com.pexip.pexkit.Event;
import com.pexip.pexkit.IStatusResponse;
import com.pexip.pexkit.PexKit;
import com.pexip.pexkit.ServiceResponse;

import org.json.JSONObject;

import java.net.URI;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Conference conference = null;
    private PexKit pexContext = null;
    private GLSurfaceView videoView;
    Toolbar toolbar;
    EditText etMessage;
    EditText etRoom;
    EditText etPin;
    ToggleButton tbLogin;
    TextView txtChat;
    ViewGroup mViewGroup;
    ScrollView scrollView;
    public static AsyncReadLog mAsyncReadLog;
    public String mUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!supportES2()) {
            Toast.makeText(this, "OpenGl ES 2.0 is not supported", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        mViewGroup = (ViewGroup) this.findViewById(android.R.id.content);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        etMessage = (EditText) findViewById(R.id.etMessage);
        etMessage.setFocusableInTouchMode(true);
        etMessage.requestFocus();
        etMessage.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    sendMessageTo(v);
                    return true;
                }
                return false;
            }
        });
        etRoom = (EditText) findViewById(R.id.etRoom);
        etPin = (EditText) findViewById(R.id.etPin);

        tbLogin = (ToggleButton) findViewById(R.id.tbLogin);
        tbLogin.setOnClickListener(this);

        scrollView = (ScrollView) this.findViewById(R.id.scrollView);

        etRoom.setText(SPC.getInstance(getApplicationContext()).getRoom());
        etPin.setText(SPC.getInstance(getApplicationContext()).getPin());

        txtChat = (TextView)   findViewById(R.id.txtChat);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               sendMessageTo(view);
            }
        });
            iniPexkit();
    }

    public void txtToChat() {
        mAsyncReadLog = new AsyncReadLog(getApplicationContext(), mViewGroup);
        mAsyncReadLog.execute("");
     }

    public void iniPexkit() {
        try {
        videoView = (GLSurfaceView) findViewById(R.id.videoView);
        pexContext = PexKit.create(getBaseContext(), (GLSurfaceView) findViewById(R.id.videoView));
        pexContext.moveSelfView(3,3,20,20);
            Log.e("MainActivity", "done initializing pexkit");
        } catch (Exception e) {
            Log.e("OnCreate", e.toString());
        }
    }

     public void createConference(){
         Random rnd = new Random();
         mUserName = "Android Example App_"+rnd.nextInt(1000);
        try {
           conference = new Conference(mUserName, new URI(etRoom.getText().toString()), etPin.getText().toString());
            Log.i("createConference", "done initializing pexkit");
        } catch (Exception e) {
            Log.i("createConference", e.toString());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        videoView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        videoView.onResume();
    }

    public void onLoginNew() {
       createConference();
       conference.connect(new IStatusResponse() {
            @Override
            public void response(ServiceResponse status) {
                conference.requestToken(new IStatusResponse() {
                    @Override
                    public void response(ServiceResponse status) {
                        Log.i("onLoginNew", "req token status is " + status);
                        conference.escalateMedia(pexContext, new IStatusResponse() {
                            @Override
                            public void response(ServiceResponse status) {
                               // conference.setAudioMute(true);
                                Log.i("onLoginNew", "escalate call status is " + status);
                                conference.listenForEvents();
                               // conference.setAudioMute(true);
                                // examples of muting video and getting audio mute status
                                // conference.setVideoMute(true);
                                // conference.getAudioMute();
                            }
                        });
                        tbLogin.setChecked(true);
                        txtToChat();
                    }
                });
            }
        });

    }

    public void getEvents(String text) {
        AsyncReadLog.event.add(new Event("message_received", conference.getUUID().toString(), createJsonString(text)));
        mAsyncReadLog.updateTxtMessage();
}
    public String createJsonString(String text){
        try {
            JSONObject e = new JSONObject();
            e.put("type", "plain/text");
            e.put("payload", text);
            e.put("origin", mUserName);
            e.put("uuid", conference.getUUID().toString());
            Log.e("createJsonString",e.toString());
            return e.toString();
        } catch (Exception var4) {
           return "";
        }
    }

    public void offLoginNew() {
        if (this.conference.isLoggedIn()) {
            Log.i("offLoginNew", "about to release");
            conference.disconnectMedia(new IStatusResponse() {
                @Override
                public void response(ServiceResponse status) {
                    conference.releaseToken(new IStatusResponse() {
                        @Override
                        public void response(ServiceResponse status) {
                            Log.i("offLoginNew", "release token status is " + status);
                            if (status == ServiceResponse.Ok) {
                                tbLogin.setChecked(false);
                                clearData();
                            }
                        }
                    });
                }
            });
        }
    }

    public void clearData() {
        mAsyncReadLog.cancel(false);
        mAsyncReadLog.event.clear();
        txtChat.setText("");
        try {
                    new ProcessBuilder()
                    .command("logcat", "-c")
                    .redirectErrorStream(true)
                    .start();
        } catch (Exception e) {

        }
    }
    public void sendMessageTo(final View  view) {
        if (conference!=null&&conference.isLoggedIn()) {
            conference.sendMessage(etMessage.getText().toString(), new IStatusResponse() {
                @Override
                public void response(ServiceResponse status) {

                    Log.i("sendMessageTo", "sendMessage " + status);
                    if (status == ServiceResponse.Ok) {
                        getEvents(etMessage.getText().toString());
                        etMessage.setText("");
                        hideKeyboard(view);

                    }

                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tbLogin:
                saveData();
               if (conference==null||!conference.isLoggedIn()) {
                   onLoginNew();
               }
                else {
                   offLoginNew();
               }
                break;
        }
    }

    public void hideKeyboard(View view) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void saveData() {
        SPC.getInstance(getApplicationContext()).setRoom(etRoom.getText().toString());
        SPC.getInstance(getApplicationContext()).setPin(etPin.getText().toString());
    }

    private boolean supportES2() {
        ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        return (configurationInfo.reqGlEsVersion >= 0x20000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
