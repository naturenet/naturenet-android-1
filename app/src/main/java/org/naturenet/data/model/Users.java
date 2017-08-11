package org.naturenet.data.model;

import android.os.Parcel;
import android.support.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

import java.util.Map;

@IgnoreExtraProperties
public class Users extends TimestampedData {

    @Exclude
    public static final String NODE_NAME = "users";

    public static Users createNew(String id, String displayName, String affiliation, String avatar) {
        Users u = new Users();
        u.id = id;
        u.displayName = displayName;
        u.affiliation = affiliation;
        u.avatar = avatar;
        return u;
    }

    public String id;

    @PropertyName("display_name")
    public String displayName;

    public String affiliation;

    @Nullable
    public String avatar;

    @Nullable
    public String bio;

    @Nullable
    public Map<String, Boolean> groups = Maps.newHashMap();

    @Nullable
    @PropertyName("latest_contribution")
    public Long latestContribution;

    @PropertyName("notification_token")
    public String notificationToken;

    private Users() {}

    private Users(Parcel in) {
        super(in);
        this.id = in.readString();
        this.displayName = in.readString();
        this.affiliation = in.readString();
        this.avatar = in.readString();
        this.bio = in.readString();
        this.notificationToken = in.readString();
        in.readMap(this.groups, null);
        this.latestContribution = in.readLong();
    }

        @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeString(id);
        parcel.writeString(displayName);
        parcel.writeString(affiliation);
        parcel.writeString(avatar);
        parcel.writeString(bio);
        parcel.writeString(notificationToken);
        parcel.writeMap(groups);
        parcel.writeLong(latestContribution == null ? 0L : latestContribution);
    }

    public static final Creator<Users> CREATOR = new Creator<Users>() {
        @Override
        public Users createFromParcel(Parcel in) {
            return new Users(in);
        }

        @Override
        public Users[] newArray(int size) {
            return new Users[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Users users = (Users) o;

        if (!id.equals(users.id)) return false;
        if (!displayName.equals(users.displayName)) return false;
        if (!affiliation.equals(users.affiliation)) return false;
        if (avatar != null ? !avatar.equals(users.avatar) : users.avatar != null) return false;
        if (bio != null ? !bio.equals(users.bio) : users.bio != null) return false;
        if (!notificationToken.equals(users.notificationToken)) return false;
        return latestContribution != null ? latestContribution.equals(users.latestContribution) : users.latestContribution == null;

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + displayName.hashCode();
        result = 31 * result + affiliation.hashCode();
        result = 31 * result + (avatar != null ? avatar.hashCode() : 0);
        result = 31 * result + (bio != null ? bio.hashCode() : 0);
        result = 31 * result + (notificationToken != null ? notificationToken.hashCode() : 0);
        result = 31 * result + (latestContribution != null ? latestContribution.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Users{" +
                super.toString() +
                ", id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", affiliation='" + affiliation + '\'' +
                ", avatar='" + avatar + '\'' +
                ", bio='" + bio + '\'' +
                ", notificationToken+" + notificationToken + '\'' +
                ", groups=" + groups +
                ", latestContribution=" + latestContribution +
                '}';
    }
}