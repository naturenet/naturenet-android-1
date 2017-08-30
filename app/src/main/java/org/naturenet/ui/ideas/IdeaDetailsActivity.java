package org.naturenet.ui.ideas;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.google.common.base.Optional;

import org.naturenet.NatureNetApplication;
import org.naturenet.R;
import org.naturenet.data.model.Idea;
import org.naturenet.data.model.Users;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class IdeaDetailsActivity extends AppCompatActivity {

    Idea idea;
    private Disposable mUserAuthSubscription;
    Users signed_user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idea_details);

        idea = getIntent().getParcelableExtra(IdeasFragment.IDEA_EXTRA);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("Design Idea");
        }

        mUserAuthSubscription = ((NatureNetApplication)getApplication()).getCurrentUserObservable().subscribe(new Consumer<Optional<Users>>() {
            @Override
            public void accept(Optional<Users> user) throws Exception {
                signed_user = user.isPresent() ? user.get() : null;
            }
        });

        goToIdeaDetailsFragment();
    }

    public void goToIdeaDetailsFragment(){
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.idea_details_container, new IdeaDetailsFragment(), IdeaDetailsFragment.IDEA_FRAGMENT_TAG)
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
