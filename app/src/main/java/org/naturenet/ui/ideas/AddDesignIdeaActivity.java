package org.naturenet.ui.ideas;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.google.common.base.Optional;

import org.naturenet.NatureNetApplication;
import org.naturenet.R;
import org.naturenet.data.model.Users;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class AddDesignIdeaActivity extends AppCompatActivity {

    Disposable mUserAuthSubscription;
    Users signed_user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_design_idea);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(R.string.add_design_idea_title);
        }

        //check to see if there is a signed in user
        mUserAuthSubscription = ((NatureNetApplication) getApplication()).getCurrentUserObservable().subscribe(new Consumer<Optional<Users>>() {
            @Override
            public void accept(Optional<Users> user) throws Exception {
                signed_user = user.isPresent() ? user.get() : null;
            }
        });

        goToAddDesignIdeaFragment();
    }

    private void goToAddDesignIdeaFragment(){
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.add_design_idea_container, new AddDesignIdeaFragment(), AddDesignIdeaFragment.ADD_DESIGN_IDEA_FRAGMENT)
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
