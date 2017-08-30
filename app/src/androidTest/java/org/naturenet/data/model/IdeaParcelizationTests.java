package org.naturenet.data.model;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.naturenet.data.model.Idea;

import static org.junit.Assert.assertEquals;

/**
 * Created by rigot on 2/18/2017.
 */

@RunWith(AndroidJUnit4.class)
public class IdeaParcelizationTests {

    final static String ID = "id";
    final static String CONTENT = "content";
    final static String SUBMITTER = "submitter";
    final static String TYPE = "new features";


    @Test
    public void test_is_idea_parcelable(){

        Idea i1 = Idea.createNew(ID, CONTENT, SUBMITTER, TYPE);

        Parcel parcel1 = Parcel.obtain();

        i1.writeToParcel(parcel1, 0);

        parcel1.setDataPosition(0);

        Idea i2 =Idea.CREATOR.createFromParcel(parcel1);

        assertEquals(i1.id, i2.id);
        assertEquals(i1.content, i2.content);
        assertEquals(i1.submitter, i2.submitter);
        assertEquals(i1.status, i2.status);
        assertEquals(i1.group, i2.group);
        assertEquals(i1.type, i2.type);
        assertEquals(i1.image, i2.image);
        assertEquals(i1.source, i2.source);
    }
}
