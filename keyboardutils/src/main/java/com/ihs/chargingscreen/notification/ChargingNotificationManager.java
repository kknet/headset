package com.ihs.chargingscreen.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.ihs.app.framework.HSApplication;
import com.ihs.charging.HSChargingManager;
import com.ihs.chargingscreen.ChargingBroadcastReceiver;
import com.ihs.chargingscreen.Constants;
import com.ihs.chargingscreen.HSChargingScreenManager;
import com.ihs.chargingscreen.utils.ChargingAnalytics;
import com.ihs.commons.notificationcenter.HSGlobalNotificationCenter;
import com.ihs.commons.notificationcenter.INotificationObserver;
import com.ihs.commons.utils.HSBundle;
import com.ihs.commons.utils.HSLog;
import com.ihs.keyboardutils.R;

/**
 * Created by zhixiangxiao on 5/17/16.
 */
public class ChargingNotificationManager {

    private static final int NOTIFICATION_ID = 19349; // TODO uniform all notification id


    private Notification notification;
    private RemoteViews remoteViews;

    private NotificationManager notificationManager;

    private INotificationObserver notificationObserver = new INotificationObserver() {
        @Override
        public void onReceive(String s, HSBundle hsBundle) {
            HSLog.e("notifaction update received");
            ChargingNotificationManager.this.update();
        }
    };

    public ChargingNotificationManager() {

        notificationManager = (NotificationManager) HSApplication.getContext().getSystemService(Context.NOTIFICATION_SERVICE);


        Intent intent = new Intent(HSApplication.getContext(), ChargingBroadcastReceiver.class);
        intent.setAction(ChargingBroadcastReceiver.ACTION_START_CHARGING_ACTIVITY);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(HSApplication.getContext(), 1000, intent, 0);

        remoteViews = new RemoteViews(HSApplication.getContext().getPackageName(), R.layout.charging_module_notification_charging);
        remoteViews.setOnClickPendingIntent(R.id.root_view, pendingIntent);
        remoteViews.setOnClickPendingIntent(R.id.tv_enable, pendingIntent);


        int priority = VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN ? Notification.PRIORITY_MAX : Notification.FLAG_AUTO_CANCEL;

        notification = new NotificationCompat.Builder(HSApplication.getContext()).setOngoing(false).setSmallIcon(R.mipmap.charging_module_notify_charging_small_icon)
                .setPriority(priority).setContent(remoteViews).setContentIntent(pendingIntent).setDefaults(Notification.DEFAULT_ALL).setWhen(0).setAutoCancel(true).build();
        HSGlobalNotificationCenter.addObserver(Constants.EVENT_SYSTEM_BATTERY_CHARGING_STATE_CHANGED, notificationObserver);
    }

    public void cancel() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public void update() {
        if (HSChargingScreenManager.getInstance().isChargingModuleOpened()) {
            return;
        }
        if (Build.VERSION.SDK_INT == 19) {
            return;
        }

        String remoteViewsTitle = getRemoteViewsTitle();
        if (!TextUtils.isEmpty(remoteViewsTitle)) {
            remoteViews.setTextViewText(R.id.txt_title, remoteViewsTitle);
            remoteViews.setTextViewText(R.id.txt_left_time_indicator, HSApplication.getContext().getResources().getString(R.string.enable_charging_detail));
            try {
                notificationManager.notify(NOTIFICATION_ID, notification);
                ChargingAnalytics.getInstance().chargingEnableNotificationShowed();
            } catch (Exception e) {
                //某些机型会出现权限bug错误。
                e.printStackTrace();
            }
        }
    }

    public static String getRemoteViewsTitle() {
        String title;

        Context context = HSApplication.getContext();
        switch (HSChargingManager.getInstance().getChargingState()) {
            case STATE_DISCHARGING:

                title = context.getResources().getString(R.string.charging_module_charging_state_unknown);
                break;
            case STATE_CHARGING_SPEED:
            case STATE_CHARGING_CONTINUOUS:
            case STATE_CHARGING_TRICKLE:

                title = context.getResources().getString(R.string.charger_connected_title_disabled);
                break;
            case STATE_CHARGING_FULL:

                title = context.getResources().getString(R.string.charging_module_charging_state_finish);
                break;
            default:
                title = "";
                break;
        }
        return title;
    }
}
