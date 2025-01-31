package my.project.sakuraproject.main.home.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.google.android.material.button.MaterialButton;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import my.project.sakuraproject.R;
import my.project.sakuraproject.adapter.HomeAdapter;
import my.project.sakuraproject.api.Api;
import my.project.sakuraproject.application.Sakura;
import my.project.sakuraproject.bean.AnimeUpdateInfoBean;
import my.project.sakuraproject.bean.HomeBean;
import my.project.sakuraproject.bean.HomeHeaderBean;
import my.project.sakuraproject.bean.Refresh;
import my.project.sakuraproject.custom.CustomToast;
import my.project.sakuraproject.main.animeList.AnimeListActivity;
import my.project.sakuraproject.main.animeTopic.AnimeTopicActivity;
import my.project.sakuraproject.main.desc.DescActivity;
import my.project.sakuraproject.main.home.HomeContract;
import my.project.sakuraproject.main.home.HomePresenter;
import my.project.sakuraproject.main.tag.MaliTagActivity;
import my.project.sakuraproject.main.tag.TagActivity;
import my.project.sakuraproject.main.updateList.UpdateListActivity;
import my.project.sakuraproject.main.week.WeekActivity;
import my.project.sakuraproject.util.Utils;

public class HomeFragment extends BaseFragment<HomeContract.View, HomePresenter> implements HomeContract.View, HomeAdapter.OnItemClick {
    private View view;
    @BindView(R.id.mSwipe)
    SwipeRefreshLayout mSwipe;
    private List<HomeHeaderBean.HeaderDataBean> headerDataBeans;
    List<MultiItemEntity> multiItemEntities = new ArrayList<>();
    @BindView(R.id.rv_list)
    RecyclerView recyclerView;
    private HomeAdapter adapter;
    private String maliHtml = "";
    @BindView(R.id.ref)
    MaterialButton ref;

    @Override
    protected void setConfigurationChanged() {

    }

    @Override
    protected View initViews(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_home, container, false);
            mUnBinder = ButterKnife.bind(this, view);
        } else {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            }
        }
        initSwipe();
        initAdapter();
        return view;
    }

    public void initSwipe() {
        mSwipe.setColorSchemeResources(R.color.pink500, R.color.blue500, R.color.purple500);
        mSwipe.setOnRefreshListener(() -> {
            loadData();
            multiItemEntities.clear();
            adapter.setNewData(multiItemEntities);
        });
    }

    private void initAdapter() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new HomeAdapter(getActivity(), multiItemEntities, this);
        adapter.openLoadAnimation(BaseQuickAdapter.SCALEIN);
        adapter.setOnItemChildClickListener((adapter, view, position) -> {
            switch (adapter.getItemViewType(position)) {
                case HomeAdapter.TYPE_LEVEL_1:
                    HomeBean homeBean = (HomeBean) adapter.getData().get(position);
                    if (homeBean.getMoreUrl().isEmpty()) return;
                    onMoreClick(homeBean.getTitle(), homeBean.getMoreUrl());
                    break;
            }
        });
        recyclerView.setAdapter(adapter);
        if (Utils.checkHasNavigationBar(getActivity())) recyclerView.setPadding(0,0,0, Utils.getNavigationBarHeight(getActivity()));
    }

    public void onMoreClick(String title, String url) {
        if (url.contains("new.html"))
            openUpdateList(title, url, true);
        else if (url.contains("new"))
            openUpdateList(title, url, false);
        else if (url.contains("list") || url.contains("movie"))
            openAnimeListActivity(Utils.getString(R.string.home_movie_title), Sakura.MOVIE_API, true);
        else
            openTagList(title, url);
    }

    private void openUpdateList(String title, String url, boolean isImomoe) {
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("url", url);
        bundle.putBoolean("isImomoe", isImomoe);
        startActivity(new Intent(getActivity(), UpdateListActivity.class).putExtras(bundle));
    }

    private void openTagList(String title, String url) {
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("url", url);
        startActivity(new Intent(getActivity(), TagActivity.class).putExtras(bundle));
    }

    private void openAnimeListActivity(String title, String url, boolean isMovie) {
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("url", url);
        bundle.putBoolean("isMovie", isMovie);
        bundle.putBoolean("isImomoe", Utils.isImomoe());
        startActivity(new Intent(getActivity(), AnimeListActivity.class).putExtras(bundle));
    }

    @Override
    protected HomePresenter createPresenter() {
        return new HomePresenter(false, "", this);
    }

    @Override
    protected void loadData() {
        if (Utils.isImomoe())
            mPresenter.loadMailiHtmlData();
        else
            mPresenter.loadData(true);
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Refresh refresh) {
        if (refresh.getIndex() == 0) {
            multiItemEntities.clear();
            adapter.setNewData(multiItemEntities);
            mPresenter.loadData(true);
        }
    }

    @Override
    public void onHeaderClick(HomeHeaderBean.HeaderDataBean bean) {
        Bundle bundle = new Bundle();
        switch (bean.getType()) {
            case HomeHeaderBean.TYPE_XFSJB:
                startActivity(new Intent(getActivity(), WeekActivity.class).putExtra("html", maliHtml));
                break;
            case HomeHeaderBean.TYPE_DMFL:
                startActivity(new Intent(getActivity(), TagActivity.class));
                break;
            case HomeHeaderBean.TYPE_DMDY:
                openAnimeListActivity(Utils.getString(R.string.home_movie_title), Sakura.MOVIE_API, true);
                break;
            case HomeHeaderBean.TYPE_DMZT:
                bundle.putString("title", Utils.getString(R.string.home_zt_title));
                bundle.putString("url", Sakura.YHDM_ZT_API);
                startActivity(new Intent(getActivity(), AnimeTopicActivity.class).putExtras(bundle));
                break;
            case HomeHeaderBean.TYPE_JCB:
                openAnimeListActivity(Utils.getString(R.string.home_jcb_title), Sakura.JCB_API, false);
                break;
            //===========================================================
            case HomeHeaderBean.TYPE_DMFL_MALIMALI_TAG:
                bundle.putString("homeParam", Api.MALIMALI_TAG_DEFAULT);
//                bundle.putString("title", "全部类型");
                bundle.putString("title", "全部");
                startActivity(new Intent(getActivity(), MaliTagActivity.class).putExtras(bundle));
                break;
            case HomeHeaderBean.TYPE_DMFL_MALIMALI_JAPAN:
                bundle.putString("homeParam", Api.MALIMALI_JAPAN);
                bundle.putString("title", bean.getTitle());
                startActivity(new Intent(getActivity(), MaliTagActivity.class).putExtras(bundle));
                break;
            case HomeHeaderBean.TYPE_DMFL_MALIMALI_CHINA:
                bundle.putString("homeParam", Api.MALIMALI_CHINA);
                bundle.putString("title", bean.getTitle());
                startActivity(new Intent(getActivity(), MaliTagActivity.class).putExtras(bundle));
                break;
            case HomeHeaderBean.TYPE_DMFL_MALIMALI_EUROPE:
                bundle.putString("homeParam", Api.MALIMALI_EUROPE);
                bundle.putString("title", bean.getTitle());
                startActivity(new Intent(getActivity(), MaliTagActivity.class).putExtras(bundle));
                break;
        }
    }

    @Override
    public void onAnimeClick(HomeBean.HomeItemBean data) {
        Bundle bundle = new Bundle();
        bundle.putString("name", data.getTitle());
        String sakuraUrl = data.getUrl();
        bundle.putString("url", sakuraUrl);
        startActivity(new Intent(getActivity(), DescActivity.class).putExtras(bundle));
    }

    @Override
    public void showLoadingView() {
        mSwipe.setRefreshing(true);
        ref.setVisibility(View.GONE);
        application.error = "";
        application.week = new JSONObject();
    }

    @OnClick(R.id.ref)
    public void refData() {
        ref.setVisibility(View.GONE);
        mSwipe.setRefreshing(true);
        multiItemEntities.clear();
        adapter.setNewData(multiItemEntities);
        loadData();
    }

    @Override
    public void showLoadErrorView(String msg) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            mSwipe.setRefreshing(false);
//            application.showErrorToastMsg(msg);
            CustomToast.showToast(getActivity(), msg, CustomToast.ERROR);
            /*errorTitle.setText(msg);
            adapter.setEmptyView(errorView);*/
            mSwipe.setEnabled(false);
            ref.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void showEmptyVIew() {

    }

    @Override
    public void showLog(String url) {

    }

    @Override
    public void showLoadSuccess(LinkedHashMap map) {

    }

    @Override
    public void showHomeLoadSuccess(List<HomeBean> beans) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            mSwipe.setRefreshing(false);
            multiItemEntities = new ArrayList<>();
            headerDataBeans = new ArrayList<>();
            headerDataBeans.add(new HomeHeaderBean.HeaderDataBean("新番时间表", R.drawable.baseline_calendar_month_white_48dp, HomeHeaderBean.TYPE_XFSJB));
            if (!Utils.isImomoe()) {
                headerDataBeans.add(new HomeHeaderBean.HeaderDataBean("动漫分类", R.drawable.baseline_filter_white_48dp, HomeHeaderBean.TYPE_DMFL));
                headerDataBeans.add(new HomeHeaderBean.HeaderDataBean("动漫电影", R.drawable.baseline_movie_white_48dp, HomeHeaderBean.TYPE_DMDY));
                headerDataBeans.add(new HomeHeaderBean.HeaderDataBean("动漫专题", R.drawable.outline_video_library_white_48dp, HomeHeaderBean.TYPE_DMZT));
                headerDataBeans.add(new HomeHeaderBean.HeaderDataBean("剧场版", R.drawable.ic_ondemand_video_white_48dp, HomeHeaderBean.TYPE_JCB));
            } else {
                headerDataBeans.add(new HomeHeaderBean.HeaderDataBean("动漫分类", R.drawable.baseline_filter_white_48dp, HomeHeaderBean.TYPE_DMFL_MALIMALI_TAG));
                headerDataBeans.add(new HomeHeaderBean.HeaderDataBean("日韩动漫", R.drawable.baseline_movie_white_48dp, HomeHeaderBean.TYPE_DMFL_MALIMALI_JAPAN));
                headerDataBeans.add(new HomeHeaderBean.HeaderDataBean("国产动漫", R.drawable.outline_video_library_white_48dp, HomeHeaderBean.TYPE_DMFL_MALIMALI_CHINA));
                headerDataBeans.add(new HomeHeaderBean.HeaderDataBean("欧美动漫", R.drawable.ic_ondemand_video_white_48dp, HomeHeaderBean.TYPE_DMFL_MALIMALI_EUROPE));
            }
            multiItemEntities.add(new HomeHeaderBean(headerDataBeans));
            for (HomeBean homeBean : beans) {
                multiItemEntities.add(homeBean);
            }
            adapter.setNewData(multiItemEntities);
        });
    }

    @Override
    public void showUpdateInfoSuccess(List<AnimeUpdateInfoBean> beans) {
        application.animeUpdateInfoBeans = beans;
    }

    @Override
    public void showMaliWeekInfoSuccess(String html) {
        maliHtml = html;
        mPresenter = new HomePresenter(false, html, this);
        mPresenter.loadData(true);
    }
}
