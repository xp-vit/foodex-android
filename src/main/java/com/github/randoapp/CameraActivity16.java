package com.github.randoapp;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.github.randoapp.animation.OnAnimationEnd;
import com.github.randoapp.log.Log;
import com.github.randoapp.preferences.Preferences;
import com.github.randoapp.task.CropToSquareImageTask;
import com.github.randoapp.util.Analytics;
import com.github.randoapp.util.LocationHelper;
import com.github.randoapp.util.PermissionUtils;
import com.github.randoapp.view.CircleMaskView;
import com.github.randoapp.view.FlipImageView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Facing;
import com.otaliastudios.cameraview.Flash;
import com.otaliastudios.cameraview.Gesture;
import com.otaliastudios.cameraview.GestureAction;
import com.otaliastudios.cameraview.Grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.fabric.sdk.android.Fabric;

import static com.github.randoapp.Constants.CAMERA_ACTIVITY_CAMERA_PERMISSION_REQUIRED;
import static com.github.randoapp.Constants.CAMERA_BROADCAST_EVENT;
import static com.github.randoapp.Constants.CAMERA_PERMISSION_REQUEST_CODE;
import static com.github.randoapp.Constants.LOCATION_PERMISSION_REQUEST_CODE;

public class CameraActivity16 extends Activity {

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            progressBar.setVisibility(View.GONE);
            Bundle extra = intent.getExtras();
            if (extra != null) {
                String photoPath = (String) extra.get(Constants.RANDO_PHOTO_PATH);
                if (photoPath != null && !photoPath.isEmpty()) {

                    Intent activityIntent = new Intent(CameraActivity16.this, ImageReviewUploadActivity.class);
                    activityIntent.putExtra(Constants.FILEPATH, photoPath);
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                    startActivity(activityIntent);

                    finish();
                    return;
                } else {
                    Toast.makeText(CameraActivity16.this, getResources().getText(R.string.image_crop_failed),
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    private CameraActivity16.UnexpectedTerminationHelper mUnexpectedTerminationHelper = new CameraActivity16.UnexpectedTerminationHelper();

    private boolean isReturningFromCameraPermissionRequest = false;
    private boolean isReturningFromLocationPermissionRequest = false;

    private CameraView cameraView;
    private ImageView captureButton;
    private FlipImageView cameraSwitchButton;
    private ImageView flashButton;
    private FlipImageView gridButton;
    private LinearLayout progressBar;
    private Handler mBackgroundHandler;
    private FirebaseAnalytics mFirebaseAnalytics;
    private CircleMaskView circleMaskView;
    private Facing mCurrentFacing;
    private Iterator<Flash> mFlashModeIterator;
    private boolean mTakingPicture = false;
    private List<Flash> mFlashModes = new ArrayList<>(FLASH_MODES.size());

    private CropToSquareImageTask mCropTask;

    private static final Map<Facing, Integer> CAMERA_FACING_ICONS = new HashMap<>(2);
    private static final List<Flash> FLASH_MODES = Arrays.asList(Flash.AUTO, Flash.ON);

    static {
        CAMERA_FACING_ICONS.put(Facing.FRONT, R.drawable.ic_camera_front_white_24dp);
        CAMERA_FACING_ICONS.put(Facing.BACK, R.drawable.ic_camera_rear_white_24dp);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_capture16);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Fabric.with(this, new Crashlytics());

        cameraView = findViewById(R.id.camera);
        cameraView.addCameraListener(cameraListener);
        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM); // Pinch to zoom!
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER); // Tap to focus!

        captureButton = findViewById(R.id.capture_button);
        captureButton.setOnClickListener(new CameraActivity16.CaptureButtonListener());
        enableButtons(false);

        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        flashButton = findViewById(R.id.flash_button);
        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mFlashModeIterator.hasNext()) {
                    mFlashModeIterator = mFlashModes.iterator();
                }
                cameraView.setFlash(mFlashModeIterator.next());
                setFlashButtonIcon(cameraView.getFlash());
                Preferences.setCameraFlashMode(getApplicationContext(), mCurrentFacing, cameraView.getFlash());
            }

        });

        circleMaskView = findViewById(R.id.circle_mask);
        adjustPreviewSize();

        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int buttonsSideMargin = (displayMetrics.widthPixels - getResources().getDimensionPixelSize(R.dimen.rando_button_size)) / 4 - getResources().getDimensionPixelSize(R.dimen.switch_camera_button_size) / 2;
        if (Camera.getNumberOfCameras() > 1) {
            cameraSwitchButton = findViewById(R.id.camera_switch_button);
            RelativeLayout.LayoutParams cameraSwitchButtonLayoutParams = (RelativeLayout.LayoutParams) cameraSwitchButton.getLayoutParams();
            cameraSwitchButtonLayoutParams.setMargins(buttonsSideMargin, 0, 0, getResources().getDimensionPixelSize(R.dimen.switch_camera_margin_bottom));
            cameraSwitchButton.setLayoutParams(cameraSwitchButtonLayoutParams);
            mCurrentFacing = Preferences.getCameraFacing(getBaseContext());
            cameraView.setFacing(mCurrentFacing);
            cameraSwitchButton.setImageResource(CAMERA_FACING_ICONS.get(mCurrentFacing));
            cameraSwitchButton.setVisibility(View.VISIBLE);
            cameraSwitchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    enableButtons(false);
                    if (cameraView != null) {
                        Facing facing;
                        if (mCurrentFacing == Facing.FRONT) {
                            facing = Facing.BACK;
                            Analytics.logSwitchCameraToBack(mFirebaseAnalytics);
                        } else {
                            facing = Facing.FRONT;
                            Analytics.logSwitchCameraToFront(mFirebaseAnalytics);
                        }
                        cameraSwitchButton.flipView(CAMERA_FACING_ICONS.get(facing), 0, null);
                        enableButtons(false);
                        cameraView.setFacing(facing);
                        mCurrentFacing = facing;
                        Preferences.setCameraFacing(getBaseContext(), facing);
                    }
                }
            });
        }
        cameraView.setGrid(Preferences.getCameraGrid(getBaseContext()));
        gridButton = findViewById(R.id.grid_button);
        RelativeLayout.LayoutParams gridButtonLayoutParams = (RelativeLayout.LayoutParams) gridButton.getLayoutParams();
        gridButtonLayoutParams.setMargins(0, 0, buttonsSideMargin, getResources().getDimensionPixelSize(R.dimen.switch_camera_margin_bottom));
        gridButton.setLayoutParams(gridButtonLayoutParams);
        gridButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Grid.OFF.equals(cameraView.getGrid())) {
                    cameraView.setGrid(Grid.DRAW_3X3);
                } else {
                    cameraView.setGrid(Grid.OFF);
                }
                Preferences.setCameraGrid(getBaseContext(), cameraView.getGrid());
                OnAnimationEnd onAnimationEnd = new OnAnimationEnd() {
                    @Override
                    public void onEnd() {
                    }
                };
                if (cameraView.getGrid().equals(Grid.DRAW_3X3)) {
                    gridButton.flipView(R.drawable.ic_grid_on_white_24dp, R.drawable.switch_camera_background, onAnimationEnd);
                } else {
                    gridButton.flipView(R.drawable.ic_grid_off_white_24dp, R.drawable.camera_action_button_background_off, onAnimationEnd);
                }
            }
        });
        setupGridIcon();
    }

    private void setFlashButtonIcon(Flash mCurrentFlashMode) {
        if (mCurrentFlashMode == Flash.OFF) {
            flashButton.setBackgroundResource(R.drawable.ic_flash_off_grey_26dp);
        }
        else if (mCurrentFlashMode == Flash.ON) {
            flashButton.setBackgroundResource(R.drawable.ic_flash_on_grey_26dp);
        }
        else if (mCurrentFlashMode == Flash.AUTO) {
            flashButton.setBackgroundResource(R.drawable.ic_flash_auto_grey_26dp);
        }
    }

    /**
     * Makes camera preview to be Square
     */
    private void adjustPreviewSize() {
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        int cameraViewleftRightMargin = (int) getResources().getDimension(R.dimen.rando_padding_portrait_column_left);
        int cameraViewtopBottomMargin = (displayMetrics.heightPixels - (displayMetrics.widthPixels - 2 * cameraViewleftRightMargin)) / 2;

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) cameraView.getLayoutParams();
        layoutParams.setMargins(cameraViewleftRightMargin, cameraViewtopBottomMargin, cameraViewleftRightMargin, cameraViewtopBottomMargin);
        cameraView.setLayoutParams(layoutParams);
        layoutParams = (RelativeLayout.LayoutParams) circleMaskView.getLayoutParams();
        layoutParams.setMargins(cameraViewleftRightMargin, cameraViewtopBottomMargin, cameraViewleftRightMargin, cameraViewtopBottomMargin);
        circleMaskView.setLayoutParams(layoutParams);
    }

    @Override
    public void onResume() {
        super.onResume();
        switchSound(true);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(CAMERA_BROADCAST_EVENT));
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (isReturningFromCameraPermissionRequest) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                mUnexpectedTerminationHelper.init();
                cameraView.start();
            } else {
                setResult(CAMERA_ACTIVITY_CAMERA_PERMISSION_REQUIRED);
                finish();
            }
            isReturningFromCameraPermissionRequest = false;
        } else {
            if (!PermissionUtils.checkAndRequestMissingPermissions(this, CAMERA_PERMISSION_REQUEST_CODE, android.Manifest.permission.CAMERA)) {
                mUnexpectedTerminationHelper.init();
                cameraView.start();
            }
        }

        if (isReturningFromLocationPermissionRequest) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                updateLocation();
            }
        } else {
            if (!PermissionUtils.checkAndRequestMissingPermissions(this, LOCATION_PERMISSION_REQUEST_CODE, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                updateLocation();
            }
        }
        isReturningFromLocationPermissionRequest = false;
    }

    private void enableButtons(boolean enable) {
        captureButton.setEnabled(enable);
        if (cameraSwitchButton != null) {
            cameraSwitchButton.setEnabled(enable);
        }
        if (flashButton != null) {
            flashButton.setEnabled(enable);
        }
    }

    private void setupGridIcon() {
        if (Grid.DRAW_3X3.equals(cameraView.getGrid())) {
            gridButton.setImageResource(R.drawable.ic_grid_on_white_24dp);
            gridButton.setBackgroundResource(R.drawable.switch_camera_background);
        } else {
            gridButton.setImageResource(R.drawable.ic_grid_off_white_24dp);
            gridButton.setBackgroundResource(R.drawable.camera_action_button_background_off);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
        switchSound(true);
        circleMaskView.recycle();
        mUnexpectedTerminationHelper.fini();
        if (progressBar.getVisibility() != View.GONE) {
            progressBar.setVisibility(View.GONE);
        }
        stopCropTask();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if ((grantResults.length > 0) && (permissions.length > 0)) {
            switch (requestCode) {
                case CAMERA_PERMISSION_REQUEST_CODE:
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        isReturningFromCameraPermissionRequest = true;
                    } else {
                        setResult(CAMERA_ACTIVITY_CAMERA_PERMISSION_REQUIRED);
                        finish();
                    }
                    break;
                case LOCATION_PERMISSION_REQUEST_CODE:
                    isReturningFromLocationPermissionRequest = true;
                    if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                            new AlertDialog.Builder(this).setTitle(R.string.location_needed_title).setMessage(R.string.location_needed_message).setPositiveButton(R.string.permission_positive_button, null).create().show();
                        } else {
                            updateLocation();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void stopCropTask() {
        if (mCropTask != null) {
            mCropTask.cancel();
        }
        mCropTask = null;
    }

    public void updateLocation() {
        if (LocationHelper.isGpsEnabled(this)) {

            LocationHelper locationHelper = new LocationHelper(this);
            locationHelper.updateLocationAsync();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage(getResources().getString(R.string.no_location_services))
                    .setPositiveButton(getResources().getString(R.string.enable_location_services),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    startActivity(new Intent(
                                            Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                }
                            }
                    )
                    .setNegativeButton(getResources().getString(R.string.close_dialog),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    dialog.cancel();
                                }
                            }
                    ).create().show();
        }
    }

    private class CaptureButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(CameraActivity16.class, "Take Pic Click ");
            enableButtons(false);
            switchSound(false);
            cameraView.capturePicture();
            mTakingPicture = true;
            progressBar.setVisibility(View.VISIBLE);
            Analytics.logTakeRando(mFirebaseAnalytics);
        }
    }

    private CameraListener cameraListener
            = new CameraListener() {

        @Override
        public void onCameraOpened(final CameraOptions options) {
            if (!mTakingPicture) {
                mFlashModes.clear();
                mFlashModes.addAll(FLASH_MODES);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // exclude xiaomi phones front camera
                                if (cameraView.getFacing() == Facing.FRONT && Build.MANUFACTURER.equalsIgnoreCase("Xiaomi")) {
                                    flashButton.setVisibility(ImageView.GONE);
                                    mFlashModes.clear();
                                } else {
                                    mFlashModes.retainAll(options.getSupportedFlash());
                                    if (!mFlashModes.isEmpty()) {
                                        flashButton.setVisibility(ImageView.VISIBLE);
                                        mFlashModes.add(0, Flash.OFF);
                                        mFlashModeIterator = mFlashModes.iterator();
                                        cameraView.setFlash(Preferences.getCameraFlashMode(getApplicationContext(), mCurrentFacing));
                                        while (cameraView.getFlash() != mFlashModeIterator.next()) ;
                                        setFlashButtonIcon(cameraView.getFlash());
                                    } else {
                                        flashButton.setVisibility(ImageView.GONE);
                                    }
                                }
                                enableButtons(true);
                            }
                        }, 500);
                    }
                });
            }
        }

        public void onCameraClosed() {
            enableButtons(false);
        }

        public void onPictureTaken(byte[] jpeg) {
            cameraView.stop();
            switchSound(true);
            mCropTask = new CropToSquareImageTask(jpeg, mCurrentFacing == Facing.FRONT, getBaseContext());
            getBackgroundHandler().post(mCropTask);
        }

        @Override
        public void onCameraError(@NonNull CameraException exception) {
            switchSound(true);
        }
    };

    private void switchSound(boolean on) {
        try {
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M || notificationManager.isNotificationPolicyAccessGranted()) {
                AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                mgr.setStreamMute(AudioManager.STREAM_SYSTEM, !on);
            }
        } catch (SecurityException e) {
            Crashlytics.logException(e);
        }
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    private class UnexpectedTerminationHelper {
        private Thread mThread;
        private Thread.UncaughtExceptionHandler mOldUncaughtExceptionHandler = null;
        private Thread.UncaughtExceptionHandler mUncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) { // gets called on the same (main) thread
                cameraView.stop();
                if (mOldUncaughtExceptionHandler != null) {
                    // it displays the "force close" dialog
                    mOldUncaughtExceptionHandler.uncaughtException(thread, ex);
                }
            }
        };

        public void init() {
            mThread = Thread.currentThread();
            mOldUncaughtExceptionHandler = mThread.getUncaughtExceptionHandler();
            mThread.setUncaughtExceptionHandler(mUncaughtExceptionHandler);
        }

        public void fini() {
            if (mThread != null) {
                mThread.setUncaughtExceptionHandler(mOldUncaughtExceptionHandler);
            }
            mOldUncaughtExceptionHandler = null;
            mThread = null;
        }
    }
}
