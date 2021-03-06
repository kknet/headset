package com.artw.lockscreen.common;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;

import com.ihs.app.framework.HSApplication;
import com.ihs.app.utils.HSMarketUtils;
import com.ihs.commons.utils.HSLog;
import com.ihs.keyboardutils.R;
import com.ihs.keyboardutils.utils.CommonUtils;
import com.kc.utils.KCAnalytics;

import java.util.List;
import java.util.NoSuchElementException;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;

/**
 * Utility class for navigating through activities.
 */
public class NavUtils {

    public static final String PREF_KEY_LAUNCH_ACTIVITY_PARAM = "launch_activity_param";

    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    /**
     * Start an activity on the given context, {@link Intent#FLAG_ACTIVITY_NEW_TASK} will be added to intent when the
     * given context is not an activity context.
     */
    public static void startActivity(Context context, Class<?> klass) {
        Intent intent = new Intent(context, klass);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    /**
     * Start an activity with a minor delay, avoid blocking animation playing on the previous activity
     * (eg. material ripple effect above Lollipop).
     */
    public static void startActivityMainThreadFriendly(final Context context, Class<?> klass) {
        startActivityMainThreadFriendly(context, new Intent(context, klass));
    }

    public static void startActivityMainThreadFriendly(final Context context, final Intent intent) {
        sMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                context.startActivity(intent);
            }
        }, 300);
    }

    public static void startActivitySafelyAndClearTask(Context context, Intent intent) {
        try {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException | NullPointerException e) {
            HSLog.e("StartActivity", "Cannot start activity: " + intent);
        }
    }

    public static void startActivitySafely(Context context, Intent intent) {
        try {
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException | NullPointerException e) {
            HSLog.e("StartActivity", "Cannot start activity: " + intent);
        }
    }

    public static void startActivityForResultSafely(Activity activity, Intent intent, int requestCode) {
        startActivityForResultSafely(activity, intent, requestCode, R.string.setting_device_not_support_message);
    }

    public static void startActivityForResultSafely(
            Activity activity, Intent intent, int requestCode, int errorMessageId) {
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            if (errorMessageId > 0) {
            }
        } catch (SecurityException e) {
            if (errorMessageId > 0) {
            }
            HSLog.e("StartActivity", "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity. Error: " + e);
        }
    }


    public static void openBrowser(Context context, String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivitySafely(context, browserIntent);
    }


    /**
     * Start a system settings page with the given action.
     *
     * @param action            Action to start. Eg. {@link Settings#ACTION_DISPLAY_SETTINGS}.
     * @param attachPackageName If package name of this launcher should be attached by {@link Intent#setData(Uri)}.
     */
    public static void startSystemSetting(Context activityContext, String action, boolean attachPackageName) {
        Intent intent = new Intent(action);
        if (attachPackageName) {
            intent.setData(Uri.parse("package:" + activityContext.getPackageName()));
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivitySafely(activityContext, intent);
    }

    public static void startSystemDataUsageSetting(Context context) {
        startSystemDataUsageSetting(context, false);
    }

    public static void startSystemDataUsageSetting(Context context, boolean attachNewTaskFlag) {
        KCAnalytics.logEvent("Toolkit_DataUsage_IconClicked");
        Intent dataUsageIntent = new Intent();
        dataUsageIntent.setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$DataUsageSummaryActivity"));
        Intent intent;
        if (dataUsageIntent.resolveActivity(context.getPackageManager()) != null) {
            intent = dataUsageIntent;
        } else {
            intent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
        }
        if (attachNewTaskFlag) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivitySafely(context, intent);
    }

    public static void bringActivityToFront(Class activity, int launchParam) {
        Intent intent = new Intent(HSApplication.getContext(), activity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(PREF_KEY_LAUNCH_ACTIVITY_PARAM, launchParam);
        HSApplication.getContext().startActivity(intent);
    }

    public static void startSystemAppInfoActivity(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName) || null == context) {
            return;
        }
        try {
            // Open the specific App Info page:
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            // Open the generic Apps page:
            Intent intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException ignored) {
            }
        }
    }

    public static boolean startCameraFromLockerScreen(Context context) {

        Intent launchIntent = new Intent("com.keyboard.colorcam.locker");
        launchIntent.setPackage(context.getPackageName());
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            context.startActivity(launchIntent);//null pointer check in case package name was not found
            KCAnalytics.logEvent("app_screen_locker_cam_shortcut_entry_successful");
            return true;
        } catch (Exception e) {
            Intent infoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(infoIntent, COMPONENT_ENABLED_STATE_DEFAULT);
            for (ResolveInfo resolveInfo : resolveInfos) {
                try {
                    String cameraPackageName = resolveInfo.activityInfo.packageName;
                    openApplication(cameraPackageName);
                    return true;
                } catch (NoSuchElementException ecc) {
                    //to continue the looper when find incorrect camera packageName for certain type phone.
                } catch (Exception ec) {
//                LockToast.makeText(HSApplication.getContext(), R.string.lock_camera_no_access, Toast.LENGTH_SHORT).show();
                }
            }
        }
        return false;
    }

    public static void openApplication(String packageName) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = HSApplication.getContext().getPackageManager().getPackageInfo(packageName, COMPONENT_ENABLED_STATE_DEFAULT);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo != null) {
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(packageInfo.packageName);

            List<ResolveInfo> resolveInfoList = HSApplication.getContext().getPackageManager().queryIntentActivities(intent, COMPONENT_ENABLED_STATE_DEFAULT);
            ResolveInfo resolveInfo = resolveInfoList.iterator().next();
            if (resolveInfo != null) {
                String pkgName = resolveInfo.activityInfo.packageName;
                String className = resolveInfo.activityInfo.name;
                Intent mIntent = new Intent(Intent.ACTION_MAIN);
                mIntent.addCategory(Intent.CATEGORY_LAUNCHER);

                ComponentName componentName = new ComponentName(pkgName, className);
                mIntent.setComponent(componentName);
                mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                HSApplication.getContext().startActivity(mIntent);
            }
        }
    }

    /**
     * Browse market app if not installed. Launch the app if installed.
     */
    public static void browseApp(String packageName) {
        if (CommonUtils.isPackageInstalled(packageName)) {
            NavUtils.openApplication(packageName);
        } else {
            HSMarketUtils.browseAPP(packageName);
        }
    }
}
