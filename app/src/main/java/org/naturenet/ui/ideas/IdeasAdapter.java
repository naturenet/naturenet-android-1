package org.naturenet.ui.ideas;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.naturenet.R;
import org.naturenet.data.model.Idea;
import org.naturenet.data.model.Site;
import org.naturenet.data.model.Users;
import org.naturenet.ui.LinkifiedTextView;
import org.naturenet.util.NatureNetUtils;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class IdeasAdapter extends ArrayAdapter<Idea> {

    private Picasso picasso;
    private DatabaseReference databaseReference;
    private Users user;
    private Site site;
    private String location;
    private Context mContext;
    private int layout;
    private List<Idea> ideas;
    private IdeasFragment fragment;

    public IdeasAdapter(Activity context, IdeasFragment fragment, int layout, List<Idea> ideas){
        super(context, layout, ideas);
        mContext = context;
        this.layout = layout;
        this.ideas = ideas;
        databaseReference = FirebaseDatabase.getInstance().getReference();
        picasso = Picasso.with(mContext);
        picasso.setIndicatorsEnabled(false);
        this.fragment = fragment;
    }

    @Nullable
    @Override
    public Idea getItem(int position) {
        return ideas.get(position);
    }

    @Override
    public int getCount() {
        return ideas.size();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        ViewHolder holder;
        if(convertView==null){
            holder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layout, null);
            holder.affiliation = (TextView) convertView.findViewById(R.id.design_ideas_affiliation);
            holder.profile_pic = (ImageView) convertView.findViewById(R.id.design_ideas_user_profile_image);
            holder.username = (TextView) convertView.findViewById(R.id.design_ideas_user_name);
            holder.ideaContent = (LinkifiedTextView) convertView.findViewById(R.id.design_idea_text_row);
            holder.status = (ImageView) convertView.findViewById(R.id.design_idea_status);
            holder.likesNum = (TextView) convertView.findViewById(R.id.design_idea_likes_number);
            holder.dislikeNum = (TextView) convertView.findViewById(R.id.design_ideas_dislike_number);
            holder.commentNum = (TextView) convertView.findViewById(R.id.design_ideas_comment_number);
            holder.ideaDate = (TextView) convertView.findViewById(R.id.ideaDate);
            holder.statusText = (TextView) convertView.findViewById(R.id.status_text);

            convertView.setTag(holder);

        }

        holder = (ViewHolder) convertView.getTag();

        Idea idea = ideas.get(position);

        //Create SpannableString from the Idea's content.
        SpannableString hashText = new SpannableString(idea.content);

        //Create matcher to match hashtags
        Matcher matcher = Pattern.compile("#([A-Za-z0-9_-]+)").matcher(hashText);

        //Iterate over the matcher for each match.
        while (matcher.find()) {
            //When there is a match, set a custom click listener on each tag and change the color of the tag
            hashText.setSpan(new ClickableSpanIdeasAdapter(hashText.subSequence(matcher.start(), matcher.end()).toString(), fragment), matcher.start(), matcher.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            hashText.setSpan(new ForegroundColorSpan(Color.parseColor("#F5C431")), matcher.start(), matcher.end(), 0);
        }

        //Finally, make the tags clickable and set the text in the TextView
        holder.ideaContent.setMovementMethod(LinkMovementMethod.getInstance());

        holder.ideaContent.setText(hashText, TextView.BufferType.SPANNABLE);
        holder.ideaContent.setFocusable(false);

        //set profile pic, username, affiliation here
        setUserInformation(idea.submitter, holder.profile_pic, holder.username, holder.affiliation);

        //set idea date
        holder.ideaDate.setText(NatureNetUtils.toDateString(idea));

        //get like/dislike numbers and set them
        if(idea.likes!=null){
            Collection list = idea.likes.values();
            int dislikeCount;
            int likeCount = dislikeCount = 0;

            for(Object like : list){
                if(like.equals(true)){
                    likeCount++;
                }else{
                    dislikeCount++;
                }
            }

            holder.likesNum.setText(String.valueOf(likeCount));
            holder.dislikeNum.setText(String.valueOf(dislikeCount));
        }

        //get comment numbers and set them
        if(idea.comments != null){
            holder.commentNum.setText(String.valueOf(idea.comments.size()));
        }

        if (idea.status != null) {
            switch (idea.status){
                case "developing":
                    picasso.load(R.drawable.developing).into(holder.status);
                    holder.statusText.setText(R.string.developing);
                    holder.statusText.setTextColor(ContextCompat.getColor(mContext, R.color.colorButton));
                    break;
                case "testing":
                    picasso.load(R.drawable.testing).into(holder.status);
                    holder.statusText.setText(R.string.testing);
                    holder.statusText.setTextColor(ContextCompat.getColor(mContext, R.color.colorButton));
                    break;
                case "done":
                    picasso.load(R.drawable.completed).into(holder.status);
                    holder.statusText.setText(R.string.done);
                    holder.statusText.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
                    break;
                default:
                    picasso.load(R.drawable.discussing).into(holder.status);
                    holder.statusText.setText(R.string.discussing);
                    holder.statusText.setTextColor(ContextCompat.getColor(mContext, R.color.colorButton));
                    break;
            }
        }

        return convertView;
    }

    private void setUserInformation(String id, final ImageView profile, final TextView username, final TextView aff){
        databaseReference.child(Users.NODE_NAME).child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                user = dataSnapshot.getValue(Users.class);
                picasso.load(Strings.emptyToNull(user.avatar)).placeholder(R.drawable.default_avatar).into(profile);
                username.setText(user.displayName);
                aff.setText(getSiteName(user.affiliation));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private String getSiteName(String s){
        databaseReference.child(Site.NODE_NAME).child(s).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                site = dataSnapshot.getValue(Site.class);   //get the site information
                location = site.name;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(mContext, R.string.login_error_message_firebase_read, Toast.LENGTH_SHORT).show();
            }
        });

        return location;
    }

    private static class ViewHolder{
        ImageView profile_pic, status;
        TextView username, affiliation, ideaContent, likesNum, dislikeNum, commentNum, ideaDate, statusText;
    }
}

/**
 * Custom class that extends ClickableSpan. The class overrides the onClick method and sets the clicked tag as the search.
 */
class ClickableSpanIdeasAdapter extends ClickableSpan{

    private String text;
    private IdeasFragment fragment;

    public ClickableSpanIdeasAdapter(String t, IdeasFragment fragment) {
        this.text = t;
        this.fragment = fragment;
    }


    @Override
    public void onClick(View view) {
        fragment.setSearchText(text);
    }
}
