package org.naturenet;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;


import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.naturenet.ui.MainActivity;


import static org.hamcrest.Matchers.allOf;


@RunWith(AndroidJUnit4.class)
public class DrawerNavigationTest {

    final static String COMMUNITIES = "Communities";
    final static String PROJECTS = "Projects";
    final static String DESIGN_IDEAS = "Design Ideas";
    final static String EXPLORE = "Explore";
    final static String NATURENET = "NatureNet";

    @Rule
    public ActivityTestRule<MainActivity> mMainActivity = new ActivityTestRule<>(MainActivity.class, true, true);

    @Test
    public void navigation_test() throws InterruptedException {


        //open the navigation drawer
        onView(withId(R.id.drawer_layout)).perform(open());

        //click on design ideas link
        onView(allOf(withText(DESIGN_IDEAS), withEffectiveVisibility(VISIBLE))).perform(click());

        //make sure we are on the correct screen
        onView(withId(R.id.app_bar_main_tv)).check(matches(withText(DESIGN_IDEAS)));

        //open the navigation drawer
        onView(withId(R.id.drawer_layout)).perform(open());

        //click on communities link
        onView(withText(COMMUNITIES)).perform(click());

        //make sure we're on the correct screen
        onView(withId(R.id.app_bar_main_tv)).check(matches(withText(COMMUNITIES)));

        //open the navigation drawer
        onView(withId(R.id.drawer_layout)).perform(open());

        //click on projects link
        onView(withText(PROJECTS)).perform(click());

        //make sure we are indeed in the projects screen
        onView(withId(R.id.app_bar_main_tv)).check(matches(withText(PROJECTS)));

        //open navigation drawer
        onView(withId(R.id.drawer_layout)).perform(open());

        //click on Explore link
        onView(withText(EXPLORE)).perform(click());

        //let map load
        Thread.sleep(2000);

        //make sure we're on the correct screen
        onView(withId(R.id.app_bar_main_tv)).check(matches(withText(NATURENET)));

    }

}
