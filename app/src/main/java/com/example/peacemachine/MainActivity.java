package com.example.peacemachine;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.IBinder;
import android.util.Log;

import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class MainActivity extends Activity {
    // Records whether the service is bound.
    private boolean mShouldUnbind;

    // To invoke the bound service, first make sure that this value
    // is not null.
    private AudioService mBoundService;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((AudioService.LocalBinder)service).getService();

            // Tell the user about this for our demo.
            Toast.makeText(MainActivity.this, R.string.local_service_connected,
                    Toast.LENGTH_SHORT).show();

            onServiceCreated();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            Toast.makeText(MainActivity.this, R.string.local_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        // Attempts to establish a connection with the service.  We use an
        // explicit class name because we want a specific service
        // implementation that we know will be running in our own process
        // (and thus won't be supporting component replacement by other
        // applications).
        Intent intent = new Intent(MainActivity.this, AudioService.class);
        startService(intent);

        if (bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            mShouldUnbind = true;
        } else {
            Log.e("MY_APP_TAG", "Error: The requested service doesn't " +
                    "exist, or this client isn't allowed access to it.");
        }
    }

    void doUnbindService() {
        if (mShouldUnbind) {
            // Release information about the service's state.
            unbindService(mConnection);
            mShouldUnbind = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("Destroy", "destroy");
        if(mBoundService != null) {
            doUnbindService();
            mBoundService = null;
            if(isFinishing()) {
                stopService(new Intent(getApplicationContext(), AudioService.class));
            }
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
        myWebView.loadUrl("file:///android_asset/peace-machine/index.html");
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
        Log.i("Save state", "save state");
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
                mBoundService.setLPFreq(val);
            }
            else if(control.equals("pm-control-uppers")) {
                mBoundService.setVolume(val);
            }
        }
    }
}
