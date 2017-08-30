package org.naturenet.ui.projects;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;

import org.naturenet.R;
import org.naturenet.data.model.Project;
import org.naturenet.ui.communities.CommunitiesFragment;
import org.naturenet.ui.communities.UsersExpandableAdapter;

import java.util.HashMap;
import java.util.List;

public class ProjectsExpandableSearchAdapter extends BaseExpandableListAdapter {

    private Context mContext;
    private int rowResource;
    private String[] groupTitles;
    private HashMap<String, List<Project>> projectsList;
    private LayoutInflater inflater;
    private HashMap<String, String> locations;

    public ProjectsExpandableSearchAdapter(Context context, int res, String[] titles, HashMap<String, List<Project>> list) {
        mContext = context;
        rowResource = res;
        groupTitles = titles;
        projectsList = list;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setSiteNames();

    }

    /*
        This method returns the number of groups
     */
    @Override
    public int getGroupCount() {
        return groupTitles.length;
    }

    /*
        This method returns the number of children for a specific group.
     */
    @Override
    public int getChildrenCount(int group) {
        return projectsList.get(groupTitles[group]).size();
    }

    /*
        This method returns the expandable group. In this case it's just a String.
     */
    @Override
    public String getGroup(int i) {
        return groupTitles[i];
    }

    /*
        This method gets the child based on listPosition and expandedListPosition.
     */
    @Override
    public Project getChild(int listPosition, int expandedListPosition) {
        //return the child by getting the specific list, then the specific user from that list
        return projectsList.get(groupTitles[listPosition]).get(expandedListPosition);
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
    public long getChildId(int groupPos, int expandedListPosition) {
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
    public View getGroupView(int listPos, boolean isExpanded, View convertView, ViewGroup viewGroup) {

        UsersExpandableAdapter.GroupViewHolder holder;

        if(convertView == null){
            holder = new UsersExpandableAdapter.GroupViewHolder();
            convertView = inflater.inflate(R.layout.communities_group_title, null);
            holder.groupTitle = (TextView) convertView.findViewById(R.id.listTitle);
            convertView.setTag(holder);
        }else
            holder = (UsersExpandableAdapter.GroupViewHolder) convertView.getTag();

        holder.groupTitle.setText(locations.get(groupTitles[listPos]));

        return convertView;

    }

    /*
        This method is for populating the child views under each expandable category. For example, a category "Countries"
        would expand to display child views such as USA, Canada, Mexico, etc.
     */
    @Override
    public View getChildView(int titlePosition, int projectPosition, boolean isLastChild, View convertView, ViewGroup viewGroup) {

        ViewHolder holder;

        if(convertView==null){
            holder = new ViewHolder();
            convertView = inflater.inflate(rowResource, viewGroup, false);

            holder.projectIcon = (ImageView) convertView.findViewById(R.id.project_thumbnail);
            holder.projectName = (TextView) convertView.findViewById(R.id.project_name);

            convertView.setTag(holder);
        }else
            holder = (ViewHolder) convertView.getTag();

        holder.projectName.setText(getChild(titlePosition, projectPosition).name);
        Picasso.with(mContext).load(getChild(titlePosition, projectPosition).iconUrl).into(holder.projectIcon);

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

    private static class ViewHolder{
        ImageView projectIcon;
        TextView projectName;
    }
}
