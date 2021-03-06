package com.github.randoapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.github.randoapp.api.API;
import com.github.randoapp.fragment.HomeWallFragment;
import com.github.randoapp.fragment.MissingStoragePermissionFragment;
import com.github.randoapp.log.Log;
import com.github.randoapp.preferences.Preferences;

import io.fabric.sdk.android.Fabric;

import static com.github.randoapp.Constants.AUTH_FAILURE_BROADCAST_EVENT;
import static com.github.randoapp.Constants.AUTH_SUCCCESS_BROADCAST_EVENT;
import static com.github.randoapp.Constants.CAMERA_ACTIVITY_UPLOAD_PRESSED_RESULT_CODE;
import static com.github.randoapp.Constants.CONTACTS_PERMISSION_REQUEST_CODE;
import static com.github.randoapp.Constants.LOGOUT_BROADCAST_EVENT;
import static com.github.randoapp.Constants.STORAGE_PERMISSION_REQUEST_CODE;
import static com.github.randoapp.Constants.SYNC_BROADCAST_EVENT;
import static com.github.randoapp.Constants.UPDATED;

public class MainActivity extends FragmentActivity {


    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(BroadcastReceiver.class, "Recieved event:", intent.getAction());

            switch (intent.getAction()) {
                case SYNC_BROADCAST_EVENT:
                    Bundle extra = intent.getExtras();
                    boolean isUpdated = UPDATED.equals(extra.get(Constants.UPDATE_STATUS));
                    Toast.makeText(MainActivity.this, (isUpdated ? R.string.sync_randos_updated : R.string.sync_nothing_new), Toast.LENGTH_LONG).show();
                    break;
                case AUTH_FAILURE_BROADCAST_EVENT:
                case LOGOUT_BROADCAST_EVENT:
                    Preferences.removeAuthToken(getBaseContext());
                    break;
                case AUTH_SUCCCESS_BROADCAST_EVENT:
                    break;
                default:
                    break;
            }

            if (isNotAuthorized()) {
                startAuthActivity();
                return;
            }

            Fragment fragment = getFragment();
            if (getSupportFragmentManager().findFragmentByTag(fragment.getClass().getName()) == null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.main_screen, fragment, fragment.getClass().getName()).commit();
            }
        }
    };

    private void startAuthActivity() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra(Constants.LOGOUT_ACTIVITY, true);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Fabric.with(this, new Crashlytics());
        //reset ban. Will be set again after next Forbidden upload.
        Preferences.setBanResetAt(getBaseContext(), 0);
    }

    private Fragment getFragment() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return new MissingStoragePermissionFragment();
        }

        return new HomeWallFragment();
    }

    private boolean isNotAuthorized() {
        return Preferences.getAuthToken(getBaseContext()).isEmpty();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        registerReceivers();

        if (isNotAuthorized()) {
            startAuthActivity();
            return;
        }

        Fragment fragment = getFragment();
        if (getSupportFragmentManager().findFragmentByTag(fragment.getClass().getName()) == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.main_screen, fragment, fragment.getClass().getName()).commit();
        }
    }

    private void registerReceivers() {
        registerReceiver(receiver, new IntentFilter(SYNC_BROADCAST_EVENT));
        registerReceiver(receiver, new IntentFilter(AUTH_FAILURE_BROADCAST_EVENT));
        registerReceiver(receiver, new IntentFilter(AUTH_SUCCCESS_BROADCAST_EVENT));
        registerReceiver(receiver, new IntentFilter(LOGOUT_BROADCAST_EVENT));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if ((grantResults.length > 0) && (permissions.length > 0)) {
            switch (requestCode) {
                case STORAGE_PERMISSION_REQUEST_CODE:
                    if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                            new AlertDialog.Builder(this).setTitle(R.string.storage_needed_title).setMessage(R.string.storage_needed_message).setPositiveButton(R.string.permission_positive_button, null).create().show();
                        }
                    } else {
                        API.statistics(getBaseContext(), null);
                        API.syncUserAsync(getBaseContext(), null, null);
                    }
                    break;
                case CONTACTS_PERMISSION_REQUEST_CODE:
                    if (grantResults[0] == PackageManager.PERMISSION_DENIED
                            && ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                        new AlertDialog.Builder(this).setTitle(R.string.contact_needed_title).setMessage(R.string.contact_needed_message).setPositiveButton(R.string.permission_positive_button, null).create().show();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == CAMERA_ACTIVITY_UPLOAD_PRESSED_RESULT_CODE) {

            if (isNotAuthorized()) {
                startAuthActivity();
                return;
            }

            Fragment fragment = getFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.main_screen, fragment, fragment.getClass().getName()).commit();
        }
    }

}
