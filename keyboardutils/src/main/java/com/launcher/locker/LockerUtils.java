package com.launcher.locker;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import com.ihs.app.framework.HSApplication;


public class LockerUtils {

    public static boolean isIntentExist(Context context, Intent intent) {
        if (intent == null) {
            return false;
        }
        if (context.getPackageManager().resolveActivity(intent, 0) == null) {
            return false;
        }
        return true;
    }

    public static boolean isTouchInView(View view, MotionEvent event) {
        if (null == view) {
            return false;
        }

        Rect rect = new Rect();
        view.getDrawingRect(rect);

        int[] location = new int[2];
        view.getLocationOnScreen(location);

        RectF viewRectF = new RectF(rect);
        viewRectF.offset(location[0], location[1]);
        return viewRectF.contains(event.getRawX(), event.getRawY());
    }

    public static boolean hasPermission(String permission) {
        boolean granted = false;
        if (!TextUtils.isEmpty(permission)) {
            try {
                granted = ContextCompat.checkSelfPermission(HSApplication.getContext(), permission)
                        == PackageManager.PERMISSION_GRANTED;
            } catch (RuntimeException e) {
            }
        }
        return granted;
    }
}
