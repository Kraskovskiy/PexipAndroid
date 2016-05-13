package com.kab.pixipforandroid;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.pexip.pexkit.Event;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by Suxx on 11.05.2016.
 */
//TODO replace AsyncTask by Service
public class AsyncReadLog extends AsyncTask<String, Void, String> {

    private Context mContext;
    private View mView;
    private static final String TAG = AsyncReadLog.class.getCanonicalName();
    private static final String processId = Integer.toString(android.os.Process.myPid());

    static ArrayList<Event> event= new ArrayList<>();
    static String type="";
    static String id="";
    static String data="";
    private TextView txtChat;
    StringBuilder sb = new StringBuilder();
    ScrollView scrollView;

    public AsyncReadLog(Context context,View view) {
        this.mContext = context;
        this.mView = view;
        txtChat = (TextView) view.findViewById(R.id.txtChat);
        scrollView = (ScrollView) view.findViewById(R.id.scrollView);
    }

    protected void onPreExecute() {
    //
    }

    public void scrolToDown() {
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    public static String onMessageReceivedToString(Event event) {
        try {
            JSONObject e = new JSONObject(event.data);
            String origin = (String)e.get("origin");
            String content = (String)e.get("payload");
            String uuid = (String)e.get("uuid");
           // Log.e("My.events!!!!", String.format("%s"+": "+"%s", new Object[]{origin,content}));
            return String.format("%s"+": "+"%s", new Object[]{origin,content});
            // this.messageReceived(origin, uuid, content);
        } catch (Exception var6) {
            Log.e("My!!!.events", "Failed to parse message received " + event.data + " exception " + var6);
            return "";
        }

    }
    @Override
    protected void onProgressUpdate(Void... pr) {
        updateTxtMessage();
    }

    public  void updateTxtMessage(){
        txtChat.setText("");
        StringBuilder sb = new StringBuilder();

        for (Event e: event) {
           sb.append(txtChat.getText()).append(onMessageReceivedToString(e)).append("\n");
        }

        txtChat.setText(sb.toString());
        scrolToDown();
    }

    @Override
    protected String doInBackground(String... arg0) {
        try {
            String[] command = new String[] { "logcat", "-v", "threadtime" };

            Process process = Runtime.getRuntime().exec(command);

            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while (((line = bufferedReader.readLine()) != null && !MainActivity.mAsyncReadLog.isCancelled()  )){
                if(line.contains(processId)&&line.contains("pexkit.EventSource")) {

                    if(line.contains("event:")) {
                        type=line.substring(line.indexOf("event:"));
                        sb.append(type+"\n");
                    }
                    if(line.contains("id:")) {
                        id=line.substring(line.indexOf("id:"));
                        sb.append(id+"\n");
                    }

                    if(line.contains("data:")) {
                        data=line.substring(line.indexOf("data:"));
                        sb.append(data+"\n");
                       // sb.append(data);

                        saveMessageFromReceiver(type);
                        publishProgress();
                    }
                }

            }
        }
        catch (IOException ex) {
            Log.e(TAG, "getCurrentProcessLog failed", ex);
        }

        return sb.toString();

    }

    public void saveMessageFromReceiver(String txtEvent){
        if (txtEvent.contains("message_received")) {
            event.add(new Event(type.replaceFirst("event:", ""), id.replaceFirst("id:", ""), data.replaceFirst("data:", "")));
        }
    }

    @Override
    protected void onPostExecute(String result) {
        txtChat.setText(result);
        Log.e("onPostExecute", "!!!!");
    }
}
