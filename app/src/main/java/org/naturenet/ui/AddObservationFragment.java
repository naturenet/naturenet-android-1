package org.naturenet.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterViewFlipper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.naturenet.R;
import org.naturenet.data.model.PhotoCaptionContent;
import org.naturenet.data.model.Project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class AddObservationFragment extends Fragment {

    public static final String FRAGMENT_TAG = "add_observation_fragment";
    private static final String DEFAULT_PROJECT_ID = "-ACES_a38";
    private static final int NUM_TO_SHOW = 4;




    private TextView send, project;
    private EditText description, whereIsIt, searchBox;
    private AdapterViewFlipper imageFlipper;
    private Button choose;
    private ExpandableListView mProjectsListView;
    private TextView noProjects;
    private LinearLayout add_observation_ll;
    private View projectsLayout;
    private AddObservationActivity add;
    private String selectedProjectId;
    private DatabaseReference dbRef;
    private String[] titles =  {CommunitiesFragment.ACES, CommunitiesFragment.ANACOSTIA,
            CommunitiesFragment.ELSEWHERE, CommunitiesFragment.RCNC};
    private ArrayList<Project> rcncList, acesList, elseList, awsList;
    private List<Project> acesResults, awsResults, elseResults, rcncResults;
    private HashMap<String, List<Project>> projectsList;
    private String search;
    private ProjectsExpandableAdapter mAdapter;
    private ProjectsExpandableSearchAdapter mAdapterSearch;
    private int numToShowAces, numToShowAws, numToShowElse, numToShowRcnc, totalAces, totalAws, totalElse, totalRcnc;
    private HashMap<String, List<Project>> resultsMap;
    private boolean activeSearch;
    private Project p;
    private GestureDetector gestureDetector;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_observation, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        add = ((AddObservationActivity) getActivity());
        dbRef = FirebaseDatabase.getInstance().getReference();

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
        projectsList = new HashMap<>();
        resultsMap = new HashMap<>();
        activeSearch = false;
        p = add.p;

        //check to see if the user is submitting to a specific project
        if(p != null){
            project.setText(p.name);
            selectedProjectId = p.id;
        }else{  //if not, set project as default
            selectedProjectId = DEFAULT_PROJECT_ID;

            FirebaseDatabase.getInstance().getReference(Project.NODE_NAME).child(DEFAULT_PROJECT_ID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Project p = dataSnapshot.getValue(Project.class);
                    project.setText(String.format(getString(R.string.add_observation_default_project), p.name));
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Timber.e(databaseError.toException(), "Could not read default project");
                }
            });
        }

        /*
            Populate the list of Projects.
         */
        dbRef.child(Project.NODE_NAME).addListenerForSingleValueEvent(new ValueEventListener() {
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

                setProjects(acesList, awsList, elseList, rcncList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        send = (TextView) getActivity().findViewById(R.id.toolbar_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (add.signed_user!=null) {
                    send.setVisibility(View.GONE);


                    PhotoCaptionContent data = new PhotoCaptionContent();
                    data.text = description.getText().toString();
                    String where = whereIsIt.getText().toString().trim();

                    if (!where.isEmpty()) {
                        add.newObservation.where = where;
                    }

                    add.newObservation.data = data;
                    add.newObservation.projectId = selectedProjectId;
                    add.submitObservation();


                } else {
                    Toast.makeText(getActivity(), "Please sign in to contribute to NatureNet", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                    startActivityForResult(loginIntent, 2);
                }
            }
        });


        mProjectsListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int listPos, int childPos, long l) {

                //if there's a search query entered, we have to get the project from a different adapter (adapterSearch)
                if(activeSearch){
                    p = mAdapterSearch.getChild(listPos, childPos);
                    project.setText(p.name);
                    selectedProjectId = p.id;
                    add_observation_ll.setVisibility(View.VISIBLE);
                    projectsLayout.setVisibility(View.GONE);
                }else{
                    switch (listPos){
                        case 0:
                            if(childPos == numToShowAces-1 && childPos != totalAces-1) {
                                updateProjectList(acesList, listPos);
                            }
                            else{
                                p = mAdapter.getChild(listPos, childPos);
                                project.setText(p.name);
                                selectedProjectId = p.id;
                                add_observation_ll.setVisibility(View.VISIBLE);
                                projectsLayout.setVisibility(View.GONE);
                            }
                            break;
                        case 1:
                            if(childPos == numToShowAws-1 && childPos != totalAws-1) {
                                updateProjectList(awsList, listPos);
                            }
                            else{
                                p = mAdapter.getChild(listPos, childPos);
                                project.setText(p.name);
                                selectedProjectId = p.id;
                                add_observation_ll.setVisibility(View.VISIBLE);
                                projectsLayout.setVisibility(View.GONE);
                            }
                            break;
                        case 2:
                            if(childPos == numToShowElse-1 && childPos != totalElse-1) {
                                updateProjectList(elseList, listPos);
                            }
                            else{
                                p = mAdapter.getChild(listPos, childPos);
                                project.setText(p.name);
                                selectedProjectId = p.id;
                                add_observation_ll.setVisibility(View.VISIBLE);
                                projectsLayout.setVisibility(View.GONE);
                            }
                            break;
                        case 3:
                            if(childPos == numToShowRcnc-1 && childPos != totalRcnc-1) {
                                updateProjectList(rcncList, listPos);
                            }
                            else{
                                p = mAdapter.getChild(listPos, childPos);
                                project.setText(p.name);
                                selectedProjectId = p.id;
                                add_observation_ll.setVisibility(View.VISIBLE);
                                projectsLayout.setVisibility(View.GONE);
                            }
                            break;
                    }

                }

                return false;
            }
        });

        /*
            Listening to any changes in the search bar
         */
        searchBox.addTextChangedListener(new TextWatcher() {
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

                //make sure the search bar isn't empty
                if(search.length() > 0){
                    //clear the arraylist of results
                    acesResults.clear();
                    awsResults.clear();
                    elseResults.clear();
                    rcncResults.clear();

                    activeSearch = true;

                    //iterate over each list of projects and populate the results lists
                    for(int j = 0; j<4; j++){
                        switch (j){
                            case 0:
                                for(Project p: acesList){
                                    if(p.name.toLowerCase().contains(search) || p.description.toLowerCase().contains(search))
                                        acesResults.add(p);
                                }
                                break;
                            case 1:
                                for(Project p: awsList){
                                    if(p.name.toLowerCase().contains(search) || p.description.toLowerCase().contains(search))
                                        awsResults.add(p);
                                }
                                break;
                            case 2:
                                for(Project p: elseList){
                                    if(p.name.toLowerCase().contains(search) || p.description.toLowerCase().contains(search))
                                        elseResults.add(p);
                                }
                                break;
                            case 3:
                                for(Project p: rcncList){
                                    if(p.name.toLowerCase().contains(search) || p.description.toLowerCase().contains(search))
                                        rcncResults.add(p);
                                }
                                break;
                        }
                    }

                    //set the search results
                    setProjectSearchResults(acesResults, awsResults, elseResults, rcncResults);
                }else{
                    //when no text is available, reuse original adapter
                    mProjectsListView.setAdapter(mAdapter);
                    activeSearch = false;
                }
            }
        });

        //create adapter with images the user wants to submit
        AddObservationImageAdapter adapter = new AddObservationImageAdapter(add, add.observationPaths);

        imageFlipper.setAdapter(adapter);
        imageFlipper.setFlipInterval(4*1000);
        imageFlipper.setAutoStart(true);

        //Create gesture detector
        gestureDetector = new GestureDetector(add, new MyGestureDetector(imageFlipper));

        //Create listener that triggers on touch events.
        View.OnTouchListener listener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //simply call gestureDetector's own onTouchEvent method
                return gestureDetector.onTouchEvent(motionEvent);
            }
        };

        //Set the on touch listener
        imageFlipper.setOnTouchListener(listener);

        choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                add_observation_ll.setVisibility(View.GONE);
                projectsLayout.setVisibility(View.VISIBLE);
            }
        });

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


        mAdapter = new ProjectsExpandableAdapter(add, R.layout.project_list_item, titles, projectsList, totalAces, totalAws,
                totalElse, totalRcnc);
        mProjectsListView.setAdapter(mAdapter);

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
                    Toast.makeText(add, R.string.no_more_projects, Toast.LENGTH_SHORT).show();
                }
                //replace the ArrayList in the Map with the updated one
                projectsList.put(titles[listPos], list.subList(0, numToShowAces));
                mAdapter = new ProjectsExpandableAdapter(add, R.layout.project_list_item, titles, projectsList, totalAces, totalAws, totalElse, totalRcnc);
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
                    Toast.makeText(add, R.string.no_more_projects, Toast.LENGTH_SHORT).show();
                }
                //replace the ArrayList in the Map with the updated one
                projectsList.put(titles[listPos], list.subList(0, numToShowAws));
                mAdapter = new ProjectsExpandableAdapter(add, R.layout.project_list_item, titles, projectsList,
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
                    Toast.makeText(add, R.string.no_more_projects, Toast.LENGTH_SHORT).show();
                }
                //replace the ArrayList in the Map with the updated one
                projectsList.put(titles[listPos], list.subList(0, numToShowElse));
                mAdapter = new ProjectsExpandableAdapter(add, R.layout.project_list_item, titles, projectsList,
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
                    Toast.makeText(add, R.string.no_more_projects, Toast.LENGTH_SHORT).show();
                }
                //replace the ArrayList in the Map with the updated one
                projectsList.put(titles[listPos], list.subList(0, numToShowRcnc));
                mAdapter = new ProjectsExpandableAdapter(add, R.layout.project_list_item, titles, projectsList,
                        totalAces, totalAws, totalElse, totalRcnc);
                mProjectsListView.setAdapter(mAdapter);
                mProjectsListView.expandGroup(3, true);
                mProjectsListView.setSelectedChild(3, numToShowRcnc, true);
                break;
        }
    }

    private void setProjectSearchResults(List<Project> aces, List<Project> aws, List<Project> elsewhere, List<Project> rcnc){

        //populate hashmap with results lists
        resultsMap.put(titles[0], aces);
        resultsMap.put(titles[1], aws);
        resultsMap.put(titles[2], elsewhere);
        resultsMap.put(titles[3], rcnc);

        mAdapterSearch = new ProjectsExpandableSearchAdapter(add, R.layout.project_list_item, titles, resultsMap);
        mProjectsListView.setAdapter(mAdapterSearch);
        mProjectsListView.expandGroup(0);
        mProjectsListView.expandGroup(1);
        mProjectsListView.expandGroup(2);
        mProjectsListView.expandGroup(3);

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        project = (TextView) view.findViewById(R.id.add_observation_tv_project);
        imageFlipper = (AdapterViewFlipper) view.findViewById(R.id.add_observation_iv);
        description = (EditText) view.findViewById(R.id.add_observation_et_description);
        whereIsIt = (EditText) view.findViewById(R.id.add_observation_et_where);
        choose = (Button) view.findViewById(R.id.add_observation_b_project);
        mProjectsListView = (ExpandableListView) view.findViewById(R.id.projects_list);
        add_observation_ll = (LinearLayout) view.findViewById(R.id.add_observation_ll);
        noProjects = (TextView) view.findViewById(R.id.projecs_tv);
        projectsLayout = view.findViewById(R.id.projects_layout);
        searchBox = (EditText) view.findViewById(R.id.searchAddObs);
    }

}

class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private AdapterViewFlipper flipper;

    public MyGestureDetector(AdapterViewFlipper flipper) {
        this.flipper = flipper;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;

            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                flipper.showPrevious();
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                flipper.showNext();
            }
        } catch (Exception e) {

        }
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }
}