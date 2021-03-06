package com.ihs.keyboardutils.utils;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.support.annotation.ColorRes;

import com.ihs.app.framework.HSApplication;
import com.ihs.chargingscreen.utils.DisplayUtils;

import java.util.Arrays;

/**
 * Created by jixiang on 16/12/22.
 */

public class RippleDrawableUtils {

    /**
     * for general button with ripple above 5.0 and selector lower than.
     *
     * @param normalColorID id for normal color
     * @return backgroundDrawable
     */
    public static Drawable getButtonRippleBackground(@ColorRes
            int normalColorID) {

        float radius = DisplayUtils.dip2px(2);
        int normalColor = HSApplication.getContext().getResources().getColor(normalColorID);
        return getCompatRippleDrawable(normalColor, getRippleColor(normalColor), radius);
    }

    public static Drawable getTransparentRippleBackground() {
        float radius = DisplayUtils.dip2px(2);
        int normalColor = Color.TRANSPARENT;
        int pressedColor = Color.parseColor("#30ffffff");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new RippleDrawable(ColorStateList.valueOf(getRippleColor(normalColor)),
                    getStateListDrawable(normalColor, pressedColor, -1, radius), getRippleMask(pressedColor, radius));
        } else {
            return getStateListDrawable(normalColor, pressedColor, -1, radius);
        }
    }

    public static Drawable getCompatRippleDrawable(
            int normalColor, int pressedColor, float radius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new RippleDrawable(ColorStateList.valueOf(getRippleColor(normalColor)),
                    getStateListDrawable(normalColor, pressedColor, -1, radius), getRippleMask(normalColor, radius));
        } else {
            return getStateListDrawable(normalColor, pressedColor, -1, radius);
        }
    }

    public static Drawable getCompatRippleDrawable(
            int normalColor, float radius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new RippleDrawable(ColorStateList.valueOf(getRippleColor(normalColor)),
                    getStateListDrawable(normalColor, -1, -1, radius), getRippleMask(normalColor, radius));
        } else {
            return getStateListDrawable(normalColor, getRippleColor(normalColor), -1, radius);
        }
    }

    public static Drawable getContainDisableStatusCompatRippleDrawable(int normalColor, int disableColor, float radius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new RippleDrawable(ColorStateList.valueOf(getRippleColor(normalColor))
                    , getStateListDrawable(normalColor, -1, disableColor, radius)
                    , getRippleMask(normalColor, radius));
        } else {
            return getStateListDrawable(normalColor, getRippleColor(normalColor), disableColor, radius);
        }
    }

    private static int getRippleColor(int normalColor) {
        int r = (int) (((normalColor >> 16) & 0xFF) * 0.8);
        int g = (int) (((normalColor >> 8) & 0xFF) * 0.8);
        int b = (int) (((normalColor) & 0xFF) * 0.8);
        return Color.rgb(r, g, b);
    }

    private static Drawable getRippleMask(int color, float radius) {
        float[] outerRadii = new float[8];
        Arrays.fill(outerRadii, radius);

        RoundRectShape r = new RoundRectShape(outerRadii, null, null);
        ShapeDrawable shapeDrawable = new ShapeDrawable(r);
        shapeDrawable.getPaint().setColor(color);
        return shapeDrawable;
    }

    private static StateListDrawable getStateListDrawable(
            int normalColor, int pressedColor, int disableColor, float radius) {
        StateListDrawable states = new StateListDrawable();
        if (disableColor != -1) {
            states.addState(new int[]{-android.R.attr.state_enabled},
                    getShapeDrawable(disableColor, radius));
        }
        if (pressedColor != -1) {
            states.addState(new int[]{android.R.attr.state_pressed},
                    getShapeDrawable(pressedColor, radius));
            states.addState(new int[]{android.R.attr.state_focused},
                    getShapeDrawable(pressedColor, radius));
            states.addState(new int[]{android.R.attr.state_activated},
                    getShapeDrawable(pressedColor, radius));
        }
        states.addState(new int[]{},
                getShapeDrawable(normalColor, radius));
        return states;
    }

    private static GradientDrawable getShapeDrawable(int color, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(radius);
        shape.setColor(color);
        return shape;
    }


    //从此处开始都是为 4个corner角度不相同的 ripple做的
    public static Drawable getCompatRippleDrawable(
            int normalColor, float radiusTopLeft, float radiusTopRight, float radiusBottomLeft, float radiusBottomRight) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new RippleDrawable(ColorStateList.valueOf(getRippleColor(normalColor)),
                    getStateListDrawable(normalColor, -1, -1, radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight), getRippleMask(normalColor, radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight));
        } else {
            return getStateListDrawable(normalColor, getRippleColor(normalColor), -1, radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight);
        }
    }

    private static StateListDrawable getStateListDrawable(
            int normalColor, int pressedColor, int disableColor,
            float radiusTopLeft, float radiusTopRight, float radiusBottomLeft, float radiusBottomRight) {
        StateListDrawable states = new StateListDrawable();
        if (disableColor != -1) {
            states.addState(new int[]{-android.R.attr.state_enabled},
                    getShapeDrawable(disableColor, radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight));
        }
        if (pressedColor != -1) {
            states.addState(new int[]{android.R.attr.state_pressed},
                    getShapeDrawable(pressedColor, radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight));
            states.addState(new int[]{android.R.attr.state_focused},
                    getShapeDrawable(pressedColor, radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight));
            states.addState(new int[]{android.R.attr.state_activated},
                    getShapeDrawable(pressedColor, radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight));
        }
        states.addState(new int[]{},
                getShapeDrawable(normalColor, radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight));
        return states;
    }

    private static GradientDrawable getShapeDrawable(int color,
                                                     float radiusTopLeft,
                                                     float radiusTopRight,
                                                     float radiusBottomLeft,
                                                     float radiusBottomRight) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadii(new float[]{radiusTopLeft, radiusTopLeft,
                radiusTopRight, radiusTopRight, radiusBottomRight,
                radiusBottomRight, radiusBottomLeft, radiusBottomLeft});
        shape.setColor(color);
        return shape;
    }


    private static Drawable getRippleMask(int color, float radiusTopLeft, float radiusTopRight, float radiusBottomLeft, float radiusBottomRight) {
        float[] outerRadii = new float[]{radiusTopLeft, radiusTopLeft, radiusTopRight, radiusTopRight, radiusBottomRight, radiusBottomRight, radiusBottomLeft, radiusBottomLeft};
        RoundRectShape r = new RoundRectShape(outerRadii, null, null);
        ShapeDrawable shapeDrawable = new ShapeDrawable(r);
        shapeDrawable.getPaint().setColor(color);
        return shapeDrawable;
    }

}
