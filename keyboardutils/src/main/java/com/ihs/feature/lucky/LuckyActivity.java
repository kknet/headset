package com.ihs.feature.lucky;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.acb.adadapter.AcbAd;
import com.acb.adadapter.AcbNativeAd;
import com.acb.nativeads.AcbNativeAdLoader;
import com.ihs.app.analytics.HSAnalytics;
import com.ihs.app.framework.HSApplication;
import com.ihs.app.framework.activity.HSAppCompatActivity;
import com.ihs.commons.utils.HSLog;
import com.ihs.commons.utils.HSPreferenceHelper;
import com.ihs.feature.common.ActivityUtils;
import com.ihs.feature.common.AnimatorListenerAdapter;
import com.ihs.feature.common.Utils;
import com.ihs.feature.common.ViewUtils;
import com.ihs.feature.lucky.view.AwardView;
import com.ihs.feature.lucky.view.GameScene;
import com.ihs.feature.lucky.view.GoButton;
import com.ihs.keyboardutils.R;
import com.ihs.keyboardutils.utils.KCAnalyticUtil;
import com.ihs.keyboardutils.utils.ToastUtils;

import java.util.Locale;

import static com.ihs.keyboardutils.utils.CommonUtils.setupTransparentSystemBarsForLmp;


/**
 * Main activity for Lucky game.
 */
public class LuckyActivity extends HSAppCompatActivity
        implements View.OnClickListener, GameState.StateListener, GameState.GameActivity,
        AcbNativeAd.AcbNativeClickListener, AcbNativeAd.AcbAdListener {

    private static final String TAG = LuckyActivity.class.getSimpleName();
    static final String LUCKY_PREFS = "lucksp";

    /**
     * Terminology:
     *   -----------------
     *  |                 |
     *  |     Banner      |
     *  | --------------- |
     *  |Bg/           \Bg|
     *  | /             \ |
     *  |/  Game Scene   \|    Game Scene = game board + targets + arm
     *  |                 |               = [belt + border line (blue shining line at left & right)] + targets + arm
     *  |                 |
     *  |                 |
     *  |                 |
     *  | --------------- |
     *  |  Action Button  |
     *   -----------------
     */

    public static final String PREF_KEY_LUCKY_CREATED = "pref_key_lucky_created";
    private static final String PREF_KEY_MUTE_SOUND_EFFECTS = "lucky_mute_sound_effects";

    private static final int MESSAGE_UPDATE_LIGHT = 0;
    private static final long BANNER_LIGHT_UPDATE_INTERVAL = 230;

    /** Ratio = - translation Y needed / top banner height */
    private static final float CHANCE_COUNTER_POSITION_Y_RATIO = 0.145f;
    private static final float CHANCE_TIMER_POSITION_Y_RATIO = 0.0421f;

    private final HSPreferenceHelper mPrefs = HSPreferenceHelper.create(HSApplication.getContext(),
            LUCKY_PREFS);

    private GameScene mScene;
    private GoButton mGoButton;
    private ImageView mBannerLight1;
    private ImageView mBannerLight2;
    private TextView mChanceCounter;
    private TextView mChanceTimer;
    private AwardView mAwardView;
    private ImageView mMuteButton;

    private String mRestoreTimeString;

    private GameState mState;

    private MusicPlayer mMusicPlayer;
    private MusicPlayerHandler mMusicHandler;

    private EventLogger mEventLogger = new EventLogger();

    private boolean mStopped = true;

    public enum ViewState {
        GAME("Game"),
        TRANSITION("Transition"), // GAME -> AWARD_*
        NETWORK_ERROR("NoNet"),
        AWARD_BOMB("Bomb"),
        AWARD_SMALL("Small"),
        AWARD_LARGE("Large"),

        //only for flurry
        AWARD_CHANCES("Chances"),
        AWARD_WALLPAPER("Wallpaper"),
        AWARD_THEME("Theme"),
        AWARD_AD("Ad"),
        AWARD_NOTHING("Nothing");

        private String mName;

        ViewState(String name) {
            mName = name;
        }

        String getName() {
            return mName;
        }
    }

    private ViewState mViewState = ViewState.GAME;


    public static void installShortCut(){
        if(!HSPreferenceHelper.getDefault().contains(PREF_KEY_LUCKY_CREATED)){
            HSPreferenceHelper.getDefault().putBoolean(PREF_KEY_LUCKY_CREATED,true);
        }else{
            return;
        }

        Intent shortcutIntent = new Intent(HSApplication.getContext(), LuckyActivity.class);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Lucky");
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(HSApplication.getContext(), R.drawable.ic_lucky));
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        HSApplication.getContext().sendBroadcast(addIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lucky);

        boolean mute = mPrefs.getBoolean(PREF_KEY_MUTE_SOUND_EFFECTS, false);
        mMusicPlayer = new MusicPlayer(mute);
        mMusicHandler = new MusicPlayerHandler();
        initView(mute);

        KCAnalyticUtil.logEvent("lucky_open");
    }


    private void initView(boolean muteSoundEffects) {
        View catchActionBtn = ViewUtils.findViewById(this, R.id.lucky_game_catch_action_btn);
        mMuteButton = ViewUtils.findViewById(this, R.id.lucky_game_mute_btn);
        catchActionBtn.setOnClickListener(this);
        mMuteButton.setOnClickListener(this);
        refreshMuteButton(muteSoundEffects);

        mScene = ViewUtils.findViewById(this, R.id.lucky_game_moving_belt);
        mGoButton = ViewUtils.findViewById(this, R.id.lucky_game_catch_action_btn);
        final View banner = ViewUtils.findViewById(this, R.id.lucky_game_top_banner);
        mChanceCounter = ViewUtils.findViewById(banner, R.id.lucky_game_chance_counter);
        mChanceTimer = ViewUtils.findViewById(banner, R.id.lucky_game_chance_restore_timer);

        mAwardView = ViewUtils.findViewById(this, R.id.lucky_game_award_view);
        mAwardView.setMusicPlayer(mMusicPlayer);

        mScene.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressLint("NewApi")
            @Override
            public void onGlobalLayout() {
                int bannerHeight = banner.findViewById(R.id.lucky_game_top_banner_image).getHeight();
                if (!checkDimensions(bannerHeight)) {
                    return;
                }
                mScene.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                mScene.setBannerHeight(bannerHeight);
                mChanceCounter.setTranslationY(-CHANCE_COUNTER_POSITION_Y_RATIO * bannerHeight);
                mChanceTimer.setTranslationY(-CHANCE_TIMER_POSITION_Y_RATIO * bannerHeight);

            }

            private boolean checkDimensions(int bannerHeight) {
                return mScene.getWidth() > 0 && bannerHeight > 0 && mScene.getHeight() - bannerHeight > 0;
            }
        });

        mBannerLight1 = ViewUtils.findViewById(banner, R.id.lucky_game_top_banner_image_light_yellow);
        mBannerLight2 = ViewUtils.findViewById(banner, R.id.lucky_game_top_banner_image_light_red);
    }


    @Override
    protected void onStart() {
        super.onStart();
        mStopped = false;
        mEventLogger.onStart();
        mLightUpdater.sendEmptyMessageDelayed(MESSAGE_UPDATE_LIGHT, BANNER_LIGHT_UPDATE_INTERVAL);
        initGame();
        requestAds();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMusicHandler.sendEmptyMessageDelayed(MusicPlayerHandler.START_BACKGROUND_MUSIC_MSG, 500);
    }

    @SuppressLint("NewApi")
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setupTransparentSystemBarsForLmp(this);
        ActivityUtils.setNavigationBarColor(this, ContextCompat.getColor(this, android.R.color.black));
        ActivityUtils.hideStatusBar(this);
    }



    @Override
    protected void onPause() {
        super.onPause();
        if (mMusicHandler.hasMessages(MusicPlayerHandler.START_BACKGROUND_MUSIC_MSG)) {
            mMusicHandler.removeMessages(MusicPlayerHandler.START_BACKGROUND_MUSIC_MSG);
        }

        mState.reset();
        TargetInfo.release();

        if (mViewState != ViewState.GAME) {
            hideAwardView(null);
        }

        AwardView.release();
        mMusicPlayer.release();
    }

    @Override

    protected void onStop() {
        super.onStop();
        mEventLogger.onStop();
        mStopped = true;
        mLightUpdater.removeCallbacksAndMessages(null);
    }

    @Override

    protected void onDestroy() {
        super.onDestroy();

        mState.setStateListener(null);

        mMusicPlayer = null;
        mMuteButton = null;
        mGoButton = null;
        mLightUpdater = null;
        mScene.release();
        mScene = null;
    }

    private void initGame() {
        GameState state = new GameState(this, mPrefs);
        mState = state;
        mState.setStateListener(this);
        mScene.setState(state);
        TargetInfo.init(state.getConfig());
        mState.reset();
        mMusicPlayer.preload(this, new int[] {
                R.raw.lucky_sound_button_click,
                R.raw.lucky_sound_award_open,
        });
        LuckyPreloadManager.getInstance().refreshTheme(true);
    }

    private void gameDelayed() {
        if (mMusicPlayer == null) {
            return;
        }
        mState.start();
        mMusicPlayer.playBackground(LuckyActivity.this, R.raw.lucky_background_music);
    }

    @SuppressLint("HandlerLeak")
    private Handler mLightUpdater = new Handler() {
        private final int[] VISIBILITY_ARRAY_1 = new int[] {
                View.VISIBLE,
                View.INVISIBLE,
                View.INVISIBLE,
                View.INVISIBLE,
        };
        private final int[] VISIBILITY_ARRAY_2 = new int[] {
                View.INVISIBLE,
                View.INVISIBLE,
                View.VISIBLE,
                View.INVISIBLE,
        };

        private int mIndex;

        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MESSAGE_UPDATE_LIGHT) {
                return;
            }
            super.handleMessage(msg);

            int visibilityIndex = ((mIndex++) % 4);
            mBannerLight1.setVisibility(VISIBILITY_ARRAY_1[visibilityIndex]);
            mBannerLight2.setVisibility(VISIBILITY_ARRAY_2[visibilityIndex]);

            sendEmptyMessageDelayed(MESSAGE_UPDATE_LIGHT, BANNER_LIGHT_UPDATE_INTERVAL);
        }
    };

    @Override
    public boolean isInForeground() {
        return !mStopped;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.lucky_game_catch_action_btn) {
            if (mViewState != ViewState.GAME) {
                return;
            }
            GameState.CatchAnimator catchInFlight = mState.getCatchAnimator();
            if (catchInFlight != null) {
                return;
            }

            if (mState.getChanceCount() <= 0) {
                HSLog.i(TAG, "No chances left, nothing caught");
                ToastUtils.showToast(mRestoreTimeString);
                return;
            }

            final TargetInfo caughtTarget = mState.performCatch();
            mMusicPlayer.play(this, R.raw.lucky_sound_button_click);
            mEventLogger.logCatchAction(caughtTarget);

            if (caughtTarget != null) {
                mState.pause(false);
                mViewState = ViewState.TRANSITION;
                GameState.CatchAnimator catchAnim = mState.getCatchAnimator();
                final Runnable postAnimAction = new Runnable() {
                    @Override
                    public void run() {
                        mState.pause(true);
                        caughtTarget.setFlags(TargetInfo.FLAG_IN_CATCH_ANIMATION, false);
                        caughtTarget.setFlags(TargetInfo.FLAG_CAUGHT, true);

                        TargetInfo.Color color = TargetInfo.Color.UNSPECIFIC;
                        if (caughtTarget.type == TargetInfo.Type.BOMB) {
                            mState.decrementChanceCount();
                            mViewState = ViewState.AWARD_BOMB;
                        } else {
                            color = TargetInfo.Color.GREEN;
                            if (caughtTarget.hasFlag(TargetInfo.FLAG_RED_COLOR)) {
                                color = TargetInfo.Color.RED;
                            }

                            mViewState = ViewState.AWARD_LARGE;
                            if (caughtTarget.type == TargetInfo.Type.SMALL_BOX) {
                                mViewState = ViewState.AWARD_SMALL;
                            }
                        }

                        if (!Utils.isNetworkAvailable(-1) && caughtTarget.type != TargetInfo.Type.BOMB) {
                            mViewState = ViewState.NETWORK_ERROR;
                        }

                        showAwardView(mViewState, color);
                    }
                };
                if (catchAnim != null) {
                    catchAnim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            postAnimAction.run();
                        }
                    });
                } else {
                    postAnimAction.run();
                }
            }

        } else if (i == R.id.lucky_game_mute_btn) {
            boolean mute = mMusicPlayer.toggleMute();
            mEventLogger.logMuteButtonClickEvent(mute);
            refreshMuteButton(mute);
            mPrefs.putBoolean(PREF_KEY_MUTE_SOUND_EFFECTS, mute);

        }
    }

    private void refreshMuteButton(boolean mute) {
        mMuteButton.setImageResource(mute ?
                R.drawable.lucky_mute_btn_muted : R.drawable.lucky_mute_btn_not_muted);
    }

    @Override

    public void onBackPressed() {
        if (mViewState != ViewState.GAME) {
            hideAwardView("Back");
        } else {
            super.onBackPressed();
        }
    }

    private void showAwardView(ViewState state, TargetInfo.Color color) {
        mViewState = state;
        mAwardView.show(state, color);
    }

    private void receiveChancesAward() {
        if (mViewState == ViewState.AWARD_CHANCES) {
            mState.incrementChanceCount(3);
        }
    }

    public void setBoxViewState(ViewState state) {
        mViewState = state;
    }

    public void requestAds() {
        //request ad
        AcbNativeAdLoader loader = new AcbNativeAdLoader(HSApplication.getContext(), HSApplication.getContext().getString(R.string.ad_placement_lucky));
        loader.load(1, null);
    }

    public int getChanceCount() {
        if (mState != null) {
            return mState.getChanceCount();
        }

        return 0;
    }

    public GameConfig getGameConfig() {
        if (mScene != null) {
            return mState.getConfig();
        }
        return null;
    }


    public void hideAwardView(@Nullable String logCloseMethod) {
        if (logCloseMethod != null) {
            mEventLogger.logAwardViewCloseEvent(mViewState, logCloseMethod);
        }
        receiveChancesAward();
        mAwardView.hide();
        mState.resume();
        mViewState = ViewState.GAME;
    }

    //region GameState.StateListener implementation

    @Override
    public void onStateAdvance() {
        mScene.invalidate();
    }

    @Override
    public void onChanceCountChanged(int chanceCount) {
        String chanceCountString = String.format(Locale.US, "%03d", chanceCount);
        mChanceCounter.setText(getString(R.string.lucky_game_chance_counter, chanceCountString));

        if (mGoButton != null) {
            mGoButton.setHasChanceLeft(chanceCount != 0);
        }
    }

    @Override
    public void onRestoreTimerUpdate(int restoreTimeLeftSeconds) {
        if (restoreTimeLeftSeconds == GameState.CHANCE_RESTORE_NOT_SCHEDULED) {
            mChanceTimer.setText("");
            return;
        }
        int minutes = restoreTimeLeftSeconds / 60;
        int seconds = restoreTimeLeftSeconds % 60;
        String timeString = String.format(Locale.US, "%02d:%02d", minutes, seconds);
        mRestoreTimeString = getString(R.string.lucky_game_chance_restore_timer, timeString);
        mChanceTimer.setText(mRestoreTimeString);
    }
    //endregion

    //region AcbNativeClickListener implementation
    @Override
    public void onAdClick(AcbAd ad) {
        HSLog.d("LifeCycleCallbacks", "Lucky native ad clicked");
        mEventLogger.onAdClick();
    }
    // endregion

    //region AcbAdListener implementation
    @Override
    public void onAdExpired(AcbAd acbAd) {

    }

    @Override
    public void onAdWillExpired(AcbAd acbAd) {

    }
    // endregion

    private class MusicPlayerHandler extends Handler {
        static final int START_BACKGROUND_MUSIC_MSG = 1003;


        MusicPlayerHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case START_BACKGROUND_MUSIC_MSG:
                    gameDelayed();
                    break;
            }
        }
    }

    private static class EventLogger {
        private long mStartTime;

        /** Flag to remove ad-click case, not counting it as user leave */
        private boolean mAdClickedUponStop;

        void onAdClick() {
            HSAnalytics.logEvent("Lucky_Award_Ad_Button_Clicked");

            mAdClickedUponStop = true; // Set the flag
        }

        void onStart() {
            if (!mAdClickedUponStop) {
                mStartTime = SystemClock.elapsedRealtime();
            } else {
                mAdClickedUponStop = false; // Consume the flag
            }
        }


        void onStop() {
            if (!mAdClickedUponStop) {
                HSAnalytics.logEvent("Lucky_Times_TimeLength", "Duration",
                        getDurationDescription(SystemClock.elapsedRealtime() - mStartTime));
            }
        }

        private String getDurationDescription(long durationMillis) {
            int durationSeconds = (int) (durationMillis / 1000);
            if (durationSeconds < 5) {
                return "0~5s";
            } else if (durationSeconds < 10) {
                return "5~10s";
            } else if (durationSeconds < 20) {
                return "10~20s";
            } else if (durationSeconds < 30) {
                return "20~30s";
            } else if (durationSeconds < 40) {
                return "30~40s";
            } else if (durationSeconds < 60) {
                return "40~60s";
            } else if (durationSeconds < 90) {
                return "60~90s";
            } else if (durationSeconds < 120) {
                return "90s~2min";
            } else if (durationSeconds < 180) {
                return "2~3min";
            } else if (durationSeconds < 300) {
                return "3~5min";
            } else if (durationSeconds < 600) {
                return "5~10min";
            } else {
                return "10min+";
            }
        }

        void logCatchAction(@Nullable TargetInfo caughtTarget) {
            String type = "";
            if (caughtTarget == null) {
                type = "Null";
            } else {
                switch (caughtTarget.type) {
                    case LARGE_BOX:
                        type = "Golden";
                        break;
                    case SMALL_BOX:
                        if (caughtTarget.hasFlag(TargetInfo.FLAG_RED_COLOR)) {
                            type = "Red";
                        } else {
                            type = "Green";
                        }
                        break;
                    case BOMB:
                        type = "Bomb";
                        break;
                }
            }
            HSAnalytics.logEvent("Lucky_Main_Go_Clicked", "Type", type);
        }

        void logMuteButtonClickEvent(boolean mute) {
        }

        void logAwardViewCloseEvent(ViewState from, String closeMethod) {
            String eventName = "Lucky_Award_" + from.getName() + "_Closed";
            HSAnalytics.logEvent(eventName, "Method", closeMethod);
        }
    }
}