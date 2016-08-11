package edu.stanford.thingengine.engine.ui;


import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;
import net.hockeyapp.android.metrics.MetricsManager;

import edu.stanford.thingengine.engine.AutoStarter;
import edu.stanford.thingengine.engine.BuildConfig;
import edu.stanford.thingengine.engine.R;

public class MainActivity extends Activity implements ActionBar.TabListener, FragmentEmbedder, ActivityCompat.OnRequestPermissionsResultCallback {
    public static final String LOG_TAG = "thingengine.UI";

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    private final MainServiceConnection engine;

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private final static int SECTION_CHAT = 0;
        private final static int SECTION_MYSTUFF = 1;
        private final static int SECTION_RULES = 2;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case SECTION_CHAT:
                    return AssistantFragment.newInstance();
                case SECTION_MYSTUFF:
                    return MyStuffFragment.newInstance();
                case SECTION_RULES:
                    return RulesFragment.newInstance();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case SECTION_CHAT:
                    return getString(R.string.chat_tab);
                case SECTION_MYSTUFF:
                    return getString(R.string.mystuff_tab);
                case SECTION_RULES:
                    return getString(R.string.myrules_tab);
                default:
                    throw new IllegalArgumentException("Invalid fragment position");
            }
        }
    }

    public MainActivity() {
        engine = new MainServiceConnection();
    }

    @Override
    public MainServiceConnection getEngine() {
        return engine;
    }

    @Override
    public void switchToMyGoods() {
        mViewPager.setCurrentItem(SectionsPagerAdapter.SECTION_MYSTUFF, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        final ActionBar actionBar = getActionBar();
        if (actionBar == null) // silence a warning
            throw new RuntimeException("Action bar is missing");

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

        AutoStarter.startService(this);
        UpdateManager.register(this);
        if (!BuildConfig.DEBUG) {
            MetricsManager.register(this, getApplication());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        engine.start(this);
        CrashManager.register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        engine.stop(this);
        UpdateManager.unregister();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_clear_chat) {
            AssistantFragment fragment = (AssistantFragment) getFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":0");
            fragment.clearHistory();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Fragment fragment = getFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":0");

        if (fragment instanceof ActivityCompat.OnRequestPermissionsResultCallback)
            ((ActivityCompat.OnRequestPermissionsResultCallback)fragment).onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        engine.onActivityResult(requestCode, resultCode, intent);
    }


    private void showConfirmDialog(boolean success) {
        new AlertDialog.Builder(this)
                .setMessage(success ? "Congratulations, you're now all set to use ThingEngine!"
                        : "Sorry, that did not work")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }
}
