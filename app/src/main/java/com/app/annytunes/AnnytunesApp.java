package com.app.annytunes;

import android.app.Application;

import com.app.annytunes.uart.CommsThread;
import com.app.annytunes.uart.channels.ChannelIo;

public class AnnytunesApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Eagerly start communications thread so getObj() is available early.
        CommsThread.getObj();
        // Ensure ChannelIo is constructed early.
        new ChannelIo();
    }
}
