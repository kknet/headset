package com.ihs.keyboardutils.adbuffer;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ihs.chargingscreen.utils.DisplayUtils;
import com.ihs.keyboardutils.BuildConfig;
import com.ihs.keyboardutils.R;
import com.ihs.keyboardutils.ads.AdUtils;
import com.ihs.keyboardutils.nativeads.KCNativeAdView;
import com.ihs.keyboardutils.utils.RippleDrawableUtils;
import com.ihs.keyboardutils.view.CustomProgressDrawable;
import com.ihs.keyboardutils.view.FlashFrameLayout;
import com.kc.commons.utils.KCCommonUtils;
import com.kc.utils.KCAnalytics;

import static java.lang.System.currentTimeMillis;

/**
 * Created by Arthur on 17/4/12.
 */

public class AdLoadingView extends RelativeLayout implements KCNativeAdView.OnAdClickedListener {


    private AdLoadingDialog dialog;
    public TextView tvApply;
    private int leastDownloadingTime = 4000;
    private ImageView progressBar;
    private boolean progressComplete;
    private boolean showCloseButtonWhenFinish;
    private long startDownloadingTime = -1;

    //下载延迟常量
    private boolean hasPurchaseNoAds = false;
    private View closeButton;
    private boolean alertDismissManually = false;

    @Override
    public void onAdClicked(KCNativeAdView adView) {
        alertDismissManually = false;
        dismissSelf();
    }


    public interface OnAdBufferingListener {
        void onDismiss(boolean progressComplete, boolean dismissManually);
    }

    private String[] onLoadingText = {"Applying...", "Applying SuccessFully"};
    private KCNativeAdView nativeAdView;
    private OnAdBufferingListener onAdBufferingListener;

    public AdLoadingView(Context context) {
        super(context);
        init();
    }

    public AdLoadingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AdLoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.layout_ad_loading, this);

        tvApply = (TextView) findViewById(R.id.tv_apply);

        LinearLayout rootView = (LinearLayout) findViewById(R.id.root_view);
        if (DisplayUtils.getScreenHeightPixels() <= 960) {
            rootView.getLayoutParams().height = (int) (DisplayUtils.getScreenHeightPixels() * 0.72);
            rootView.getLayoutParams().width = (int) (DisplayUtils.getScreenWidthPixels() * 0.9);
        } else if (DisplayUtils.getScreenHeightPixels() <= 1280) {
            rootView.getLayoutParams().height = (int) (DisplayUtils.getScreenHeightPixels() * 0.67);
            rootView.getLayoutParams().width = (int) (DisplayUtils.getScreenWidthPixels() * 0.9);
        } else {
            rootView.getLayoutParams().height = (int) (DisplayUtils.getScreenHeightPixels() * 0.6);
            rootView.getLayoutParams().width = (int) (DisplayUtils.getScreenWidthPixels() * 0.85);
        }


        FlashFrameLayout sponsoredContent = (FlashFrameLayout) findViewById(R.id.sponsored_content);
        sponsoredContent.setDuration(3000);
        sponsoredContent.setAutoStart(true);
        sponsoredContent.setRepeatMode(ObjectAnimator.RESTART);

        if (!hasPurchaseNoAds) {
            initAdView();
        }

        closeButton = findViewById(R.id.iv_close);
        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDismissManually = true;
                dismissSelf();
            }
        });

        closeButton.setBackgroundDrawable(
                RippleDrawableUtils.getTransparentRippleBackground());

        progressBar = (ImageView) findViewById(R.id.iv_pb);
        progressBar.setImageDrawable(new CustomProgressDrawable());
    }

    private void initAdView() {
        View inflate = inflate(getContext(), R.layout.layout_ad_loading_adview, null);
        nativeAdView = new KCNativeAdView(getContext());
        nativeAdView.setAdLayoutView(inflate);
        inflate.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FlashFrameLayout flashFrameLayout = inflate.findViewById(R.id.ad_loading_flash_container);
        flashFrameLayout.setAutoStart(false);
        nativeAdView.setOnAdLoadedListener(new KCNativeAdView.OnAdLoadedListener() {
            @Override
            public void onAdLoaded(KCNativeAdView adView) {
                adView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (adView.getWidth() > 0 && adView.getHeight() > 0) {
                            adView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            if (!TextUtils.equals(adView.getAdVendorName(), AdUtils.FACEBOOK_VENDOR_NAME)) {
                                flashFrameLayout.startShimmerAnimation();
                            }
                        }
                    }
                });
            }
        });

        inflate.findViewById(R.id.ad_call_to_action)
                .setBackgroundDrawable(
                        RippleDrawableUtils.getButtonRippleBackground(R.color.ad_action_button_bg));


        nativeAdView.setOnAdClickedListener(this);

        ViewGroup adContainer = (ViewGroup) findViewById(R.id.fl_ad_container);
        if (nativeAdView.getParent() != null) {
            ((ViewGroup) nativeAdView.getParent()).removeView(nativeAdView);
        }
        nativeAdView.setBackgroundColor(getContext().getResources().getColor(android.R.color.white));
        adContainer.addView(nativeAdView);
    }

    private AdLoadingView setIcon(Drawable icon) {
        ((ImageView) findViewById(R.id.iv_icon)).setImageDrawable(icon);
        return this;
    }

    private AdLoadingView setBackgroundPreview(Drawable icon) {
        findViewById(R.id.iv_icon).setBackgroundDrawable(icon);
        return this;
    }

    private AdLoadingView setOnLoadingText(String loadingText, String loadComplete) {
        onLoadingText[0] = loadingText;
        onLoadingText[1] = loadComplete;
        tvApply.setText(onLoadingText[0]);
        return this;
    }

    private AdLoadingView setAdPlacementName(String adPlacementName) {
        if (!TextUtils.isEmpty(adPlacementName) && !hasPurchaseNoAds) {
            nativeAdView.setPrimaryViewSize((int) (DisplayUtils.getScreenWidthPixels() * 0.9), (int) (DisplayUtils.getScreenWidthPixels() * 0.9 / 1.9));
            nativeAdView.load(adPlacementName);
            return this;
        }
        if(BuildConfig.DEBUG){
            throw new RuntimeException("ad loading 广告池名字未配置");
        }
        return this;
    }

    /**
     * 如果真正下载时间不足 leastDownloadingTime
     *
     * @param percent
     */
    public void updateProgressPercent(int percent) {
        if (percent < 100) {
            if (startDownloadingTime < 0) {
                startDownloadingTime = currentTimeMillis();
            }
            progressBar.getDrawable().setLevel(percent);
        } else {
            long fakeLoadingTime = leastDownloadingTime - (System.currentTimeMillis() - startDownloadingTime);
            if (fakeLoadingTime > 0) {
                fakeLoadingProgress(percent, 101, fakeLoadingTime);
            } else {
                fakeLoadingProgress(percent, 101, 1);
            }
        }
    }

    public void setConnectionStateText(String text) {
        tvApply.setText(text);
    }

    public void setConnectionProgressVisibility(int visibility) {
        progressBar.setVisibility(visibility);
    }

    public void startFakeLoading() {
        fakeLoadingProgress(0, 100, leastDownloadingTime);
    }

    private void fakeLoadingProgress(final int startPercent, final int endPercent, long duration) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(startPercent, endPercent);
        valueAnimator.setDuration(duration);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatedValue = (int) animation.getAnimatedValue();
                progressBar.getDrawable().setLevel(animatedValue);
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                tvApply.setText(onLoadingText[1]);

                progressBar.setVisibility(INVISIBLE);
                closeButton.setVisibility(VISIBLE);
                progressComplete = true;

                startDownloadingTime = -1;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                startDownloadingTime = -1;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        valueAnimator.start();
    }

    public void configParams(Drawable bg, Drawable icon, String loadingText, String loadComplete, String adPlacementName, OnAdBufferingListener onAdBufferingListener
            , int delayAfterDownloadComplete, boolean hasPurchaseNoAds) {
        setBackgroundPreview(bg).setIcon(icon).setAdPlacementName(adPlacementName).setOnLoadingText(loadingText, loadComplete);
        if(leastDownloadingTime < delayAfterDownloadComplete){
            this.leastDownloadingTime = delayAfterDownloadComplete;
        }
        this.onAdBufferingListener = onAdBufferingListener;
        this.hasPurchaseNoAds = hasPurchaseNoAds;
        if (hasPurchaseNoAds) {
            LinearLayout rootView = (LinearLayout) this.findViewById(R.id.root_view);
            rootView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
            FrameLayout frameLayout = (FrameLayout) this.findViewById(R.id.out_ad_container);
            frameLayout.setVisibility(View.GONE);
        }
    }

    public void setIconImageVisibility(int visibility) {
        findViewById(R.id.iv_icon).setVisibility(visibility);
    }

    public void setLoadingTextSize(float size) {
        tvApply.setTextSize(size);
    }

    public void hideCloseButtonUntilSuccess(boolean showCloseButtonWhenFinish) {
        this.showCloseButtonWhenFinish = showCloseButtonWhenFinish;
        if (showCloseButtonWhenFinish) {
            closeButton.setVisibility(INVISIBLE);
        }
    }

    public void showInDialog() {
        KCAnalytics.logEvent("app_alert_applyingItem_show");
        dialog = new AdLoadingDialog(getContext());
        dialog.setContentView(this);
        dialog.setCancelable(false);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (onAdBufferingListener != null) {
                    onAdBufferingListener.onDismiss(progressComplete, alertDismissManually);
                }
            }
        });
        KCCommonUtils.showDialog(dialog);
    }

    public void dismissSelf() {
        if (nativeAdView != null) {
            nativeAdView.release();
            nativeAdView = null;
        }

        if (dialog == null) {
            onAdBufferingListener.onDismiss(progressComplete, alertDismissManually);
        } else {
            KCCommonUtils.dismissDialog(dialog);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (nativeAdView != null) {
            nativeAdView.release();
            nativeAdView = null;
        }
    }
}
