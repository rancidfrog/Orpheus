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

package org.opensilk.music.library.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.compare.FolderTrackCompare;
import org.opensilk.music.library.internal.BundleableListTransformer;
import org.opensilk.music.library.internal.BundleableSubscriber;
import org.opensilk.music.library.internal.DeleteSubscriber;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.model.sort.BaseSortOrder;
import org.opensilk.music.model.Container;

import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

import static org.opensilk.music.library.internal.LibraryException.Kind.BAD_BINDER;
import static org.opensilk.music.library.internal.LibraryException.Kind.METHOD_NOT_IMPLEMENTED;

/**
 * Created by drew on 4/26/15.
 */
public abstract class LibraryProvider extends ContentProvider {
    public static final String TAG = LibraryProvider.class.getSimpleName();

    public static final String ACTION_FILTER = "org.opensilk.music.action.LIBRARY_PROVIDER";

    private Scheduler scheduler = Schedulers.computation();

    @Override
    public boolean onCreate() {
        return true;
    }

    /**
     * This providers authority
     */
    protected abstract String getAuthority();

    /**
     * @return This libraries config
     */
    protected abstract LibraryConfig getLibraryConfig();

    /**
     * @return true if library is currently reachable
     */
    protected boolean isAvailable() {
        return true;
    }

    protected void setScheduler(Scheduler scheduler) {
        if (scheduler == null) throw new NullPointerException("Scheduler cannot be null");
        this.scheduler = scheduler;
    }

    protected Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public final Bundle call(String method, String arg, Bundle extras) {

        final LibraryExtras.Builder ok = LibraryExtras.b();
        ok.putOk(true);

        if (method == null) method = "";
        switch (method) {
            case LibraryMethods.LIST:
            case LibraryMethods.GET:
            case LibraryMethods.MULTI_GET:
            case LibraryMethods.SCAN:
            case LibraryMethods.ROOTS: {
                extras.setClassLoader(getClass().getClassLoader());

                final IBinder binder = LibraryExtras.getBundleableObserverBinder(extras);
                if (binder == null || !binder.isBinderAlive()) {
                    //this is mostly for the null, if the binder is dead then
                    //sending them a reason is moot. but we check the binder here
                    //so we dont have to do it 50 times below and we can be pretty
                    //sure the linkToDeath will succeed
                    ok.putOk(false);
                    ok.putCause(new LibraryException(BAD_BINDER, null));
                    return ok.get();
                }

                //Rebuild the extras to remove the binder;
                final Uri uri = LibraryExtras.getUri(extras);
                final String sortOrder = LibraryExtras.getSortOrder(extras);
                final List<Uri> uriList = LibraryExtras.getUriList(extras);
                LibraryExtras.Builder eb = LibraryExtras.b();
                eb.putUri(uri)
                        .putSortOrder(sortOrder != null ? sortOrder : BaseSortOrder.A_Z);
                if (uriList != null) {
                    eb.putUriList(uriList);
                }
                final Bundle args = eb.get();

                switch (method) {
                    case LibraryMethods.LIST: {
                        listObjsInternal(uri, binder, args);
                        break;
                    }
                    case LibraryMethods.GET: {
                        getObjInternal(uri, binder, args);
                        break;
                    }
                    case LibraryMethods.MULTI_GET: {
                        multiGetObjsInternal(uriList, binder, args);
                    }
                    case LibraryMethods.SCAN: {
                        scanObjsInternal(uri, binder, args);
                        break;
                    }
                    case LibraryMethods.ROOTS: {
                        listRootsInternal(uri, binder, args);
                        break;
                    }
                }

                return ok.get();
            }
            case LibraryMethods.DELETE: {
                extras.setClassLoader(getClass().getClassLoader());

                final ResultReceiver resultReceiver = LibraryExtras.getResultReceiver(extras);
                if (resultReceiver == null) {
                    ok.putOk(false).putCause(new LibraryException(BAD_BINDER, null));
                    return ok.get();
                }

                final List<Uri> uris = LibraryExtras.getUriList(extras);
                final DeleteSubscriber subscriber = new DeleteSubscriber(resultReceiver);
                final Bundle args = LibraryExtras.b()
                        .putNotifyUri(LibraryExtras.getNotifyUri(extras))
                        .get();

                deleteObjsInternal(uris, subscriber, args);

                return ok.get();
            }
            case LibraryMethods.UPDATE_ITEM: {
                //TODO
                return ok.get();
            }
            case LibraryMethods.CONFIG: {
                return getLibraryConfig().dematerialize();
            }
            case LibraryMethods.CHECK_AVAILABILITY: {
                return ok.putOk(isAvailable()).get();
            }
            default: {
                return callCustom(method, arg, extras);
            }
        }
    }

    protected Bundle callCustom(String method, String arg, Bundle extras) {
        Log.e(TAG, "Unknown method " + method);
        LibraryExtras.Builder ok = LibraryExtras.b();
        return ok.putOk(false).putCause(new LibraryException(METHOD_NOT_IMPLEMENTED,
                new UnsupportedOperationException(method))).get();
    }

    /*
     * Start internal methods.
     * You can override these if you need specialized handling.
     *
     * You don't need to override these for caching, you can just send the cached list, then hit the network
     * and once it comes in send a notify on the Uri, Orpheus will requery and you can send the updated cached list.
     */

    protected void listObjsInternal(final Uri uri, final IBinder binder, final Bundle args){
        final BundleableSubscriber<Model> subscriber = new BundleableSubscriber<>(binder);
        Observable<Model> o = getListObjsObservable(uri, args)
                .subscribeOn(scheduler);
        o.compose(
                new BundleableListTransformer<Model>(FolderTrackCompare.func(LibraryExtras.getSortOrder(args)))
        ).subscribe(subscriber);
    }

    protected void getObjInternal(final Uri uri, final IBinder binder, final Bundle args){
        final BundleableSubscriber<Model> subscriber = new BundleableSubscriber<>(binder);
        Observable<Model> o = getGetObjObservable(uri, args)
                .subscribeOn(scheduler);
        o.compose(
                new BundleableListTransformer<Model>(null)
        ).subscribe(subscriber);
    }

    protected void multiGetObjsInternal(final List<Uri> uriList, final IBinder binder,final Bundle args) {
        final BundleableSubscriber<Model> subscriber = new BundleableSubscriber<>(binder);
        Observable<Model> o = getMultiGetObjsObservale(uriList, args)
                .subscribeOn(scheduler);
        o.compose(
                new BundleableListTransformer<Model>(new Func2<Model, Model, Integer>() {
                    @Override
                    public Integer call(Model bundleable, Model bundleable2) {
                        int idx1 = uriList.indexOf(bundleable.getUri());
                        int idx2 = uriList.indexOf(bundleable2.getUri());
                        return idx1 - idx2;
                    }
                })
        ).subscribe(subscriber);
    }

    protected void scanObjsInternal(final Uri uri, final IBinder binder, final Bundle args){
        final BundleableSubscriber<Model> subscriber = new BundleableSubscriber<>(binder);
        Observable<Model> o = getScanObjsObservable(uri, args)
                .subscribeOn(scheduler);
        o.compose(
                new BundleableListTransformer<Model>(null)
        ).subscribe(subscriber);
    }

    protected void listRootsInternal(final Uri uri, final IBinder binder, final Bundle args){
        final BundleableSubscriber<Container> subscriber = new BundleableSubscriber<>(binder);
        Observable<Container> o= getListRootsObservable(uri, args)
                .subscribeOn(scheduler);
        o.compose(
                new BundleableListTransformer<Container>(null)
        ).subscribe(subscriber);
    }

    /*
     * Start query stubs
     *
     * You must call subscriber.onComplete after emitting the list
     */

    protected Observable<Model> getListObjsObservable(final Uri uri, final Bundle args) {
        return Observable.create(new Observable.OnSubscribe<Model>() {
            @Override
            public void call(Subscriber<? super Model> subscriber) {
                subscriber.onError(new UnsupportedOperationException());
            }
        });
    }

    protected Observable<Model> getGetObjObservable(final Uri uri, final Bundle args) {
        return Observable.create(new Observable.OnSubscribe<Model>() {
            @Override
            public void call(Subscriber<? super Model> subscriber) {
                subscriber.onError(new UnsupportedOperationException());
            }
        });
    }

    /**
     * Emitted items need not be sorted
     */
    protected Observable<Model> getMultiGetObjsObservale(final List<Uri> uriList, final Bundle args) {
        return Observable.create(new Observable.OnSubscribe<Model>() {
            @Override
            public void call(Subscriber<? super Model> subscriber) {
                subscriber.onError(new UnsupportedOperationException());
            }
        });
    }

    /**
     * By default just does a listing, can be overridden for special handling
     * @return Observable created by {@link #getListObjsObservable(Uri, Bundle)}
     */
    protected Observable<Model> getScanObjsObservable(final Uri uri, final Bundle args) {
        return getListObjsObservable(uri, args);
    }

    protected Observable<Container> getListRootsObservable(final Uri uri, final Bundle args) {
        return Observable.create(new Observable.OnSubscribe<Container>() {
            @Override
            public void call(Subscriber<? super Container> subscriber) {
                subscriber.onError(new UnsupportedOperationException());
            }
        });
    }

    /*
     * End query stubs
     */

    /*
     * Start internal delete methods
     */

    protected void deleteObjsInternal(final List<Uri> uris, final Subscriber<List<Uri>> subscriber, final Bundle args) {
        getDeleteObjsObservable(uris, args)
                .subscribeOn(scheduler)
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        Uri u = LibraryExtras.getNotifyUri(args);
                        if (u != null) {
                            getContext().getContentResolver().notifyChange(u, null);
                        }
                    }
                })
                .subscribe(subscriber);
    }

    /*
     * End internal delete methods
     */

    /*
     * Start delete stubs
     */

    /**
     * Delete the object specified by <code>uri</code>. Emmit all uri's removed by change (ie
     * children of object)
     */
    protected Observable<List<Uri>> getDeleteObjsObservable(final List<Uri> uris, final Bundle args) {
        return Observable.create(new Observable.OnSubscribe<List<Uri>>() {
            @Override
            public void call(Subscriber<? super List<Uri>> subscriber) {
                subscriber.onError(new UnsupportedOperationException());
            }
        });
    }

    /*
     * End delete stubs
     */

    /*
     * Start abstract methods, we are 100% out-of-band and do not support any of these
     */

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    /*
     * End abstract methods
     */

}
