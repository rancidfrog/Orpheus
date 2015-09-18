/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package org.opensilk.music.index.client;

import android.content.Context;
import android.net.Uri;

import org.opensilk.common.core.dagger2.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 9/17/15.
 */
@Singleton
public class IndexClientImpl implements IndexClient {

    final Context appContext;

    @Inject
    public IndexClientImpl(@ForApplication Context appContext) {
        this.appContext = appContext;
    }

    @Override
    public boolean isIndexed(Uri uri) {
        return false;
    }

    @Override
    public boolean add(Uri uri) {
        return false;
    }

    @Override
    public boolean remove(Uri uri) {
        return false;
    }

}
