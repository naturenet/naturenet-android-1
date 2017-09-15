package org.naturenet.ui.projects;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.naturenet.R;
import org.naturenet.data.model.Project;
import org.naturenet.ui.LoginActivity;

import java.util.HashMap;
import java.util.Map;


public class AddProjectFragment extends Fragment {

    public static final String ADD_PROJECT_FRAGMENT = "add_project_fragment";
    public static final String PROJECT_ICON = "https://res.cloudinary.com/university-of-colorado/image/upload/v1464880363/static/Backyard_bd5me8.png";

    private EditText projectTitle, projectDescription;
    private TextView submitButton;
    private String title, description;
    private Switch acesSwitch, awsSwitch, elseSwitch, rcncSwitch;
    private static final String ELSEWHERE = "zz_elsewhere";
    private static final String ACES = "aces";
    private static final String ANACOSTIA = "aws";
    private static final String RCNC = "rcnc";
    DatabaseReference dbRef;

    public AddProjectFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_project, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final AddProjectActivity activity = (AddProjectActivity) getActivity();

        dbRef = FirebaseDatabase.getInstance().getReference();
        projectDescription = (EditText) activity.findViewById(R.id.projectDescription);
        projectTitle = (EditText) activity.findViewById(R.id.projectTitle);
        submitButton = (TextView) activity.findViewById(R.id.projectSubmitButton);
        acesSwitch = (Switch) activity.findViewById(R.id.acesSwitchButton);
        awsSwitch = (Switch) activity.findViewById(R.id.awsSwitchButton);
        elseSwitch = (Switch) activity.findViewById(R.id.elseSwitchButton);
        rcncSwitch = (Switch) activity.findViewById(R.id.rcncSwitchButton);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                title = projectTitle.getText().toString();
                description = projectDescription.getText().toString();

                //make sure user is logged in
                if(activity.signed_user!=null){
                    if(!title.isEmpty() && !description.isEmpty() && isSiteSelected()){

                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch (i){
                                    //submit the project
                                    case DialogInterface.BUTTON_POSITIVE:
                                        submitButton.setVisibility(View.GONE);
                                        //create node for the new Project and get the unique id
                                        DatabaseReference projectRef = dbRef.child(Project.NODE_NAME).push();

                                        //create new Project object
                                        Project project = Project.createNew(projectRef.getKey(), PROJECT_ICON,
                                                description, capitalizeAll(title), null, null, null, activity.signed_user.id);

                                        setSites(project);

                                        Log.d("printProject", project.toString());

                                        projectRef.setValue(project, new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                                //make sure there isn't an error
                                                if(databaseError!=null){
                                                    Toast.makeText(activity, "Project could not be submitted.", Toast.LENGTH_LONG).show();
                                                    Log.d("permissionerror", databaseError.toString());
                                                }else{
                                                    Toast.makeText(activity, "Project submitted!", Toast.LENGTH_LONG).show();
                                                    projectTitle.getText().clear();
                                                    projectDescription.getText().clear();
                                                    activity.setResult(Activity.RESULT_OK);
                                                    activity.finish();
                                                }
                                            }
                                        });
                                        break;
                                    case DialogInterface.BUTTON_NEGATIVE:
                                        break;
                                }
                            }
                        };

                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                        alertDialog.setMessage(R.string.are_you_sure).setPositiveButton(R.string.yes, dialogClickListener)
                                .setNegativeButton(R.string.no, dialogClickListener).show();


                    }else{  //notify user they must enter all field information
                        Toast.makeText(activity, "Please complete all fields.", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(activity, "Please log in to submit a project.", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(activity, LoginActivity.class);
                    startActivityForResult(loginIntent, 100);
                }
            }
        });
    }

    /*
        This method sets the selected sites to the Project object.
     */
    private void setSites(Project p){

        Map<String, Boolean> sites = new HashMap<>();

        if(acesSwitch.isChecked())
            sites.put(ACES, true);
        else
            sites.put(ACES, false);

        if(awsSwitch.isChecked())
            sites.put(ANACOSTIA, true);
        else
            sites.put(ANACOSTIA, false);

        if(elseSwitch.isChecked())
            sites.put(ELSEWHERE, true);
        else
            sites.put(ELSEWHERE, false);

        if(rcncSwitch.isChecked())
            sites.put(RCNC, true);
        else
            sites.put(RCNC, false);

        //set the sites
        p.sites = sites;

    }

    private boolean isSiteSelected(){
        return acesSwitch.isChecked() || awsSwitch.isChecked() || elseSwitch.isChecked() || rcncSwitch.isChecked();
    }

    private String capitalizeAll(String s){

        //split the words
        String[] words = s.split(" ");
        StringBuilder stringBuilder =new StringBuilder();

        //iterate over each word and capitalize it
        for(String word: words){
            stringBuilder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }

        return stringBuilder.toString().trim();
    }
}
