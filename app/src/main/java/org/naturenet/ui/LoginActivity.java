package org.naturenet.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.naturenet.R;
import org.naturenet.data.model.Site;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_LOGIN = "login";
    public static final String EXTRA_JOIN = "join";
    private DatabaseReference mFirebase;
    private List<String> ids, names;
    private String[] affiliation_ids, affiliation_names;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mFirebase = FirebaseDatabase.getInstance().getReference();
        goToLoginFragment();
    }

    public void continueAsSignedUser() {
        Intent resultIntent = new Intent(this, MainActivity.class);
        setResult(Activity.RESULT_OK, resultIntent);
        resultIntent.putExtra(EXTRA_LOGIN, EXTRA_LOGIN);
        finish();
    }

    public void goToLoginFragment() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment(), LoginFragment.FRAGMENT_TAG)
                .commit();
    }

    /*
        This method is called when user selects join.
     */
    public void goToJoinActivity() {

        //will be used to store the sites
        ids = new ArrayList<>();
        names = new ArrayList<>();

        //retrieve site information that will be used to populate dropdown list of site options when user signs up
        mFirebase.child(Site.NODE_NAME).orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Site site = postSnapshot.getValue(Site.class);
                    ids.add(site.id);
                    names.add(site.name);
                }
                if (ids.size() != 0 && names.size() != 0) {
                    affiliation_ids = ids.toArray(new String[ids.size()]);
                    affiliation_names = names.toArray(new String[names.size()]);
                    //create intent and attach site information
                    Intent joinIntent = new Intent(getApplicationContext(), JoinActivity.class);
                    joinIntent.putExtra(JoinActivity.EXTRA_SITE_IDS, affiliation_ids);
                    joinIntent.putExtra(JoinActivity.EXTRA_SITE_NAMES, affiliation_names);
                    //start JoinActivity
                    startActivity(joinIntent);
                    overridePendingTransition(R.anim.slide_up, R.anim.stay);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.join_error_message_firebase_read) + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}