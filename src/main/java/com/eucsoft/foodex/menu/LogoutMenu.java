package com.eucsoft.foodex.menu;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;

import com.eucsoft.foodex.MainActivity;
import com.eucsoft.foodex.R;
import com.eucsoft.foodex.fragment.AuthFragment;
import com.eucsoft.foodex.listener.TaskResultListener;
import com.eucsoft.foodex.task.LogoutTask;
import com.eucsoft.foodex.view.Progress;

import java.util.HashMap;

public class LogoutMenu {

    public static final int ID =  R.id.action_logout;

    public void select () {
        Progress.show();
        new LogoutTask(new TaskResultListener() {
            @Override
            public void onTaskResult(int taskCode, long resultCode, HashMap<String, Object> data) {
                FragmentManager fragmentManager = ((ActionBarActivity) MainActivity.activity).getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.main_screen, new AuthFragment()).commit();
                Progress.hide();
        }}).execute();
    }
}
