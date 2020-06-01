package com.demo.hijack;

import android.app.Application;
import android.util.Log;

import com.andframe.hijack.AntiHijack;

/**
 *
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        AntiHijack.getInstance(this).init();
        AntiHijack.getInstance(this).setOnHijackListener(new AntiHijack.OnHijackListener() {
            @Override
            public void onHijack(String packageName) {
                Log.d("Tag", "程序可能被劫持，请注意隐私保护");
            }
        });
    }
}
