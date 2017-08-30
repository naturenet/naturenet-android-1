package org.naturenet.ui.projects;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.naturenet.R;
import org.naturenet.data.model.Project;
import org.naturenet.ui.MainActivity;
import org.naturenet.ui.communities.CommunitiesFragment;
import org.naturenet.util.NatureNetUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;


public class ProjectsFragment extends Fragment {

    public static final String FRAGMENT_TAG = "projects_fragment";
    private static final int NUM_TO_SHOW = 4;
    private static final int PROJECT_ADDED = 1010;

    DatabaseReference dbRef;
    FloatingActionButton addProjectButton;
    MainActivity main;
    private ExpandableListView mProjectsListView = null;
    private String[] titles =  {CommunitiesFragment.ACES, CommunitiesFragment.ANACOSTIA,
            CommunitiesFragment.ELSEWHERE, CommunitiesFragment.RCNC};
    private ArrayList<Project> rcncList, acesList, elseList, awsList;
    private List<Project> acesResults, awsResults, elseResults, rcncResults;
    private HashMap<String, List<Project>> projectsList;
    private String search;
    private EditText searchBox;
    private ProjectsExpandableAdapter  mAdapter;
    private ProjectsExpandableSearchAdapter mAdapterSearch;
    private int numToShowAces, numToShowAws, numToShowElse, numToShowRcnc, totalAces, totalAws, totalElse, totalRcnc;
    private HashMap<String, List<Project>> resultsMap;
    private boolean activeSearch, isExpanded, isDataLoaded;
    private TextWatcher textWatcher;
    private ValueEventListener projectListener;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_projects, container, false);
        main = ((MainActivity) getActivity());
        TextView toolbar_title = (TextView) main.findViewById(R.id.app_bar_main_tv);
        toolbar_title.setText(R.string.projects_title);

        searchBox = (EditText) root.findViewById(R.id.searchProjectText);
        mProjectsListView = (ExpandableListView) root.findViewById(R.id.projects_list);
        addProjectButton = (FloatingActionButton) root.findViewById(R.id.fabAddProject);
        progressBar = (ProgressBar) root.findViewById(R.id.progressBarProjects);
        progressBar.setIndeterminate(true);

        return root;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        dbRef = FirebaseDatabase.getInstance().getReference();
        projectsList = new HashMap<>();

        //initializations
        acesList = new ArrayList<>();
        rcncList = new ArrayList<>();
        awsList = new ArrayList<>();
        elseList = new ArrayList<>();
        rcncResults = new ArrayList<>();
        awsResults = new ArrayList<>();
        elseResults = new ArrayList<>();
        acesResults = new ArrayList<>();
        numToShowAces = numToShowAws = numToShowElse = numToShowRcnc = NUM_TO_SHOW;
        totalAces = totalAws = totalElse = totalRcnc = 0;
        resultsMap = new HashMap<>();
        activeSearch = false;
        isExpanded = false;
        isDataLoaded = false;

        if(!isConnectedToInternet())
            Toast.makeText(main, R.string.no_network, Toast.LENGTH_SHORT).show();

        mProjectsListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int listPos, int childPos, long l) {

                //if there's a search query entered already
                if(activeSearch){
                    main.goToProjectActivity(mAdapterSearch.getChild(listPos, childPos));
                }else{
                    switch (listPos){
                        case 0:
                            if(childPos == numToShowAces-1 && childPos != totalAces-1) {
                                updateProjectList(acesList, listPos);
                                isExpanded = true;
                            }
                            else
                                main.goToProjectActivity(mAdapter.getChild(listPos, childPos));
                            break;
                        case 1:
                            if(childPos == numToShowAws-1 && childPos != totalAws-1) {
                                updateProjectList(awsList, listPos);
                                isExpanded = true;
                            }
                            else
                                main.goToProjectActivity(mAdapter.getChild(listPos, childPos));
                            break;
                        case 2:
                            if(childPos == numToShowElse-1 && childPos != totalElse-1) {
                                updateProjectList(elseList, listPos);
                                isExpanded = true;
                            }
                            else
                                main.goToProjectActivity(mAdapter.getChild(listPos, childPos));
                            break;
                        case 3:
                            if(childPos == numToShowRcnc-1 && childPos != totalRcnc-1) {
                                updateProjectList(rcncList, listPos);
                                isExpanded = true;
                            }
                            else
                                main.goToProjectActivity(mAdapter.getChild(listPos, childPos));
                            break;
                    }
                }


                return false;
            }
        });

        addProjectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addProjectIntent = new Intent(main, AddProjectActivity.class);
                startActivityForResult(addProjectIntent, PROJECT_ADDED);
                getActivity().overridePendingTransition(R.anim.slide_up, R.anim.stay);
            }
        });

        mProjectsListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                if (scrollState == SCROLL_STATE_FLING) {
                    Picasso.with(getActivity()).pauseTag(NatureNetUtils.PICASSO_TAGS.PICASSO_TAG_PROJECT_LIST);
                } else {
                    Picasso.with(getActivity()).resumeTag(NatureNetUtils.PICASSO_TAGS.PICASSO_TAG_PROJECT_LIST);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {}
        });

        createSearchListener();
        projectListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //clear the projectslist prior to checking for projects
                projectsList.clear();
                acesList.clear();
                awsList.clear();
                elseList.clear();
                rcncList.clear();
                totalAces = totalAws = totalElse = totalRcnc = 0;
                numToShowAces = numToShowAws = numToShowElse = numToShowRcnc = NUM_TO_SHOW;

                //get all the projects from the snapshot
                Project project;
                for(DataSnapshot data : dataSnapshot.getChildren()){
                    project = data.getValue(Project.class);

                    if(project.sites!= null && project.sites.size() > 0){
                        Iterator it = project.sites.entrySet().iterator();

                        while(it.hasNext()){
                            Map.Entry pair = (Map.Entry)it.next();

                            if((boolean)pair.getValue()){
                                if(pair.getKey().equals(titles[0]))
                                    acesList.add(project);
                                if(pair.getKey().equals(titles[1]))
                                    awsList.add(project);
                                if(pair.getKey().equals(titles[2]))
                                    elseList.add(project);
                                if(pair.getKey().equals(titles[3]))
                                    rcncList.add(project);

                            }
                        }
                    }else   //if sites is null or empty simply add the project to elsewhere site
                        elseList.add(project);

                }

                //set the total number of projects to these global variables
                totalAces = acesList.size();
                totalAws = awsList.size();
                totalElse = elseList.size();
                totalRcnc = rcncList.size();

                progressBar.setVisibility(View.GONE);
                setProjects(acesList, awsList, elseList, rcncList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(main, "Could not load Projects.", Toast.LENGTH_SHORT).show();
            }
        };
        dbRef.child(Project.NODE_NAME).addValueEventListener(projectListener);
    }

    private void setProjectSearchResults(List<Project> aces, List<Project> aws, List<Project> elsewhere, List<Project> rcnc){

        //populate hashmap with results lists
        resultsMap.put(titles[0], aces);
        resultsMap.put(titles[1], aws);
        resultsMap.put(titles[2], elsewhere);
        resultsMap.put(titles[3], rcnc);

        mAdapterSearch = new ProjectsExpandableSearchAdapter(main, R.layout.project_list_item, titles, resultsMap);
        mProjectsListView.setAdapter(mAdapterSearch);
        mProjectsListView.expandGroup(0);
        mProjectsListView.expandGroup(1);
        mProjectsListView.expandGroup(2);
        mProjectsListView.expandGroup(3);

    }

    private void updateProjectList(ArrayList<Project> list, int listPos){

        switch (listPos){
            case 0:
                //update the number of users we will show
                numToShowAces += 5;
                //if all the users have been displayed, set the number to the size of the list
                if(numToShowAces > list.size()){
                    numToShowAces = list.size();
                    Toast.makeText(main, R.string.no_more_projects, Toast.LENGTH_SHORT).show();
                }
                //replace the ArrayList in the Map with the updated one
                projectsList.put(titles[listPos], list.subList(0, numToShowAces));
                mAdapter = new ProjectsExpandableAdapter(main, R.layout.project_list_item, titles, projectsList, totalAces, totalAws, totalElse, totalRcnc);
                mProjectsListView.setAdapter(mAdapter);
                mProjectsListView.expandGroup(0, true);
                mProjectsListView.setSelectedChild(0, numToShowAces, true);
                break;
            case 1:
                //update the number of users we will show
                numToShowAws += 5;
                //if all the users have been displayed, set the number to the size of the list
                if(numToShowAws > list.size()){
                    numToShowAws = list.size();
                    Toast.makeText(main, R.string.no_more_projects, Toast.LENGTH_SHORT).show();
                }
                //replace the ArrayList in the Map with the updated one
                projectsList.put(titles[listPos], list.subList(0, numToShowAws));
                mAdapter = new ProjectsExpandableAdapter(main, R.layout.project_list_item, titles, projectsList,
                        totalAces, totalAws, totalElse, totalRcnc);
                mProjectsListView.setAdapter(mAdapter);
                mProjectsListView.expandGroup(1, true);
                mProjectsListView.setSelectedChild(1, numToShowAws, true);
                break;
            case 2:
                //update the number of users we will show
                numToShowElse += 5;
                //if all the users have been displayed, set the number to the size of the list
                if(numToShowElse > list.size()){
                    numToShowElse = list.size();
                    Toast.makeText(main, R.string.no_more_projects, Toast.LENGTH_SHORT).show();
                }
                //replace the ArrayList in the Map with the updated one
                projectsList.put(titles[listPos], list.subList(0, numToShowElse));
                mAdapter = new ProjectsExpandableAdapter(main, R.layout.project_list_item, titles, projectsList,
                        totalAces, totalAws, totalElse, totalRcnc);
                mProjectsListView.setAdapter(mAdapter);
                mProjectsListView.expandGroup(2, true);
                mProjectsListView.setSelectedChild(2, numToShowElse, true);
                break;
            case 3:
                //update the number of users we will show
                numToShowRcnc += 5;
                //if all the users have been displayed, set the number to the size of the list
                if(numToShowRcnc > list.size()){
                    numToShowRcnc = list.size();
                    Toast.makeText(main, R.string.no_more_projects, Toast.LENGTH_SHORT).show();
                }
                //replace the ArrayList in the Map with the updated one
                projectsList.put(titles[listPos], list.subList(0, numToShowRcnc));
                mAdapter = new ProjectsExpandableAdapter(main, R.layout.project_list_item, titles, projectsList,
                        totalAces, totalAws, totalElse, totalRcnc);
                mProjectsListView.setAdapter(mAdapter);
                mProjectsListView.expandGroup(3, true);
                mProjectsListView.setSelectedChild(3, numToShowRcnc, true);
                break;
        }
    }

    /*
        This method will be called when we've retrieved all the Projects.
     */
    public void setProjects(ArrayList<Project> aces, ArrayList<Project> aws, ArrayList<Project> elsewhere, ArrayList<Project> rcnc) {

        if(aces.size()>NUM_TO_SHOW)
            projectsList.put(titles[0], aces.subList(0, NUM_TO_SHOW));
        else
            projectsList.put(titles[0], aces);
        if(aws.size()>NUM_TO_SHOW)
            projectsList.put(titles[1], aws.subList(0, NUM_TO_SHOW));
        else
            projectsList.put(titles[1], aws);
        if(elsewhere.size()>NUM_TO_SHOW)
            projectsList.put(titles[2], elsewhere.subList(0, NUM_TO_SHOW));
        else
            projectsList.put(titles[2], elsewhere);
        if(rcnc.size()>NUM_TO_SHOW)
            projectsList.put(titles[3], rcnc.subList(0, NUM_TO_SHOW));
        else
            projectsList.put(titles[3], rcnc);


        mAdapter = new ProjectsExpandableAdapter(main, R.layout.project_list_item, titles, projectsList, totalAces, totalAws,
                totalElse, totalRcnc);
        mProjectsListView.setAdapter(mAdapter);

        mProjectsListView.expandGroup(0);
        mProjectsListView.expandGroup(1);
        mProjectsListView.expandGroup(2);
        mProjectsListView.expandGroup(3);

        //If we get this far, we know we actually pulled data from Firebase. So, we set our variable to true and add a TextChangedListener to our EditText.
        isDataLoaded = true;
        searchBox.addTextChangedListener(textWatcher);
        searchBox.getText().clear();
    }

    private void createSearchListener(){
        //Create TextWatcher to listen for queries.
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

                //make sure we actually have data loaded into the adapter
                if(isDataLoaded) {
                    //make sure the search bar isn't empty
                    if (search.length() > 0) {
                        //clear the arraylist of results
                        acesResults.clear();
                        awsResults.clear();
                        elseResults.clear();
                        rcncResults.clear();

                        activeSearch = true;

                        //iterate over each list of projects and populate the results lists
                        for (int j = 0; j < 4; j++) {
                            switch (j) {
                                case 0:
                                    for (Project p : acesList) {
                                        if (p.name.toLowerCase().contains(search) || p.description.toLowerCase().contains(search))
                                            acesResults.add(p);
                                    }
                                    break;
                                case 1:
                                    for (Project p : awsList) {
                                        if (p.name.toLowerCase().contains(search) || p.description.toLowerCase().contains(search))
                                            awsResults.add(p);
                                    }
                                    break;
                                case 2:
                                    for (Project p : elseList) {
                                        if (p.name.toLowerCase().contains(search) || p.description.toLowerCase().contains(search))
                                            elseResults.add(p);
                                    }
                                    break;
                                case 3:
                                    for (Project p : rcncList) {
                                        if (p.name.toLowerCase().contains(search) || p.description.toLowerCase().contains(search))
                                            rcncResults.add(p);
                                    }
                                    break;
                            }
                        }

                        //set the search results
                        setProjectSearchResults(acesResults, awsResults, elseResults, rcncResults);
                    } else {
                        //when no text is available, reuse original adapter
                        mProjectsListView.setAdapter(mAdapter);
                        activeSearch = false;
                        mProjectsListView.expandGroup(0);
                        mProjectsListView.expandGroup(1);
                        mProjectsListView.expandGroup(2);
                        mProjectsListView.expandGroup(3);

                    }
                    //If data was never set in the adapter, display a message explaining this. Also, remove "this" TextChangedListener so we don't keep displaying
                    //after every character that's entered
                }else{
                    Toast.makeText(main, "Project data may not have been loaded. Make sure you are connected to the Internet and try again.", Toast.LENGTH_LONG).show();
                    searchBox.removeTextChangedListener(this);
                }
            }
        };

    }

    @Override
    public void onDestroyView() {
        dbRef.child(Project.NODE_NAME).removeEventListener(projectListener);
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PROJECT_ADDED){

            //if the project was succesfully added, we refresh screen
            if(resultCode == RESULT_OK){
                //clear the search if there was anything
                searchBox.getText().clear();
            }
        }
    }

    /*
        This method detects if we are connected to the internet.
     */
    private boolean isConnectedToInternet(){
        ConnectivityManager connectivityManager = (ConnectivityManager) main.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
