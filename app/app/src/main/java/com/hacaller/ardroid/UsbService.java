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
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static com.hacaller.ardroid.IUsbInit.ACTION_USB_ATTACHED;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_DETACHED;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_PERMISSION;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_PERMISSION_GRANTED;
import static com.hacaller.ardroid.IUsbInit.ACTION_USB_READY;
import static com.hacaller.ardroid.IUsbInit.BAUD_RATE;
import static com.hacaller.ardroid.IUsbInit.MESSAGE_FROM_SERIAL_PORT;

/**
 * Created by Herbert on 05/11/2016.
 */

public class UsbService extends Service {

    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private boolean serialPortConnected;
    private Handler mHandler;

    private class UsbReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    // User accepted our USB connection.
                    // DO something
                    // Try to open the device as a serial port
                    connection = usbManager.openDevice(device);
                    new ConnectionThread().start();
                } else {
                    // User not accepted our USB connection.
                    // DO something
                }
            } else if (intent.getAction().equals(ACTION_USB_ATTACHED)) {
                if (!serialPortConnected)
                    findSerialPortDevice();
                // A USB device has been attached. Try to open it as a Serial port
            } else if (intent.getAction().equals(ACTION_USB_DETACHED)) {
                // Usb device was disconnected.
                // DO something
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
        setFilters();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();
    }

    private void setFilters(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                if (deviceVID != IUsbInit.ARDUINO_VENDOR_ID) {
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
                // DO Something.
            }
        } else {
            // There is no USB devices connected.ç
            // DO Something.
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
                    // DO something
                } else {
                    // Serial port could not be opened, maybe an I/O error
                    // or if CDC driver was chosen, it does not really fit
                    // DO something
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                // DO something
            }
        }
    }

    public void write(byte[] data) {
        if (serialPort != null)
            serialPort.write(data);
    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

}
