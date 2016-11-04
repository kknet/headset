package com.ihs.keyboardutils.panelcontainer;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.utils.HSLog;
import com.ihs.keyboardutils.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Arthur on 16/10/21.
 */

public class KeyboardPanelSwitchContainer extends RelativeLayout implements IPanelSwitcher {

    public static final int BAR_TOP = RelativeLayout.ABOVE;
    public static final int BAR_BOTTOM = RelativeLayout.BELOW;

    private int barPosition = BAR_BOTTOM;
    private FrameLayout barView = null;
    private FrameLayout panelView = null;
    private PanelBean currentPanel = null;
    private Map<Class, PanelBean> panelMap = new HashMap<>();

    public KeyboardPanelSwitchContainer(int barPosition) {
        super(HSApplication.getContext());
        this.barPosition = barPosition;

        barView = new FrameLayout(getContext());
        barView.setId(R.id.container_bar_id);

        panelView = new FrameLayout(getContext());
        panelView.setId(R.id.container_panel_id);

        adjustViewPosition();
    }

    private KeyboardPanelSwitchContainer(Context context) {
        super(context);
    }

    private KeyboardPanelSwitchContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private KeyboardPanelSwitchContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void showPanel(Class panelClass) {
        showPanel(panelClass, true);
    }

    public void showPanel(Class panelClass, boolean autoRelease) {
        if (!BasePanel.class.isAssignableFrom(panelClass)) {
            Log.e("panelCOntainer", "wrong type");
            return;
        }

        if (currentPanel != null) {
            if (panelClass == currentPanel.getPanel().getClass()) {
                Log.e("panel", "panel Showed");
                return;
            } else {
                if (currentPanel.isAutoRelease()) {
                    panelView.removeView(currentPanel.getPanel().getPanelView());
                    panelMap.remove(currentPanel.getPanel().getClass());
                    currentPanel = null;
                    System.gc();
                    HSLog.e("cause GC");
                }
            }
        }

        BasePanel panel;
        if (panelMap.get(panelClass) != null) {
            panel = panelMap.get(panelClass).getPanel();
        } else {
            try {
                panel = (BasePanel) panelClass.getConstructor(IPanelSwitcher.class).newInstance(this);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }


        PanelBean panelBean = new PanelBean(panel, autoRelease);
        panelMap.put(panelClass, panelBean);
        currentPanel = panelBean;

        View view = panel.onCreatePanelView();

        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        panelView.addView(view, layoutParams);
    }


    public void setTabBarPosition(int position) {
        barPosition = position;
        adjustViewPosition();
    }


    public void setBarView(View view) {
        if (view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        barView.addView(view, layoutParams);
    }


    @Override
    public void setTabBarVisibility(boolean hide, boolean expandPanel) {
        if (hide) {
            if (expandPanel) {
                barView.setVisibility(GONE);
            } else {
                barView.setVisibility(INVISIBLE);
            }
        } else {
            barView.setVisibility(VISIBLE);
        }
    }

    public void addMoreContainer(View container) {
        if (container.getParent() != null) {
            ((ViewGroup) container.getParent()).removeView(container);
        }
        addView(container, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }


    public void removeContainer(View container) {
        if (container == null || container.getParent() == null) {
            return;
        }
        removeView(container);
    }


    private void adjustViewPosition() {
        LayoutParams barParams = (LayoutParams) barView.getLayoutParams();
        LayoutParams panelParams = (LayoutParams) panelView.getLayoutParams();
        boolean needAddView = false;

        if (barParams == null || panelParams == null) {
            needAddView = true;
            barParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            panelParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        switch (barPosition) {
            case BAR_TOP:
                panelParams.addRule(ABOVE, 0);
                barParams.addRule(ALIGN_PARENT_BOTTOM, 0);

                barParams.addRule(ABOVE, panelView.getId());
                panelParams.addRule(ALIGN_PARENT_BOTTOM, TRUE);
                break;
            case BAR_BOTTOM:
                barParams.addRule(ABOVE, 0);
                panelParams.addRule(ALIGN_PARENT_BOTTOM, 0);

                panelParams.addRule(ABOVE, barView.getId());
                barParams.addRule(ALIGN_PARENT_BOTTOM, TRUE);
                break;
        }

        if (needAddView) {
            addView(barView, barParams);
            addView(panelView, panelParams);
        } else {
            requestLayout();
        }
    }


}
