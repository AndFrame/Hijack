package com.andframe.hijack;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Description activity劫持监测
 */
public class AntiHijack {

    private static final String TAG = "AntiHijack";
    private static AntiHijack instance;
    private Application application;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private OnHijackListener onHijackListener;

    private AntiHijack(Application application) {
        this.application = application;
    }

    public static AntiHijack getInstance(Application application) {
        if (instance == null) {
            synchronized (AntiHijack.class) {
                if (instance == null) {
                    instance = new AntiHijack(application);
                }
            }
        }
        return instance;
    }


    public void setOnHijackListener(OnHijackListener onHijackListener) {
        this.onHijackListener = onHijackListener;
    }

    public void init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            log("anti hijack adapt only below Android N");
            return;
        }
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {
                detectHijack();
            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }


    private void detectHijack() {
        executor.schedule(detectHijackRunnable, 2, TimeUnit.SECONDS);
    }

    private Runnable detectHijackRunnable = new Runnable() {
        @Override
        public void run() {
            List<AndroidAppProcess> processList = AndroidProcesses.getRunningForegroundApps(application);
            if (processList == null || processList.size() == 0) return;

            StringBuilder sb = new StringBuilder();
            PackageManager manager = application.getPackageManager();
            Iterator<AndroidAppProcess> iterator = processList.iterator();
            while (iterator.hasNext()) {
                AndroidAppProcess process = iterator.next();
                try {
                    ApplicationInfo info = manager.getApplicationInfo(process.getPackageName(), PackageManager.GET_UNINSTALLED_PACKAGES);
                    if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) {
                        log("ignore system app->" + process.getPackageName());
                        iterator.remove();
                        continue;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                sb.append("pid:" + process.pid).append("\t")
                        .append("uid:" + process.uid).append("\t")
                        .append("package:" + process.getPackageName()).append("\n");
            }


            log("running foreground apps\n" + sb.toString());

            if (processList == null || processList.size() == 0) return;

            String foregroundPackageName = processList.get(0).getPackageName();
            String currentPackageName = application.getPackageName();
            if (!foregroundPackageName.equals(currentPackageName)) {
                log("detected hijack process, package->" + foregroundPackageName);
                if (onHijackListener != null) {
                    onHijackListener.onHijack(foregroundPackageName);
                }
            }

        }
    };

    private void log(Object obj) {
        if (obj == null) {
            Log.d(TAG, "null");
        } else {
            Log.d(TAG, obj.toString());
        }
    }

    public interface OnHijackListener {
        void onHijack(String packageName);
    }
}
