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

package org.opensilk.music.ui3.profile;

import android.content.Context;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewClickEvent;

import org.opensilk.bundleable.Bundleable;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.common.ui.mortar.ToolbarOwner;
import org.opensilk.common.ui.recycler.HeaderRecyclerAdapter;
import org.opensilk.common.ui.recycler.ItemClickSupport;
import org.opensilk.common.ui.util.ViewUtils;
import org.opensilk.music.R;
import org.opensilk.music.index.model.BioSummary;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleableRecyclerAdapter;
import org.opensilk.music.ui3.common.BundleableRecyclerView;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 10/17/15.
 */
public class ProfileView2Land extends CoordinatorLayout implements BundleableRecyclerView {

    @Inject @Named("profile_heros") Boolean wantMultiHeros;
    @Inject @Named("profile_title") String mTitleText;
    @Inject @Named("profile_subtitle") String mSubTitleText;
    @Inject protected BundleablePresenter mPresenter;
    @Inject protected BundleableRecyclerAdapter mAdapter;
    @Inject ToolbarOwner mToolbarOwner;

    @InjectView(R.id.toolbar) Toolbar mToolbar;
    @InjectView(R.id.collapsing_toolbar) CollapsingToolbarLayout mCollapsingToolbar;
    @InjectView(R.id.recyclerview) RecyclerView mList;
    @InjectView(R.id.floating_action_button) View mFab;

    CompositeSubscription mClicks = new CompositeSubscription();

    public ProfileView2Land(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            ProfileComponent component = DaggerService.getDaggerComponent(getContext());
            component.inject(this);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        mCollapsingToolbar.setTitle(mTitleText);
        initRecyclerView();
        subscribeClicks();
        if (!isInEditMode()) {
            mPresenter.takeView(this);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(!isInEditMode()) {
            mToolbarOwner.attachToolbar(mToolbar);
            mToolbarOwner.setConfig(ActionBarConfig.builder().setMenuConfig(mPresenter.getMenuConfig()).build());
            mPresenter.takeView(this);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mClicks.clear();
        mToolbarOwner.detachToolbar(mToolbar);
        mPresenter.dropView(this);
    }

    void subscribeClicks(){
        mClicks.add(RxView.clickEvents(mFab).subscribe(new Action1<ViewClickEvent>() {
            @Override
            public void call(ViewClickEvent viewClickEvent) {
                mPresenter.onFabClicked(viewClickEvent.view());
            }
        }));
    }

    protected void initRecyclerView() {
        getListView().setHasFixedSize(true);
        WrappedHeaderRecyclerAdapter wrapper = new WrappedHeaderRecyclerAdapter(mAdapter);
        int headerlayout = wantMultiHeros ? R.layout.profile_view2_hero_multi : R.layout.profile_view2_hero;
        wrapper.addHeader(ViewUtils.inflate(getContext(), headerlayout, null, false));
        getListView().setAdapter(wrapper);
    }

    public void setupRecyclerView() {
        mAdapter.setGridStyle(mPresenter.isGrid());
        mAdapter.setNumberTracks(mPresenter.wantsNumberedTracks());
        getListView().getRecycledViewPool().clear();
        getListView().setLayoutManager(getLayoutManager());
    }

    public void notifyAdapterResetIncoming() {

    }

    protected RecyclerView.LayoutManager getLayoutManager() {
        if (mPresenter.isGrid()) {
            return makeGridLayoutManager();
        } else {
            return makeListLayoutManager();
        }
    }

    protected RecyclerView.LayoutManager makeGridLayoutManager() {
        final int numCols = getContext().getResources().getInteger(R.integer.grid_columns);
        GridLayoutManager m = new GridLayoutManager(getContext(), numCols, GridLayoutManager.VERTICAL, false);
        m.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 0) {
                    return numCols;
                }
                Bundleable item = mAdapter.getItem(position-1);
                return (item instanceof BioSummary) ? numCols : 1;
            }
        });
        return m;
    }

    protected RecyclerView.LayoutManager makeListLayoutManager() {
        return new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
    }

    public BundleableRecyclerAdapter getAdapter() {
        return mAdapter;
    }

    public BundleablePresenter getPresenter() {
        return mPresenter;
    }

    RecyclerView getListView() {
        return mList;
    }

    @Override
    public void showLoading() {

    }

    @Override
    public void showList(boolean animate) {

    }

    @Override
    public void showEmpty(boolean animate) {

    }

    @Override
    public void setEmptyText(int resId) {

    }

    static class WrappedHeaderRecyclerAdapter extends HeaderRecyclerAdapter<BundleableRecyclerAdapter.ViewHolder>
            implements ItemClickSupport.OnItemLongClickListener, ItemClickSupport.OnItemClickListener {
        public WrappedHeaderRecyclerAdapter(BundleableRecyclerAdapter wrappedAdapter) {
            super(wrappedAdapter);
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            ItemClickSupport.addTo(recyclerView)
                    .setOnItemClickListener(this)
                    .setOnItemLongClickListener(this);
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            ItemClickSupport.removeFrom(recyclerView);
        }

        @Override
        public boolean onItemLongClicked(RecyclerView recyclerView, int position, View v) {
            return position != 0 && ((ItemClickSupport.OnItemLongClickListener) wrappedAdapter)
                    .onItemLongClicked(recyclerView, position - 1, v);
        }

        @Override
        public void onItemClicked(RecyclerView recyclerView, int position, View v) {
            if (position != 0) {
                ((ItemClickSupport.OnItemClickListener)wrappedAdapter)
                        .onItemClicked(recyclerView, position - 1, v);
            }
        }
    }
}
