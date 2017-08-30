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


public class UsersExpandableSearchAdapter extends BaseExpandableListAdapter {
    private Context mContext;
    private String[] expandableListTitle;
    private HashMap<String, List<Users>> userLists;
    private int userRowResource;
    private HashMap<String, String> locations;
    private LayoutInflater inflater;

    public UsersExpandableSearchAdapter(Context context, int resource, String[] titles, HashMap<String, List<Users>> lists){
        mContext = context;
        expandableListTitle = titles;
        userLists = lists;
        userRowResource = resource;
        inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

        UsersExpandableSearchAdapter.GroupViewHolder holder;

        if(convertView == null){
            holder = new UsersExpandableSearchAdapter.GroupViewHolder();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.communities_group_title, null);
            holder.groupTitle = (TextView) convertView.findViewById(R.id.listTitle);
            convertView.setTag(holder);
        }else
            holder = (UsersExpandableSearchAdapter.GroupViewHolder) convertView.getTag();

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
        if(convertView == null) {
            holder = new UserViewHolder();
            convertView = inflater.inflate(userRowResource, viewGroup, false);
            holder.userName = (TextView) convertView.findViewById(R.id.username_communities);
            holder.affiliation = (TextView) convertView.findViewById(R.id.location_communities);
            holder.profileImage = (ImageView) convertView.findViewById(R.id.user_profile_pic_communities);
            convertView.setTag(holder);
        }else{
            holder = (UserViewHolder) convertView.getTag();
        }

        holder.affiliation.setText(locations.get(getGroup(titlePosition)));
        holder.userName.setText(getChild(titlePosition, usersListPosition).displayName);
        if (getChild(titlePosition, usersListPosition).avatar != null && !getChild(titlePosition, usersListPosition).avatar.equals(""))
            Picasso.with(mContext).load(userLists.get(getGroup(titlePosition)).get(usersListPosition).avatar).placeholder(R.drawable.default_avatar).into(holder.profileImage);

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

    private static class GroupViewHolder{
        TextView groupTitle;
    }
}
