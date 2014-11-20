/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.ui2.profile;

import android.content.Context;
import android.view.View;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.core.android.ActionBarOwner;

import mortar.ViewPresenter;
import rx.Subscription;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 11/18/14.
 */
public abstract class BasePresenter extends ViewPresenter<ProfileView> {

    final ActionBarOwner actionBarOwner;
    final ArtworkRequestManager requestor;

    Subscription loaderSubscription;

    protected BasePresenter(ActionBarOwner actionBarOwner,
                            ArtworkRequestManager requestor) {
        this.actionBarOwner = actionBarOwner;
        this.requestor = requestor;
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        if (isSubscribed(loaderSubscription)) {
            loaderSubscription.unsubscribe();
            loaderSubscription = null;
        }
    }

    abstract String getTitle(Context context);
    abstract String getSubtitle(Context context);
    abstract int getNumArtwork();

}