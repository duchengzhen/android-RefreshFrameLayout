package com.github.calv1n.ptrl.demo.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import com.github.calv1n.ptrl.demo.R;

public class SecondActivity extends FragmentActivity {
    private ViewPager mPager;

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, SecondActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_refresh);
        mPager = (ViewPager) findViewById(R.id.vp);

        mPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return RefreshFragment.newInstance();
            }

            @Override
            public int getCount() {
                return 2;
            }
        });
    }
}
