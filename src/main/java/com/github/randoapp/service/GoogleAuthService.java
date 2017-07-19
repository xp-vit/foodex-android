package com.github.randoapp.service;

import android.app.Activity;
import android.widget.Toast;

import com.github.randoapp.MainActivity;
import com.github.randoapp.api.API;
import com.github.randoapp.api.beans.Error;
import com.github.randoapp.api.listeners.NetworkResultListener;
import com.github.randoapp.log.Log;
import com.github.randoapp.preferences.Preferences;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

public class GoogleAuthService extends BaseAuthService {

    public GoogleAuthService(Activity activity) {
        super(activity);
    }

    public void process(GoogleSignInResult result) {
        handleGoogleSignInResult(result);
    }

    private void handleGoogleSignInResult(GoogleSignInResult result) {
        Log.d(MainActivity.class, "handleGoogleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            final GoogleSignInAccount acct = result.getSignInAccount();
            final String email = acct.getEmail();
            String familyName = acct.getFamilyName();
            String userId = acct.getId();
            String token = acct.getIdToken();
            API.google(email, token, familyName, activity.getBaseContext(), new NetworkResultListener() {
                @Override
                public void onOk() {
                    Preferences.setAccount(activity.getBaseContext(), acct.getEmail());
                    Preferences.setAccount(activity.getBaseContext(), email);
                    done();
                }

                @Override
                public void onError(Error error) {
                    Toast.makeText(activity.getBaseContext(), "Google Signed out.", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(activity.getBaseContext(), "Sorry, Google sign in failed", Toast.LENGTH_LONG).show();
        }
    }
}
