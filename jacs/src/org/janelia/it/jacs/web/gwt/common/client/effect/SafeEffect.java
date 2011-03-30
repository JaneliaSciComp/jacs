/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.gwt.common.client.effect;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import org.gwtwidgets.client.style.Color;
import org.gwtwidgets.client.wrap.Effect;
import org.gwtwidgets.client.wrap.EffectOption;

/**
 * This class delegates to .gwtwidgets.client.wrap.Effect, except that it is safe for GWT hosted
 * mode (by ignoring EffectOptions if in hosted mode).
 *
 * @author Michael Press
 */
public class SafeEffect {

    public static void appear(Widget widget) {
        Effect.appear(widget);
    }

    public static void appear(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.appear(widget, opts);
        else
            Effect.appear(widget);
    }

    public static void blindDown(Widget widget) {
        Effect.blindDown(widget);
    }

    public static void blindDown(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.blindDown(widget, opts);
        else
            Effect.blindDown(widget);
    }

    public static void blindUp(Widget widget) {
        Effect.blindUp(widget);
    }

    public static void blindUp(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.blindUp(widget, opts);
        else
            Effect.blindUp(widget);
    }

    public static void dropOut(Widget widget) {
        Effect.dropOut(widget);
    }

    public static void dropOut(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.dropOut(widget, opts);
        else
            Effect.dropOut(widget);
    }

    public static void fade(Widget widget) {
        Effect.fade(widget);
    }

    public static void fade(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.fade(widget, opts);
        else
            Effect.fade(widget);
    }

    public static void fold(Widget widget) {
        Effect.fold(widget);
    }

    public static void fold(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.fold(widget, opts);
        else
            Effect.fold(widget);
    }

    public static void grow(Widget widget) {
        Effect.grow(widget);
    }

    public static void grow(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.grow(widget, opts);
        else
            Effect.grow(widget);
    }

    public static void highlight(Widget widget) {
        Effect.highlight(widget);
    }

    public static void highlight(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.highlight(widget, opts);
        else
            Effect.highlight(widget);
    }

    public static void highlight(Widget widget, Color startColor, Color endColor, double duration) {
        if (GWT.isScript())
            Effect.highlight(widget, new EffectOption[]{
                    new EffectOption("startcolor", startColor.getHexValue()),
                    new EffectOption("endcolor", endColor.getHexValue()),
                    new EffectOption("duration", duration)});
        else
            Effect.highlight(widget);
    }

    public static void keepFixed(Widget widget) {
        Effect.keepFixed(widget);
    }

    public static void keepFixed(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.keepFixed(widget, opts);
        else
            Effect.keepFixed(widget);
    }

    public static void move(Widget widget) {
        Effect.move(widget);
    }

    public static void move(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.move(widget, opts);
        else
            Effect.move(widget);
    }

    public static void moveBy(Widget widget, int y, int x) {
        Effect.moveBy(widget, y, x);
    }

    public static void moveBy(Widget widget, int y, int x, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.moveBy(widget, y, x, opts);
        else
            Effect.moveBy(widget, y, x);
    }

    public static void opacity(Widget widget) {
        Effect.opacity(widget);
    }

    public static void opacity(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.opacity(widget, opts);
        else
            Effect.opacity(widget);
    }

    public static void parallel(Widget widget) {
        Effect.parallel(widget);
    }

    public static void parallel(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.parallel(widget, opts);
        else
            Effect.parallel(widget);
    }

    public static void puff(Widget widget) {
        Effect.puff(widget);
    }

    public static void puff(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.puff(widget, opts);
        else
            Effect.puff(widget);
    }

    public static void pulsate(Widget widget) {
        Effect.pulsate(widget);
    }

    public static void pulsate(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.pulsate(widget, opts);
        else
            Effect.pulsate(widget);
    }

    public static void scale(Widget widget) {
        Effect.scale(widget);
    }

    public static void scale(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.scale(widget, opts);
        else
            Effect.scale(widget);
    }

    public static void scrollTo(Widget widget) {
        Effect.scrollTo(widget);
    }

    public static void scrollTo(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.scrollTo(widget, opts);
        else
            Effect.scrollTo(widget);
    }

    public static void shake(Widget widget) {
        Effect.shake(widget);
    }

    public static void shake(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.shake(widget, opts);
        else
            Effect.shake(widget);
    }

    public static void shrink(Widget widget) {
        Effect.shrink(widget);
    }

    public static void shrink(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.shrink(widget, opts);
        else
            Effect.shrink(widget);
    }

    public static void slideDown(Widget widget) {
        Effect.slideDown(widget);
    }

    public static void slideDown(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.slideDown(widget, opts);
        else
            Effect.slideDown(widget);
    }

    public static void slideUp(Widget widget) {
        Effect.slideUp(widget);
    }

    public static void slideUp(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.slideUp(widget, opts);
        else
            Effect.slideUp(widget);
    }

    public static void squish(Widget widget) {
        Effect.squish(widget);
    }

    public static void squish(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.squish(widget, opts);
        else
            Effect.squish(widget);
    }

    public static void switchOff(Widget widget) {
        Effect.switchOff(widget);
    }

    public static void switchOff(Widget widget, EffectOption[] opts) {
        if (GWT.isScript())
            Effect.switchOff(widget, opts);
        else
            Effect.switchOff(widget);
    }
}
