/*
 * Copyright (C) 2012 Andrew Neal
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

package org.opensilk.music;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import org.opensilk.music.BuildConfig;
import com.andrew.apollo.provider.RecentStore;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.ThemeHelper;
import com.bugsense.trace.BugSenseHandler;

import org.opensilk.cast.manager.MediaCastManager;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.artwork.ArtworkServiceImpl;
import org.opensilk.music.artwork.cache.BitmapDiskLruCache;
import org.opensilk.music.ui2.LauncherActivity;
import org.opensilk.silkdagger.DaggerInjector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dagger.ObjectGraph;
import mortar.Mortar;
import mortar.MortarScope;
import timber.log.Timber;

/**
 * Use to initilaze singletons and global static variables that require context
 */
public class MusicApp extends Application implements DaggerInjector {
    private static final boolean DEBUG = BuildConfig.DEBUG;

    /**
     * Maximum size for artwork, this will be smallest screen width
     */
    public static int sDefaultMaxImageWidthPx;

    /**
     * Largest size of any thumbnail displayed
     */
    public static int DEFAULT_THUMBNAIL_SIZE_DP = 200;

    /**
     * Largest size a thumbnail will be
     */
    public static int sDefaultThumbnailWidthPx;

    /**
     * Disable some features depending on device type
     */
    public static boolean sIsLowEndHardware;

    /**
     * Contains the object graph, we use a singleton instance
     * to obtain the graph so we can inject our countent providers
     * which will be created before onCreate() is called.
     */
    private GraphHolder mGraphHolder;

    protected ObjectGraph mScopedGraphe;

    protected MortarScope mRootScope;

    @Override
    //@DebugLog
    public void onCreate() {
        super.onCreate();

        if (DEBUG) {
//            BugSenseHandler.initAndStartSession(this, "751fd228");
        } else {
            BugSenseHandler.initAndStartSession(this, "7c67fe46");
        }

        setupDagger();
        setupMortar();

        if (DEBUG) {
            // Plant the forest
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new ReleaseTree());
        }

        /*
         * Init global static variables
         */
        sDefaultMaxImageWidthPx = getMinDisplayWidth(getApplicationContext());
        sDefaultThumbnailWidthPx = convertDpToPx(getApplicationContext(), DEFAULT_THUMBNAIL_SIZE_DP);
        sIsLowEndHardware = isLowEndHardware(getApplicationContext());

        /*
         * XXXX Note to future drew. DO NOT INIT SINGLETONS HERE. They will be created twice!
         */

        /*
         * Debugging
         */
        // Enable strict mode logging
        enableStrictMode();
    }

    protected void setupDagger() {
        mGraphHolder = GraphHolder.get(this);
        mScopedGraphe = mGraphHolder.getObjectGraph().plus(new AppModule(this));
    }

    protected void setupMortar() {
        mRootScope = Mortar.createRootScope(DEBUG, mScopedGraphe);
    }

    @Override
    public void inject(Object o) {
        mScopedGraphe.inject(o);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return mScopedGraphe;
    }

    @Override
    public Object getSystemService(String name) {
        if (Mortar.isScopeSystemService(name)) {
            return mRootScope;
        }
        return super.getSystemService(name);
    }

    protected void enableStrictMode() {
        if (DEBUG) {
            final StrictMode.ThreadPolicy.Builder threadPolicyBuilder
                    = new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen();
            StrictMode.setThreadPolicy(threadPolicyBuilder.build());

            final StrictMode.VmPolicy.Builder vmPolicyBuilder
                    = new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .setClassInstanceLimit(MusicUtils.class, 1)
                    .setClassInstanceLimit(RecentStore.class, 1)
                    .setClassInstanceLimit(PreferenceUtils.class, 1)
                    .setClassInstanceLimit(ThemeHelper.class, 1)
                    .setClassInstanceLimit(MediaCastManager.class, 1)
                    .setClassInstanceLimit(BitmapDiskLruCache.class, 1)
                    .setClassInstanceLimit(BitmapDiskLruCache.class, 1)
                    .setClassInstanceLimit(ArtworkManager.class, 1)
                    .setClassInstanceLimit(LauncherActivity.class, 1)
                    .setClassInstanceLimit(ArtworkServiceImpl.class, 1);
            StrictMode.setVmPolicy(vmPolicyBuilder.build());
        }
    }

    /**
     * Converts given dp value to density specific pixel value
     * @param context
     * @param dp
     * @return
     */
    public static int convertDpToPx(Context context, float dp) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        return Math.round(dp * (metrics.densityDpi / 160f));
    }

    /**
     * Returns smallest screen dimension
     * @param context
     * @return
     */
    public static int getMinDisplayWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//        Point size = new Point();
//        wm.getDefaultDisplay().getSize(size);
//        return Math.min(size.x, size.y);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        return Math.min(metrics.widthPixels, metrics.heightPixels);
    }

    public static boolean isLowEndHardware(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            return am.isLowRamDevice();
        } else {
            return Runtime.getRuntime().availableProcessors() == 1;
        }
    }

    private static class ReleaseTree extends Timber.HollowTree {
        private static final Pattern ANONYMOUS_CLASS = Pattern.compile("\\$\\d+$");

        private static String createTag() {
            String tag = new Throwable().getStackTrace()[5].getClassName();
            Matcher m = ANONYMOUS_CLASS.matcher(tag);
            if (m.find()) {
                tag = m.replaceAll("");
            }
            return tag.substring(tag.lastIndexOf('.') + 1);
        }

        private static String formatString(String message, Object... args) {
            // If no varargs are supplied, treat it as a request to log the string without formatting.
            return args.length == 0 ? message : String.format(message, args);
        }

        private static void sendException(Throwable t, String message, Object... args) {
            try {
                BugSenseHandler.sendExceptionMessage(createTag(), formatString(message, args), new Exception(t));
            } catch (Exception ignored) {/*safety*/}
        }

        @Override
        public void e(Throwable t, String message, Object... args) {
            sendException(t, message, args);
        }

    }
}
