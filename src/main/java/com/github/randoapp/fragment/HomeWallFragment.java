package com.github.randoapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.github.randoapp.CameraActivity16;
import com.github.randoapp.Constants;
import com.github.randoapp.R;
import com.github.randoapp.adapter.HomePagerAdapter;
import com.github.randoapp.db.model.Rando;
import com.github.randoapp.util.Analytics;
import com.google.firebase.analytics.FirebaseAnalytics;

public class HomeWallFragment extends Fragment {

    private FirebaseAnalytics mFirebaseAnalytics;

    private static boolean isPhotoResult;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.home, container, false);
        ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.colums_pager);
        Bundle extras = getActivity().getIntent().getExtras();
        Rando scrollToRando = null;
        if (extras != null) {
            scrollToRando = (Rando) extras.getSerializable(Constants.RANDO_PARAM);
        }
        HomePagerAdapter homePagerAdapter = new HomePagerAdapter(getChildFragmentManager());
        homePagerAdapter.setScrollToRando(scrollToRando);
        viewPager.setAdapter(homePagerAdapter);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //not needed
            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        Analytics.logOpenTabSettings(mFirebaseAnalytics);
                        break;
                    case 2:
                        Analytics.logOpenTabOwnRandos(mFirebaseAnalytics);
                        break;
                    case 1:
                    default:
                        Analytics.logOpenTabStrangerRandos(mFirebaseAnalytics);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //not needed
            }
        });
        if (isPhotoResult || (scrollToRando != null && scrollToRando.status == Rando.Status.OUT))  {
            viewPager.setCurrentItem(2);
            isPhotoResult = false;
        } else {
            viewPager.setCurrentItem(1);
        }
        ImageView takePictureButton = (ImageView) rootView.findViewById(R.id.camera_button);
        takePictureButton.setEnabled(true);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(rootView.getContext(), CameraActivity16.class);
                startActivityForResult(intent, Constants.CAMERA_ACTIVITY_UPLOAD_PRESSED_REQUEST_CODE);
            }
        });


        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.CAMERA_ACTIVITY_UPLOAD_PRESSED_REQUEST_CODE &&
                resultCode == Constants.CAMERA_ACTIVITY_UPLOAD_PRESSED_RESULT_CODE) {
            isPhotoResult = true;
        }
    }
}
