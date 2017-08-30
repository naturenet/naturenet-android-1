package org.naturenet;


import android.support.test.espresso.NoMatchingViewException;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.naturenet.ui.MainActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class ProjectsTests {

    private final static String PROJECTS = "Projects";
    private final static String FREE_OBSERVATION = "Free Observation";
    private final static String NATIVE_OR_NOT = "Native or Not?";
    private final static String RUNOFF_RUNDOWN = "Runoff Rundown";
    private final static String NEW_PROJECT = "New Project";
    private final static String NEW_PROJECT_DESCRIPTION = "Description";

    @Rule
    public ActivityTestRule<MainActivity> mMainActivity = new ActivityTestRule<>(MainActivity.class, true, true);

    @Test
    public void projects_navigation_test() throws InterruptedException {

        //Open navigation drawer
        onView(withId(R.id.drawer_layout)).perform(open());

        //Click on projects link
        onView(withText(PROJECTS)).perform(click());

        Thread.sleep(2000);

        String[] sites = {"Aspen", "Anacostia", "Elsewhere", "Reedy Creek"};
        int index = 0;

        while(true){

            //Expand all sites to show all projects
            try{
                //Click on the first 'load more' button. if no exception is thrown, we know there is a 'show more' button on the screen
                onView(withIndex(withText("Load More"), 0)).perform(click());

                Thread.sleep(1000);
            }catch (NoMatchingViewException e){
                //If a NoMatchingViewException is throw, we must click on the next Site
                index++;
                if(index == sites.length)
                    break;

                try{
                    //Check to see if there are two views with the same site text displayed
                    //If so, no exception will be thrown and we will know the user's affiliation is the same as the site we're trying to click
                    onView(withIndex(withText(sites[index]), 1)).check(matches(isDisplayed()));
                    //Therefore, we click the second one which should be the one in the ExpandableListView
                    onView(withIndex(withText(sites[index]), 1)).perform(click());
                }catch (NoMatchingViewException ex){
                    //If an exception is thrown, we know there's only one view with the text we're trying to click
                    onView(withText(sites[index])).perform(click());
                }

                Thread.sleep(1000);
            }

        }

        //At this point, we've clicked through all the sites and viewed the projects under each site.
        //Now, we test the search function
        onView(withId(R.id.searchProjectText)).perform(typeText(FREE_OBSERVATION), closeSoftKeyboard());

        //Click on the first search result
        onView(withIndex(withText(FREE_OBSERVATION), 1)).perform(click());

        //Make sure the correct project is being displayed
        onView(withId(R.id.project_tv_name)).check(matches(withText(FREE_OBSERVATION)));

        pressBack();

        //Search for Native or Not
        onView(withId(R.id.searchProjectText)).perform(replaceText(NATIVE_OR_NOT), closeSoftKeyboard());

        //Click on the first result
        onView(withIndex(withText(NATIVE_OR_NOT), 1)).perform(click());

        //Make sure the correct project is being displayed
        onView(withId(R.id.project_tv_name)).check(matches(withText(NATIVE_OR_NOT)));

        pressBack();

        //Search for Runoff Rundown
        onView(withId(R.id.searchProjectText)).perform(replaceText(RUNOFF_RUNDOWN), closeSoftKeyboard());

        //Click on the first result
        onView(withIndex(withText(RUNOFF_RUNDOWN), 1)).perform(click());

        //Make sure the correct project is being displayed
        onView(withId(R.id.project_tv_name)).check(matches(withText(RUNOFF_RUNDOWN)));

        pressBack();
    }

    @Test
    public void submit_project_test() throws InterruptedException {

        //Open navigation drawer
        onView(withId(R.id.drawer_layout)).perform(open());

        //Click on projects link
        onView(withText(PROJECTS)).perform(click());

        Thread.sleep(2000);

        onView(withId(R.id.fabAddProject)).perform(click());

        //Type project name
        onView(withId(R.id.projectTitle)).perform(typeText(NEW_PROJECT), closeSoftKeyboard());

        //Type project description
        onView(withId(R.id.projectDescription)).perform(typeText(NEW_PROJECT_DESCRIPTION), closeSoftKeyboard());

        //Select Elsewhere site
        onView(withId(R.id.elseSwitchButton)).perform(click());

        //Click submit
        onView(withId(R.id.projectSubmitButton)).perform(click());

        onView(withText("Yes")).perform(click());

        Thread.sleep(2000);

        //Now search for the project we just created
        onView(withId(R.id.searchProjectText)).perform(typeText(NEW_PROJECT), closeSoftKeyboard());

        //It should show up as a result. Click
        onView(withIndex(withText(NEW_PROJECT), 1)).perform(click());

        //Check that we're on the correct project screen
        onView(withId(R.id.project_tv_name)).check(matches(withText(NEW_PROJECT)));


    }


    /*
    CUSTOM MATCHERS
     */

    public static Matcher<View> withIndex(final Matcher<View> matcher, final int index) {
        return new TypeSafeMatcher<View>() {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description) {
                description.appendText("with index: ");
                description.appendValue(index);
                matcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                return matcher.matches(view) && currentIndex++ == index;
            }
        };
    }
}
