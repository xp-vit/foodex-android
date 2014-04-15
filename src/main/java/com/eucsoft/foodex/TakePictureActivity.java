package com.eucsoft.foodex;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.eucsoft.foodex.activity.BaseActivity;
import com.eucsoft.foodex.log.Log;
import com.eucsoft.foodex.service.SyncService;
import com.eucsoft.foodex.task.CropImageTask;
import com.eucsoft.foodex.task.FoodUploadTask;
import com.eucsoft.foodex.task.callback.OnDone;
import com.eucsoft.foodex.task.callback.OnError;
import com.eucsoft.foodex.task.callback.OnOk;
import com.eucsoft.foodex.util.BitmapUtil;
import com.eucsoft.foodex.util.FileUtil;
import com.eucsoft.foodex.util.LocationUpdater;
import com.eucsoft.foodex.view.FoodexSurfaceView;

import java.util.Map;

import static android.view.View.VISIBLE;

public class TakePictureActivity extends BaseActivity {
    private FoodexSurfaceView foodexSurfaceView;
    private Camera camera;
    private FrameLayout preview;

    private static final int REQ_CODE_SELECT_PHOTO = 100;
    private LocationUpdater locationUpdater = new LocationUpdater();

    public static Location currentLocation;
    public static String picFileName = null;
    private ImageButton uploadPictureButton;
    private ImageButton flashLightButton;

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            String tmpFile = FileUtil.writeImageToTempFile(data);
            new CropImageTask(tmpFile)
                .onOk(new OnOk() {
                    @Override
                    public void onOk(Map<String, Object> data) {
                        picFileName = (String) data.get(Constants.FILEPATH);
                        showUploadScreen();
                    }
                })
                .onError(new OnError() {
                    @Override
                    public void onError(Map<String, Object> data) {
                        Toast.makeText(TakePictureActivity.this, "Crop Failed.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .onDone(new OnDone() {
                    @Override
                    public void onDone(Map<String, Object> data) {
                        hideProgressbar();
                    }
                })
                .execute();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQ_CODE_SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    showProgressbar("Processing...");
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(
                            selectedImage, filePathColumn, null, null, null);
                    if (cursor == null) {
                        Log.w(TakePictureActivity.class, "Selecting from Album failed.");
                        break;
                    }
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();
                    new CropImageTask(filePath)
                        .onOk(new OnOk() {
                            @Override
                            public void onOk(Map<String, Object> data) {
                                picFileName = (String) data.get(Constants.FILEPATH);
                                showUploadScreen();
                            }
                        })
                        .onError(new OnError() {
                            @Override
                            public void onError(Map<String, Object> data) {
                                Toast.makeText(TakePictureActivity.this, "Crop Failed.",
                                        Toast.LENGTH_LONG).show();
                            }
                        })
                        .onDone(new OnDone() {
                            @Override
                            public void onDone(Map<String, Object> data) {
                                hideProgressbar();
                            }
                        })
                        .execute();
                }
                break;
        }
    }

    private void showUploadScreen() {
        showUploadButton();
        showPictureOnPreview();
    }

    private void showPictureOnPreview() {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picFileName, options);

        // Calculate inSampleSize
        options.inSampleSize = BitmapUtil.calculateInSampleSize(options, preview.getWidth(), preview.getWidth());

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(picFileName, options);
        preview.removeAllViews();
        ImageView imagePreview = new ImageView(getApplicationContext());
        imagePreview.setImageBitmap(bitmap);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(preview.getWidth(), preview.getWidth());
        imagePreview.setLayoutParams(layoutParams);
        preview.addView(imagePreview);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_takepicture);

        updateLocation();
        setBackButtonListener();
        setFlashLightButtonListener();
        setTakePictureButtonListener();
        setImageSelectButtonListener();
        setUploadButtonListener();
        normalizeCameraPreview();
    }

    private void setFlashLightButtonListener() {
        flashLightButton = (ImageButton) findViewById(R.id.flashlight_button);
        flashLightButton.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Camera.Parameters params = camera.getParameters();
                String flashMode = params.getFlashMode();

                if (Camera.Parameters.FLASH_MODE_AUTO.equals(flashMode)) {
                    flashLightButton.setBackgroundResource(R.drawable.ic_flash_disable);
                    flashMode = Camera.Parameters.FLASH_MODE_OFF;
                } else if (Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)) {
                    flashLightButton.setBackgroundResource(R.drawable.ic_flash);
                    flashMode = Camera.Parameters.FLASH_MODE_ON;
                } else if (Camera.Parameters.FLASH_MODE_ON.equals(flashMode)) {
                    flashLightButton.setBackgroundResource(R.drawable.ic_flash_auto);
                    flashMode = Camera.Parameters.FLASH_MODE_AUTO;
                }
                params.setFlashMode(flashMode);

                camera.setParameters(params);
            }
        });
    }

    private void setBackButtonListener() {
        ImageButton backButton = (ImageButton) findViewById(R.id.back_button);
        backButton.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                setResult(RESULT_CANCELED);
                if (TextUtils.isEmpty(picFileName)) {
                    finish();
                } else {
                    picFileName = null;
                    releaseCamera();
                    hideUploadButton();
                    createCameraPreview();
                }
            }
        });
    }

    private void setTakePictureButtonListener() {
        ImageButton takePictureButton = (ImageButton) findViewById(R.id.take_picture_button);
        takePictureButton.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.i(CropImageTask.class, "TOTAL 0:" + Runtime.getRuntime().totalMemory() / (1024 * 1024));
                Log.i(CropImageTask.class, "0:" + Runtime.getRuntime().freeMemory() / (1024 * 1024));
                showProgressbar("processing...");
                camera.takePicture(null, null, pictureCallback);
            }
        });
    }

    private void setImageSelectButtonListener() {
        ImageButton openPictureButton = (ImageButton) findViewById(R.id.select_photo_button);
        openPictureButton.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType(Constants.IMAGE_FILTER);
                startActivityForResult(photoPickerIntent, REQ_CODE_SELECT_PHOTO);
            }
        });
    }

    private void setUploadButtonListener() {
        uploadPictureButton = (ImageButton) findViewById(R.id.upload_photo_button);
        uploadPictureButton.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (picFileName != null) {
                    showProgressbar("uploading...");
                    uploadPictureButton.setEnabled(false);
                    ((LinearLayout) uploadPictureButton.getParent()).setBackgroundColor(getResources().getColor(R.color.button_disabled_background));
                    new FoodUploadTask(picFileName).onOk(new OnOk() {
                        @Override
                        public void onOk(Map<String, Object> data) {
                            SyncService.run();
                            hideProgressbar();
                            Toast.makeText(TakePictureActivity.this,
                                    R.string.photo_upload_ok,
                                    Toast.LENGTH_LONG).show();
                            TakePictureActivity.this.setResult(Activity.RESULT_OK);
                            TakePictureActivity.this.finish();
                        }
                    }).onError(new OnError() {
                        @Override
                        public void onError(Map<String, Object> data) {
                            hideProgressbar();
                            Toast.makeText(TakePictureActivity.this, R.string.photo_upload_failed,
                                    Toast.LENGTH_LONG).show();

                            if (uploadPictureButton != null) {
                                uploadPictureButton.setEnabled(true);
                                ((LinearLayout) uploadPictureButton.getParent()).setBackgroundColor(getResources().getColor(R.color.auth_button));
                            }
                        }
                    }).execute();

                }
            }
        });
    }

    private void createCameraPreview() {
        camera = getCameraInstance();

        if (camera != null) {

            Camera.Parameters params = camera.getParameters();
            //disable flashlight button if flash light not supported
            if (params.getFlashMode() == null) {
                flashLightButton.setEnabled(false);
                flashLightButton.setBackgroundResource(R.drawable.ic_flash_disable);
            }
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            params.setRotation(90);
            params.set("orientation", "portrait");
            params.set("rotation", 90);
            camera.setParameters(params);

            foodexSurfaceView = new FoodexSurfaceView(getApplicationContext(), camera);

            preview = (FrameLayout) findViewById(R.id.cameraPreview);
            preview.removeAllViews();
            preview.addView(foodexSurfaceView);

        } else {
            //TODO: Handle camera not available.
        }
    }

    private void normalizeCameraPreview() {
        WindowManager windowManager = (WindowManager) App.context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();

        int actionBarHeight = getResources().getDimensionPixelSize(android.support.v7.appcompat.R.dimen.abc_action_bar_default_height);
        int cameraPreviewHeight = display.getWidth();
        int statusBarHeight = getStatusBarHeight();

        int bottomToolbarHeight = display.getHeight() - statusBarHeight - actionBarHeight - cameraPreviewHeight;

        LinearLayout bottomColoredStubPanel = (LinearLayout) findViewById(R.id.bottom_colored_stub_panel);
        LinearLayout bottomPanelWithButtons = (LinearLayout) findViewById(R.id.bottom_panel_with_buttons);
        RelativeLayout.LayoutParams bottomColoredStubPanelParams = (RelativeLayout.LayoutParams) bottomColoredStubPanel.getLayoutParams();
        RelativeLayout.LayoutParams bottomPanelWithButtonsLayoutParams = (RelativeLayout.LayoutParams) bottomPanelWithButtons.getLayoutParams();

        bottomColoredStubPanelParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        bottomColoredStubPanelParams.height = bottomToolbarHeight;

        bottomPanelWithButtonsLayoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;

        int minPanelHeight = (int) getResources().getDimension(R.dimen.takepicture_bottom_panel_with_buttons_min_height);
        bottomPanelWithButtonsLayoutParams.height = bottomToolbarHeight >=  minPanelHeight ? bottomToolbarHeight: minPanelHeight;
    }

    public int getStatusBarHeight() {
        int statusBarId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (statusBarId > 0) {
            return getResources().getDimensionPixelSize(statusBarId);
        }
        return 0;
    }

    private void updateLocation() {
        LocationUpdater.LocationResult locationResult = new LocationUpdater.LocationResult() {
            @Override
            public void gotLocation(Location location) {
                currentLocation = location;
            }
        };
        locationUpdater.getLocation(getApplicationContext(), locationResult);
    }

    private void showUploadButton() {
        findViewById(R.id.take_picture_button).setVisibility(View.GONE);
        findViewById(R.id.select_photo_button).setVisibility(View.GONE);
        findViewById(R.id.flashlight_button).setVisibility(View.GONE);

        uploadPictureButton.setEnabled(true);
        findViewById(R.id.upload_photo_button).setVisibility(VISIBLE);
    }

    private void hideUploadButton() {
        findViewById(R.id.take_picture_button).setVisibility(VISIBLE);
        findViewById(R.id.select_photo_button).setVisibility(VISIBLE);
        findViewById(R.id.flashlight_button).setVisibility(VISIBLE);

        findViewById(R.id.upload_photo_button).setVisibility(View.GONE);
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    private Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCamera() {
        if (preview != null) {
            preview.removeAllViews();
        }
        if (camera != null) {
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }

    private void resumeUploadScreenIfNeed() {
        if (!TextUtils.isEmpty(picFileName)) {
            showUploadScreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        createCameraPreview();
        resumeUploadScreenIfNeed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
        locationUpdater.cancelTimer();
    }

}

