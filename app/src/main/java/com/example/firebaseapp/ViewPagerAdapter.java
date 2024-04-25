package com.example.firebaseapp;

import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {


    public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                return new UploadFragment();
            case 1:
                return new ImageFragment();
            default:
                return new UploadFragment();

        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
//        String title = "";
//        switch(position){
//            case 0:
//                title = "Upload";
//                break;
//            case 1:
//                title = "Image";
//                break;
//        }
//        return title;
        return null;
    }
}
