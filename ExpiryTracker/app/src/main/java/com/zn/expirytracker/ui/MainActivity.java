package com.zn.expirytracker.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.tabs.TabLayout;
import com.stephentuso.welcome.WelcomeHelper;
import com.zn.expirytracker.R;
import com.zn.expirytracker.data.firebase.FirebaseUpdaterHelper;
import com.zn.expirytracker.data.viewmodel.FoodViewModel;
import com.zn.expirytracker.settings.SettingsActivity;
import com.zn.expirytracker.utils.AuthToolbox;
import com.zn.expirytracker.utils.Constants;
import com.zn.expirytracker.utils.DataToolbox;
import com.zn.expirytracker.utils.Toolbox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.viewPager_main)
    ViewPager mViewPager;
    @BindView(R.id.tabLayout_main)
    TabLayout mTabLayout;
    @BindView(R.id.root_main)
    View mRootMain;

    MainPagerAdapter mPagerAdapter;

    WelcomeHelper mWelcomeScreen;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWelcomeScreen.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWelcomeScreen = new WelcomeHelper(this, IntroActivity.class);
//        mWelcomeScreen.forceShow(); // for debugging only
        mWelcomeScreen.show(savedInstanceState);

        setContentView(R.layout.activity_main);
        Timber.tag(MainActivity.class.getSimpleName());
        ButterKnife.bind(this);

        mPagerAdapter = new MainPagerAdapter(getSupportFragmentManager(), Toolbox.isLeftToRightLayout());
        mViewPager.setAdapter(mPagerAdapter);
        // Required for RTL layouts, so AAG is always the first tab
        mViewPager.setCurrentItem(MainPagerAdapter.FRAGMENT_AT_A_GLANCE);
        // This needs to be set so tab layout can be linked with view pager. Despite what the
        // documentation says, this is required to be called, otherwise tab layout will have no tabs
        mTabLayout.setupWithViewPager(mViewPager);

        setupTabs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFS_NAME, MODE_PRIVATE);
        if (AuthToolbox.isSignedIn()) {
            FirebaseUpdaterHelper.setPrefsChildEventListener(this);
            FirebaseUpdaterHelper.listenForPrefsTimestampChanges(true, this);
            FirebaseUpdaterHelper.listenForFoodTimestampChanges(true, this);
            // If we're coming from "guest" state, then upload all currently stored foods into
            // Firebase
            if (sp.getBoolean(Constants.AUTH_GUEST, true)) {
                obtainViewModel(this).upload();
                Toolbox.showSnackbarMessage(mRootMain, getString(R.string.message_welcome_sync));
            }
        }
        // Since MainActivity is the gate into the app after SignInActivity, and after,
        // a user signs out or deletes their account, store auth status here to determine
        // a change in auth mode
        sp.edit().putBoolean(Constants.AUTH_GUEST, !AuthToolbox.isSignedIn()).apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (AuthToolbox.isSignedIn()) {
            FirebaseUpdaterHelper.listenForPrefsTimestampChanges(false, this);
            FirebaseUpdaterHelper.listenForPrefsChanges(false);
            FirebaseUpdaterHelper.listenForFoodTimestampChanges(false, this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                launchSettings();
                return true;
            case R.id.action_share:
                shareApp();
                return true;
            // TODO: Hide for now
//            case R.id.action_search:
//                launchSearch();
//                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() == MainPagerAdapter.FRAGMENT_LIST) {
            // Go back to At A Glance if currently in List
            mViewPager.setCurrentItem(MainPagerAdapter.FRAGMENT_AT_A_GLANCE);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Helper for setting up tab decor
     */
    private void setupTabs() {
        TabLayout.Tab tabAtAGlance = mTabLayout.getTabAt(MainPagerAdapter.FRAGMENT_AT_A_GLANCE);
        if (tabAtAGlance != null) {
            tabAtAGlance.setCustomView(mPagerAdapter.getTabView(
                    MainPagerAdapter.FRAGMENT_AT_A_GLANCE, this));
            Timber.e("MainActivity/At a glance tab was null! Not setting tab elements...");
        }
        TabLayout.Tab tabList = mTabLayout.getTabAt(MainPagerAdapter.FRAGMENT_LIST);
        if (tabList != null) {
            tabList.setCustomView(mPagerAdapter.getTabView(
                    MainPagerAdapter.FRAGMENT_LIST, this));
            mPagerAdapter.setAlpha(tabList, 0.5f);
        } else {
            Timber.e("MainActivity/List tab was null! Not setting tab elements...");
        }

        // Change color of icons by selection
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mPagerAdapter.setAlpha(tab, 1f);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                mPagerAdapter.setAlpha(tab, 0.5f);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void launchSettings() {
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
    }

    private void shareApp() {
        Pair<String, String> emoji = DataToolbox.getRandomAnimalEmojiNamePair();
        Intent shareIntent = new Intent()
                .setAction(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text,
                        emoji.first, emoji.second))
                .setType("text/plain");
        startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share)));
    }

    /**
     * Allows this ViewModel instance to be bound to MainActivity, and allow it to be shared
     * across AAG and List Fragments
     *
     * @param activity
     * @return
     */
    public static FoodViewModel obtainViewModel(FragmentActivity activity) {
        return ViewModelProviders.of(activity).get(FoodViewModel.class);
    }

    private void launchSearch() {
        // TODO: Implement
        Toolbox.showToast(this, "This will launch Search!");
    }
}
