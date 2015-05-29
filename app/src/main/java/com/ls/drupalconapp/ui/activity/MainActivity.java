package com.ls.drupalconapp.ui.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.ls.drupalconapp.R;
import com.ls.drupalconapp.app.App;
import com.ls.drupalconapp.model.DatabaseManager;
import com.ls.drupalconapp.model.PreferencesManager;
import com.ls.drupalconapp.model.data.Level;
import com.ls.drupalconapp.model.data.Track;
import com.ls.drupalconapp.ui.adapter.item.EventListItem;
import com.ls.drupalconapp.ui.dialog.FilterDialog;
import com.ls.drupalconapp.ui.drawer.DrawerAdapter;
import com.ls.drupalconapp.ui.drawer.DrawerManager;
import com.ls.drupalconapp.ui.drawer.DrawerMenu;
import com.ls.drupalconapp.ui.drawer.DrawerMenuItem;
import com.ls.utils.AnalyticsManager;
import com.ls.utils.KeyboardUtils;
import com.ls.utils.ScheduleManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends StateActivity implements FilterDialog.OnCheckedPositionsPass {

	private DrawerManager mFrManager;
	private DrawerAdapter mAdapter;
	private List<Long> levelIds = new ArrayList<>();
	private List<Long> trackIds = new ArrayList<>();
	private String mPresentTitle;
	private int mSelectedItem = 0;
	private int mLastSelectedItem = 0;
	private boolean isIntentHandled = false;

	private View mStatusBar;
	private Toolbar mToolbar;
	private DrawerLayout mDrawerLayout;

	public FilterDialog mFilterDialog;
	public boolean mIsDrawerItemClicked;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ac_main);

		initStatusBar();
		initToolbar();
		loadFilter();
		initNavigationDrawer();
		initFilterDialog();

		initFragmentManager();
		if (getIntent().getExtras() != null) {
			isIntentHandled = true;
		}
		handleIntent(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(null);
		if (!isIntentHandled) {
			handleIntent(intent);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		GoogleAnalytics.getInstance(this).reportActivityStart(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		GoogleAnalytics.getInstance(this).reportActivityStop(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		AnalyticsManager.sendEvent(this, "Application", R.string.action_close);
	}

	private void initStatusBar() {
		int currentApiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentApiVersion >= Build.VERSION_CODES.LOLLIPOP) {
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

			mStatusBar = findViewById(R.id.viewStatusBar);
			mStatusBar.setBackgroundColor(getResources().getColor(R.color.title_color));
			mStatusBar.setVisibility(View.VISIBLE);
			findViewById(R.id.viewStatusBarTrans).setVisibility(View.VISIBLE);
		}
	}

	private void initToolbar() {
		mPresentTitle = DrawerMenu.MENU_STRING_ARRAY[0];
		mToolbar = (Toolbar) findViewById(R.id.toolBar);
		if (mToolbar != null) {
			mToolbar.setBackgroundColor(getResources().getColor(R.color.title_color));
			mToolbar.setTitle(mPresentTitle);
			setSupportActionBar(mToolbar);
		}
	}

	private void initNavigationDrawer() {
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
		ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.app_name, R.string.app_name);

		mDrawerLayout.setDrawerListener(drawerToggle);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerLayout.closeDrawers();
		initDrawerListener();
		drawerToggle.syncState();
		initNavigationDrawerList();
	}

	private void initDrawerListener() {
		mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
			@Override
			public void onDrawerSlide(View drawerView, float slideOffset) {
				KeyboardUtils.hideKeyboard(getCurrentFocus());
			}

			@Override
			public void onDrawerOpened(View drawerView) {
			}

			@Override
			public void onDrawerClosed(View drawerView) {
				if (mIsDrawerItemClicked) {
					mIsDrawerItemClicked = false;
					changeFragment();
				}
			}

			@Override
			public void onDrawerStateChanged(int newState) {
			}
		});
	}

	private void initNavigationDrawerList() {
		List<DrawerMenuItem> menu = DrawerMenu.getNavigationDrawerItems();
		mAdapter = new DrawerAdapter(this, menu);
		mAdapter.setDrawerItemClickListener(new DrawerAdapter.OnDrawerItemClickListener() {
			@Override
			public void onDrawerItemClicked(int position) {
				onItemClick(position);
			}
		});

		ListView listMenu = (ListView) findViewById(R.id.leftDrawer);
		listMenu.addHeaderView(
				getLayoutInflater().inflate(R.layout.nav_drawer_header, null),
				null,
				false);
		listMenu.setAdapter(mAdapter);
	}

	private void loadFilter() {
		List<Long> expLevels = PreferencesManager.getInstance().loadExpLevel();
		List<Long> tracks = PreferencesManager.getInstance().loadTracks();

		levelIds.addAll(expLevels);
		trackIds.addAll(tracks);
	}

	public void initFilterDialog() {
		new AsyncTask<Void, Void, List<EventListItem>>() {
			@Override
			protected List<EventListItem> doInBackground(Void... params) {
				DatabaseManager databaseManager = new DatabaseManager(MainActivity.this);
				List<Track> trackList = databaseManager.getTracks();
				List<Level> levelList = databaseManager.getLevels();

				Collections.sort(trackList, new Comparator<Track>() {
					@Override
					public int compare(Track track1, Track track2) {
						String name1 = track1.getName();
						String name2 = track2.getName();
						return name1.compareToIgnoreCase(name2);
					}
				});

				String[] tracks = new String[trackList.size()];
				String[] levels = new String[levelList.size()];

				for (int i = 0; i < trackList.size(); i++) {
					tracks[i] = trackList.get(i).getName();
				}

				for (int i = 0; i < levelList.size(); i++) {
					levels[i] = levelList.get(i).getName();
				}
				mFilterDialog = FilterDialog.newInstance(tracks, levels);
				mFilterDialog.setData(levelList, trackList);
				return null;
			}

			@Override
			protected void onPostExecute(List<EventListItem> eventListItems) {
			}
		}.execute();
	}

	private void handleIntent(Intent intent) {
		if (intent.getExtras() != null) {
			long eventId = intent.getLongExtra(EventDetailsActivity.EXTRA_EVENT_ID, -1);
			long day = intent.getLongExtra(EventDetailsActivity.EXTRA_DAY, -1);
			redirectToDetails(eventId, day);
			isIntentHandled = false;
			new ScheduleManager(this).cancelAlarm(eventId);
		}
	}

	private void redirectToDetails(long id, long day) {
		Intent intent = new Intent(this, EventDetailsActivity.class);
		intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, id);
		intent.putExtra(EventDetailsActivity.EXTRA_DAY, day);
		startActivity(intent);
	}

	private void onItemClick(int position) {
		mDrawerLayout.closeDrawers();
		if (mSelectedItem == position) {
			return;
		}
		mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		mSelectedItem = position;
		mIsDrawerItemClicked = true;
	}

	private void changeFragment() {
		mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

		if (mSelectedItem == DrawerManager.EventMode.About.ordinal()) {
			AboutActivity.startThisActivity(this);
			mSelectedItem = mLastSelectedItem;
		} else {
			DrawerMenuItem item = mAdapter.getItem(mSelectedItem);
			if (!item.isGroup() && mFrManager != null) {
				mFrManager.setFragment(DrawerManager.EventMode.values()[mSelectedItem]);
				mPresentTitle = DrawerMenu.MENU_STRING_ARRAY[mSelectedItem];
				AnalyticsManager.sendEvent(this, mPresentTitle + " screen", R.string.action_open);
				mToolbar.setTitle(mPresentTitle);

				mAdapter.setSelectedPos(mSelectedItem);
				mAdapter.notifyDataSetChanged();

				if (mSelectedItem == DrawerManager.EventMode.Location.ordinal()) {
					if (mStatusBar != null) mStatusBar.setBackgroundColor(getResources().getColor(R.color.location_color));
					mToolbar.setBackgroundColor(getResources().getColor(R.color.location_color));
				} else {
					if (mStatusBar != null) mStatusBar.setBackgroundColor(getResources().getColor(R.color.title_color));
					mToolbar.setBackgroundColor(getResources().getColor(R.color.title_color));
				}
			}
		}
		mLastSelectedItem = mSelectedItem;
	}

	private void initFragmentManager() {
		mFrManager = DrawerManager.getInstance(getSupportFragmentManager(), R.id.mainFragment);
		AnalyticsManager.sendEvent(this, App.getContext().getString(R.string.Schedule) + " screen", R.string.action_open);
		mFrManager.setFragment(DrawerManager.EventMode.Program);
	}

	@Override
	public void onCheckedPositionsPass(List<List<Long>> selectedIds) {
		if (selectedIds == null || selectedIds.size() == 0) {
			return;
		}
		List<Long> levelIds = new ArrayList<>();
		List<Long> trackIds = new ArrayList<>();

		for (int i = 0; i < selectedIds.size(); i++) {
			List<Long> ids = selectedIds.get(i);
			if (i == 0) {
				levelIds.addAll(ids);
			} else if (i == 1) {
				trackIds.addAll(ids);
			}
		}
		this.levelIds = levelIds;
		this.trackIds = trackIds;
		mFrManager.reloadPrograms();
	}

	public List<Long> getLevelIds() {
		return levelIds;
	}

	public List<Long> getTrackIds() {
		return trackIds;
	}
}