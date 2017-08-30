package org.naturenet.ui.communities;

import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.naturenet.R;
import org.naturenet.data.model.Idea;
import org.naturenet.data.model.Observation;
import org.naturenet.ui.observations.ObservationActivity;
import org.naturenet.ui.observations.ObservationAdapter;


public class UsersDetailFragment extends Fragment {

    public static final String FRAGMENT_TAG = "user_detail_fragment";

    UsersDetailActivity detAct;
    ImageView profilePic;
    TextView username, bio, obsNumber, ideasNumber, noContributions;
    GridView recentObservationsGrid;
    DatabaseReference fbRef;
    long obsCount = 0;
    long ideasCount = 0;
    private ProgressBar queryProgress;


    public UsersDetailFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_users_detail, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        detAct = (UsersDetailActivity) this.getActivity();
        recentObservationsGrid = (GridView) detAct.findViewById(R.id.user_details_recent_obs);
        profilePic = (ImageView) detAct.findViewById(R.id.user_details_avatar);
        obsNumber = (TextView) detAct.findViewById(R.id.user_details_obs_number);
        ideasNumber = (TextView) detAct.findViewById(R.id.user_details_ideas_number);
        username = (TextView) detAct.findViewById(R.id.user_details_name);
        queryProgress = (ProgressBar) detAct.findViewById(R.id.loadingViewUserDetails);
        noContributions =(TextView) detAct.findViewById(R.id.user_no_contributions);
        bio = (TextView) detAct.findViewById(R.id.user_details_bio);

        fbRef = FirebaseDatabase.getInstance().getReference();

        username.setText(detAct.user.displayName);

        //if there is an avatar associated with this user
        if(detAct.user.avatar != null && !detAct.user.avatar.equals("")){
            Picasso.with(detAct).load(detAct.user.avatar).fit().placeholder(R.drawable.default_avatar).into(profilePic);
        }
        //if there is a bio associated with this user
        if(detAct.user.bio != null && !detAct.user.bio.equals("")){
            bio.setText(detAct.user.bio);
        }

        getObservationCount(detAct.user.id, obsNumber);
        getIdeasCount(detAct.user.id, ideasNumber);
        getRecentObservations(detAct.user.id);


    }

    public void getObservationCount(String s, final TextView textView){

        fbRef.child(Observation.NODE_NAME).orderByChild("observer").equalTo(s).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                obsCount = dataSnapshot.getChildrenCount();
                textView.setText(String.valueOf(obsCount));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void getIdeasCount(String s, final TextView textView){
        fbRef.child(Idea.NODE_NAME).orderByChild("submitter").equalTo(s).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ideasCount = dataSnapshot.getChildrenCount();
                textView.setText(String.valueOf(ideasCount));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void getRecentObservations(String s){
        //Set progress bar
        queryProgress.setVisibility(View.VISIBLE);

        Query obsQuery = fbRef.child(Observation.NODE_NAME).orderByChild("observer").equalTo(s).limitToLast(20);

        //Set listener on query so we know if there's data or not
        obsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //if the user has no observations
                if(dataSnapshot.getChildrenCount() == 0)
                    recentObservationsGrid.setEmptyView(noContributions);

                //disable progress bar after query is complete
                queryProgress.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //if an error occurs simply disable progress bar and display the empty view
                recentObservationsGrid.setEmptyView(noContributions);
                queryProgress.setVisibility(View.GONE);
            }
        });

        ObservationAdapter observationAdapter = new ObservationAdapter(detAct, obsQuery);
        recentObservationsGrid.setAdapter(observationAdapter);

        recentObservationsGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent observationIntent = new Intent(getActivity(), ObservationActivity.class);
                observationIntent.putExtra(ObservationActivity.EXTRA_OBSERVATION_ID, ((Observation)view.getTag()).id);
                startActivity(observationIntent);
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
