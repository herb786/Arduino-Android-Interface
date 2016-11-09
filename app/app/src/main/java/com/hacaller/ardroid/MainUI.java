package com.hacaller.ardroid;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.hacaller.ardroid.IUsbInit.ACTION_NO_USB;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_DISCONNECTED;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_NOT_SUPPORTED;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_PERMISSION_GRANTED;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_PERMISSION_NOT_GRANTED;
import static com.hacaller.ardroid.IUsbInit.ALERT;
import static com.hacaller.ardroid.IUsbInit.MESSAGE_FROM_SERIAL_PORT;

public class MainUI extends AppCompatActivity {

    @BindView(R.id.testBoard) Button testBoard;
    @BindView(R.id.redLedsOn) Button redLedsOn;
    @BindView(R.id.greenLedsOn) Button greenLedsOn;
    @BindView(R.id.yellowLedsOn) Button yellowLedsOn;
    @BindView(R.id.allLedsOff) Button allLedsOff;
    @BindView(R.id.allLedsOn) Button allLedsOn;
    @BindView(R.id.msgArduino) TextView msgArduino;

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private UsbService usbService;
    private UiHandler mHandler;
    boolean mBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to UsbService, cast the IBinder and get UsbService instance
            Log.d("::Ardroid", "Starting service connection");
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_ui);
        ButterKnife.bind(this);

        Log.d("::Ardroid", "Create handler");
        mHandler = new UiHandler(this);

        //TextView msgArduino = (TextView) findViewById(R.id.msgArduino);
        //Button testBoard = (Button) findViewById(R.id.testBoard);
        testBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte input = 0x06;
                msgArduino.setText("");
                if (usbService != null) {
                    // if UsbService was correctly binded, Send data
                    usbService.writeByte(input);
                }
            }
        });

        redLedsOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte input = 0x01;
                if (usbService != null) {
                    usbService.writeByte(input);
                    msgArduino.setText("Red Led ON");
                }
            }
        });

        greenLedsOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte input = 0x02;
                if (usbService != null) {
                    usbService.writeByte(input);
                    msgArduino.setText("Green Led ON");
                }
            }
        });

        yellowLedsOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte input = 0x03;
                if (usbService != null) {
                    usbService.writeByte(input);
                    msgArduino.setText("Yellow Led ON");
                }
            }
        });

        allLedsOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte input = 0x04;
                if (usbService != null) {
                    usbService.writeByte(input);
                    msgArduino.setText("All Leds ON");
                }
            }
        });

        allLedsOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte input = 0x05;
                if (usbService != null) {
                    usbService.writeByte(input);
                    msgArduino.setText("All Leds OFF");
                }
            }
        });

    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(ACTION_NO_USB);
        filter.addAction(ACTION_USB_DISCONNECTED);
        filter.addAction(ACTION_USB_NOT_SUPPORTED);
        filter.addAction(ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    private void startUsbService(){
        Log.d("::Ardroid", "Starting service");
        Intent intent = new Intent(this, UsbService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();
        startUsbService();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(mConnection);
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
                    CharSequence data = (CharSequence) msg.obj;
                    mActivity.get().msgArduino.append(data);
                    break;
                case ALERT:
                    String alert = (String) msg.obj;
                    mActivity.get().msgArduino.setText(alert);
                    break;
            }
        }
    }

}
