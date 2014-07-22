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

package org.opensilk.music.ui.folder;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.R;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.filebrowser.FileBrowserArgs;
import org.opensilk.filebrowser.FileItem;
import org.opensilk.filebrowser.MediaProviderUtil;
import org.opensilk.music.ui.cards.event.FileItemCardClick;
import org.opensilk.music.ui.modules.ActionBarController;
import org.opensilk.music.ui.modules.BackButtonListener;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.music.util.Command;
import org.opensilk.music.util.CommandRunner;
import org.opensilk.silkdagger.DaggerInjector;
import org.opensilk.silkdagger.qualifier.ForActivity;
import org.opensilk.silkdagger.qualifier.ForFragment;
import org.opensilk.silkdagger.support.ScopedDaggerFragment;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

/**
 * Created by drew on 7/15/14.
 */
public class FolderFragment extends ScopedDaggerFragment implements BackButtonListener {

    @Inject @ForActivity
    ActionBarController mActionBarController;
    @Inject @ForActivity
    DrawerHelper mDrawerHelper;

    @Inject @ForFragment
    Bus mBus;

    private FileBrowserArgs mBrowserArgs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBrowserArgs = new FileBrowserArgs();
        Set<Integer> mediaTypes = new HashSet<>(2);
        mediaTypes.add(FileItem.MediaType.AUDIO);
        mediaTypes.add(FileItem.MediaType.DIRECTORY);
        mBrowserArgs.setMediaTypes(mediaTypes);
        mBrowserArgs.setPath(FolderPickerActivity.SDCARD_ROOT);//TODO save restore previous
        registerHandlers();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.blank_framelayout_topmargin, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            mActionBarController.setTitle(FolderPickerActivity.makeTitle(mBrowserArgs.getPath()));
            mActionBarController.setSubTitle(FolderPickerActivity.makeSubtitle(mBrowserArgs.getPath()));
            FolderChildFragment f = FolderChildFragment.newInstance(mBrowserArgs);
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.container, f)
                    .commit();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterHandlers();
    }

    FileItemCardHandler mFileCardHandler;

    private void registerHandlers() {
        mFileCardHandler = new FileItemCardHandler();
        mBus.register(mFileCardHandler);
    }

    private void unregisterHandlers() {
        mBus.unregister(mFileCardHandler);
    }

    @Override
    public boolean onBackButtonPressed() {
        return getChildFragmentManager().popBackStackImmediate();
    }

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new FolderModule(),
        };
    }

    @Override
    protected DaggerInjector getParentInjector(Activity activity) {
        return (DaggerInjector) activity;
    }

    class FileItemCardHandler {
        @Subscribe
        public void onFileItemClicked(FileItemCardClick e) {
            final FileItem file = e.file;
            Command c = null;
            switch (e.event) {
                case OPEN:
                    if (file.getMediaType() == FileItem.MediaType.DIRECTORY) {
                        goToFolder(file, true);
                        return;
                    } else if (file.getMediaType() == FileItem.MediaType.UP_DIRECTORY) {
                        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
                            getChildFragmentManager().popBackStackImmediate();
                        } else {
                            goToFolder(file, false);
                        }
                        return;
                    } else if (file.getMediaType() == FileItem.MediaType.AUDIO) {
                        c = new Command() {
                            @Override
                            public CharSequence execute() {
                                LocalSong[] songs = MusicUtils.getLocalSongList(getActivity(), new long[] {file.getId()});
                                MusicUtils.playAllSongs(getActivity(), songs, 0, false);
                                return null;
                            }
                        };
                        break;
                    }
                    break;
                case PLAY_NEXT:
                    c = new Command() {
                        @Override
                        public CharSequence execute() {
                            long[] ids = null;
                            if (file.getMediaType() == FileItem.MediaType.DIRECTORY) {
                                ids = MediaProviderUtil.getChildFiles(getActivity(), file.getId(), FileItem.MediaType.AUDIO);
                            } else if (file.getMediaType() == FileItem.MediaType.AUDIO) {
                                ids = new long[] {file.getId()};
                            }
                            if (ids != null && ids.length > 0) {
                                LocalSong[] songs = MusicUtils.getLocalSongList(getActivity(), ids);
                                MusicUtils.playNext(getActivity(), songs);
                            }
                            return null;
                        }
                    };
                    break;
                case PLAY_ALL:
                    c = new Command() {
                        @Override
                        public CharSequence execute() {
                            long[] ids = null;
                            if (file.getMediaType() == FileItem.MediaType.DIRECTORY) {
                                ids = MediaProviderUtil.getChildFiles(getActivity(), file.getId(), FileItem.MediaType.AUDIO);
                            } else if (file.getMediaType() == FileItem.MediaType.AUDIO) {
                                ids = new long[] {file.getId()};
                            }
                            if (ids != null && ids.length > 0) {
                                LocalSong[] songs = MusicUtils.getLocalSongList(getActivity(), ids);
                                MusicUtils.playAllSongs(getActivity(), songs, 0, false);
                            }
                            return null;
                        }
                    };
                    break;
                case SHUFFLE_ALL:
                    c = new Command() {
                        @Override
                        public CharSequence execute() {
                            long[] ids = null;
                            if (file.getMediaType() == FileItem.MediaType.DIRECTORY) {
                                ids = MediaProviderUtil.getChildFiles(getActivity(), file.getId(), FileItem.MediaType.AUDIO);
                            } else if (file.getMediaType() == FileItem.MediaType.AUDIO) {
                                ids = new long[] {file.getId()};
                            }
                            if (ids != null && ids.length > 0) {
                                LocalSong[] songs = MusicUtils.getLocalSongList(getActivity(), ids);
                                MusicUtils.playAllSongs(getActivity(), songs, 0, true);
                            }
                            return null;
                        }
                    };
                    break;
                case ADD_TO_QUEUE:
                    c = new Command() {
                        @Override
                        public CharSequence execute() {
                            long[] ids = null;
                            if (file.getMediaType() == FileItem.MediaType.DIRECTORY) {
                                ids = MediaProviderUtil.getChildFiles(getActivity(), file.getId(), FileItem.MediaType.AUDIO);
                            } else if (file.getMediaType() == FileItem.MediaType.AUDIO) {
                                ids = new long[] {file.getId()};
                            }
                            if (ids != null && ids.length > 0) {
                                LocalSong[] songs = MusicUtils.getLocalSongList(getActivity(), ids);
                                return MusicUtils.addSongsToQueueSilent(getActivity(), songs);
                            }
                            return null;
                        }
                    };
                    break;
                case ADD_TO_PLAYLIST:
                    return;
                case SET_RINGTONE:
                    return;
                case DELETE:
                    return;
            }
            if (c != null) {
                ApolloUtils.execute(false, new CommandRunner(getActivity(), c));
            }
        }

        private void goToFolder(FileItem file, boolean addToBackstack) {
            mActionBarController.setTitle(FolderPickerActivity.makeTitle(file.getPath()));
            mActionBarController.setSubTitle(FolderPickerActivity.makeSubtitle(file.getPath()));
            FragmentTransaction ft = getChildFragmentManager().beginTransaction()
                    .replace(R.id.container,
                            FolderChildFragment.newInstance(mBrowserArgs.setPath(file.getPath())));
            if (addToBackstack) {
                ft.addToBackStack(null);
            }
            ft.commit();
        }
    }

}