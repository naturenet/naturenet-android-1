package org.naturenet.data.model;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ProjectClassTests {

    final static String ID = "12345";
    final static String ICON_URL = "www.google.com";
    final static String DESCRIPTION = "description";
    final static String NAME = "trees";
    final static String STATUS = "current";
    final static long LATEST_CONTRIBUTION = 34234234234L;
    final static String SUBMITTER = "submitter";

    @Test
    public void test_is_project_parcelable(){

        Map<String, Boolean> sites = new HashMap<>();

        sites.put("stuff", false);
        sites.put("things", true);
        sites.put("information", true);
        sites.put("stuff", false);
        sites.put("bbbb", true);

        Project project = new Project(ID, ICON_URL, DESCRIPTION, NAME, STATUS, LATEST_CONTRIBUTION, sites, SUBMITTER);

        Parcel parcel = Parcel.obtain();

        project.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        Project parcelProject = Project.CREATOR.createFromParcel(parcel);

        //assert that all fields are the same after parcelization
        assertEquals(null, parcelProject.id, ID);
        assertEquals(null, parcelProject.iconUrl, ICON_URL);
        assertEquals(null, parcelProject.description, DESCRIPTION);
        assertEquals(null, parcelProject.name, NAME);
        assertEquals(null, parcelProject.status, STATUS);
        assertEquals(parcelProject.latestContribution, Long.valueOf(LATEST_CONTRIBUTION));
        assertEquals(null, parcelProject.sites, sites);

    }


}
