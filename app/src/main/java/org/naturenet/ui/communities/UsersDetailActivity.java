package org.naturenet.ui.communities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.naturenet.R;
import org.naturenet.data.model.Users;

public class UsersDetailActivity extends AppCompatActivity {

    Users user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_detail);
        user = getIntent().getParcelableExtra(CommunitiesFragment.USER_EXTRA);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(user.displayName);
        }

        goToUsersFragment();
    }

    public void goToUsersFragment(){
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.user_details_container, new UsersDetailFragment(), UsersDetailFragment.FRAGMENT_TAG)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if(getFragmentManager().getBackStackEntryCount() == 0) {
            finish();
            overridePendingTransition(R.anim.stay, R.anim.slide_down);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.stay, R.anim.slide_down);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
