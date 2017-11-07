package com.example.mosei.control;

import android.app.Activity;
import android.content.Context;
import android.nfc.Tag;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIOFactory;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOConnectionRegistry;
import ioio.lib.util.android.IOIOActivity;

public class CommuncationThread extends Thread
{
    // refecnce to parent activity
    Activity parentActivity;

    /**
     * Communication thread constructor
     * @param activity
     */
    public CommuncationThread(Activity activity){
        this.parentActivity = activity;
    }

    @Override
    public void run() {
        try {
            Looper ioioLooper = new Looper();
            ioioLooper.setup();
            while(true){
                ioioLooper.loop();
            }
        }
        catch (Exception ex){
            showToast("Communication Thread: " + ex.getMessage());
        }
    }

    class Looper extends BaseIOIOLooper {
        /** The on-board LED. */
        private DigitalOutput led_;
        private OutputStream ostream;
        private Uart uart;

        /**
         * Called every time a connection with IOIO has been established.
         * Typically used to open pins.
         *
         * @throws ConnectionLostException When IOIO connection is lost.
         *
         */
        @Override
        protected void setup() throws ConnectionLostException, InterruptedException {
            //showVersions(ioio_, "IOIO connected!");
            ioio_ = IOIOFactory.create();
            Thread.sleep(5000);
            //led_ = ioio_.openDigitalOutput(0, true);
            uart = ioio_.openUart(35,34,57600, Uart.Parity.NONE, Uart.StopBits.ONE);
            ostream = uart.getOutputStream();
        }

        /**
         * Called repetitively while the IOIO is connected.
         *
         * @throws ConnectionLostException When IOIO connection is lost.
         * @throws InterruptedException When the IOIO thread has been interrupted.
         *
         * @see ioio.lib.util.IOIOLooper#loop()
         */
        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            try {
                ostream.write("sending data".getBytes());
                led_.write(true);
                Thread.sleep(100);
            }
            catch (IOException ex) {
                showToast("CommunicationThread: "+ ex.getMessage());
            }
        }

        /**
         * Called when the IOIO is disconnected.
         *
         * @see ioio.lib.util.IOIOLooper#disconnected()
         */
        @Override
        public void disconnected() { showToast("Bluetooth connection disabled!"); }

        /**
         * Called when the IOIO is connected, but has an incompatible firmware version.
         *
         * @see ioio.lib.util.IOIOLooper#incompatible(IOIO)
         */
        @Override
        public void incompatible() { showToast("Incompatible IOIO board firmware versions!"); }
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
