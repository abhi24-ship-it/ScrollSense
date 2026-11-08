package com.reelmonster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Starts the watcher again after a phone restart or an app update. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        WatcherService.start(context);
    }
}
