package com.example.mosei.control;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIOFactory;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.exception.IncompatibilityException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOConnectionRegistry;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

import static android.content.ContentValues.TAG;

public class CommuncationService extends IOIOService
{
    // refecnce to parent activity
    Activity parentActivity;

    /**
     * Communication thread constructor
     * @param activity
     */
    public CommuncationService(Activity activity){
        this.parentActivity = activity;
    }


    @Override
    protected IOIOLooper createIOIOLooper() {
        return new BaseIOIOLooper(){
            Uart uart;
            OutputStream ostream;

            @Override
            protected void setup() throws ConnectionLostException, InterruptedException {
                ioio_ = IOIOFactory.create();
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
                    showToast("CommunicationThread: "+ ex.getMessage());
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
            new Notification.Builder(parentActivity.getApplicationContext())
                    .setContentTitle("CommunicationService")
                    .setContentText("Connected! click to stop.")
                    .setSmallIcon(R.drawable.ic_dashboard_black_24dp)
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

    private void showToast(final String message) {
        final Context context = parentActivity.getApplicationContext();
        parentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
