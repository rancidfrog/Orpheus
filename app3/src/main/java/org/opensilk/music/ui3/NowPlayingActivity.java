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

package org.opensilk.music.ui3;

import android.content.Context;
import android.content.Intent;
import android.view.WindowManager;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.AppComponent;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.ui.theme.OrpheusTheme;

import javax.inject.Inject;

import butterknife.ButterKnife;
import mortar.MortarScope;
import timber.log.Timber;

/**
 * Created by drew on 10/2/15.
 */
public class NowPlayingActivity extends MusicActivity {

    public static void startSelf(Context context, boolean startQueue) {
        Intent i = new Intent(context, NowPlayingActivity.class);
        i.putExtra("startqueue", startQueue);
        context.startActivity(i);
    }

    @Inject AppPreferences mSettings;

    @Override
    protected void setupContentView() {
        setContentView(R.layout.activity_nowplaying);
        ButterKnife.inject(this);
    }

    @Override
    protected void themeActivity(AppPreferences preferences) {
        boolean darkTheme = preferences.isDarkTheme();
        OrpheusTheme theme = preferences.getTheme();
        setTheme(theme.dark);//TODO light themes
    }

    @Override
    protected void performInjection() {
        NowPlayingActivityComponent component = DaggerService.getDaggerComponent(this);
        component.inject(this);
    }

    @Override
    public int getContainerViewId() {
        return 0;
    }

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        AppComponent appComponent = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE,
                NowPlayingActivityComponent.FACTORY.call(appComponent));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mSettings.getBoolean(AppPreferences.NOW_PLAYING_KEEP_SCREEN_ON, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerOwnerDelegate != null && mDrawerOwnerDelegate.onBackPressed()) {
            return;
        }
        if (onSupportNavigateUp()) {
            return;
        }
        super.onBackPressed();
    }
}
