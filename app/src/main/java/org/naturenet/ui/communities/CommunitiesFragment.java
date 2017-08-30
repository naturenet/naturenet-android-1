package org.naturenet.ui.communities;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.naturenet.R;
import org.naturenet.data.model.Users;
import org.naturenet.ui.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class CommunitiesFragment extends Fragment {

    public static final String FRAGMENT_TAG = "communities_fragment";
    public static final String USER_EXTRA = "user";
    public static final String ELSEWHERE = "zz_elsewhere";
    public static final String ACES = "aces";
    public static final String ANACOSTIA = "aws";
    public static final String RCNC = "rcnc";
    private static final int INITIAL_USERS_COUNT = 6;

    private DatabaseReference mFirebase = FirebaseDatabase.getInstance().getReference();
    private ExpandableListView  mCommunitiesListView = null;
    private UsersExpandableAdapter mAdapterOrig;
    private UsersExpandableSearchAdapter mAdapter;
    private EditText searchText;
    private String[] titles =  {ACES, ANACOSTIA, ELSEWHERE, RCNC};
    private boolean activeSearch = false;
    private int numToShowAces, numToShowAws, numToShowRcnc, numToShowElse;
    private ArrayList<Users> rcncList, acesList, elseList, awsList;
    private String search;
    private HashMap<String, List<Users>> userListMap;
    private List<Users> acesResults, awsResults, elseResults, rcncResults;
    private HashMap<String, List<Users>> resultsMap;
    private int acesMax, awsMax, elseMax, rcncMax;
    private boolean isDataLoaded;
    private TextWatcher textWatcher;
    private ArrayList<Users> users;

    MainActivity main;
    TextView toolbar_title;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //initialize view elements
        View root = inflater.inflate(R.layout.fragment_communities, container, false);
        main = (MainActivity) getActivity();
        toolbar_title = (TextView) main.findViewById(R.id.app_bar_main_tv);
        toolbar_title.setText(R.string.communities_title);
        searchText = (EditText) root.findViewById(R.id.searchText);
        mCommunitiesListView = (ExpandableListView) root.findViewById(R.id.communities_listview);

        return root;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //initializations
        acesList = new ArrayList<>();
        rcncList = new ArrayList<>();
        awsList = new ArrayList<>();
        elseList = new ArrayList<>();
        rcncResults = new ArrayList<>();
        awsResults = new ArrayList<>();
        elseResults = new ArrayList<>();
        acesResults = new ArrayList<>();
        userListMap = new HashMap<>();
        numToShowAces = numToShowAws = numToShowElse = numToShowRcnc = INITIAL_USERS_COUNT;
        acesMax = awsMax = elseMax = rcncMax = 0;
        resultsMap = new HashMap<>();
        isDataLoaded = false;
        users = new ArrayList<>();

        //Check to see if we're connected to the Internet.
        if(!isConnectedToInternet())
            Toast.makeText(main, R.string.no_network, Toast.LENGTH_SHORT).show();

        //Get and set user data. If this fails, query again from here.
        if(!getUserData()){

            mFirebase.child(Users.NODE_NAME).orderByChild("display_name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    acesList.clear();
                    awsList.clear();
                    rcncList.clear();
                    elseList.clear();

                    //get all users and store in their respective ArrayList
                    for(DataSnapshot user: dataSnapshot.getChildren()){
                        Users u = user.getValue(Users.class);

                        switch (u.affiliation){
                            case ELSEWHERE: elseList.add(u);
                                break;
                            case ACES: acesList.add(u);
                                break;
                            case ANACOSTIA: awsList.add(u);
                                break;
                            case RCNC: rcncList.add(u);
                                break;
                            default: elseList.add(u);
                        }
                    }

                    acesMax = acesList.size();
                    awsMax = awsList.size();
                    elseMax = elseList.size();
                    rcncMax = rcncList.size();

                    //set the Users in the ExpandableListView
                    setUsers(acesList, awsList, elseList, rcncList, INITIAL_USERS_COUNT, acesMax, awsMax, elseMax, rcncMax);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(main, "There's been an error retrieving the data.", Toast.LENGTH_SHORT).show();
                    isDataLoaded = false;
                }
            });
        }


        /*
            Click listener for the ExpandableListView
         */
        mCommunitiesListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int listPos, int expandedListPos, long l) {
                Intent userIntent = new Intent(main, UsersDetailActivity.class);

                //if the user has entered a search query
                if(activeSearch){
                    userIntent.putExtra(USER_EXTRA, mAdapter.getChild(listPos, expandedListPos));
                    startActivity(userIntent);
                }else{  //the user hasn't entered a search query
                    //determine which section was clicked
                    switch (listPos){
                        case 0:
                            //if show more button has been clicked (last child view), update the user list
                            if(expandedListPos == numToShowAces-1)
                                updateUserList(acesList, listPos);
                            else{
                                userIntent.putExtra(USER_EXTRA, mAdapterOrig.getChild(listPos, expandedListPos));
                                startActivity(userIntent);
                            }
                            break;
                        case 1:
                            if(expandedListPos == numToShowAws-1)
                                updateUserList(awsList, listPos);
                            else{
                                userIntent.putExtra(USER_EXTRA, mAdapterOrig.getChild(listPos, expandedListPos));
                                startActivity(userIntent);
                            }
                            break;
                        case 2:
                            if(expandedListPos == numToShowElse-1)
                                updateUserList(elseList, listPos);
                            else{
                                userIntent.putExtra(USER_EXTRA, mAdapterOrig.getChild(listPos, expandedListPos));
                                startActivity(userIntent);
                            }
                            break;
                        case 3:
                            if(expandedListPos == numToShowRcnc-1)
                                updateUserList(rcncList, listPos);
                            else{
                                userIntent.putExtra(USER_EXTRA, mAdapterOrig.getChild(listPos, expandedListPos));
                                startActivity(userIntent);
                            }
                            break;
                    }
                }

                return false;
            }
        });

    }

    /**
     * This method retrieves user data from the Singleton instance for quick loading. Also, the text watcher is set for user queries.
     * @return - boolean reflecting whether or not the user data was successfully retrieved.
     */
    private boolean getUserData(){

        users = main.userList;

        for(Users u: users){
            switch (u.affiliation){
                case ELSEWHERE: elseList.add(u);
                    break;
                case ACES: acesList.add(u);
                    break;
                case ANACOSTIA: awsList.add(u);
                    break;
                case RCNC: rcncList.add(u);
                    break;
                default: elseList.add(u);
            }
        }

        acesMax = acesList.size();
        awsMax = awsList.size();
        elseMax = elseList.size();
        rcncMax = rcncList.size();

        //Create TextWatcher that will handle any search queries.
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                //make the search query lowercase
                search = editable.toString().toLowerCase();

                //Check to see if data was set in the adapter.
                if(isDataLoaded) {
                    //make sure the search bar isn't empty
                    if (search.length() > 0) {
                        //clear the ArrayLists of results
                        acesResults.clear();
                        elseResults.clear();
                        awsResults.clear();
                        rcncResults.clear();
                        //set activeSearch flag to true
                        activeSearch = true;

                        //iterate over each location arrayList and populate the respective result list with matching results
                        for (int j = 0; j < 4; j++) {
                            switch (j) {
                                case 0:
                                    for (Users user : acesList) {
                                        if (user.displayName.toLowerCase().contains(search))
                                            acesResults.add(user);
                                    }
                                    break;
                                case 1:
                                    for (Users user : awsList) {
                                        if (user.displayName.toLowerCase().contains(search))
                                            awsResults.add(user);
                                    }
                                    break;
                                case 2:
                                    for (Users user : elseList) {
                                        if (user.displayName.toLowerCase().contains(search))
                                            elseResults.add(user);
                                    }
                                    break;
                                case 3:
                                    for (Users user : rcncList) {
                                        if (user.displayName.toLowerCase().contains(search))
                                            rcncResults.add(user);
                                    }
                                    break;

                            }
                        }

                        //set the adapter with updated lists to reflect the search results
                        setUsersSearchResults(acesResults, awsResults, elseResults, rcncResults);
                    } else {
                        //when no text is available, reuse original adapter
                        mCommunitiesListView.setAdapter(mAdapterOrig);
                        activeSearch = false;
                        mCommunitiesListView.expandGroup(0);
                        mCommunitiesListView.expandGroup(1);
                        mCommunitiesListView.expandGroup(2);
                        mCommunitiesListView.expandGroup(3);

                    }
                }
            }
        };

        //Set TextChangedListener
        searchText.addTextChangedListener(textWatcher);

        if(users.size() > 0){
            //set the Users in the ExpandableListView
            setUsers(acesList, awsList, elseList, rcncList, INITIAL_USERS_COUNT, acesMax, awsMax, elseMax, rcncMax);
            isDataLoaded = true;
        }

        return isDataLoaded;

    }

    /**
        This method is called whenever the user clicks on the "Show More" button under a group.
        It accepts the list that will be updated (list) and the position of the group (listPos)
     */
    private void updateUserList(ArrayList<Users> list, int listPos){

        //here we determine which list we will update
        switch (listPos){
            case 0:
                //update the number of users we will show
                numToShowAces += 5;
                //if all the users have been displayed, set the number to the size of the list
                if(numToShowAces > list.size()){
                    numToShowAces = list.size();
                    Toast.makeText(main, R.string.no_more_users, Toast.LENGTH_SHORT).show();
                }
                //replace the ArrayList in the Map with the updated one
                userListMap.put(titles[listPos], list.subList(0, numToShowAces));
                mAdapterOrig = new UsersExpandableAdapter(main, R.layout.communities_row_layout, titles, userListMap, acesMax, awsMax, elseMax, rcncMax);
                mCommunitiesListView.setAdapter(mAdapterOrig);
                mCommunitiesListView.expandGroup(0);
                mCommunitiesListView.setSelectedChild(0, numToShowAces, true);
                break;
            case 1:
                numToShowAws += 5;
                if(numToShowAws > list.size()){
                    numToShowAws = list.size();
                    Toast.makeText(main, R.string.no_more_users, Toast.LENGTH_SHORT).show();
                }
                userListMap.put(titles[listPos], list.subList(0, numToShowAws));
                mAdapterOrig = new UsersExpandableAdapter(main, R.layout.communities_row_layout, titles, userListMap, acesMax, awsMax, elseMax, rcncMax);
                mCommunitiesListView.setAdapter(mAdapterOrig);
                mCommunitiesListView.expandGroup(1);
                mCommunitiesListView.setSelectedChild(1, numToShowAws, true);
                break;
            case 2:
                numToShowElse += 5;
                if(numToShowElse > list.size()){
                    numToShowElse = list.size();
                    Toast.makeText(main, R.string.no_more_users, Toast.LENGTH_SHORT).show();
                }
                userListMap.put(titles[listPos], list.subList(0, numToShowElse));
                mAdapterOrig = new UsersExpandableAdapter(main, R.layout.communities_row_layout, titles, userListMap, acesMax, awsMax, elseMax, rcncMax);
                mCommunitiesListView.setAdapter(mAdapterOrig);
                mCommunitiesListView.expandGroup(2);
                mCommunitiesListView.setSelectedChild(2, numToShowElse, true);
                break;
            case 3:
                numToShowRcnc += 5;
                if(numToShowRcnc > list.size()){
                    numToShowRcnc = list.size();
                    Toast.makeText(main, R.string.no_more_users, Toast.LENGTH_SHORT).show();
                }

                userListMap.put(titles[listPos], list.subList(0, numToShowRcnc));
                mAdapterOrig = new UsersExpandableAdapter(main, R.layout.communities_row_layout, titles, userListMap, acesMax, awsMax, elseMax, rcncMax);
                mCommunitiesListView.setAdapter(mAdapterOrig);
                mCommunitiesListView.expandGroup(3);
                mCommunitiesListView.setSelectedChild(3, numToShowRcnc, true);
                break;
        }

    }

    /**
        This method is called when we first populate the user list.
        It accepts each ArrayList for each site and the number of users to display (numShow).
     */
    private void setUsers(ArrayList<Users> aces, ArrayList<Users> aws, ArrayList<Users> elsewhere, ArrayList<Users> rcnc, int numShow, int aceMax, int awsMax, int elseMax, int rcncMax){

        //populate the HashMap with the user lists for each site
        userListMap.put(titles[0], aces.subList(0,numShow));
        userListMap.put(titles[1], aws.subList(0, numShow));
        userListMap.put(titles[2], elsewhere.subList(0, numShow));
        userListMap.put(titles[3], rcnc.subList(0, numShow));

        mAdapterOrig = new UsersExpandableAdapter(main, R.layout.communities_row_layout, titles, userListMap, aceMax, awsMax, elseMax, rcncMax);
        mCommunitiesListView.setAdapter(mAdapterOrig);
        mCommunitiesListView.expandGroup(0);
        mCommunitiesListView.expandGroup(1);
        mCommunitiesListView.expandGroup(2);
        mCommunitiesListView.expandGroup(3);

        isDataLoaded = true;
        searchText.addTextChangedListener(textWatcher);
    }

    /**
        This method is used to set the search results of queries that the user makes.
        It accepts each ArrayList for each site.
     */
    private void setUsersSearchResults(List<Users> aces, List<Users> aws, List<Users> elsewhere, List<Users> rcnc){
        //populate the HashMap with the lists
        resultsMap.put(titles[0], aces);
        resultsMap.put(titles[1], aws);
        resultsMap.put(titles[2], elsewhere);
        resultsMap.put(titles[3], rcnc);

        //initailize and set the adapter to display the search results
        mAdapter = new UsersExpandableSearchAdapter(main, R.layout.communities_row_layout, titles, resultsMap);
        mCommunitiesListView.setAdapter(mAdapter);
        mCommunitiesListView.expandGroup(0);
        mCommunitiesListView.expandGroup(1);
        mCommunitiesListView.expandGroup(2);
        mCommunitiesListView.expandGroup(3);
    }

    /**
        This method detects if we are connected to the internet.
     */
    private boolean isConnectedToInternet(){
        ConnectivityManager connectivityManager = (ConnectivityManager) main.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

}