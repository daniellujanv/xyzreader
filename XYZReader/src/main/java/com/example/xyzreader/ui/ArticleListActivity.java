package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.SyncAdapter;
import com.example.xyzreader.remote.RemoteEndpointUtil;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private String CURRENT_ITEM_INDEX = "top_item";
    private String COLLAPSED_INDEX = "collapsed";

    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private Cursor mCursor;
    private CoordinatorLayout mCoordinatorLayout;
    private View mEmptyView;
    private StaggeredGridLayoutManager mStaggeredGridLayoutManager;
    private int mFirstVisibleChild = 0;
    private AppBarLayout mAppBarLayout;
    private boolean mIsAppBarCollapsed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.color_primary_dark
                , R.color.color_primary
                , R.color.accent_color);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });


        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mEmptyView = findViewById(R.id.empty_view_list);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.appbarlayout);
        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (verticalOffset == 0) {
                    mIsAppBarCollapsed = true;
                } else {
                    mIsAppBarCollapsed = false;
                }
            }
        });

        if(savedInstanceState != null){
            mFirstVisibleChild = savedInstanceState.getInt(CURRENT_ITEM_INDEX);
            mIsAppBarCollapsed = savedInstanceState.getBoolean(COLLAPSED_INDEX);
            mAppBarLayout.setExpanded(mIsAppBarCollapsed, true);
            Log.i("listAct", "retrieving poisition " + mFirstVisibleChild);

        }else{
            refresh();
        }

        getSupportLoaderManager().initLoader(0, null, this);
    }

    private void refresh() {
        if(RemoteEndpointUtil.isConnectionAvailable(getApplicationContext())) {
            //Start sync adapter
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    //so its called after onMeasure
                    // --- apparently calling setRefreshing==true does not work
                    //      if called before that
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
            if(!SyncAdapter.requestSyncNow(getApplicationContext(), null)){
                Snackbar
                        .make(mCoordinatorLayout, "Error syncing data!", Snackbar.LENGTH_LONG)
                        .setAction("Try Again!", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                refresh();
                            }
                        })
                        .show();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }else{
            mSwipeRefreshLayout.setRefreshing(false);
            Snackbar.make(mCoordinatorLayout, "No internet connection!", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void startAdapter(Cursor cursor){
        Adapter adapter = new Adapter(cursor);
        mCursor = cursor;
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        mStaggeredGridLayoutManager =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mStaggeredGridLayoutManager);
        mSwipeRefreshLayout.setRefreshing(false);
        mRecyclerView.scrollToPosition(mFirstVisibleChild);

    }

    @Override
    public void onSaveInstanceState(Bundle outBundle) {
        outBundle.putBoolean(COLLAPSED_INDEX, mIsAppBarCollapsed);
        if (mRecyclerView != null && mStaggeredGridLayoutManager != null) {
            int[] position = mStaggeredGridLayoutManager.findFirstCompletelyVisibleItemPositions(null);
            if(position[0] != -1) {
                outBundle.putInt(CURRENT_ITEM_INDEX, position[0]);
            }else{
                //in phone - landscape mode the views are not always completely visible
                position = mStaggeredGridLayoutManager.findFirstVisibleItemPositions(null);
                outBundle.putInt(CURRENT_ITEM_INDEX, position[0]);
            }
//            Log.i("listAct", "saving poisition "+position[0]);
        }
    }
    /**************************************************************/
    /****************** Cursor Loader      ************************/
    /**************************************************************/

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if(cursor.getCount() != 0){
            mEmptyView.setVisibility(View.GONE);
        }else{
            mEmptyView.setVisibility(View.VISIBLE);
        }
        startAdapter(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    /**************************************************************/
    /****************** Adapter            ************************/
    /**************************************************************/

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));

                    Bundle bundle = null;
                    //set shared elements in transition
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

                        View transitionViewPhoto = vh.thumbnailView;
                        transitionViewPhoto.setTransitionName(
                                getString(R.string.transition_photo) + mCursor.getLong(ArticleLoader.Query._ID));
                        View transitionViewText = vh.titleView;
                        transitionViewText.setTransitionName(
                                getString(R.string.transition_text) + mCursor.getLong(ArticleLoader.Query._ID));
                        Pair[] sharedElements = new Pair[2];
                        sharedElements[0] = new Pair<>(transitionViewPhoto, transitionViewPhoto.getTransitionName());
                        sharedElements[1] = new Pair<>(transitionViewText, transitionViewText.getTransitionName());

                        Log.i("Transitions-MainAct", "transition name :: " + transitionViewPhoto.getTransitionName());
                        bundle = ActivityOptionsCompat
                                .makeSceneTransitionAnimation(
                                        ArticleListActivity.this
                                        , sharedElements
                                ).toBundle();
                    }
                    startActivity(intent, bundle);
                }
            });

            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));

            Log.i("FILE", "decoding file :: " + mCursor.getString(ArticleLoader.Query.THUMB_URI));
            String thumbUri = mCursor.getString(ArticleLoader.Query.THUMB_URI);
            if(!thumbUri.equals("error")) {
                holder.thumbnailView.setImageBitmap(
                        BitmapFactory.decodeFile(thumbUri));
            }else{
                holder.thumbnailView.setImageBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.empty_detail));
            }

        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        //        public DynamicHeightNetworkImageView thumbnailView;
        public ImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;
        public CardView cardView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);
//            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            thumbnailView.setAdjustViewBounds(true);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
            cardView = (CardView) view.findViewById(R.id.cardview);
        }
    }
}
