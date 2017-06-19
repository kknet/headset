package com.artw.lockscreen.common;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.view.MenuItem;

import com.ihs.feature.common.ActivityUtils;
import com.ihs.feature.common.BasePermissionActivity;

public abstract class BaseSettingsActivity extends BasePermissionActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getLayoutId());
        ActivityUtils.configSimpleAppBar(this, getTitleId());

        ActivityUtils.setBlueStatusBar(this);
    }

    protected abstract @LayoutRes
    int getLayoutId();

    protected abstract @StringRes
    int getTitleId();

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
