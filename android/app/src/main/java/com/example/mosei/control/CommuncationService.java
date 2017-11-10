package com.example.mosei.control;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

import ioio.lib.api.IOIOFactory;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;


public class CommuncationService extends IOIOService
{
    // refecnce to parent activity

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new BaseIOIOLooper(){
            Uart uart;
            OutputStream ostream;

            @Override
            protected void setup() throws ConnectionLostException, InterruptedException {
                Thread.sleep(5000);
                //led_ = ioio_.openDigitalOutput(0, true);
                uart = ioio_.openUart(35,34,57600, Uart.Parity.NONE, Uart.StopBits.ONE);
                ostream = uart.getOutputStream();
            }
            @Override
            public void loop() throws ConnectionLostException, InterruptedException {
                try {
                    ostream.write("sending data".getBytes());
                    Thread.sleep(100);
                }
                catch (IOException ex) {
                    //showToast("CommunicationThread: "+ ex.getMessage());
                    Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        int result = super.onStartCommand(intent, flags, startId);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (intent != null && intent.getAction() != null && intent.getAction().equals("stop")) {
            // User clicked the notification. Need to stop the service.
            nm.cancel(0);
            stopSelf();
        } else {
            // Service starting. Create a notification.
            Notification notification =
            new Notification.Builder(getApplicationContext())
                    .setContentTitle("CommunicationService")
                    .setContentText("Connected! click to stop.")
                    .build();
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            nm.notify(0, notification);
            //showToast(notification.category);
        }
        return result;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
