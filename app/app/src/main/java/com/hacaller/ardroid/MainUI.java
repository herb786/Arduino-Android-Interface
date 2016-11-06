package com.hacaller.ardroid;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;

import static com.hacaller.ardroid.IUsbInit.ACTION_NO_USB;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_PERMISSION;
import static com.hacaller.ardroid.IUsbInit.MESSAGE_FROM_SERIAL_PORT;

public class MainUI extends AppCompatActivity {

    @BindView(R.id.redLedsOn) Button redLedsOn;
    @BindView(R.id.greenLedsOn) Button greenLedsOn;
    @BindView(R.id.yellowLedsOn) Button yellowLedsOn;
    @BindView(R.id.allLedsOff) Button allLedsOff;
    @BindView(R.id.allLedsOn) Button allLedsOn;
    @BindView(R.id.msgArduino) TextView msgArduino;

    private UsbService usbService;
    private UiHandler mHandler;
    boolean mBound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_ui);

        mHandler = new UiHandler(this);

        Button redLedsOn = (Button) findViewById(R.id.redLedsOn);
        redLedsOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = "Hello!";
                if (usbService != null) {
                    // if UsbService was correctly binded, Send data
                    usbService.write(data.getBytes());
                }
            }
        });

    }


    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to UsbService, cast the IBinder and get UsbService instance
            UsbService.UsbBinder binder = (UsbService.UsbBinder) service;
            usbService = binder.getService();
            usbService.setHandler(mHandler);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private void startUsbService(){
        Intent intent = new Intent(this, UsbService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startUsbService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private static class UiHandler extends Handler {
        private final WeakReference<MainUI> mActivity;

        public UiHandler(MainUI activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    mActivity.get().msgArduino.setText(data);
                    break;
            }
        }
    }

}
