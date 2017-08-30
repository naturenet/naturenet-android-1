package org.naturenet.ui.ideas;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.base.Optional;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.naturenet.R;
import org.naturenet.data.model.Comment;
import org.naturenet.data.model.Idea;
import org.naturenet.data.model.Site;
import org.naturenet.data.model.Users;
import org.naturenet.ui.CommentsAdapter;
import org.naturenet.ui.LoginActivity;
import org.naturenet.util.NatureNetUtils;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 */
public class IdeaDetailsFragment extends Fragment {

    public static final String IDEA_FRAGMENT_TAG = "idea_details_fragment";

    private Users user;
    private Idea idea;
    private Site site;
    private DatabaseReference dbRef;
    private IdeaDetailsActivity ideaAct;
    private ImageView userPic, likeButton, dislikeButton, status;
    private TextView userName, userAffiliation, submittedDate, ideaContent, likesNum, dislikeNum, sendButton, editButton, deleteButton;
    private ListView commentList;
    private EditText commentText;
    private Collection list;
    private int likes = 0;
    private int dislikes = 0;
    private Picasso picasso;

    public IdeaDetailsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_idea_details, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        picasso = Picasso.with(ideaAct);
        picasso.setIndicatorsEnabled(false);
        ideaAct = (IdeaDetailsActivity) this.getActivity();
        dbRef = FirebaseDatabase.getInstance().getReference();
        userPic = (ImageView) ideaAct.findViewById(R.id.idea_submitter_pic);
        likeButton = (ImageView) ideaAct.findViewById(R.id.idea_like);
        dislikeButton = (ImageView) ideaAct.findViewById(R.id.idea_dislike);
        userName = (TextView) ideaAct.findViewById(R.id.idea_submitter_tv);
        userAffiliation = (TextView) ideaAct.findViewById(R.id.idea_submitter_affiliation_tv);
        submittedDate = (TextView) ideaAct.findViewById(R.id.idea_submitted_date_tv);
        ideaContent = (TextView) ideaAct.findViewById(R.id.design_ideas_content);
        likesNum = (TextView) ideaAct.findViewById(R.id.idea_likes_num);
        dislikeNum = (TextView) ideaAct.findViewById(R.id.idea_dislike_num);
        commentList = (ListView) ideaAct.findViewById(R.id.design_idea_comment_lv);
        sendButton = (TextView) ideaAct.findViewById(R.id.design_idea_comment_send);
        commentText = (EditText) ideaAct.findViewById(R.id.design_idea_comment);
        status = (ImageView) ideaAct.findViewById(R.id.idea_status_iv);
        editButton = (TextView) ideaAct.findViewById(R.id.editIdeaButton);
        deleteButton = (TextView) ideaAct.findViewById(R.id.deleteIdeaButton);
        idea = ideaAct.idea;

        //Create SpannableString from the Idea's content.
        SpannableString hashText = new SpannableString(idea.content);

        //Create a Matcher to match any tags within the Idea's content.
        Matcher matcher = Pattern.compile("#([A-Za-z0-9_-]+)").matcher(hashText);

        //Iterate and find each matching tag.
        while(matcher.find()){
            //For each tag, set a custom ClickableSpan and change the tag color
            hashText.setSpan(new ClickableSpanDetailsFragment(hashText.subSequence(matcher.start(), matcher.end()).toString(), ideaAct), matcher.start(), matcher.end(), 0);
            hashText.setSpan(new ForegroundColorSpan(Color.parseColor("#F5C431")), matcher.start(), matcher.end(), 0);
        }

        //Finally, set the text and make the tags clickable.
        ideaContent.setText(hashText);
        ideaContent.setMovementMethod(LinkMovementMethod.getInstance());

        getSubmitterInfo(ideaAct.idea.submitter);
        getComments(ideaAct.idea.id);
        submittedDate.setText(NatureNetUtils.toDateString(ideaAct.idea));

        //set the number of likes
        setLikes();

        if(idea.status != null) {
            switch (idea.status) {
                case "developing":
                    picasso.load(R.drawable.developing).into(status);
                    break;
                case "testing":
                    picasso.load(R.drawable.testing).into(status);
                    break;
                case "done":
                    picasso.load(R.drawable.completed).into(status);
                    break;
                default:
                    picasso.load(R.drawable.discussing).into(status);
                    break;
            }
        }

        //if the signed in user is the user who submitted the idea, display the edit and delete buttons
        if(ideaAct.signed_user == null || !idea.submitter.equals(ideaAct.signed_user.id)){
            editButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        }

        if(ideaAct.signed_user!=null){
            //check to see if the current user has liked the design idea
            if(idea.likes!=null && idea.likes.containsKey(ideaAct.signed_user.id) && idea.likes.get(ideaAct.signed_user.id)){
                likeButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.like));
            }else if(idea.likes!=null && idea.likes.containsKey(ideaAct.signed_user.id) && !idea.likes.get(ideaAct.signed_user.id)){
                dislikeButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.dislike));
            }
        }

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder editPopup = new AlertDialog.Builder(getActivity());
                editPopup.setTitle("Edit Idea");

                final EditText text = new EditText(getActivity());
                text.setText(ideaContent.getText().toString());
                editPopup.setView(text);

                editPopup.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    dbRef.child(Idea.NODE_NAME).child(idea.id).child("content").setValue(text.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                SpannableString string = new SpannableString(text.getText());
                                Matcher m = Pattern.compile("#([A-Za-z0-9_-]+)").matcher(string);

                                while(m.find()){
                                    //For each tag, set a custom ClickableSpan and change the tag color
                                    string.setSpan(new ClickableSpanDetailsFragment(string.subSequence(m.start(), m.end()).toString(), ideaAct), m.start(), m.end(), 0);
                                    string.setSpan(new ForegroundColorSpan(Color.parseColor("#F5C431")), m.start(), m.end(), 0);
                                }

                                ideaContent.setText(string);
                            }else
                                Toast.makeText(ideaAct, "Couldn't edit idea", Toast.LENGTH_SHORT).show();
                        }
                    });
                    }
                });

                editPopup.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

                editPopup.show();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder deletePopup = new AlertDialog.Builder(getActivity());
                deletePopup.setTitle("Delete");
                deletePopup.setMessage("Are you sure you want to delete your Design Idea?");

                deletePopup.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dbRef.child(Idea.NODE_NAME).child(idea.id).child("status").setValue("deleted").addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    Toast.makeText(ideaAct, "Design Idea deleted", Toast.LENGTH_SHORT).show();
                                    ideaAct.finish();
                                }else
                                    Toast.makeText(ideaAct, "Could not delete idea", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

                deletePopup.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

                deletePopup.show();
            }
        });

        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ideaAct.signed_user!=null){ //make sure a user is signed in
                    Optional<Boolean> value;

                    //if user has already liked the idea
                    if(idea.likes!=null && idea.likes.containsKey(ideaAct.signed_user.id) && idea.likes.get(ideaAct.signed_user.id)){
                        likeButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.likes));
                        value = Optional.absent();
                    }else{
                        likeButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.like));
                        dislikeButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.dislikes));
                        value = Optional.of(true);
                    }

                    //update firebase with the like
                    dbRef.child(Idea.NODE_NAME).child(idea.id).child("likes").child(ideaAct.signed_user.id).setValue(value.orNull());

                    //update with number of likes
                    getCurrentLikes();
                }else{
                    Toast.makeText(getActivity(), "Please log in to like an Idea", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dislikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ideaAct.signed_user!=null){
                    Optional<Boolean> value;

                    //if user has already disliked the idea
                    if(idea.likes!=null && idea.likes.containsKey(ideaAct.signed_user.id) && !idea.likes.get(ideaAct.signed_user.id)){
                        dislikeButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.dislikes));
                        value = Optional.absent();
                    }else{
                        dislikeButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.dislike));
                        likeButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.likes));
                        value = Optional.of(false);
                    }

                    //update firebase with the dislike
                    dbRef.child(Idea.NODE_NAME).child(idea.id).child("likes").child(ideaAct.signed_user.id).setValue(value.orNull());

                    //update with current number of likes
                    getCurrentLikes();
                }else{
                    Toast.makeText(getActivity(), "Please log in to dislike an Idea", Toast.LENGTH_SHORT).show();
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(ideaAct.signed_user!=null){
                    String text = commentText.getText().toString();

                    if(!text.isEmpty()){

                        sendButton.setEnabled(false);
                        commentText.setEnabled(false);

                        final DatabaseReference commentRef = dbRef.child(Comment.NODE_NAME).push();
                        Comment newComment = Comment.createNew(commentRef.getKey(), text, ideaAct.signed_user.id, idea.id, Idea.NODE_NAME);

                        commentRef.setValue(newComment, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                                sendButton.setEnabled(true);
                                commentText.setEnabled(true);

                                if(databaseError!=null){
                                    Toast.makeText(getActivity(), "Your comment could not be submitted.", Toast.LENGTH_LONG).show();
                                }else{
                                    dbRef.child(Idea.NODE_NAME).child(idea.id).child("comments").child(commentRef.getKey()).setValue(true);
                                    commentText.getText().clear();
                                    Toast.makeText(getActivity(), "Your comment has been submitted", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }else{
                        Toast.makeText(getActivity(), "Enter a comment", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(getActivity(), "Please login to comment.", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                    startActivityForResult(loginIntent, 3);
                }
            }
        });

    }

    private void getSubmitterInfo(String userId){
        dbRef.child(Users.NODE_NAME).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                user = dataSnapshot.getValue(Users.class);
                userName.setText(user.displayName);
                getSiteName(user.affiliation);

                NatureNetUtils.showUserAvatar(ideaAct, userPic, user.avatar);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getComments(String parent){
        Query query = FirebaseDatabase.getInstance().getReference().child(Comment.NODE_NAME).orderByChild("parent").equalTo(parent);
        CommentsAdapter adapter = new CommentsAdapter(ideaAct, query);
        commentList.setAdapter(adapter);
    }

    private void getSiteName(String s){
        dbRef.child(Site.NODE_NAME).child(s).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                site = dataSnapshot.getValue(Site.class);   //get the site information
                userAffiliation.setText(site.name);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ideaAct, R.string.login_error_message_firebase_read, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void setLikes(){
        if(idea.likes != null){
            list = idea.likes.values();

            likes = 0;
            dislikes = 0;

            for(Object like: list){
                if(like.equals(true))
                    likes ++;
                else
                    dislikes++;
            }

            likesNum.setText(String.valueOf(likes));
            dislikeNum.setText(String.valueOf(dislikes));

        }else{
            likesNum.setText("0");
            dislikeNum.setText("0");
        }
    }

    private void getCurrentLikes(){
        dbRef.child(Idea.NODE_NAME).child(idea.id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild("likes")){
                    dataSnapshot.getRef().child("likes").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            idea.likes = (Map<String, Boolean>) dataSnapshot.getValue();
                            setLikes();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                    setLikes();
                }else{
                    idea.likes = null;
                    setLikes();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ideaAct, "Could not retrieve likes.", Toast.LENGTH_SHORT).show();
            }


        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


}

/**
 * This class extends ClickableSpan. It's purpose is to handle the clicks of the tags in this Fragment.
 * When a tag is clicked, we set the result of the Intent and pass back the tag that was clicked so it can be searched for in the IdeasFragment and show
 * any other Ideas with the same tag.
 */
class ClickableSpanDetailsFragment extends ClickableSpan {

    private String text;
    private Activity mContext;

    public ClickableSpanDetailsFragment(String t, Activity c){
        this.text = t;
        this.mContext = c;
    }

    @Override
    public void onClick(View view) {
        Intent resultIntent = new Intent(mContext, IdeasFragment.class);
        resultIntent.putExtra("tag", text);
        mContext.setResult(Activity.RESULT_OK, resultIntent);
        mContext.finish();
    }
}
