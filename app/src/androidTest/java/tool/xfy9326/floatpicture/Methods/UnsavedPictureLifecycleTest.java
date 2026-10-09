package tool.xfy9326.floatpicture.Methods;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;

import tool.xfy9326.floatpicture.Activities.PictureSettingsActivity;
import tool.xfy9326.floatpicture.Activities.PrivacyPolicyActivity;
import tool.xfy9326.floatpicture.R;
import tool.xfy9326.floatpicture.MainApplication;
import tool.xfy9326.floatpicture.Utils.Config;
import tool.xfy9326.floatpicture.Utils.PictureData;
import tool.xfy9326.floatpicture.View.FloatImageView;
import tool.xfy9326.floatpicture.View.PictureSettingsFragment;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class UnsavedPictureLifecycleTest {
    private Instrumentation instrumentation;
    private Context context;
    private PictureSettingsActivity activity;
    private PrivacyPolicyActivity coveringActivity;
    private PictureSettingsFragment fragment;
    private FloatImageView preview;
    private String pictureId;
    private File testDirectory;

    @Before public void setUp() throws Exception {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        assumeTrue("Run without a loaded UI library",
                ((MainApplication) context.getApplicationContext()).getRegister().isEmpty());
        testDirectory = File.createTempFile("unsaved-preview-", ".tmp", context.getCacheDir());
        assertTrue(testDirectory.delete());
        assertTrue(testDirectory.mkdir());
        Config.initialize(new ContextWrapper(context) {
            @Override public File getFilesDir() { return testDirectory; }
        });
    }

    private void openEditor() {
        activity = (PictureSettingsActivity) instrumentation.startActivitySync(
                new Intent(context, PictureSettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setData(ImportTestProvider.uri("pipe")));
        await(() -> {
            fragment = (PictureSettingsFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.layout_picture_settings_content);
            if (fragment == null) return false;
            preview = (FloatImageView) field("floatImageView");
            pictureId = (String) field("PictureId");
            return preview != null && preview.isAttachedToWindow();
        });
    }

    private Object field(String name) {
        try {
            Field field = PictureSettingsFragment.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(fragment);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private void coverEditor() {
        coveringActivity = (PrivacyPolicyActivity) instrumentation.startActivitySync(
                new Intent(context, PrivacyPolicyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        instrumentation.waitForIdleSync();
    }

    @Test public void switchingAwayHidesUnsavedPreviewAndReturningRestoresIt() {
        openEditor();
        coverEditor();
        instrumentation.runOnMainSync(() -> {
            assertFalse("Unsaved image remained over the other screen", preview.isShown());
            assertFalse(new PictureData().getListArray().containsKey(pictureId));
            coveringActivity.finish();
        });
        await(() -> preview.isShown());
        instrumentation.runOnMainSync(() -> assertTrue(new File(Config.DEFAULT_PICTURE_DIR + pictureId).exists()));
    }

    @Test public void closingBackgroundEditorRemovesWindowAndTemporaryFile() {
        openEditor();
        coverEditor();
        instrumentation.runOnMainSync(activity::finish);
        await(() -> !preview.isAttachedToWindow());
        assertFalse(new File(Config.DEFAULT_PICTURE_DIR + pictureId).exists());
        assertFalse(new PictureData().getListArray().containsKey(pictureId));
    }

    @Test public void homeAndRecentsHideUnsavedPicture() throws Exception {
        openEditor();
        for (int key : new int[]{3, 187}) {
            await(() -> activity.hasWindowFocus());
            long eventTime = SystemClock.uptimeMillis();
            assertTrue(instrumentation.getUiAutomation().injectInputEvent(
                    new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, key, 0), true));
            assertTrue(instrumentation.getUiAutomation().injectInputEvent(
                    new KeyEvent(eventTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, key, 0), true));
            await(() -> !preview.isShown());
            instrumentation.runOnMainSync(() -> {
                assertFalse("Preview visible in Home/Recents", preview.isShown());
                context.startActivity(new Intent(context, PictureSettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            });
            await(() -> fragment.isResumed() && preview.isShown());
        }
    }

    @Test public void editingSavedPictureKeepsItsNormalFloatingBehavior() {
        openEditor();
        instrumentation.runOnMainSync(() -> {
            assertTrue(fragment.saveAllData());
            activity.finish();
        });
        await(activity::isDestroyed);
        instrumentation.runOnMainSync(() -> ManageMethods.prepareWindowForEditing(context, pictureId));
        activity = (PictureSettingsActivity) instrumentation.startActivitySync(
                new Intent(context, PictureSettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(Config.INTENT_PICTURE_EDIT_MODE, true)
                        .putExtra(Config.INTENT_PICTURE_EDIT_ID, pictureId));
        await(() -> {
            fragment = (PictureSettingsFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.layout_picture_settings_content);
            return fragment != null && !((Boolean) field("loadingPicture"));
        });
        coverEditor();
        instrumentation.runOnMainSync(() -> {
            assertTrue(preview.isShown());
            fragment.exit();
            activity.finish();
        });
        await(activity::isDestroyed);
        assertTrue(new File(Config.DEFAULT_PICTURE_DIR + pictureId).exists());
    }

    @Test public void adjustmentWindowAndDialogAreHiddenAndCleanedUp() {
        openEditor();
        instrumentation.runOnMainSync(() -> {
            Preference preference = fragment.findPreference(Config.PREFERENCE_PICTURE_DEGREE);
            preference.getOnPreferenceClickListener().onPreferenceClick(preference);
        });
        FloatImageView[] adjustment = {null};
        AlertDialog[] dialog = {null};
        instrumentation.runOnMainSync(() -> {
            adjustment[0] = (FloatImageView) field("floatImageView_Edit");
            dialog[0] = (AlertDialog) field("currentDialog");
            assertTrue(adjustment[0].isAttachedToWindow());
        });
        coverEditor();
        instrumentation.runOnMainSync(() -> {
            assertFalse(adjustment[0].isShown());
            assertEquals(View.GONE, dialog[0].getWindow().getDecorView().getVisibility());
            activity.finish();
        });
        await(() -> !adjustment[0].isAttachedToWindow());
        assertFalse(preview.isAttachedToWindow());
        assertFalse(new File(Config.DEFAULT_PICTURE_DIR + pictureId).exists());
    }

    @Test public void savedPictureRemainsFloatingAfterEditorCloses() {
        openEditor();
        instrumentation.runOnMainSync(() -> assertTrue(fragment.saveAllData()));
        coverEditor();
        instrumentation.runOnMainSync(() -> {
            assertTrue(preview.isShown());
            activity.finish();
        });
        await(activity::isDestroyed);
        instrumentation.runOnMainSync(() -> {
            assertTrue(preview.isAttachedToWindow());
            assertTrue(preview.isShown());
            assertTrue(new PictureData().getListArray().containsKey(pictureId));
        });
    }

    @Test public void lateImportStaysHiddenWhileEditorIsInBackground() throws Exception {
        openDelayedEditor();
        coverEditor();
        releaseImport();
        await(() -> !((Boolean) field("loadingPicture")));
        instrumentation.runOnMainSync(() -> {
            preview = (FloatImageView) field("floatImageView");
            pictureId = (String) field("PictureId");
            assertNotNull(preview);
            assertFalse("Import completion made a background preview visible", preview.isShown());
        });
    }

    @Test public void closingDuringImportDoesNotLeaveAWindowOrFile() throws Exception {
        openDelayedEditor();
        instrumentation.runOnMainSync(() -> {
            assertFalse("An unfinished import must not be saved", fragment.saveAllData());
            activity.finish();
        });
        await(activity::isDestroyed);
        releaseImport();
        await(() -> !((Boolean) field("loadingPicture")));
        instrumentation.runOnMainSync(() -> {
            assertNull(field("floatImageView"));
            assertNull(field("PictureId"));
            assertTrue(new PictureData().getListArray().isEmpty());
            File[] pictures = new File(Config.DEFAULT_PICTURE_DIR).listFiles(File::isFile);
            assertTrue(pictures == null || pictures.length == 0);
        });
    }

    private void openDelayedEditor() throws Exception {
        context.getContentResolver().call(ImportTestProvider.uri("delayed"), "holdImport", null, null);
        activity = (PictureSettingsActivity) instrumentation.startActivitySync(
                new Intent(context, PictureSettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setData(ImportTestProvider.uri("delayed")));
        assertTrue(context.getContentResolver().call(ImportTestProvider.uri("delayed"),
                "awaitImport", null, null).getBoolean("started"));
        instrumentation.runOnMainSync(() -> fragment = (PictureSettingsFragment) activity
                .getSupportFragmentManager().findFragmentById(R.id.layout_picture_settings_content));
    }

    private interface Condition { boolean ready(); }

    private void await(Condition condition) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        boolean[] ready = {false};
        while (!ready[0] && SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync(() -> ready[0] = condition.ready());
            if (!ready[0]) SystemClock.sleep(50);
        }
        assertTrue("Lifecycle transition did not complete", ready[0]);
    }

    @After public void tearDown() {
        releaseImport();
        instrumentation.runOnMainSync(() -> {
            if (activity != null && !activity.isDestroyed()) activity.finish();
            if (coveringActivity != null) coveringActivity.finish();
        });
        instrumentation.waitForIdleSync();
        if (fragment != null) await(() -> !((Boolean) field("loadingPicture")));
        if (pictureId != null && new PictureData().getListArray().containsKey(pictureId)) {
            instrumentation.runOnMainSync(() -> ManageMethods.DeleteWin(context, pictureId));
        }
        if (testDirectory != null) {
            instrumentation.runOnMainSync(() -> ManageMethods.prepareForDataReload(context));
            Config.initialize(context);
            BackupArchive.deleteTree(testDirectory);
        }
    }

    private void releaseImport() {
        context.getContentResolver().call(ImportTestProvider.uri("delayed"), "releaseImport", null, null);
    }
}
