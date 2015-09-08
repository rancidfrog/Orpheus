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

package org.opensilk.music.ui3.index;

import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.music.R;
import org.opensilk.music.ui3.index.albums.AlbumsScreen;
import org.opensilk.music.ui3.index.artists.ArtistsScreen;
import org.opensilk.music.ui3.index.genres.GenresScreen;
import org.opensilk.music.ui3.index.tracks.TracksScreen;

import rx.functions.Func0;

/**
 * Created by drew on 10/3/14.
 */
public enum GalleryPage {
//    PLAYLIST(R.string.page_playlists, new Func0<Screen>() {
//        @Override
//        public Screen call() {
//            return new PlaylistsScreen();
//        }
//    }),
    ARTIST(R.string.page_artists, new Func0<Screen>() {
        @Override
        public Screen call() {
            return new ArtistsScreen();
        }
    }),
    ALBUM(R.string.page_albums, new Func0<Screen>() {
        @Override
        public Screen call() {
            return new AlbumsScreen();
        }
    }),
    SONG(R.string.page_songs, new Func0<Screen>() {
        @Override
        public Screen call() {
            return new TracksScreen();
        }
    }),
    GENRE(R.string.page_genres, new Func0<Screen>() {
        @Override
        public Screen call() {
            return new GenresScreen();
        }
    });

    public final int titleRes;
    public final Func0<Screen> FACTORY;

    GalleryPage(int titleRes, Func0<Screen> FACTORY) {
        this.titleRes = titleRes;
        this.FACTORY = FACTORY;
    }

}