package com.hacaller.ardroid;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static com.hacaller.ardroid.IUsbInit.ACTION_NO_USB;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_DEVICE_NOT_WORKING;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_DISCONNECTED;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_NOT_SUPPORTED;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_PERMISSION;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_PERMISSION_GRANTED;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_PERMISSION_NOT_GRANTED;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_READY;
import static com.hacaller.ardroid.IUsbInit.BAUD_RATE;
import static com.hacaller.ardroid.IUsbInit.MESSAGE_FROM_SERIAL_PORT;


/**
 * Created by Herbert on 05/11/2016.
 */

public class UsbService extends Service {

    private IBinder binder = new UsbBinder();

    public static boolean SERVICE_CONNECTED = false;

    private final int BEEP_ERROR = 1;
    private final int BEEP_OK = 2;
    private final int BEEP_DONE = 3;
    private final int BEEP_CANCEL = 4;

    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private boolean serialPortConnected;
    private Handler mHandler;
    SoundPool soundPool;

    private class UsbReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    // User accepted our USB connection.
                    playBeep(BEEP_OK);
                    Intent newIntent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                    context.sendBroadcast(newIntent);
                    // Try to open the device as a serial port
                    connection = usbManager.openDevice(device);
                    new ConnectionThread().start();
                } else {
                    // User not accepted our USB connection.
                    playBeep(BEEP_CANCEL);
                    Intent newIntent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    context.sendBroadcast(newIntent);
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)) {
                if (!serialPortConnected)
                    findSerialPortDevice();
                // A USB device has been attached. Try to open it as a Serial port
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                // Usb device was disconnected.
                Intent newIntent = new Intent(ACTION_USB_DISCONNECTED);
                context.sendBroadcast(newIntent);
                if (serialPortConnected) {
                    serialPort.close();
                }
                serialPortConnected = false;
            }
        }
    }

    private UsbReceiver usbReceiver = new UsbReceiver();


    @Override
    public void onCreate() {
        super.onCreate();
        serialPortConnected = false;
        SERVICE_CONNECTED = true;
        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        serialPortConnected = false;
        setFilters();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();
    }

    private void setFilters(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class UsbBinder extends Binder{
        UsbService getService(){
            return UsbService.this;
        }
    }

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            try {
                String data = new String(arg0, "UTF-8");
                playBeep(BEEP_DONE);
                if (mHandler != null)
                    mHandler.obtainMessage(MESSAGE_FROM_SERIAL_PORT, data).sendToTarget();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    private void findSerialPortDevice(){
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();
                if (deviceVID != 0x1d6b &&
                        (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003)) {
                    requestUserPermission();
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }
                if (!keep)
                    break;
            }
            if (!keep) {
                // There is no USB devices connected (but usb host were listed).
                Intent newIntent = new Intent(ACTION_NO_USB);
                sendBroadcast(newIntent);
            }
        } else {
            // There is no USB devices connected.
            playBeep(BEEP_ERROR);
            Intent newIntent = new Intent(ACTION_NO_USB);
            sendBroadcast(newIntent);
        }
    }

    private void requestUserPermission(){
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }


    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort != null) {
                if (serialPort.open()) {
                    serialPortConnected = true;
                    serialPort.setBaudRate(BAUD_RATE);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialPort.read(mCallback);
                    // Some Arduinos would need some sleep
                    // because firmware wait some time to know
                    // whether a new sketch is going to be uploaded or not
                    // Thread.sleep(2000); // sleep some. YMMV with different chips.
                    // Everything went as expected.
                    Intent newIntent = new Intent(ACTION_USB_READY);
                    sendBroadcast(newIntent);
                } else {
                    // Serial port could not be opened, maybe an I/O error
                    // or if CDC driver was chosen, it does not really fit
                    Intent newIntent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                    sendBroadcast(newIntent);
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                playBeep(BEEP_CANCEL);
                Intent newIntent = new Intent(ACTION_USB_NOT_SUPPORTED);
                sendBroadcast(newIntent);
            }
        }
    }

    public void write(byte[] data) {
        if (serialPort != null)
            serialPort.write(data);
        playBeep(BEEP_DONE);
    }

    public void writeByte(byte input) {
        if (serialPort != null)
            serialPort.write(new byte[]{input});
        playBeep(BEEP_DONE);
    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    private void playBeep(int msg){
        int resId = R.raw.beep_warning;
        switch(msg){
            case BEEP_ERROR:
                resId = R.raw.beep_error_short;
                break;
            case BEEP_CANCEL:
                resId = R.raw.beep_cancel;
                break;
            case BEEP_OK:
                resId = R.raw.beep_alert;
                break;
            case BEEP_DONE:
                resId = R.raw.beep_ok_short;
                break;
        }
        int soundId = soundPool.load(this, resId, 1);
        soundPool.play(soundId,1f,1f,0,0,1f);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);
        SERVICE_CONNECTED = false;
        soundPool.release();
    }



}
