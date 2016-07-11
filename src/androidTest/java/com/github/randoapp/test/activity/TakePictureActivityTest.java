package com.github.randoapp.test.activity;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import com.github.randoapp.CameraActivity;

public class TakePictureActivityTest extends ActivityInstrumentationTestCase2<CameraActivity> implements ActivityTestI {

    //Activity to test
    private CameraActivity takePictureActivity;

    // Be careful about letting the IDE create the constructor.  As of this writing,
    // it creates a constructor that's compiles cleanly but doesn't run any tests
    public TakePictureActivityTest() {
        super(CameraActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        takePictureActivity = getActivity();
        Thread.sleep(ONE_SECOND * UGLY_DELAY_FOR_TRAVIS);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (takePictureActivity != null) {
            getInstrumentation().callActivityOnDestroy(takePictureActivity);
            takePictureActivity.finish();
            setActivity(null);
        }
        //Sleep is necessary because Camera Service is not always freed in time
        Thread.sleep(ONE_SECOND * UGLY_DELAY_FOR_TRAVIS);
    }

    // Methods whose names are prefixed with test will automatically be run
    @LargeTest
    public void ignoretestTakePictureOnStart() {
       /* onView(withId(R.id.camera_preview)).check(matches(isDisplayed()));
        onView(withId(R.id.capture_button)).check(matches(isDisplayed()));
        onView(withId(R.id.back_button)).check(matches(isDisplayed()));
        onView(withId(R.id.upload_button)).check(matches(not(isDisplayed())));*/
    }

/*    @LargeTest
    public void testTakePictureOnReStart() {
        assertNotNull(takePictureActivity);
        getInstrumentation().callActivityOnDestroy(takePictureActivity);
        takePictureActivity.finish();
        setActivity(null);

        takePictureActivity = getActivity();
        //Sleep is necessary because Camera Service is not always freed in time and Activity not starts properly
        try {
            Thread.sleep(ONE_SECOND * UGLY_DELAY_FOR_TRAVIS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        takePictureActivity = getActivity();
        onView(withId(R.id.cameraPreview)).check(matches(isDisplayed()));
        onView(withId(R.id.select_photo_button)).check(matches(isDisplayed()));
        onView(withId(R.id.take_picture_button)).check(matches(isDisplayed()));
        onView(withId(R.id.back_button)).check(matches(isDisplayed()));
        onView(withId(R.id.upload_photo_button)).check(matches(not(isDisplayed())));
    }*/

    //TODO: Findout how to work with external Activities in tests (looks like impossible)
    // Methods whose names are prefixed with test will automatically be run
        /*public void testTakePictureAfterPhotoSelected(){
            onView(withId(R.id.select_photo_button)).perform(click());
            onView(withId(R.id.cameraPreview)).check(matches(isDisplayed()));
            onView(withId(R.id.select_photo_button)).check(matches(isDisplayed()));
            onView(withId(R.id.take_picture_button)).check(matches(isDisplayed()));
            onView(withId(R.id.back_button)).check(matches(isDisplayed()));
            onView(withId(R.id.upload_photo_button)).check(doesNotExist());
        }*/

    // Methods whose names are prefixed with test will automatically be run
    @LargeTest
    public void ignoretestTakePictureAfterPictureTaken() {
        /*onView(withId(R.id.capture_button)).perform(click());
        onView(withId(R.id.camera_preview)).check(matches(isDisplayed()));
        onView(withId(R.id.capture_button)).check(matches(not(isDisplayed())));
        onView(withId(R.id.back_button)).check(matches(isDisplayed()));
        onView(withId(R.id.upload_button)).check(matches(isDisplayed()));*/
    }

    // Methods whose names are prefixed with test will automatically be run
    @LargeTest
    public void ignoretestTakePictureAndUpload() {
        /*onView(withId(R.id.capture_button)).check(matches(isDisplayed()));
        onView(withId(R.id.capture_button)).perform(click());
        //Sleep is necessary because Camera produces picture on callback
        try {
            Thread.sleep(ONE_SECOND * UGLY_DELAY_FOR_TRAVIS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        onView(withId(R.id.camera_preview)).check(matches(isDisplayed()));
        onView(withId(R.id.capture_button)).check(matches(not(isDisplayed())));
        onView(withId(R.id.back_button)).check(matches(isDisplayed()));
        onView(withId(R.id.upload_button)).check(matches(isDisplayed()));
        try {
            APITestHelper.mockAPIForUploadFood();
        } catch (IOException e) {
            fail("API mock failed.");
        }
        onView(withId(R.id.upload_button)).perform(click());*/
    }
}

