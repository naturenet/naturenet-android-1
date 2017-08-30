package org.naturenet.ui.communities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.naturenet.R;
import org.naturenet.data.model.Users;

import java.util.HashMap;
import java.util.List;


public class UsersExpandableAdapter extends BaseExpandableListAdapter {

    private Context mContext;
    private String[] expandableListTitle;
    private HashMap<String, List<Users>> userLists;
    private int userRowResource;
    private HashMap<String, String> locations;
    private LayoutInflater inflater;
    private int acesMax, awsMax, elseMax, rcncMax;
    private Picasso picasso;

    public UsersExpandableAdapter(Context context, int resource, String[] titles, HashMap<String, List<Users>> lists, int acesMax, int awsMax, int elseMax, int rcncMax){
        mContext = context;
        expandableListTitle = titles;
        userLists = lists;
        userRowResource = resource;
        inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.acesMax = acesMax;
        this.awsMax = awsMax;
        this.elseMax = elseMax;
        this.rcncMax = rcncMax;
        picasso = Picasso.with(context);
        picasso.setIndicatorsEnabled(false);
        setSiteNames();
    }

    /*
        This method return the number of expandable groups.
     */
    @Override
    public int getGroupCount() {
        return expandableListTitle.length;
    }

    /*
        This method returns the number of children for a specific group designated by the listPosition;
     */
    @Override
    public int getChildrenCount(int listPosition) {
        return userLists.get(expandableListTitle[listPosition]).size();
    }

    /*
        This method returns the expandable group. In this case it's just a String.
     */
    @Override
    public String getGroup(int i) {
        return expandableListTitle[i];
    }

    /*
        This method gets the child based on listPosition and expandedListPosition.
     */
    @Override
    public Users getChild(int listPosition, int expandedListPosition) {
        //return the child by getting the specific list, then the specific user from that list
        return userLists.get(expandableListTitle[listPosition]).get(expandedListPosition);
    }

    /*
        This method returns the expandable group index.
     */
    @Override
    public long getGroupId(int i) {
        return i;
    }

    /*
        This method returns the position of the child view.
     */
    @Override
    public long getChildId(int listPosition, int expandedListPosition) {
        return expandedListPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    /*
        This method is for populating the expandable group titles.
     */
    @Override
    public View getGroupView(int listPos, boolean isExpanded, View convertView, ViewGroup parent) {

        GroupViewHolder holder;

        if(convertView == null){
            holder = new GroupViewHolder();
            convertView = inflater.inflate(R.layout.communities_group_title, parent, false);
            holder.groupTitle = (TextView) convertView.findViewById(R.id.listTitle);
            convertView.setTag(holder);
        }else
            holder = (GroupViewHolder) convertView.getTag();

        holder.groupTitle.setText(locations.get(expandableListTitle[listPos]));

        return convertView;
    }


    /*
        This method is for populating the child views under each expandable category. For example, a category "Countries"
        would expand to display child views such as USA, Canada, Mexico, etc.
     */
    @Override
    public View getChildView(int titlePosition, int usersListPosition, boolean isLastChild, View convertView, ViewGroup viewGroup) {
        UserViewHolder holder;

        //if convertView is null, it is a new un-inflated view
        if(convertView == null){
            holder = new UserViewHolder();

            //if this isn't the last child
            if(!isLastChild){
                //inflate the user row and set references to the views using the holder
                convertView = inflater.inflate(userRowResource, viewGroup, false);
                holder.affiliation = (TextView) convertView.findViewById(R.id.location_communities);
                holder.profileImage = (ImageView) convertView.findViewById(R.id.user_profile_pic_communities);
                holder.userName = (TextView) convertView.findViewById(R.id.username_communities);

                //set tag on convertView
                convertView.setTag(holder);

                //populate the views
                holder.affiliation.setText(locations.get(getGroup(titlePosition)));
                holder.userName.setText(getChild(titlePosition, usersListPosition).displayName);
                if (getChild(titlePosition, usersListPosition).avatar != null && !getChild(titlePosition, usersListPosition).avatar.equals(""))
                    picasso.load(userLists.get(getGroup(titlePosition)).get(usersListPosition).avatar).placeholder(R.drawable.default_avatar).into(holder.profileImage);

            }else{  //otherwise, inflate the show more button layout
                convertView = inflater.inflate(R.layout.show_more_button, viewGroup, false);

            }
            //if convertView isn't null, we know it's been inflated so we can recycle it
        }else {
            //check to see if it's not the last child
            if (!isLastChild) {
                holder = (UserViewHolder) convertView.getTag();
                if(holder != null){
                    holder.userName.setText(getChild(titlePosition, usersListPosition).displayName);
                    holder.affiliation.setText(locations.get(getGroup(titlePosition)));
                    if (getChild(titlePosition, usersListPosition).avatar != null && !getChild(titlePosition, usersListPosition).avatar.equals(""))
                        picasso.load(userLists.get(getGroup(titlePosition)).get(usersListPosition).avatar).placeholder(R.drawable.default_avatar).into(holder.profileImage);
                }else{
                    holder = new UserViewHolder();
                    convertView = inflater.inflate(userRowResource, viewGroup, false);
                    holder.affiliation = (TextView) convertView.findViewById(R.id.location_communities);
                    holder.profileImage = (ImageView) convertView.findViewById(R.id.user_profile_pic_communities);
                    holder.userName = (TextView) convertView.findViewById(R.id.username_communities);

                    holder.userName.setText(getChild(titlePosition, usersListPosition).displayName);
                    holder.affiliation.setText(locations.get(getGroup(titlePosition)));
                    if (getChild(titlePosition, usersListPosition).avatar != null && !getChild(titlePosition, usersListPosition).avatar.equals(""))
                        picasso.load(userLists.get(getGroup(titlePosition)).get(usersListPosition).avatar).placeholder(R.drawable.default_avatar).into(holder.profileImage);

                    convertView.setTag(holder);
                }
            }else{  //here we know it's the last child view, so we check to see if it's the very last child in which case we don't need a show more button
                holder = (UserViewHolder) convertView.getTag();

                switch (titlePosition){
                    case 0:
                        //if we've reached the end of the aces list
                        if(userLists.get(getGroup(titlePosition)).size() == acesMax){
                            if(holder==null){
                                holder = new UserViewHolder();
                                convertView = inflater.inflate(userRowResource, viewGroup, false);
                                holder.userName = (TextView) convertView.findViewById(R.id.username_communities);
                                holder.profileImage = (ImageView) convertView.findViewById(R.id.user_profile_pic_communities);
                                holder.affiliation = (TextView) convertView.findViewById(R.id.location_communities);
                            }

                            holder.userName.setText(getChild(titlePosition, usersListPosition).displayName);
                            holder.affiliation.setText(locations.get(getGroup(titlePosition)));
                            if (getChild(titlePosition, usersListPosition).avatar != null && !getChild(titlePosition, usersListPosition).avatar.equals(""))
                                picasso.load(userLists.get(getGroup(titlePosition)).get(usersListPosition).avatar).placeholder(R.drawable.default_avatar).into(holder.profileImage);
                        }else
                            convertView = inflater.inflate(R.layout.show_more_button, viewGroup, false);

                        break;

                    case 1:
                        //if we've reached the end of the aws list
                        if(userLists.get(getGroup(titlePosition)).size() == awsMax){
                            if(holder==null){
                                holder = new UserViewHolder();
                                convertView = inflater.inflate(userRowResource, viewGroup, false);
                                holder.userName = (TextView) convertView.findViewById(R.id.username_communities);
                                holder.profileImage = (ImageView) convertView.findViewById(R.id.user_profile_pic_communities);
                                holder.affiliation = (TextView) convertView.findViewById(R.id.location_communities);
                            }

                            holder.userName.setText(getChild(titlePosition, usersListPosition).displayName);
                            holder.affiliation.setText(locations.get(getGroup(titlePosition)));
                            if (getChild(titlePosition, usersListPosition).avatar != null && !getChild(titlePosition, usersListPosition).avatar.equals(""))
                                picasso.load(userLists.get(getGroup(titlePosition)).get(usersListPosition).avatar).placeholder(R.drawable.default_avatar).into(holder.profileImage);
                        }else
                            convertView = inflater.inflate(R.layout.show_more_button, viewGroup, false);

                        break;

                    case 2:
                        //if we've reached the end of the elsewhere list
                        if(userLists.get(getGroup(titlePosition)).size() == elseMax){
                            if(holder==null){
                                holder = new UserViewHolder();
                                convertView = inflater.inflate(userRowResource, viewGroup, false);
                                holder.userName = (TextView) convertView.findViewById(R.id.username_communities);
                                holder.profileImage = (ImageView) convertView.findViewById(R.id.user_profile_pic_communities);
                                holder.affiliation = (TextView) convertView.findViewById(R.id.location_communities);
                            }

                            holder.userName.setText(getChild(titlePosition, usersListPosition).displayName);
                            holder.affiliation.setText(locations.get(getGroup(titlePosition)));
                            if (getChild(titlePosition, usersListPosition).avatar != null && !getChild(titlePosition, usersListPosition).avatar.equals(""))
                                picasso.load(userLists.get(getGroup(titlePosition)).get(usersListPosition).avatar).placeholder(R.drawable.default_avatar).into(holder.profileImage);
                        }else
                            convertView = inflater.inflate(R.layout.show_more_button, viewGroup, false);

                        break;

                    case 3:
                        //if we've reached the end of the rcnc list
                        if(userLists.get(getGroup(titlePosition)).size() == rcncMax){
                            if(holder==null){
                                holder = new UserViewHolder();
                                convertView = inflater.inflate(userRowResource, viewGroup, false);
                                holder.userName = (TextView) convertView.findViewById(R.id.username_communities);
                                holder.profileImage = (ImageView) convertView.findViewById(R.id.user_profile_pic_communities);
                                holder.affiliation = (TextView) convertView.findViewById(R.id.location_communities);
                            }

                            holder.userName.setText(getChild(titlePosition, usersListPosition).displayName);
                            holder.affiliation.setText(locations.get(getGroup(titlePosition)));
                            if (getChild(titlePosition, usersListPosition).avatar != null && !getChild(titlePosition, usersListPosition).avatar.equals(""))
                                picasso.load(userLists.get(getGroup(titlePosition)).get(usersListPosition).avatar).placeholder(R.drawable.default_avatar).into(holder.profileImage);
                        }else
                            convertView = inflater.inflate(R.layout.show_more_button, viewGroup, false);

                        break;
                }
            }
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    private void setSiteNames(){
        locations = new HashMap<>();
        locations.put(CommunitiesFragment.ELSEWHERE, "Elsewhere");
        locations.put(CommunitiesFragment.ANACOSTIA, "Anacostia");
        locations.put(CommunitiesFragment.ACES, "Aspen");
        locations.put(CommunitiesFragment.RCNC, "Reedy Creek");
    }

    private static class UserViewHolder{
        ImageView profileImage;
        TextView userName, affiliation;
    }

    public static class GroupViewHolder{
        public TextView groupTitle;
    }
}
