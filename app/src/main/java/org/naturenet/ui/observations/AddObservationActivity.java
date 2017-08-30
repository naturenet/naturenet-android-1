package org.naturenet.ui.observations;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import org.naturenet.NatureNetApplication;
import org.naturenet.R;
import org.naturenet.UploadService;
import org.naturenet.data.model.Observation;
import org.naturenet.data.model.Project;
import org.naturenet.data.model.Users;

import java.util.ArrayList;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class AddObservationActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_PATH = "observation_path";
    public static final String EXTRA_USER = "signed_user";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_PROJECT = "project";

    ArrayList<Uri> observationPaths;
    Observation newObservation;
    private Disposable mUserAuthSubscription;
    Users signed_user;
    Project p;
    boolean fromCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_observation);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(R.string.add_observation_title);
        }

        //check to see if there is a signed in user
        mUserAuthSubscription = ((NatureNetApplication) getApplication()).getCurrentUserObservable().subscribe(new Consumer<Optional<Users>>() {
            @Override
            public void accept(Optional<Users> user) throws Exception {
                signed_user = user.isPresent() ? user.get() : null;
            }
        });

        newObservation = new Observation();
        fromCamera = getIntent().getBooleanExtra("fromCamera", false);
        double lat = getIntent().getDoubleExtra(EXTRA_LATITUDE, 0.0);
        double lon = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0.0);
        newObservation.location = Lists.newArrayList(lat, lon);
        Users user = getIntent().getParcelableExtra(EXTRA_USER);
        if (user != null) {
            newObservation.userId = user.id;
            newObservation.siteId = user.affiliation;
        }
        observationPaths = getIntent().getParcelableArrayListExtra(EXTRA_IMAGE_PATH);

        if(getIntent().getParcelableExtra(EXTRA_PROJECT) != null){
            p = getIntent().getParcelableExtra(EXTRA_PROJECT);
        }

        getFragmentManager().
                beginTransaction().
                replace(R.id.fragment_container, new AddObservationFragment(), AddObservationFragment.FRAGMENT_TAG).
                commit();
    }

    public void submitObservation() {
        findViewById(R.id.toolbar_send).setVisibility(View.GONE);
        findViewById(R.id.toolbar_busy).setVisibility(View.VISIBLE);

        //here we add the user id and site id to the observation
        if(signed_user!=null){
            newObservation.userId = signed_user.id;
            newObservation.siteId = signed_user.affiliation;
        }

        Intent uploadIntent = new Intent(this, UploadService.class);
        uploadIntent.putExtra(UploadService.EXTRA_OBSERVATION, newObservation);
        uploadIntent.putParcelableArrayListExtra(UploadService.EXTRA_URI_PATH, observationPaths);
        startService(uploadIntent);

        finish();
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