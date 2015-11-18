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

package org.opensilk.music.ui3.playlist;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.jakewharton.rxbinding.view.ViewClickEvent;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.rx.RxUtils;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.common.ui.mortar.DialogFactory;
import org.opensilk.common.ui.mortar.DialogPresenter;
import org.opensilk.music.R;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.library.client.TypedBundleableLoader;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.sort.PlaylistSortOrder;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.MenuHandler;
import org.opensilk.music.ui3.common.MenuHandlerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by drew on 10/24/15.
 */
@Module
public class PlaylistChooseScreenModule {
    final PlaylistChooseScreen screen;

    public PlaylistChooseScreenModule(PlaylistChooseScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri(@Named("IndexProviderAuthority") String authority) {
        return IndexUris.playlists(authority);
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            ItemClickListener itemClickListener,
            MenuHandler menuConfig,
            @ForApplication Context context
    ) {
        return BundleablePresenterConfig.builder()
                .setWantsGrid(false)
                .setItemClickListener(itemClickListener)
                .setMenuConfig(menuConfig)
                .setToolbarTitle(context.getString(R.string.title_playlists))
                .build();
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener(
            final DialogPresenter dialogPresenter, final ActivityResultsController activityResultsController) {
        return new ItemClickListener() {
            @Override
            public void onItemClicked(final BundleablePresenter presenter, final Context context, final Model item) {
                final Subscriber<Playlist> subscriber = new Subscriber<Playlist>() {
                    @Override
                    public void onCompleted() {
                        //pass
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(context, R.string.err_generic, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent();
                        activityResultsController.setResultAndFinish(Activity.RESULT_CANCELED, intent);
                    }

                    @Override
                    public void onNext(Playlist playlist) {
                        Intent intent = new Intent().putExtra("plist", playlist.toBundle());
                        activityResultsController.setResultAndFinish(Activity.RESULT_OK, intent);
                    }
                };
                final Subscription s;
                if (screen.tracksUris == null) {
                    s = Observable.create(
                            new Observable.OnSubscribe<Integer>() {
                                @Override
                                public void call(Subscriber<? super Integer> subscriber) {
                                    int count = presenter.getIndexClient().addToPlaylist(item.getUri(), screen.tracks);
                                    if (!subscriber.isUnsubscribed()) {
                                        subscriber.onNext(count);
                                        subscriber.onCompleted();
                                    }
                                }
                            })
                            .subscribeOn(Schedulers.computation())
                            .delay(1, TimeUnit.SECONDS)
                            .map(new Func1<Integer, Playlist>() {
                                @Override
                                public Playlist call(Integer integer) {
                                    Playlist playlist = presenter.getIndexClient().getPlaylist(item.getUri());
                                    if (playlist == null) {
                                        playlist = (Playlist) item;
                                    }
                                    return playlist;
                                }
                            })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(subscriber);
                } else {
                    Observable<Observable<List<Track>>> loaderCreator = Observable.from(screen.tracksUris)
                            .map(new Func1<Uri, Observable<List<Track>>>() {
                                @Override
                                public Observable<List<Track>> call(final Uri uri) {
                                    //Use defer for lazy creation
                                    return Observable.defer(new Func0<Observable<List<Track>>>() {
                                        @Override
                                        public Observable<List<Track>> call() {
                                            return TypedBundleableLoader.<Track>create(context)
                                                    .setUri(uri).createObservable();
                                        }
                                    });
                                }
                            });
                    s = Observable.mergeDelayError(loaderCreator, 5)
                            .subscribeOn(Schedulers.computation())
                            .map(new Func1<List<Track>, Integer>() {
                                @Override
                                public Integer call(List<Track> tracks) {
                                    List<Uri> uris = new ArrayList<Uri>(tracks.size());
                                    for (Track track : tracks) {
                                        uris.add(track.getUri());
                                    }
                                    return presenter.getIndexClient().addToPlaylist(item.getUri(), uris);
                                }
                            })
                            .last()
                            .delay(1, TimeUnit.SECONDS)
                            .map(new Func1<Integer, Playlist>() {
                                @Override
                                public Playlist call(Integer integer) {
                                    Playlist playlist = presenter.getIndexClient().getPlaylist(item.getUri());
                                    if (playlist == null) {
                                        playlist = (Playlist) item;
                                    }
                                    return playlist;
                                }
                            })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(subscriber);
                }
                dialogPresenter.showDialog(new DialogFactory() {
                    @Override
                    public Dialog call(Context context) {
                        ProgressDialog pd = new ProgressDialog(context);
                        pd.setIndeterminate(true);
                        pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                RxUtils.unsubscribe(s);
                            }
                        });
                        return pd;
                    }
                });
            }
        };
    }

    @Provides @ScreenScope
    public MenuHandler provideMenuHandler(@Named("loader_uri") final Uri loaderUri, final ActivityResultsController activityResultsController) {
        return new MenuHandlerImpl(loaderUri, activityResultsController) {
            @Override
            public boolean onBuildMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                inflateMenus(menuInflater, menu,
                        R.menu.playlist_sort_by
                );
                return true;
            }

            @Override
            public boolean onMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_sort_by_az:
                        setNewSortOrder(presenter, PlaylistSortOrder.A_Z);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(presenter, PlaylistSortOrder.Z_A);
                        return true;
                    case R.id.menu_sort_by_date_added:
                        setNewSortOrder(presenter, PlaylistSortOrder.LAST_ADDED);
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onBuildActionMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                return false;
            }
        };
    }

    @Provides @Named("fabaction")
    Action1<ViewClickEvent> provideFabClickAction(
            final BundleablePresenter presenter, final DialogPresenter dialogPresenter) {
        return new Action1<ViewClickEvent>() {
            @Override
            @DebugLog
            public void call(ViewClickEvent viewClickEvent) {
                dialogPresenter.showDialog(new DialogFactory() {
                    @Override
                    public Dialog call(Context context) {
                        AlertDialog.Builder b = new AlertDialog.Builder(context)
                                .setTitle(R.string.new_playlist);
                        final EditText editText = new EditText(context);
                        editText.setSingleLine(true);
                        editText.setInputType(editText.getInputType()
                                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                        b.setView(editText)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        presenter.getIndexClient().createPlaylist(editText.getText().toString());
                                        dialog.dismiss();
                                    }
                                });
                        return b.create();
                    }
                });
            }
        };
    }
}