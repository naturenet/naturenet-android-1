package org.naturenet;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.assertion.ViewAssertions.matches;


import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static android.support.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.naturenet.ui.MainActivity;

@RunWith(AndroidJUnit4.class)
public class DesignIdeasTests {

    final static String DESIGN_IDEAS = "Design Ideas";
    final static String NEW_IDEA = "Gpj36tb7";


    @Rule
    public ActivityTestRule<MainActivity> mMainActivity = new ActivityTestRule<>(MainActivity.class, true, true);

    @Test
    public void submit_idea_test() throws InterruptedException{

        //open nav drawer
        onView(withId(R.id.drawer_layout)).perform(open());

        Thread.sleep(2000);

        //select design ideas
        onView(allOf(withText(DESIGN_IDEAS), withEffectiveVisibility(VISIBLE))).perform(click());

        //click the add idea button
        onView(withId(R.id.fabAddIdea)).perform(click());

        //type an idea in
        onView(withId(R.id.design_idea_text)).perform(typeText(NEW_IDEA), closeSoftKeyboard());

        //click send button
        onView(withId(R.id.design_idea_send_button)).perform(click());

        Thread.sleep(2000);

        //click on the first design idea which should be the one we just created
        onData(anything()).inAdapterView(withId(R.id.design_ideas_lv)).atPosition(0).perform(click());

        //check to see that the idea content matches what we just submitted
        onView(withId(R.id.design_ideas_content)).check(matches(withText(NEW_IDEA)));
    }

}


