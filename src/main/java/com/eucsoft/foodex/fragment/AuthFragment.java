package com.eucsoft.foodex.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.eucsoft.foodex.R;
import com.eucsoft.foodex.auth.EmailAndPasswordAuth;
import com.eucsoft.foodex.auth.GoogleAuth;
import com.eucsoft.foodex.auth.SkipAuth;

public class AuthFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.auth, container, false);

        TextView textViewSkipLink = (TextView) rootView.findViewById(R.id.textViewSkipLink);
        textViewSkipLink.setOnClickListener(new SkipAuth(this));

        Button signupButton = (Button) rootView.findViewById(R.id.signupButton);
        signupButton.setOnClickListener(new EmailAndPasswordAuth(rootView, this));

        createGoogleAuthButton(rootView);

        return rootView;
    }

    private void createGoogleAuthButton(View rootView) {
        Button googleButton = (Button) rootView.findViewById(R.id.googleAuthButton);
        googleButton.setBackground(getResources().getDrawable(com.google.android.gms.R.drawable.common_signin_btn_text_normal_dark));
        googleButton.setText(getResources().getString(com.google.android.gms.R.string.common_signin_button_text_long));

        GoogleAuth googleAuthListener = new GoogleAuth(this, googleButton);
        googleButton.setOnTouchListener(googleAuthListener);
        googleButton.setOnClickListener(googleAuthListener);
    }

}
