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
package org.opensilk.music.ui.cards;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.widgets.NowPlayingAnimation;

import it.gmariotti.cardslib.library.internal.CardHeader;

/**
 * Created by drew on 2/12/14.
 */
public abstract class CardBaseList<D> extends CardBaseThumb<D> {

    protected String mSubTitle;
    protected String mExtraText;
    protected NowPlayingAnimation mAnimation;

    public CardBaseList(Context context, D data) {
        super(context, data);
    }

    public CardBaseList(Context context, D data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        // Super sets title
        super.setupInnerViewElements(parent, view);
        TextView v = (TextView) view.findViewById(R.id.card_main_inner_sub_title);
        if (v != null) {
            if (mSubTitle != null) {
                v.setText(mSubTitle);
            } else {
                v.setVisibility(View.GONE);
            }
        }
        TextView v2 = (TextView) view.findViewById(R.id.card_main_inner_extra_text);
        if (v2 != null) {
            if (mExtraText != null) {
                v2.setText(mExtraText);
            } else {
                v2.setVisibility(View.GONE);
            }
        }
        mAnimation = (NowPlayingAnimation) view.findViewById(R.id.play_animation);
        final IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED);
        getContext().registerReceiver(mReceiver, filter);
    }

    @Override
    protected void initHeader() {
        final CardHeader header = new CardHeader(getContext());
        header.setButtonOverflowVisible(true);
        header.setPopupMenu(getHeaderMenuId(), getNewHeaderPopupMenuListener());
        addCardHeader(header);
    }

    /**
     * @return Resource id of popup menu
     */
    protected abstract int getHeaderMenuId();

    /**
     * @return Listener for popup menu actions
     */
    protected abstract CardHeader.OnClickCardHeaderPopupMenuListener getNewHeaderPopupMenuListener();

    public void setSubTitle(String title) {
        mSubTitle = title;
    }

    protected abstract boolean shouldStartAnimating(long trackId);

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MusicPlaybackService.PLAYSTATE_CHANGED.equals(intent.getAction())
                    || MusicPlaybackService.META_CHANGED.equals(intent.getAction())) {
                if (mAnimation != null) {
                    long trackId = MusicUtils.getCurrentAudioId();
                    if (MusicUtils.isPlaying() && shouldStartAnimating(trackId)) {
                        mAnimation.startAnimating(trackId);
                    } else {
                        if (mAnimation.isAnimating()) {
                            mAnimation.stopAnimating();
                        }
                    }
                }
            }
        }
    };
}