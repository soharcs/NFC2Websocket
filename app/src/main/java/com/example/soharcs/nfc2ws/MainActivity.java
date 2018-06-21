package com.example.soharcs.nfc2ws;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.example.soharcs.nfcrese.R;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;


public class MainActivity extends AppCompatActivity {

    NfcAdapter mAdapter;
    PendingIntent mPendingIntent;
    private WebSocketClient mWebSocketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //-----------------------------------------
        //Check is there NFC adapter exist or on?
        //-----------------------------------------
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            Toast.makeText(this, "No NFC adapter exist!!!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        try {
            resolveIntent(getIntent());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //--------------------
        //Intent object call
        //--------------------
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    private void resolveIntent(Intent intent) throws UnsupportedEncodingException {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            //NdefMessage[] msgs;
            if (rawMsgs != null) {

                //----------
                //NDEF Tag
                //----------
                Toast.makeText(this, "!NDEF Tag!", Toast.LENGTH_SHORT).show();
            } else {

                //---------------
                // Non NDEF Tag
                //---------------
                Toast.makeText(this, "!NON-NDEF Tag!", Toast.LENGTH_SHORT).show();

                //------------
                //Get Tag ID
                //------------
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                byte[] id = tag.getId();

                //--------------------------
                //Convert Tag ID to String
                //--------------------------
                String serialNumber = bytesToHex(id);
                Toast.makeText(this, "Tag Serial: " + serialNumber, Toast.LENGTH_LONG).show();

                //----------------------
                //Connect to websocket
                //----------------------
                connectWebSocket(serialNumber);
                //sendMessage(serialNumber);
                //disconnectWebSocket();
            }
        }
        // Get instance of Vibrator from current Context
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(300);
    }

    //-----------------------------------
    //Convert TagID hex array to string
    //-----------------------------------
    final protected static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    //--------------------
    //Websocket handling
    //--------------------

    //-----------------
    //Connection open
    //-----------------
    private void connectWebSocket(final String serial) {
        URI uri;
        try {
            uri = new URI("ws://192.168.1.101:8090/");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                mWebSocketClient.send(serial);
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(MainActivity.this, "Message from websocket server: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }

}
