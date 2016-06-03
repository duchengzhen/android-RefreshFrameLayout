package com.github.calv1n.ptrl.demo.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.github.calv1n.ptrl.PullToRefreshLayout;
import com.github.calv1n.ptrl.demo.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class RefreshFragment extends Fragment {

    private PullToRefreshLayout mPtr;

    public RefreshFragment() {
        // Required empty public constructor
    }

    public static RefreshFragment newInstance() {
        RefreshFragment fragment = new RefreshFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_second, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPtr = (PullToRefreshLayout) view.findViewById(R.id.ptr);
        view.findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPtr.isEnableLoadMore()) {
                    mPtr.enableLoadMore(false);
                    Toast.makeText(getContext(), "关闭LoadMore", Toast.LENGTH_SHORT).show();
                } else {
                    mPtr.enableLoadMore(true);
                    Toast.makeText(getContext(), "开启LoadMore", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mPtr.setOnRefreshLoadListener(new PullToRefreshLayout.OnRefreshLoadListener() {
            @Override
            public void onRefresh() {
                mPtr.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPtr.doneRefresh();
                    }
                }, 8000);
            }

            @Override
            public void onLoad() {
                mPtr.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPtr.doneRefresh();
                    }
                }, 8000);
            }
        });
    }
}
