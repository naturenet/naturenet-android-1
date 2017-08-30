package org.naturenet.ui.ideas;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.naturenet.R;
import org.naturenet.data.model.Idea;
import org.naturenet.ui.LoginActivity;

public class AddDesignIdeaFragment extends Fragment {

    public static final String ADD_DESIGN_IDEA_FRAGMENT = "add_design_idea_fragment";

    EditText ideaTextEntry;
    TextView sendButton, participatoryLink, exploreTag, projectsTag, ideasTag, communitiesTag, contributionsTag, interactionTag, mapTag, visualTag, iconTag, menuTag, appTag, webTag;
    AddDesignIdeaActivity addIdeaAct;
    DatabaseReference dbRef;
    Spinner ideaTypeSpinner;
    String ideaText;
    View[] views;
    LinearLayout tagLayout;
    int c = Color.parseColor("#F5C431");

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

        tagLayout = (LinearLayout) addIdeaAct.findViewById(R.id.tagLayout);
        exploreTag = (TextView) addIdeaAct.findViewById(R.id.exploreTag);
        projectsTag = (TextView) addIdeaAct.findViewById(R.id.projectsTag);
        ideasTag = (TextView) addIdeaAct.findViewById(R.id.designTag);
        communitiesTag = (TextView) addIdeaAct.findViewById(R.id.communitiesTag);
        contributionsTag = (TextView) addIdeaAct.findViewById(R.id.contributionsTag);
        interactionTag = (TextView) addIdeaAct.findViewById(R.id.interactionTag);
        mapTag = (TextView) addIdeaAct.findViewById(R.id.mapTag);
        visualTag = (TextView) addIdeaAct.findViewById(R.id.visualTag);
        iconTag = (TextView) addIdeaAct.findViewById(R.id.iconTag);
        menuTag = (TextView) addIdeaAct.findViewById(R.id.menuTag);
        appTag = (TextView) addIdeaAct.findViewById(R.id.appTag);
        webTag = (TextView) addIdeaAct.findViewById(R.id.webTag);
        views = new View[]{exploreTag, projectsTag, ideasTag, communitiesTag, contributionsTag, interactionTag, mapTag, visualTag, iconTag, menuTag, appTag, webTag};

        ideaTextEntry = (EditText) addIdeaAct.findViewById(R.id.design_idea_text);
        sendButton = (TextView) addIdeaAct.findViewById(R.id.design_idea_send_button);
        ideaTypeSpinner = (Spinner) addIdeaAct.findViewById(R.id.ideaTypeSpinner);
        dbRef = FirebaseDatabase.getInstance().getReference();
        final ArrayAdapter<CharSequence> ideaTypeAdapter = ArrayAdapter.createFromResource(addIdeaAct, R.array.idea_types, android.R.layout.simple_spinner_item);
        ideaTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ideaTypeSpinner.setAdapter(ideaTypeAdapter);
        participatoryLink = (TextView) addIdeaAct.findViewById(R.id.participatory_design_link);
        participatoryLink.setMovementMethod(LinkMovementMethod.getInstance());

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView tag = (TextView) view;
                SpannableString text = new SpannableString(tag.getText() + " ");
                text.setSpan(new ForegroundColorSpan(Color.parseColor("#F5C431")), 0, text.length(), 0);
                ideaTextEntry.getText().insert(ideaTextEntry.getSelectionStart(), text);
            }
        };

        populateText(tagLayout, views, addIdeaAct, onClickListener);

        TextWatcher textWatcher = new TextWatcher() {

            int beginningIndex, endingIndex;
            String prevText, newText;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                //Get the previous text before the newly entered text is displayed
                prevText = charSequence.toString();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                //Get the newly entered text
                newText = editable.toString();
                String newChar = "";
                //Make sure the new text isn't blank, and the new text length is greater than the previous text (means it wasn't a backspace
                if (newText.length() != 0 && newText.length() > prevText.length()){
                    //get the newly entered character
                    newChar = newText.substring(ideaTextEntry.getSelectionStart()-1, ideaTextEntry.getSelectionEnd());
                }

                try{
                    //If a new word has been entered
                    if(newChar.equals(" ")){
                        //Get the text before the space was added
                        String textBefore = newText.substring(0, ideaTextEntry.getSelectionStart()-1);

                        //Get the index of the space before the last word that was added
                        beginningIndex = textBefore.lastIndexOf(" ")+1;

                        //Get the new word by getting the substring from the index of the space before the last word and the end of the string
                        String newWord = textBefore.substring(beginningIndex, textBefore.length());

                        endingIndex = beginningIndex + newWord.length();
                        //If the new word is a hashtag
                        if(newWord.matches("#([A-Za-z0-9_-]+)")){
                            ideaTextEntry.removeTextChangedListener(this);
                            editable.setSpan(new ForegroundColorSpan(c), beginningIndex, endingIndex, 0);
                            ideaTextEntry.setText(editable);
                            ideaTextEntry.addTextChangedListener(this);
                            ideaTextEntry.setSelection(endingIndex+1);
                        }

                    }
                }catch (IndexOutOfBoundsException exc){
                    //In case of an IndexOutOfBoundsException, simply add the intended text without trying to format it
                    ideaTextEntry.removeTextChangedListener(this);
                    ideaTextEntry.setText(editable);
                    ideaTextEntry.addTextChangedListener(this);
                    ideaTextEntry.setSelection(editable.length());
                }
            }
        };

        ideaTextEntry.addTextChangedListener(textWatcher);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ideaText = ideaTextEntry.getText().toString();

                if(!ideaText.isEmpty()){

                    if(addIdeaAct.signed_user!=null){
                        sendButton.setVisibility(View.GONE);

                        //create a node for the new idea
                        DatabaseReference ideaRef = dbRef.child(Idea.NODE_NAME).push();
                        //create the new Idea object
                        Idea newIdea = Idea.createNew(ideaRef.getKey(), ideaText, addIdeaAct.signed_user.id, ideaTypeSpinner.getSelectedItem().toString());

                        //push the new idea to the newly created node
                        ideaRef.setValue(newIdea, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                                //if we get an error, display an appropriate message
                                if(databaseError!=null){
                                    Toast.makeText(addIdeaAct, "Design Idea could not be submitted.", Toast.LENGTH_LONG).show();
                                    Log.d("permissionerror", databaseReference.toString());
                                }else{
                                    Toast.makeText(addIdeaAct, "Design Idea submitted!", Toast.LENGTH_SHORT).show();
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

    private void populateText(LinearLayout ll, View[] views , Context mContext, View.OnClickListener listener) {
        DisplayMetrics display = getResources().getDisplayMetrics();
        ll.removeAllViews();
        int maxWidth = display.widthPixels;

        LinearLayout.LayoutParams params;
        LinearLayout newLL = new LinearLayout(mContext);
        newLL.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        newLL.setGravity(Gravity.START);
        newLL.setOrientation(LinearLayout.HORIZONTAL);

        int widthSoFar = 0;

        for (int i = 0 ; i < views.length ; i++ ){
            LinearLayout LL = new LinearLayout(mContext);
            LL.setOrientation(LinearLayout.HORIZONTAL);
            LL.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);
            LL.setLayoutParams(new ListView.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            views[i].setOnClickListener(listener);
            views[i].measure(0,0);
            params = new LinearLayout.LayoutParams(views[i].getMeasuredWidth() + 10,
                    LinearLayout.LayoutParams.WRAP_CONTENT);

            LL.addView(views[i], params);
            LL.measure(0, 0);
            widthSoFar += views[i].getMeasuredWidth();
            if (widthSoFar >= maxWidth) {
                ll.addView(newLL);

                newLL = new LinearLayout(mContext);
                newLL.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                newLL.setOrientation(LinearLayout.HORIZONTAL);
                newLL.setGravity(Gravity.START);
                params = new LinearLayout.LayoutParams(LL
                        .getMeasuredWidth(), LL.getMeasuredHeight());
                newLL.addView(LL, params);
                widthSoFar = LL.getMeasuredWidth();
            } else {
                newLL.addView(LL);
            }
        }
        ll.addView(newLL);
    }
}
