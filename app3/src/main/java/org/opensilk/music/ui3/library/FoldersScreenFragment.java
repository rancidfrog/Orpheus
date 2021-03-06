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

package org.opensilk.music.ui3.library;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Bundle;

import org.opensilk.bundleable.BadBundleableException;
import org.opensilk.bundleable.BundleableUtil;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortarfragment.MortarFragment;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.model.Container;

/**
 * Created by drew on 5/2/15.
 */
public class FoldersScreenFragment extends MortarFragment {
    public static final String NAME = FoldersScreenFragment.class.getName();

    public static FoldersScreenFragment ni(Context context, LibraryConfig config, Container container) {
        Bundle args = new Bundle();
        args.putBundle("conf", config.dematerialize());
        args.putBundle("fldr", container.toBundle());
        FoldersScreenFragment f = factory(context, NAME, args);
        if (VersionUtils.hasLollipop()) {
            f.applyTransitions21();
        }
        return f;
    }

    @Override
    protected Screen newScreen() {
        LibraryConfig config = LibraryConfig.materialize(getArguments().getBundle("conf"));
        Container container = null;
        try {
            container = BundleableUtil.materializeBundle(getArguments().getBundle("fldr"));
        } catch (BadBundleableException ignored) {/*cant recover let it crash from null*/}
        return new FoldersScreen(config, container);
    }

    @TargetApi(21)
    void applyTransitions21() {
        //TODO blowsup onbackpressed (scope destroyed)
//        setEnterTransition(new Slide());
    }

}
