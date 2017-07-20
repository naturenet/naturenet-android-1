package org.naturenet.ui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.FloatingActionButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.naturenet.R;
import org.naturenet.data.model.Idea;
import org.naturenet.data.model.Users;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IdeasFragment extends Fragment {

    public static final String FRAGMENT_TAG = "designideas_fragment";
    public static final String IDEA_EXTRA = "idea";

    MainActivity main;
    TextView toolbar_title;
    Idea idea;
    FloatingActionButton addIdeaButton;
    private EditText searchBar;
    private TextWatcher textWatcher;
    private String search;
    private boolean isDataLoaded;

    private DatabaseReference mFirebase = FirebaseDatabase.getInstance().getReference();
    private ListView ideas_list = null;
    private IdeasAdapter mAdapter, mAdapterSearch;
    private ArrayList<Idea> ideas, searchResults;
    private List<Users> users;
    private View showMoreButton;
    private int numToShow;
    private boolean moreIdeasToShow;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ideas, container, false);

        main = ((MainActivity)  this.getActivity());
        toolbar_title = (TextView) main.findViewById(R.id.app_bar_main_tv);
        toolbar_title.setText(R.string.design_ideas_title_design_ideas);
        searchBar = (EditText) root.findViewById(R.id.ideasSearchBar);

        ideas_list = (ListView) root.findViewById(R.id.design_ideas_lv);
        addIdeaButton = (FloatingActionButton) root.findViewById(R.id.fabAddIdea);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(!isConnectedToInternet())
            Toast.makeText(main, R.string.no_network, Toast.LENGTH_SHORT).show();

        isDataLoaded = false;
        numToShow = 25;

        //initialize
        moreIdeasToShow = true;
        ideas = new ArrayList<>();
        searchResults = new ArrayList<>();
        //retrieve users from Singleton instance
        users = FetchData.getInstance().getUsers();

        LayoutInflater inflater = (LayoutInflater) main.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        showMoreButton = inflater.inflate(R.layout.show_more_button, null);

        mFirebase.child(Idea.NODE_NAME).orderByChild("created_at").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                searchBar.getText().clear();

                //clear any ideas that we may have already stored
                ideas.clear();
                //get all the ideas
                for(DataSnapshot idea : dataSnapshot.getChildren()){
                    ideas.add(idea.getValue(Idea.class));
                }
                //reset these variables
                numToShow = 25;
                moreIdeasToShow = true;

                //reverse ideas list to have the most recent first
                Collections.reverse(ideas);
                setIdeas();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        addIdeaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                main.goToAddDesignIdeaActivity();
            }
        });

        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                search = editable.toString().toLowerCase();

                //make sure there is actually ideas loaded
                if(isDataLoaded){
                    if(search.length()>0){
                        searchResults.clear();

                        //iterate over the ideas to see if we match anything
                        for(int i = 0; i<ideas.size(); i++){
                            Idea idea = ideas.get(i);
                            //check to see if the search query matches any content OR usernames
                            if(idea.content.toLowerCase().contains(search) || getUserName(idea.submitter).toLowerCase().contains(search))
                                searchResults.add(ideas.get(i));
                        }

                        //we have all the matches, so show the results and remove "show more" button from footer
                        setSearchResults();
                        ideas_list.removeFooterView(showMoreButton);

                    }else {
                        setIdeas();
                    }
                }

            }
        };

        searchBar.addTextChangedListener(textWatcher);

    }

    /**
     * This function queries our sorted list of users (sorted by id) and returns the corresponding display name.
     * This is done by implementing binary search.
     * @param id - the id of the user whose display name we want.
     * @return - String - the display name of the user whose id we provided.
     */
    private String getUserName(String id){

        String name = "";

        //make sure users didn't return null and size of users list is greater than 0
        if(users != null && users.size() != 0){

            //set left index
            int low = 0;

            //set right index
            int high = users.size() -1;

            //set middle index
            int mid = 0;

            while(low <= high) {
                //id is in users[low..high] or not present
                mid = low + (high - low) / 2;

                if (users.get(mid).id.compareTo(id) > 0)
                    high = mid - 1;
                else if(users.get(mid).id.compareTo(id) < 0)
                    low = mid + 1;
                else {
                    name = users.get(mid).displayName;
                    break;
                }

            }

            return name;
        }

        return name;
    }

    /**
     * This function is called when our query for all the ideas has completed. It sets the adapter and ListView.
     */
    private void setIdeas(){
        mAdapter = new IdeasAdapter(main, R.layout.design_ideas_row_layout, ideas.subList(0, numToShow));
        ideas_list.setAdapter(mAdapter);

        //add show more button at the bottom of the list
        if(ideas_list.getFooterViewsCount() != 1)
            ideas_list.addFooterView(showMoreButton);

        ideas_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if(numToShow == i){
                    showMoreIdeas();
                }else {
                    idea = mAdapter.getItem(i);  //get clicked idea
                    Intent ideaDetailIntent = new Intent(main, IdeaDetailsActivity.class);
                    ideaDetailIntent.putExtra(IDEA_EXTRA, idea);
                    startActivity(ideaDetailIntent);
                }
            }
        });

        isDataLoaded = true;
    }

    /**
     * This function is called when the user selects "Load More" button.
     */
    private void showMoreIdeas(){

        //check to see if there are more ideas
        if(moreIdeasToShow){
            numToShow += 15;

            //check to see if we've reached the end of the ideas
            if(numToShow > ideas.size()){
                numToShow = ideas.size();
                Toast.makeText(main, "No more ideas to show", Toast.LENGTH_SHORT).show();
                moreIdeasToShow = false;
                ideas_list.removeFooterView(showMoreButton);
            }

            final IdeasAdapter updatedAdapter = new IdeasAdapter(main, R.layout.design_ideas_row_layout, ideas.subList(0, numToShow));
            ideas_list.setAdapter(updatedAdapter);
            ideas_list.setSelection(numToShow);

            ideas_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if(numToShow == i)
                        showMoreIdeas();
                    else{
                        idea = updatedAdapter.getItem(i);   //get clicked idea
                        Intent ideaDetailIntent = new Intent(main, IdeaDetailsActivity.class);
                        ideaDetailIntent.putExtra(IDEA_EXTRA, idea);
                        startActivity(ideaDetailIntent);
                    }
                }
            });
        }

    }

    /**
     * This function is called after a user has entered a search query. It simply creates a new adapter for the search results and sets the adapter on the ListView.
     */
    private void setSearchResults(){
        mAdapterSearch = new IdeasAdapter(main, R.layout.design_ideas_row_layout, searchResults);
        ideas_list.setAdapter(mAdapterSearch);

        ideas_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                idea = mAdapterSearch.getItem(i);
                Intent ideaDetailIntent = new Intent(main, IdeaDetailsActivity.class);
                ideaDetailIntent.putExtra(IDEA_EXTRA, idea);
                startActivity(ideaDetailIntent);
            }
        });
    }

    /*
        This method detects if we are connected to the Internet.
     */
    private boolean isConnectedToInternet(){
        ConnectivityManager connectivityManager = (ConnectivityManager) main.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

}