package org.naturenet.data.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.common.collect.Maps;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

import java.io.Serializable;
import java.util.Map;

@IgnoreExtraProperties
public class Project extends TimestampedData implements Parcelable, Serializable {

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    Project(String id, String iconUrl, String description, String name,  String status, Long latestContribution, Map<String, Boolean> sites, String submitter) {
        this.sites = sites;
        this.latestContribution = latestContribution;
        this.status = status;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.submitter = submitter;
        this.source = "android";
        this.id = id;
    }

    public static Project createNew(String id, String iconUrl, String description, String name, String status, Long latestContribution, Map<String, Boolean> sites, String submitter){
        Project project = new Project();
        project.id = id;
        project.iconUrl = iconUrl;
        project.description = description;
        project.name = name;
        project.status = status;
        project.latestContribution = latestContribution;
        project.sites = sites;
        project.source = "android";
        project.submitter = submitter;

        return project;
    }

    @Exclude
    public static final String NODE_NAME = "activities";

    public String id;

    @PropertyName("icon_url")
    public String iconUrl;

    public String description;

    public String name;

    @Nullable
    public String status;

    public String submitter;

    public String source;

    @Nullable
    @PropertyName("latest_contribution")
    public Long latestContribution;

    @Nullable
    public Map<String, Boolean> sites = Maps.newHashMap();

    /**
     * Private no-arg constructor needed for Firebase serialization. Do not use.
     */
    @SuppressWarnings("unused")
    private Project() {}

    private Project(Parcel in) {
        this.id = in.readString();
        this.iconUrl = in.readString();
        this.description = in.readString();
        this.name = in.readString();
        this.status = in.readString();
        this.latestContribution = in.readLong();
        this.submitter = in.readString();
        this.source = in.readString();
        in.readMap(this.sites, null);
    }

    public static final Creator<Project> CREATOR = new Creator<Project>() {
        @Override
        public Project createFromParcel(Parcel in) {
            return new Project(in);
        }

        @Override
        public Project[] newArray(int size) {
            return new Project[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(iconUrl);
        parcel.writeString(description);
        parcel.writeString(name);
        parcel.writeString(status);
        parcel.writeLong(latestContribution == null ? 0L : latestContribution);
        parcel.writeString(submitter);
        parcel.writeString(source);
        parcel.writeMap(sites);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Project project = (Project) o;

        if (!id.equals(project.id)) return false;
        if (!iconUrl.equals(project.iconUrl)) return false;
        if (!description.equals(project.description)) return false;
        if (!name.equals(project.name)) return false;
        if (status != null ? !status.equals(project.status) : project.status != null) return false;
        if (!submitter.equals(project.submitter)) return false;
        if (!source.equals(project.source)) return false;
        return latestContribution != null ? latestContribution.equals(project.latestContribution) : project.latestContribution == null;

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + iconUrl.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (latestContribution != null ? latestContribution.hashCode() : 0);
        result = 31 * result + submitter.hashCode();
        result = 31 * result + source.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Project{" +
                "id='" + id + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", description='" + description + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", latestContribution=" + latestContribution +
                ", submitter=" + submitter +
                ", sites=" + sites +
                ", submitter=" + submitter +
                ", source=" + source +
                '}';
    }
}