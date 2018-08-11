package com.example.xyzreader.ui;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.util.Toolbox;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 * This class is instantiated from intent actions, and is not created directly from within list
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;

    private SharedPreferences mSp;
    private int mCurrentPagerPosition;

    @BindView(R.id.pager)
    ViewPager mPager;
    private DetailPagerAdapter mPagerAdapter;
    @BindView(R.id.appbar_container_detail)
    Toolbar mAppBarContainer;
    @BindView(R.id.gap_status_bar)
    View mStatusBarGap;
    @BindView(R.id.ib_action_up)
    ImageButton mUpButton;
    @BindView(R.id.ib_action_menu)
    ImageButton mOverflowButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);
        ButterKnife.bind(this);
        Timber.tag(ArticleDetailActivity.class.getSimpleName());
        setUpStatusAppBar();

        mSp = getSharedPreferences(ArticleListActivity.SHARED_PREFERENCES, MODE_PRIVATE);

        getSupportLoaderManager().initLoader(0, null, this);

        mPagerAdapter = new DetailPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                // TROD: This allows the up button to fade in and out as the page changes
                // (up button still works even while it's faded out for the time being)
                Toolbox.showView(mUpButton, state == ViewPager.SCROLL_STATE_IDLE, false);

                // Hide the overflow button appropriately, even after user switches pages, by
                // directly accessing the detail fragment, and if the app bar is showing, then hide
                // For accessing the fragment directly, use instantiateItem() on the pager adapter
                // https://stackoverflow.com/questions/10656323/android-fragmentstatepageradapter
                boolean isShowing = ((ArticleDetailFragment) mPagerAdapter.instantiateItem(mPager, mCurrentPagerPosition))
                        .mAppBarShowing;
                Toolbox.showView(mOverflowButton,
                        state == ViewPager.SCROLL_STATE_IDLE && isShowing
                        , true);
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                    mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
                    mCurrentPagerPosition = position;
                    mSp.edit()
                            .putInt(ArticleListActivity.KEY_CURRENT_POSITION, position)
                            .apply();

//                    updateUpButtonPosition();
                } else {
                    Timber.e("Cursor is null when paging");
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }
        });

        mAppBarContainer = findViewById(R.id.appbar_container_detail);

        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // This is required for preventing previous activity from being recreated needlessly
                // Shared elements transition will also not work unless we call this, instead of
//                onSupportNavigateUp();
                onBackPressed();
            }
        });
        mOverflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toolbox.showArticleActionsMenuPopup(ArticleDetailActivity.this, v,
                        mCursor, mCurrentPagerPosition);
            }
        });


        prepareSharedElementTransition();

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
            // Even the shared element is within the fragment, this is still needed to be called
            // in the activity, NOT in the fragment since this activity is created first.
            // Also, calling it here when savedInstanceState is null avoids this from being
            // called on orientation change
            supportPostponeEnterTransition();
        }
    }

    private void prepareSharedElementTransition() {
//        Transition transition = TransitionInflater.from(this)
//                .inflateTransition(R.transition.image_shared_element_transition);
        // TODO: What if this one is not called?
        //setSharedElementEnterTransition(transition);


        setEnterSharedElementCallback(
                new SharedElementCallback() {
                    @Override
                    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                        int position = mSp.getInt(ArticleListActivity.KEY_CURRENT_POSITION, 0);
                        // Find the view for the current fragment
                        Fragment currentFragment = (Fragment) mPagerAdapter
                                .instantiateItem(mPager, position);
                        View view = currentFragment.getView();
                        if (view == null) return;

                        // Map the first shared element name to the child image view
                        sharedElements.put(names.get(0), view.findViewById(R.id.iv_photo_details));
                    }
                });
    }

    /**
     * Helper method for interfacing with ArticleDetailFragment
     */
    public void shareArticle() {
        Toolbox.shareArticle(ArticleDetailActivity.this,
                mCursor, mCurrentPagerPosition);
    }

    /**
     * Helper for making the status bar translucent and setting the action bar layout
     */
    private void setUpStatusAppBar() {
        // Makes the status bar translucent (you can also set the status bar color in this way)
        // Source: https://stackoverflow.com/questions/26702000/change-status-bar-color-with-appcompat-actionbaractivity
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // By doing the above, this means the toolbar will get drawn behind the status bar. The
        // below code fixes that, but it results in no views behind the status bar, so the status
        // bar could be black or white background before the views scroll up behind it
//        mAppBar.setPadding(0, Toolbox.getStatusBarHeight(getResources()), 0, 0);

        // fill-in the empty view space from the translucent status bar
        mStatusBarGap.getLayoutParams().height = Toolbox.getStatusBarHeight(this);
        setSupportActionBar(mAppBarContainer);
        // From https://stackoverflow.com/questions/7655874/how-do-you-remove-the-title-text-from-the-android-actionbar
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    /**
     * Helper function for showing and hiding the action overflow button
     *
     * @param show
     */
    public void showOverflowButton(boolean show) {
        Toolbox.showView(mOverflowButton, show, true);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    // TODO: This method seems to throw errors
//    public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
//        if (itemId == mSelectedItemId) {
//            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
//            updateUpButtonPosition();
//        }
//    }
//
//    private void updateUpButtonPosition() {
//        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
//        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
//    }

    private class DetailPagerAdapter extends FragmentStatePagerAdapter {
        public DetailPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            ArticleDetailFragment fragment = (ArticleDetailFragment) object;
            if (fragment != null) {
//                mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
//                updateUpButtonPosition();
            }
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
        }

        @Override
        public int getCount() {
            // This seems to rely on the cursor for returning number of pages, rather than looking
            // at number of pages
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}