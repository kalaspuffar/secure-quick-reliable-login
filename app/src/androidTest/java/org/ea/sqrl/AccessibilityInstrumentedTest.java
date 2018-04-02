package org.ea.sqrl;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.accessibility.AccessibilityChecks;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.ea.sqrl.activites.AdvancedActivity;
import org.ea.sqrl.activites.ClearIdentityActivity;
import org.ea.sqrl.activites.IntroductionActivity;
import org.ea.sqrl.activites.MainActivity;
import org.ea.sqrl.activites.SettingsActivity;
import org.ea.sqrl.activites.ShowIdentityActivity;
import org.ea.sqrl.activites.StartActivity;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Daniel Persson
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AccessibilityInstrumentedTest {
    @Rule
    public ActivityTestRule<AdvancedActivity> advancedActivityRule =
            new ActivityTestRule<>(AdvancedActivity.class, true, false);

    @Rule
    public ActivityTestRule<ClearIdentityActivity> clearIdentityActivityRule =
            new ActivityTestRule<>(ClearIdentityActivity.class, true, false);

    @Rule
    public ActivityTestRule<IntroductionActivity> introductionActivityRule =
            new ActivityTestRule<>(IntroductionActivity.class, true, false);

    @Rule
    public ActivityTestRule<MainActivity> mainActivityRule =
            new ActivityTestRule<>(MainActivity.class, true, false);

    @Rule
    public ActivityTestRule<SettingsActivity> settingsActivityRule =
            new ActivityTestRule<>(SettingsActivity.class, true, false);

    @Rule
    public ActivityTestRule<ShowIdentityActivity> showIdentityActivityRule =
            new ActivityTestRule<>(ShowIdentityActivity.class, true, false);

    @Rule
    public ActivityTestRule<StartActivity> startActivityRule =
            new ActivityTestRule<>(StartActivity.class, true, false);

    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }

    @Test
    public void testAdvancedActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, AdvancedActivity.class);
        advancedActivityRule.launchActivity(intent);
    }

    @Test
    public void testClearIdentityActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, ClearIdentityActivity.class);
        clearIdentityActivityRule.launchActivity(intent);

        onView(withId(R.id.txtClearIdentity)).perform(click());
    }

    @Test
    public void testIntroductionActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, IntroductionActivity.class);
        introductionActivityRule.launchActivity(intent);

        onView(withId(R.id.main_content)).perform(click());
    }

    @Test
    public void testMainActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, MainActivity.class);
        mainActivityRule.launchActivity(intent);

        onView(withId(R.id.mainActivityView)).perform(click());

        mainActivityRule.getActivity()
                .getSupportFragmentManager().beginTransaction();

        onView(withId(R.id.btnRename)).perform(click());
        onView(withId(R.id.btnCloseRename)).perform(click());

        onView(withId(R.id.btnSettings)).perform(click());
        onView(withId(R.id.btnSettingsSave)).perform(click());
    }

    @Test
    public void testSettingsActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, SettingsActivity.class);
        settingsActivityRule.launchActivity(intent);

        onView(withId(R.id.txtSettingsHintLength)).perform(click());
    }

    @Test
    public void testShowIdentityActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, ShowIdentityActivity.class);
        showIdentityActivityRule.launchActivity(intent);

        onView(withId(R.id.txtIdentityText)).perform(click());
    }

    @Test
    public void testStartActivityAccessability() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, StartActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        startActivityRule.launchActivity(intent);

        onView(withId(R.id.startActivityView)).perform(click());
    }
}
