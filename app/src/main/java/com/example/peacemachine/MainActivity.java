package com.example.peacemachine;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import android.os.IBinder;
import android.util.Log;

import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private String TAG = "Peace Machine";

    private AudioService mAudioService;
    private boolean mServiceIsBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mAudioService = ((AudioService.LocalBinder)service).getService();
            onServiceCreated();
        }

        public void onServiceDisconnected(ComponentName className) {
            mAudioService = null;
            Toast.makeText(MainActivity.this, R.string.local_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        Intent intent = new Intent(MainActivity.this, AudioService.class);
        startService(intent);

        if (bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            mServiceIsBound = true;
        } else {
            mServiceIsBound = false;
            Log.e(TAG, "Error: The requested service doesn't " +
                    "exist, or this client isn't allowed access to it.");
        }
    }

    void doUnbindService() {
        if (mServiceIsBound) {
            unbindService(mConnection);
            mServiceIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mAudioService != null) {
            if(isFinishing()) {
                mAudioService.destroy();
                stopService(new Intent(getApplicationContext(), AudioService.class));
            }
            doUnbindService();
            mAudioService = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        doBindService();
    }

    public void onServiceCreated() {
        WebView myWebView = (WebView) findViewById(R.id.webview);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.addJavascriptInterface(new PeaceMachineInterface(this), "PeaceMachineInterface");
        WebSettings settings = myWebView.getSettings();
        settings.setDomStorageEnabled(true);
        myWebView.loadUrl("file:///android_asset/web/index.html");
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * Javascript interface for use by the WebView
     */
    public class PeaceMachineInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        PeaceMachineInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void handleFloat(String control, float val) {
            Log.i(control + " changed: ", Float.toString(val));

            if(control.equals("pm-control-downers")) {
                mAudioService.setLPFreq(val);
            }
            else if(control.equals("pm-control-uppers")) {
                mAudioService.setVolume(val);
            }
        }
    }
}
