package com.github.calv1n.ptrl.demo.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import com.github.calv1n.ptrl.PullToRefreshLayout;
import com.github.calv1n.ptrl.demo.MainBean;
import com.github.calv1n.ptrl.demo.R;
import com.joanzapata.android.BaseAdapterHelper;
import com.joanzapata.android.QuickAdapter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private Button mBtn;
    private ListView mListView;
    private PullToRefreshLayout mPtr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtn = (Button) findViewById(R.id.btn);
        mListView = (ListView) findViewById(R.id.listView);
        mPtr = (PullToRefreshLayout) findViewById(R.id.ptr);

        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this).setTitle("标题")
                        .setMessage("我只是一个对话框")
                        .setNegativeButton("刷新", (new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mPtr.doRefresh();
                            }
                        }))
                        .setPositiveButton("启动", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(SecondActivity.createIntent(MainActivity.this));
                                //mPtr.doLoadMore();
                            }
                        })
                        .show();
            }
        });

        final QuickAdapter<MainBean> adapter =
                new QuickAdapter<MainBean>(this, R.layout.item_main) {
                    @Override
                    protected void convert(BaseAdapterHelper helper, MainBean item) {
                        helper.setText(R.id.tvTitle, item.getTitle());
                        helper.setText(R.id.tvInfo, item.getInfo());
                    }
                };
        mListView.setAdapter(adapter);

        adapter.addAll(getList(null));

        mPtr.setOnRefreshLoadListener(new PullToRefreshLayout.OnRefreshLoadListener() {
            @Override
            public void onRefresh() {
                final List<MainBean> list = getList(true);
                mPtr.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        adapter.replaceAll(list);
                        mPtr.doneRefresh();
                    }
                }, 6000);
            }

            @Override
            public void onLoad() {
                final List<MainBean> list = getList(false);
                mPtr.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        adapter.addAll(list);
                        mPtr.doneRefresh();
                    }
                }, 5000);
            }
        });
    }

    private List<MainBean> getList(Boolean flag) {
        List<MainBean> beans = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            String suf = "";
            if (null != flag) {
                suf = flag ? "刷新.." : "加载..";
            }
            beans.add(new MainBean().setTitle(suf + "标题" + i).setInfo("这里是详情" + i));
        }
        return beans;
    }
}
