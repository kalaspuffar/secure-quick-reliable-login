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
import org.ea.sqrl.activites.CreateIdentityActivity;
import org.ea.sqrl.activites.EntropyGatherActivity;
import org.ea.sqrl.activites.IntroductionActivity;
import org.ea.sqrl.activites.MainActivity;
import org.ea.sqrl.activites.NewIdentityDoneActivity;
import org.ea.sqrl.activites.RekeyIdentityActivity;
import org.ea.sqrl.activites.RekeyVerifyActivity;
import org.ea.sqrl.activites.RescueCodeEnterActivity;
import org.ea.sqrl.activites.RescueCodeShowActivity;
import org.ea.sqrl.activites.SaveIdentityActivity;
import org.ea.sqrl.activites.SettingsActivity;
import org.ea.sqrl.activites.ShowIdentityActivity;
import org.ea.sqrl.activites.StartActivity;
import org.ea.sqrl.activites.UrlLoginActivity;
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
    public ActivityTestRule<CreateIdentityActivity> createIdentityActivityRule =
            new ActivityTestRule<>(CreateIdentityActivity.class, true, false);

    @Rule
    public ActivityTestRule<EntropyGatherActivity> entropyGatherActivityRule =
            new ActivityTestRule<>(EntropyGatherActivity.class, true, false);

    @Rule
    public ActivityTestRule<IntroductionActivity> introductionActivityRule =
            new ActivityTestRule<>(IntroductionActivity.class, true, false);

    @Rule
    public ActivityTestRule<MainActivity> mainActivityRule =
            new ActivityTestRule<>(MainActivity.class, true, false);

    @Rule
    public ActivityTestRule<NewIdentityDoneActivity> newIdentityDoneActivityRule =
            new ActivityTestRule<>(NewIdentityDoneActivity.class, true, false);

    @Rule
    public ActivityTestRule<RekeyIdentityActivity> rekeyIdentityActivityRule =
            new ActivityTestRule<>(RekeyIdentityActivity.class, true, false);

    @Rule
    public ActivityTestRule<RekeyVerifyActivity> rekeyVerifyActivityRule =
            new ActivityTestRule<>(RekeyVerifyActivity.class, true, false);

    @Rule
    public ActivityTestRule<RescueCodeEnterActivity> rescueCodeEnterActivityRule =
            new ActivityTestRule<>(RescueCodeEnterActivity.class, true, false);

    @Rule
    public ActivityTestRule<RescueCodeShowActivity> rescueCodeShowActivityRule =
            new ActivityTestRule<>(RescueCodeShowActivity.class, true, false);

    @Rule
    public ActivityTestRule<SaveIdentityActivity> saveIdentityActivityRule =
            new ActivityTestRule<>(SaveIdentityActivity.class, true, false);

    @Rule
    public ActivityTestRule<SettingsActivity> settingsActivityRule =
            new ActivityTestRule<>(SettingsActivity.class, true, false);

    @Rule
    public ActivityTestRule<ShowIdentityActivity> showIdentityActivityRule =
            new ActivityTestRule<>(ShowIdentityActivity.class, true, false);

    @Rule
    public ActivityTestRule<StartActivity> startActivityRule =
            new ActivityTestRule<>(StartActivity.class, true, false);

    @Rule
    public ActivityTestRule<UrlLoginActivity> urlLoginActivityRule =
            new ActivityTestRule<>(UrlLoginActivity.class, true, false);


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

        onView(withId(R.id.advancedActivityView)).perform(click());
    }

    @Test
    public void testClearIdentityActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, ClearIdentityActivity.class);
        clearIdentityActivityRule.launchActivity(intent);

        onView(withId(R.id.clearIdentityActivityView)).perform(click());
    }

    @Test
    public void testCreateIdentityActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, CreateIdentityActivity.class);
        createIdentityActivityRule.launchActivity(intent);

        onView(withId(R.id.createIdentityActivityView)).perform(click());
    }

    @Test
    public void testEntropyGatherActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, EntropyGatherActivity.class);
        entropyGatherActivityRule.launchActivity(intent);

        onView(withId(R.id.entropyGatherActivityView)).perform(click());
    }

    @Test
    public void testIntroductionActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, IntroductionActivity.class);
        introductionActivityRule.launchActivity(intent);

        onView(withId(R.id.introductionActivityView)).perform(click());
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

        onView(withId(R.id.btnSettings)).perform(click());
        onView(withId(R.id.btnSettingsSave)).perform(click());
    }

    @Test
    public void testNewIdentityDoneActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, NewIdentityDoneActivity.class);
        newIdentityDoneActivityRule.launchActivity(intent);

        onView(withId(R.id.newIdentityDoneActivityView)).perform(click());
    }

    @Test
    public void testRekeyIdentityActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, RekeyIdentityActivity.class);
        rekeyIdentityActivityRule.launchActivity(intent);

        onView(withId(R.id.rekeyIdentityActivityView)).perform(click());
    }

    @Test
    public void testRekeyVerifyActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, RekeyVerifyActivity.class);
        rekeyVerifyActivityRule.launchActivity(intent);

        onView(withId(R.id.rekeyVerifyActivityView)).perform(click());
    }

    @Test
    public void testRescueCodeEnterActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, RescueCodeEnterActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        rescueCodeEnterActivityRule.launchActivity(intent);

        onView(withId(R.id.rescueCodeEntryActivityView)).perform(click());
    }

    @Test
    public void testRescueCodeShowActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, RescueCodeShowActivity.class);
        rescueCodeShowActivityRule.launchActivity(intent);

        onView(withId(R.id.rescueCodeShowActivityView)).perform(click());
    }

    @Test
    public void testSaveIdentityShowActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, SaveIdentityActivity.class);
        saveIdentityActivityRule.launchActivity(intent);

        onView(withId(R.id.saveIdentityActivityView)).perform(click());
    }

    @Test
    public void testSettingsActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, SettingsActivity.class);
        settingsActivityRule.launchActivity(intent);

        onView(withId(R.id.settingsActivityView)).perform(click());
    }

    @Test
    public void testShowIdentityActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, ShowIdentityActivity.class);
        showIdentityActivityRule.launchActivity(intent);

        onView(withId(R.id.showIdentityActivityView)).perform(click());
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

    @Test
    public void testUrlLoginActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, UrlLoginActivity.class);
        urlLoginActivityRule.launchActivity(intent);

        onView(withId(R.id.urlLoginActivityView)).perform(click());
    }
}
