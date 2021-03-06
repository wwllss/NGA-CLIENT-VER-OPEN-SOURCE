package sp.phone.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.internal.NavigationMenuView;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.alibaba.android.arouter.launcher.ARouter;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;
import java.util.List;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.activity.ForumListActivity;
import gov.anzong.androidnga.arouter.ARouterConstants;
import gov.anzong.androidnga.util.GlideApp;
import sp.phone.adapter.BoardPagerAdapter;
import sp.phone.common.PreferenceKey;
import sp.phone.common.User;
import sp.phone.common.UserManager;
import sp.phone.common.UserManagerImpl;
import sp.phone.fragment.dialog.AddBoardDialogFragment;
import sp.phone.interfaces.PageCategoryOwner;
import sp.phone.mvp.contract.BoardContract;
import sp.phone.theme.ThemeManager;
import sp.phone.util.ActivityUtils;
import sp.phone.util.ImageUtils;


/**
 * 首页的容器
 * Created by Justwen on 2017/6/29.
 */

public class BoardFragment extends BaseFragment implements BoardContract.View, AdapterView.OnItemClickListener {

    private BoardContract.Presenter mPresenter;

    private ViewPager mViewPager;

    private ViewFlipper mHeaderView;

    private TextView mReplyCountView;

    private BoardPagerAdapter mBoardPagerAdapter;

    private Drawable mDefaultAvatar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setTitle(R.string.start_title);
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_board, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mViewPager = view.findViewById(R.id.pager);
        TabLayout tabLayout = view.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        DrawerLayout drawerLayout = view.findViewById(R.id.drawer_layout);
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                setTitle("赞美片总");
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                setTitle(R.string.start_title);
                super.onDrawerClosed(drawerView);
            }
        });

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(getActivity(), drawerLayout, toolbar, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        NavigationView navigationView = view.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this::onOptionsItemSelected);
        NavigationMenuView menuView = (NavigationMenuView) navigationView.getChildAt(0);
        menuView.setVerticalScrollBarEnabled(false);
        MenuItem menuItem = navigationView.getMenu().findItem(R.id.menu_gun);
        View actionView = getLayoutInflater().inflate(R.layout.nav_menu_action_view_gun, null);
        menuItem.setActionView(actionView);
        menuItem.expandActionView();
        mReplyCountView = actionView.findViewById(R.id.reply_count);
        navigationView.getHeaderView(0).setBackgroundColor(ThemeManager.getInstance().getPrimaryColor(getContext()));
        mHeaderView = navigationView.getHeaderView(0).findViewById(R.id.viewFlipper);
        updateHeaderView();
        super.onViewCreated(view, savedInstanceState);
        mPresenter.loadBoardInfo();
    }

    private void setReplyCount(int count) {
        mReplyCountView.setText(String.valueOf(count));
    }

    @Override
    public void updateHeaderView() {
        mHeaderView.removeAllViews();
        UserManager um = UserManagerImpl.getInstance();
        final List<User> userList = um.getUserList();
        if (userList.isEmpty()) {
            mHeaderView.addView(getUserView(null, 0));
        } else {
            for (int i = 0; i < userList.size(); i++) {
                mHeaderView.addView(getUserView(userList, i));
            }
            mHeaderView.setDisplayedChild(um.getActiveUserIndex());
        }
        mHeaderView.setOnClickListener(v -> mPresenter.toggleUser(userList));
        mHeaderView.setInAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.right_in));
        mHeaderView.setOutAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.right_out));
    }

    @Override
    public void notifyDataSetChanged() {
        mBoardPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public int getCurrentItem() {
        return mViewPager.getCurrentItem();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                gotoForumList();
                break;
            case R.id.menu_add_id:
                showAddBoardDialog();
                break;
            case R.id.menu_login:
                jumpToLogin();
                break;
            case R.id.menu_clear_recent:
                mPresenter.clearRecentBoards();
                break;
            default:
                return getActivity().onOptionsItemSelected(item);
        }
        return true;
    }

    private void gotoForumList() {
        Intent intent = new Intent(getActivity(), ForumListActivity.class);
        startActivity(intent);
    }

    public boolean isTablet() {
        boolean xlarge = ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 0x04);// Configuration.SCREENLAYOUT_SIZE_XLARGE);
        boolean large = ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
        return xlarge || large;
    }

    @Override
    public void jumpToLogin() {
//        if (isTablet()) {
//            DialogFragment df = new LoginDialogFragment();
//            df.show(getSupportFragmentManager(), "login");
//        } else {
//            Intent intent = new Intent();
//            intent.setClass(getContext(), LoginActivity.class);
//            startActivityForResult(intent, ActivityUtils.REQUEST_CODE_LOGIN);
//        }
        ARouter.getInstance().build(ARouterConstants.ACTIVITY_LOGIN).navigation();
    }

    private void showAddBoardDialog() {
        new AddBoardDialogFragment().show(getChildFragmentManager());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ActivityUtils.REQUEST_CODE_LOGIN && resultCode == Activity.RESULT_OK) {
            updateHeaderView();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public View getUserView(List<User> userList, int position) {
        View privateView = getLayoutInflater().inflate(R.layout.nav_header_view_login_user, null);
        TextView loginState = privateView.findViewById(R.id.loginstate);
        TextView loginId = privateView.findViewById(R.id.loginnameandid);
        ImageView avatarImage = privateView.findViewById(R.id.avatarImage);
        ImageView nextImage = privateView.findViewById(R.id.nextImage);
        if (userList == null) {
            loginState.setText("未登录");
            loginId.setText("点击下面的登录账号登录");
            nextImage.setVisibility(View.GONE);
        } else {
            if (userList.size() <= 1) {
                nextImage.setVisibility(View.GONE);
            }
            if (userList.size() == 1) {
                loginState.setText("已登录1个账户");
            } else {
                loginState.setText(String.format("已登录%s", String.valueOf(userList.size() + "个账户,点击切换")));
            }
            if (userList.size() > 0) {
                User user = userList.get(position);
                loginId.setText(String.format("当前:%s(%s)", user.getNickName(), user.getUserId()));
                handleUserAvatar(avatarImage, user.getAvatarUrl());
            }
        }
        return privateView;
    }

    public void handleUserAvatar(ImageView avatarIV, String url) {
        if (mDefaultAvatar == null) {
            Bitmap defaultAvatar = BitmapFactory.decodeResource(getResources(), R.drawable.default_avatar);
            mDefaultAvatar = new BitmapDrawable(getResources(), ImageUtils.toRoundCorner(defaultAvatar, 2));
        }

        avatarIV.setImageTintList(null);
        GlideApp.with(this)
                .load(url)
                .placeholder(mDefaultAvatar)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .transforms(new BitmapTransformation() {
                    @Override
                    protected Bitmap transform(@NonNull BitmapPool bitmapPool, @NonNull Bitmap bitmap, int i, int i1) {
                        return ImageUtils.toRoundCorner(bitmap, 2);
                    }

                    @Override
                    public void updateDiskCacheKey(MessageDigest messageDigest) {

                    }
                })
                .into(avatarIV);
    }

    @Override
    public void onResume() {
        if (mBoardPagerAdapter == null) {
            mBoardPagerAdapter = new BoardPagerAdapter(getChildFragmentManager(), (PageCategoryOwner) mPresenter);
            mViewPager.setAdapter(mBoardPagerAdapter);
            if (((PageCategoryOwner) mPresenter).getCategory(0).size() == 0) {
                mViewPager.setCurrentItem(1);
            }
        } else {
            mBoardPagerAdapter.notifyDataSetChanged();
        }
        setReplyCount(PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(PreferenceKey.KEY_REPLY_COUNT,0));
        super.onResume();
    }

    @Override
    public void setPresenter(BoardContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public int switchToNextUser() {
        mHeaderView.showPrevious();
        return mHeaderView.getDisplayedChild();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String fidString;
        if (parent != null) {
            fidString = (String) parent.getItemAtPosition(position);
        } else {
            fidString = String.valueOf(id);
        }

        mPresenter.toTopicListPage(position, fidString);
    }
}
