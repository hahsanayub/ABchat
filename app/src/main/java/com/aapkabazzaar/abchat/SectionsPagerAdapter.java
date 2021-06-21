package com.aapkabazzaar.abchat;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class SectionsPagerAdapter extends FragmentPagerAdapter {
    public SectionsPagerAdapter(FragmentManager fm) {
        super(fm);

    }


    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                ChatsFragment chatsFragment = new ChatsFragment();
                return chatsFragment;
            case 1:
                RequestsFragment requestsFragment = new RequestsFragment();
                return requestsFragment;
            case 2:
                FriendsFragment friendsFragment =  new FriendsFragment();
                return friendsFragment;

            default:
                return null;
        }

    }

    @Override
    public int getCount() {
        return 3;
    }

    public  CharSequence getPageTitle(int position) {
        switch (position)
        {
            case 0:
                return "CHATS";
            case 1:
                return "REQUESTS";
            case 2:
                return "FRIENDS";

            default:
                return null;
        }
    }




}

