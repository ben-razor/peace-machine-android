package com.benrazor.peacemachine;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jsyn.data.FloatSample;
import com.jsyn.util.SampleLoader;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import android.os.IBinder;
import android.util.Log;

import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends Activity {
    private String TAG = "Peace Machine";

    private AudioService mAudioService;
    private Audio mAudio;
    private boolean mServiceIsBound;
    private WebView webView;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mAudioService = ((AudioService.LocalBinder)service).getService();
            mAudio = mAudioService.getAudio();

            onServiceCreated();
        }

        public void onServiceDisconnected(ComponentName className) {
            mAudioService = null;
            mAudio = null;
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

        Log.d(TAG, "onDestroy");
        if(mAudioService != null) {
            if(isFinishing()) {
                mAudio.setVolume(0, 0.05f);
                stopService(new Intent(getApplicationContext(), AudioService.class));
            }
            doUnbindService();
            mAudioService = null;
            mAudio = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        doBindService();
    }

    public void onServiceCreated() {
        webView = (WebView) findViewById(R.id.webview);
        webView.setWebContentsDebuggingEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new PeaceMachineInterface(this), "PeaceMachineInterface");
        WebSettings settings = webView.getSettings();
        settings.setDomStorageEnabled(true);
        if(mAudioService.audioInitialized) {
            webView.loadUrl("file:///android_asset/web/index.html?resume=1");
        }
        else {
            webView.loadUrl("file:///android_asset/web/index.html");
        }
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
     * WebView.evaluateJavascript returns an escaped string wrapped in double quotes.
     *
     * This function turns this into a string that will be valid when passed to a JSON
     * parser.
     *
     * @param s The input string
     * @return The cleaned String
     */
    public String cleanReceivedJSON(String s) {
        return s.substring(1, s.length() - 1)
                .replace("\\\\", "\\")
                .replace("\\\"", "\"");
    }

    public void runJS(final String js, final ValueCallback<String> valueCallback) {
        webView.post(new Runnable() {
            @Override
            public void run() { webView.evaluateJavascript(js, valueCallback); }
        });
    }

    /**
     * Javascript interface for use by the WebView
     */
    public class PeaceMachineInterface {
        Context mContext;
        Gson gson = new Gson();

        /** Instantiate the interface and set the context */
        PeaceMachineInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void initAudio() {
            if(!mAudioService.audioInitialized) {
                runJS("pMachine.getVibesConfig()" , new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        s = cleanReceivedJSON(s);
                        List<VibeInfo> vibeInfos = gson.fromJson(s, new TypeToken<List<VibeInfo>>(){}.getType());
                        mAudio.addVibeInfos(vibeInfos);

                        for (VibeInfo vibeInfo : vibeInfos) {
                            if(vibeInfo.audio.contains(".")) {
                                loadSample(vibeInfo.id, vibeInfo.audio);
                            }
                            mAudio.addVibe(vibeInfo);
                        }

                    }
                });
                mAudioService.audioInitialized = true;
            }
        }

        @JavascriptInterface
        public void handleFloat(String control, float val, float t) {
            if(control.equals("pm-control-downers")) {
                mAudio.setLPFreq(val, t);
            }
            else if(control.equals("pm-control-uppers")) {
                mAudio.setVolume(val, t);
            }
        }

        @JavascriptInterface
        public void selectVibe(final String vibeID) {
            Log.d(TAG, "selectVibe()");
            mAudio.changeVibe(vibeID);
        }

        public void loadSample(String id, String path) {
            Log.d(TAG, "loadSample()");
            try {
                InputStream iStr = getAssets().open("web/audio/" + path);
                FloatSample sample = SampleLoader.loadFloatSample(iStr);
                mAudio.addSample(id, sample);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public void turnOn() {
            runJS("pMachine.handleTurnOn()", null);
        }

        @JavascriptInterface
        public void turnOff() {
            Log.d(TAG, "turnOff");
            finishAndRemoveTask();
        }
    }
}
