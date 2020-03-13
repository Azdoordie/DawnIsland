package com.yanrou.dawnisland;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drakeet.multitype.MultiTypeAdapter;

import java.util.List;

/**
 * @author suche
 */
public class SeriesContentActivity extends AppCompatActivity implements SeriesContentView {
    private static final String TAG = "SeriesContentActivity";
    RecyclerView recyclerView;
    Toolbar toolbar;
    ActionBar actionBar;

    String id;
    String forumName;

    SwipeRefreshLayout swipeRefreshLayout;

    ReplyDialog replyDialog = null;

    SeriesData seriesData;

    private MultiTypeAdapter multiTypeAdapter;
    private SeriesContentPresenter presenter;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.series_content_menu, menu);
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series_content);

        ReadableTime.initialize(this);

        initView();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Window window = getWindow();
        //这一步要做，因为如果这两个flag没有清除的话下面没有生效
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        //设置布局能够延伸到状态栏(StatusBar)和导航栏(NavigationBar)里面
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        //设置状态栏(StatusBar)颜色透明
        window.setStatusBarColor(getResources().getColor(R.color.colorPrimary));
        //设置导航栏(NavigationBar)颜色透明
        //window.setNavigationBarColor(getResources().getColor(R.color.colorPrimary));

        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        forumName = intent.getStringExtra("forumTextView");
        Log.d(TAG, "onCreate: " + id);
        toolbar.setTitle("A岛 · " + forumName);
        toolbar.setSubtitle(">>No." + id + " · adnmb.com");

        presenter = new SeriesContentPresenter(id, this);
        multiTypeAdapter = new MultiTypeAdapter();

        multiTypeAdapter.register(ContentItem.class, new ContentViewBinder());
        multiTypeAdapter.register(FooterView.class, new SeriesListFooterViewBinder());

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnScrollListener(new SeriesRecyclerOnScrollListener() {
            @Override
            public void onLoadMore() {
                presenter.loadMore();
            }
        });

        recyclerView.setAdapter(multiTypeAdapter);

        swipeRefreshLayout.setOnRefreshListener(presenter::refresh);
        presenter.loadFirstPage();
    }

    void initView() {
        recyclerView = findViewById(R.id.series_content_recycleview);
        toolbar = findViewById(R.id.cotent_toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        swipeRefreshLayout = findViewById(R.id.series_content_swipe_refresh);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            //左上角的箭头按钮的动作
            case android.R.id.home:
                finish();
                break;
            case R.id.new_reply_count:

                break;
            case R.id.delete_all_note:

                break;
            case R.id.jump_page:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("跳页");
                EditText editText = new EditText(this);
                editText.setText("101");
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                builder.setView(editText);
                builder.setPositiveButton("跳页", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int page = Integer.parseInt(editText.getText().toString());
                        presenter.jumpPage(page);
                        dialog.dismiss();
                    }
                });
                builder.create().show();
                break;
            case R.id.reply:
                //防止启动多次
                if (replyDialog == null) {
                    replyDialog = new ReplyDialog();
                    Bundle bundle = new Bundle();
                    bundle.putString("seriesId", id);
                    replyDialog.setArguments(bundle);

                }

                replyDialog.show(getSupportFragmentManager(), "reply");
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setFirstPage(List<Object> items) {
        multiTypeAdapter.setItems(items);
        runOnUiThread(() -> {
            multiTypeAdapter.notifyItemInserted(0);
        });
    }

    @Override
    public void loadMoreSuccess() {
        runOnUiThread(() -> {
            multiTypeAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void refreshSuccess(int itemCount) {
        runOnUiThread(() -> {
            if (itemCount != 0) {
                multiTypeAdapter.notifyItemRangeInserted(0, itemCount);
                recyclerView.smoothScrollToPosition(itemCount - 1);
            }
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    @Override
    public void jumpSuccess() {
        runOnUiThread(() -> {
            multiTypeAdapter.notifyDataSetChanged();
        });
    }
}
