package com.hacaller.ardroid;

/**
 * Created by Herbert on 05/11/2016.
 */

public interface IUsbInit {

    String ACTION_USB_READY = "USB_READY";
    String ACTION_USB_NOT_SUPPORTED = "USB_NOT_SUPPORTED";
    String ACTION_NO_USB = "NO_USB";
    String ACTION_USB_PERMISSION_GRANTED = "USB_PERMISSION_GRANTED";
    String ACTION_USB_PERMISSION_NOT_GRANTED = "USB_PERMISSION_NOT_GRANTED";
    String ACTION_USB_DISCONNECTED = "USB_DISCONNECTED";
    String ACTION_CDC_DRIVER_NOT_WORKING = "ACTION_CDC_DRIVER_NOT_WORKING";
    String ACTION_USB_DEVICE_NOT_WORKING = "ACTION_USB_DEVICE_NOT_WORKING";
    String ACTION_USB_PERMISSION = "USB_PERMISSION";

    int MESSAGE_FROM_SERIAL_PORT = 0;
    int ALERT = 1;
    int ARDUINO_VID = 0x2341;
    int ARDUINO_PID = 0x0042;
    int BAUD_RATE = 9600;

}
