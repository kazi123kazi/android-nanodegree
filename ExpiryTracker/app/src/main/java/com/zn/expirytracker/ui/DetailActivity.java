package com.zn.expirytracker.ui;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.paging.PagedList;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.zn.expirytracker.R;
import com.zn.expirytracker.data.model.Food;
import com.zn.expirytracker.data.viewmodel.FoodViewModel;
import com.zn.expirytracker.utils.DataToolbox;
import com.zn.expirytracker.utils.Toolbox;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailActivity extends AppCompatActivity {

    public static final String ARG_ITEM_ID_LONG = Toolbox.createStaticKeyString(
            "detail_activity.item_id_long");

    @BindView(R.id.container_detail_activity)
    View mRootview;
    @BindView(R.id.viewPager_detail)
    ViewPager mViewPager;

    private DetailPagerAdapter mPagerAdapter;
    private FoodViewModel mViewModel;
    private List<Food> mFoodsList; // sync'd with pager adapter in onChanged()
    private long mLaunchedItemId;
    private int mCurrentPosition;

    // TODO: Save in savedInstanceState
    private boolean mInitialized; // true once PagerAdapter has been loaded for the first time

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        if (intent != null) {
            mLaunchedItemId = intent.getLongExtra(ARG_ITEM_ID_LONG, 0);
        }

        mViewModel = ViewModelProviders.of(this).get(FoodViewModel.class);
        mViewModel.getAllFoods(false).observe(this, new Observer<PagedList<Food>>() {
            @Override
            public void onChanged(@Nullable PagedList<Food> foods) {
                if (foods != null) {
                    // Don't do anything if foods is null
                    if (!mInitialized) {
                        mPagerAdapter.setFoodsList(foods);
                        mViewPager.setCurrentItem(
                                DataToolbox.getFoodPositionFromId(foods, mLaunchedItemId),
                                false);
                        mInitialized = true;
                    } else {
                        if (foods.size() == 0) {
                            // If there are no more foods, close Details
                            finish();
                        }
                        mPagerAdapter.setFoodsList(foods);
                    }
                    mFoodsList = foods;
                }
            }
        });

        mPagerAdapter = new DetailPagerAdapter(getSupportFragmentManager());
        mViewPager.setClipToPadding(false);
        mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.pager_page_margin));
        mViewPager.setAdapter(mPagerAdapter);

        // Keep track of the user's current page
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mCurrentPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    public void startEditActivity(View view) {
        Intent intent = new Intent(DetailActivity.this, EditActivity.class);
        intent.putExtra(EditFragment.ARG_ITEM_ID_LONG, mFoodsList.get(mViewPager.getCurrentItem()).get_id());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Emulate the back button when pressing the up button, to prevent parent activity from
            // getting recreated
            // https://stackoverflow.com/questions/22947713/make-the-up-button-behave-like-the-back-button-on-android
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_delete:
                deleteItem();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Deletes the food item at the current position of the adapter
     */
    private void deleteItem() {
        Food food = mFoodsList.get(mCurrentPosition);
        mViewModel.delete(true, food.get_id());
        Toolbox.showSnackbarMessage(mRootview, getString(R.string.message_item_removed,
                food.getFoodName()));
    }
}
