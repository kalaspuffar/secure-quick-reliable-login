package org.ea.sqrl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.accessibility.AccessibilityChecks;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.WindowManager;

import org.ea.sqrl.activites.identity.IdentityManagementActivity;
import org.ea.sqrl.activites.LanguageActivity;
import org.ea.sqrl.activites.SimplifiedActivity;
import org.ea.sqrl.activites.account.AccountOptionsActivity;
import org.ea.sqrl.activites.account.DisableAccountActivity;
import org.ea.sqrl.activites.account.EnableAccountActivity;
import org.ea.sqrl.activites.account.RemoveAccountActivity;
import org.ea.sqrl.activites.identity.ChangePasswordActivity;
import org.ea.sqrl.activites.identity.ClearIdentityActivity;
import org.ea.sqrl.activites.create.CreateIdentityActivity;
import org.ea.sqrl.activites.create.EntropyGatherActivity;
import org.ea.sqrl.activites.IntroductionActivity;
import org.ea.sqrl.activites.create.NewIdentityDoneActivity;
import org.ea.sqrl.activites.create.RekeyIdentityActivity;
import org.ea.sqrl.activites.create.RekeyVerifyActivity;
import org.ea.sqrl.activites.create.RescueCodeEnterActivity;
import org.ea.sqrl.activites.create.RescueCodeShowActivity;
import org.ea.sqrl.activites.create.SaveIdentityActivity;
import org.ea.sqrl.activites.identity.IdentitySettingsActivity;
import org.ea.sqrl.activites.identity.ExportOptionsActivity;
import org.ea.sqrl.activites.identity.ImportActivity;
import org.ea.sqrl.activites.identity.RenameActivity;
import org.ea.sqrl.activites.identity.ResetPasswordActivity;
import org.ea.sqrl.activites.identity.ShowIdentityActivity;
import org.ea.sqrl.activites.StartActivity;
import org.ea.sqrl.activites.LoginActivity;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 *
 * @author Daniel Persson
 */
@RunWith(AndroidJUnit4.class)
public class AccessibilityInstrumentedTest {
    @Rule
    public ActivityTestRule<SimplifiedActivity> simplifiedActivityTestRule =
            new ActivityTestRule<>(SimplifiedActivity.class, true, false);

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
    public ActivityTestRule<IdentityManagementActivity> identityManagementActivityRule =
            new ActivityTestRule<>(IdentityManagementActivity.class, true, false);

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
    public ActivityTestRule<IdentitySettingsActivity> identitySettingsActivityRule =
            new ActivityTestRule<>(IdentitySettingsActivity.class, true, false);

    @Rule
    public ActivityTestRule<ShowIdentityActivity> showIdentityActivityRule =
            new ActivityTestRule<>(ShowIdentityActivity.class, true, false);

    @Rule
    public ActivityTestRule<StartActivity> startActivityRule =
            new ActivityTestRule<>(StartActivity.class, true, false);

    @Rule
    public ActivityTestRule<LoginActivity> loginActivityRule =
            new ActivityTestRule<>(LoginActivity.class, true, false);

    @Rule
    public ActivityTestRule<ChangePasswordActivity> changePasswordActivityRule =
            new ActivityTestRule<>(ChangePasswordActivity.class, true, false);

    @Rule
    public ActivityTestRule<ResetPasswordActivity> resetPasswordActivityRule =
            new ActivityTestRule<>(ResetPasswordActivity.class, true, false);

    @Rule
    public ActivityTestRule<DisableAccountActivity> disableAccountActivityRule =
            new ActivityTestRule<>(DisableAccountActivity.class, true, false);

    @Rule
    public ActivityTestRule<EnableAccountActivity> enableAccountActivityRule =
            new ActivityTestRule<>(EnableAccountActivity.class, true, false);

    @Rule
    public ActivityTestRule<RemoveAccountActivity> removeAccountActivityRule =
            new ActivityTestRule<>(RemoveAccountActivity.class, true, false);

    @Rule
    public ActivityTestRule<RenameActivity> renameIdentityActivityRule =
            new ActivityTestRule<>(RenameActivity.class, true, false);

    @Rule
    public ActivityTestRule<ExportOptionsActivity> exportOptionsActivityRule =
            new ActivityTestRule<>(ExportOptionsActivity.class, true, false);

    @Rule
    public ActivityTestRule<ImportActivity> importActivityRule =
            new ActivityTestRule<>(ImportActivity.class, true, false);

    @Rule
    public ActivityTestRule<AccountOptionsActivity> accountOptionsActivityRule =
            new ActivityTestRule<>(AccountOptionsActivity.class, true, false);

    @Rule
    public ActivityTestRule<LanguageActivity> languageActivityRule =
            new ActivityTestRule<>(LanguageActivity.class, true, false);

    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }

    public void unlockScreen(Activity activity) {
        Runnable wakeUpDevice = () -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        activity.runOnUiThread(wakeUpDevice);
    }

    @Test
    public void testAdvancedActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, SimplifiedActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        SimplifiedActivity a = simplifiedActivityTestRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.simplifiedActivityView)).perform(click());
    }

    @Test
//    @Ignore // Still failing test, not important as it don't have that many elements.
    public void testClearIdentityActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, ClearIdentityActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        ClearIdentityActivity a = clearIdentityActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.clearIdentityActivityView)).perform(click());
    }

    @Test
    public void testCreateIdentityActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, CreateIdentityActivity.class);
        CreateIdentityActivity a = createIdentityActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.createIdentityActivityView)).perform(click());
    }

    @Test
    public void testEntropyGatherActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, EntropyGatherActivity.class);
        EntropyGatherActivity a = entropyGatherActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.entropyGatherActivityView)).perform(click());
    }

    @Test
    public void testIntroductionActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, IntroductionActivity.class);
        IntroductionActivity a = introductionActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.introductionActivityView)).perform(click());
    }

    @Test
    public void testMainActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, IdentityManagementActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        IdentityManagementActivity a = identityManagementActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.identityManagementActivityView)).perform(click());
    }

    @Test
    public void testNewIdentityDoneActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, NewIdentityDoneActivity.class);
        NewIdentityDoneActivity a = newIdentityDoneActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.newIdentityDoneActivityView)).perform(click());
    }

    @Test
    public void testRekeyIdentityActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, RekeyIdentityActivity.class);
        RekeyIdentityActivity a = rekeyIdentityActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.rekeyIdentityActivityView)).perform(click());
    }

    @Test
    public void testRekeyVerifyActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, RekeyVerifyActivity.class);
        RekeyVerifyActivity a = rekeyVerifyActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.rekeyVerifyActivityView)).perform(click());
    }

    @Test
    public void testRescueCodeEnterActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, RescueCodeEnterActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        RescueCodeEnterActivity a = rescueCodeEnterActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.rescueCodeEntryActivityView)).perform(click());
    }

    @Test
    public void testRescueCodeShowActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, RescueCodeShowActivity.class);
        RescueCodeShowActivity a = rescueCodeShowActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.rescueCodeShowActivityView)).perform(click());
    }

    @Test
    public void testSaveIdentityShowActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, SaveIdentityActivity.class);
        SaveIdentityActivity a = saveIdentityActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.saveIdentityActivityView)).perform(click());
    }

    @Test
    public void testIdentitySettingsActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, IdentitySettingsActivity.class);
        IdentitySettingsActivity a = identitySettingsActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.identitySettingsActivityView)).perform(click());
    }

    @Test
    public void testShowIdentityActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, ShowIdentityActivity.class);
        ShowIdentityActivity a = showIdentityActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.showIdentityActivityView)).perform(click());
    }

    @Test
    public void testStartActivityAccessability() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, StartActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        StartActivity a = startActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.startActivityView)).perform(click());
    }

    @Test
    public void testLoginActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, LoginActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        LoginActivity a = loginActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.loginActivityView)).perform(click());
    }

    @Test
    public void changePasswordActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, ChangePasswordActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        ChangePasswordActivity a = changePasswordActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.changePasswordActivityView)).perform(click());
    }

    @Test
    public void resetPasswordActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, ResetPasswordActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        ResetPasswordActivity a = resetPasswordActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.resetPasswordActivityView)).perform(click());
    }

    @Test
    public void disableAccountActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, DisableAccountActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        DisableAccountActivity a = disableAccountActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.disableAccountActivityView)).perform(click());
    }

    @Test
    public void enableAccountActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, EnableAccountActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        EnableAccountActivity a = enableAccountActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.enableAccountActivityView)).perform(click());
    }

    @Test
    public void removeAccountActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, RemoveAccountActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        RemoveAccountActivity a = removeAccountActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.removeAccountActivityView)).perform(click());
    }

    @Test
    public void renameIdentityActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, RenameActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        RenameActivity a = renameIdentityActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.renameActivityView)).perform(click());
    }

    @Test
    public void exportOptionsActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, ExportOptionsActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        ExportOptionsActivity a = exportOptionsActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.exportOptionsActivityView)).perform(click());
    }

    @Test
    public void importActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, ImportActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        ImportActivity a = importActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.importActivityView)).perform(click());
    }

    @Test
    public void accountOptionsActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, AccountOptionsActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        AccountOptionsActivity a = accountOptionsActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.accountOptionsActivityView)).perform(click());
    }

    @Test
    public void languageActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, LanguageActivity.class);
        intent.putExtra("RUNNING_TEST", true);
        LanguageActivity a = languageActivityRule.launchActivity(intent);
        unlockScreen(a);

        onView(withId(R.id.languageActivityView)).perform(click());
    }
}
