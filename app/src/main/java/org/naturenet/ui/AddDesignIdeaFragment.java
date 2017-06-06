package org.naturenet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.naturenet.R;
import org.naturenet.data.model.Idea;


public class AddDesignIdeaFragment extends Fragment {

    public static final String ADD_DESIGN_IDEA_FRAGMENT = "add_design_idea_fragment";

    EditText ideaTextEntry;
    TextView sendButton;
    AddDesignIdeaActivity addIdeaAct;
    DatabaseReference dbRef;
    String ideaText;

    public AddDesignIdeaFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_design_idea, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addIdeaAct = (AddDesignIdeaActivity) getActivity();
        ideaTextEntry = (EditText) addIdeaAct.findViewById(R.id.design_idea_text);
        sendButton = (TextView) addIdeaAct.findViewById(R.id.design_idea_send_button);
        dbRef = FirebaseDatabase.getInstance().getReference();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ideaText = ideaTextEntry.getText().toString();

                if(!ideaText.isEmpty()){

                    if(addIdeaAct.signed_user!=null){
                        sendButton.setVisibility(View.GONE);

                        DatabaseReference ideaRef = dbRef.child(Idea.NODE_NAME).push();
                        Idea newIdea = Idea.createNew(ideaRef.getKey(), ideaText, addIdeaAct.signed_user.id);

                        ideaRef.setValue(newIdea, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                                if(databaseError!=null){
                                    Toast.makeText(addIdeaAct, "Design Idea could not be submitted.", Toast.LENGTH_LONG).show();
                                    Log.d("permissionerror", databaseReference.toString());
                                }else{
                                    Toast.makeText(addIdeaAct, "Design Idea submitted", Toast.LENGTH_SHORT).show();
                                    ideaTextEntry.getText().clear();
                                    sendButton.setVisibility(View.VISIBLE);
                                    addIdeaAct.finish();
                                }
                            }
                        });

                    }else{
                        Toast.makeText(addIdeaAct, "Please login to submit an Idea.", Toast.LENGTH_SHORT).show();
                        Intent loginIntent = new Intent(addIdeaAct, LoginActivity.class);
                        startActivityForResult(loginIntent, 99);
                    }

                }else{
                    Toast.makeText(addIdeaAct, "Enter an Idea", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}