/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui2.library;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.opensilk.music.ui2.core.CanShowScreen;

import javax.inject.Inject;

import flow.Flow;
import flow.Layouts;
import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarScope;
import timber.log.Timber;

import static android.view.animation.AnimationUtils.loadAnimation;

/**
 * Created by drew on 10/6/14.
 */
public class PluginView extends LinearLayout implements CanShowScreen<Blueprint> {

    @Inject
    PluginScreen.Presenter presenter;

    public PluginView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        presenter.takeView(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    @Override
    public void showScreen(Blueprint screen, Flow.Direction direction) {
        MortarScope myScope = Mortar.getScope(getContext());
        MortarScope newChildScope = myScope.requireChild(screen);

        View oldChild = getChildView();
        View newChild;

        if (oldChild != null) {
            MortarScope oldChildScope = Mortar.getScope(oldChild.getContext());
            if (oldChildScope.getName().equals(screen.getMortarScopeName())) {
                // If it's already showing, short circuit.
                Timber.d("Short Circuit");
                return;
            }

            myScope.destroyChild(oldChildScope);
        }

        // Create the new child.
        Context childContext = newChildScope.createContext(getContext());
        newChild = Layouts.createView(childContext, screen);

        setAnimation(direction, oldChild, newChild);

        // Out with the old, in with the new.
        if (oldChild != null) getContainer().removeView(oldChild);
        getContainer().addView(newChild);
    }

    protected void setAnimation(Flow.Direction direction, View oldChild, View newChild) {
//        if (oldChild == null) return;
//
//        int out = direction == Flow.Direction.FORWARD ? R.anim.slide_out_left : R.anim.slide_out_right;
//        int in = direction == Flow.Direction.FORWARD ? R.anim.slide_in_right : R.anim.slide_in_left;
//
//        oldChild.setAnimation(loadAnimation(context, out));
//        newChild.setAnimation(loadAnimation(context, in));
    }

    private View getChildView() {
        return getContainer().getChildAt(0);
    }

    private ViewGroup getContainer() {
        return this;
    }
}