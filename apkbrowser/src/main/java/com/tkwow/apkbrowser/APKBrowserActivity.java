package com.tkwow.apkbrowser;

import com.tkwow.apkbrowser.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class APKBrowserActivity extends Activity {
    private APKBrowserFragmentFragment mFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_apkbrowser);

        mFragment = APKBrowserFragmentFragment.newInstance("http://192.168.1.105:8080/HelloChina/rest/android/");
        getFragmentManager().beginTransaction()
                .replace(R.id.apk_browser, mFragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (mFragment.handleBackPressed()) {
            return;
        }
        super.onBackPressed();
    }
}
