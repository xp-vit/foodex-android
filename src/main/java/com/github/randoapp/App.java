package com.github.randoapp;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.github.randoapp.log.Log;
import com.github.randoapp.preferences.Preferences;
import com.github.randoapp.service.UploadService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import static org.acra.sender.HttpSender.Method;
import static org.acra.sender.HttpSender.Type;


@ReportsCrashes(
        formUri = "http://reports.rando4.me/" + BuildConfig.RANDO_REPORTS_ACRA_DB + "/_design/acra-storage/_update/report",
        reportType = Type.JSON,
        httpMethod = Method.PUT,
        formUriBasicAuthLogin = BuildConfig.RANDO_REPORTS_USER,
        formUriBasicAuthPassword = BuildConfig.RANDO_REPORTS_PASSWORD,
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text
)
public class App extends Application {

    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        startServices();
        if (!FirebaseApp.getApps(this).isEmpty()) {
            Preferences.setFirebaseInstanceId(FirebaseInstanceId.getInstance().getToken());
            Log.i(App.class, "Firebase ID: " + FirebaseInstanceId.getInstance().getToken());
        }
    }

    public static App getInstance(Context context) {
        return (App) context.getApplicationContext();
    }

    private void startServices() {
        //App onCreate called twice. Prevent double service run, if it is already created
        if (!UploadService.isRunning()) {
            startService(new Intent(getApplicationContext(), UploadService.class));
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ACRA.init(this);
    }
}
