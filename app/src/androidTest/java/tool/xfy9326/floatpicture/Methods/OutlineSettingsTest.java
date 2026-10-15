package tool.xfy9326.floatpicture.Methods;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ContentUris;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.SystemClock;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import tool.xfy9326.floatpicture.Activities.PictureSettingsActivity;
import tool.xfy9326.floatpicture.MainApplication;
import tool.xfy9326.floatpicture.R;
import tool.xfy9326.floatpicture.Utils.Config;
import tool.xfy9326.floatpicture.Utils.PictureData;
import tool.xfy9326.floatpicture.View.PictureSettingsFragment;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Uses an isolated library so the emulator's existing pictures stay untouched. */
@RunWith(AndroidJUnit4.class)
public class OutlineSettingsTest {
    private static final String PICTURE_ID = "outline-settings-regression";
    private Instrumentation instrumentation;
    private Context context;
    private File testDirectory;
    private PictureSettingsActivity activity;
    private PictureSettingsFragment fragment;
    private AlertDialog dialog;
    private Set<Uri> originalGalleryItems;

    @Before public void setUp() throws Exception {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        if (Build.VERSION.SDK_INT >= 29) originalGalleryItems = new HashSet<>(galleryItems());
        assumeTrue("Run without a loaded UI library",
                ((MainApplication) context.getApplicationContext()).getRegister().isEmpty());
        testDirectory = File.createTempFile("outline-settings-", ".tmp", context.getCacheDir());
        assertTrue(testDirectory.delete());
        assertTrue(testDirectory.mkdir());
        Config.initialize(new ContextWrapper(context) {
            @Override public File getFilesDir() { return testDirectory; }
        });
    }

    private void createPicture(String id) {
        Bitmap source = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888);
        source.eraseColor(Color.CYAN);
        assertTrue(IOMethods.replaceBitmap(source, 100, Config.DEFAULT_PICTURE_DIR + id));
        source.recycle();
        PictureData data = new PictureData();
        data.setDataControl(id);
        data.put(Config.DATA_PICTURE_SHOW_ENABLED, false);
        data.commit(id);
    }

    private void openEditor(String id) {
        Intent intent = new Intent(context, PictureSettingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (id == null) {
            intent.setData(ImportTestProvider.uri("pipe"));
        } else {
            instrumentation.runOnMainSync(() -> ManageMethods.ensureWindowsInitialized(context));
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> ManageMethods.prepareWindowForEditing(context, id));
            instrumentation.waitForIdleSync();
            intent.putExtra(Config.INTENT_PICTURE_EDIT_MODE, true)
                    .putExtra(Config.INTENT_PICTURE_EDIT_ID, id);
        }
        activity = (PictureSettingsActivity) instrumentation.startActivitySync(intent);
        await(() -> {
            fragment = (PictureSettingsFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.layout_picture_settings_content);
            Preference preference = fragment == null ? null
                    : fragment.findPreference(Config.PREFERENCE_PICTURE_OUTLINE);
            return preference != null && preference.getOnPreferenceClickListener() != null;
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

    private void openOutline() {
        instrumentation.runOnMainSync(() -> {
            Preference preference = fragment.findPreference(Config.PREFERENCE_PICTURE_OUTLINE);
            preference.getOnPreferenceClickListener().onPreferenceClick(preference);
            dialog = (AlertDialog) field("currentDialog");
            assertTrue(dialog.isShowing());
        });
    }

    private void assertControls(int radius, int contrast, int colorIndex, boolean gray) {
        instrumentation.runOnMainSync(() -> {
            assertEquals(radius - 1, ((SeekBar) dialog.findViewById(R.id.seek_outline_detail)).getProgress());
            assertEquals(contrast, ((SeekBar) dialog.findViewById(R.id.seek_outline_contrast)).getProgress());
            assertEquals(colorIndex, ((Spinner) dialog.findViewById(R.id.spinner_outline_color)).getSelectedItemPosition());
            assertEquals(gray, ((CheckBox) dialog.findViewById(R.id.check_outline_gray_background)).isChecked());
        });
    }

    private void setControls(int radius, int contrast, int colorIndex, boolean gray) {
        instrumentation.runOnMainSync(() -> {
            ((SeekBar) dialog.findViewById(R.id.seek_outline_detail)).setProgress(radius - 1);
            ((SeekBar) dialog.findViewById(R.id.seek_outline_contrast)).setProgress(contrast);
            ((Spinner) dialog.findViewById(R.id.spinner_outline_color)).setSelection(colorIndex);
            ((CheckBox) dialog.findViewById(R.id.check_outline_gray_background)).setChecked(gray);
        });
    }

    private void applyOutline() {
        instrumentation.runOnMainSync(() -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        await(() -> !dialog.isShowing());
    }

    private void closeEditor() {
        instrumentation.runOnMainSync(() -> {
            if (dialog != null && dialog.isShowing()) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            }
            activity.finish();
        });
        await(activity::isDestroyed);
        dialog = null;
    }

    @Test public void freshPictureDefaultsToTransparentBackground() {
        createPicture(PICTURE_ID);
        openEditor(PICTURE_ID);
        openOutline();
        assertControls(2, 65, 0, false);
    }

    @Test public void appliedSettingsSurviveReopeningAndCancelDoesNotOverwriteThem() {
        createPicture(PICTURE_ID);
        createPicture("another-picture");
        openEditor(PICTURE_ID);
        openOutline();
        setControls(3, 42, 3, true);
        applyOutline();
        openOutline();
        assertControls(3, 42, 3, true);
        setControls(8, 100, 5, false);
        instrumentation.runOnMainSync(() -> dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
        openOutline();
        assertControls(3, 42, 3, true);
        closeEditor();
        // A fresh editor loads the persisted JSON, rather than fragment memory.
        openEditor(PICTURE_ID);
        openOutline();
        assertControls(3, 42, 3, true);
        setControls(1, 0, 4, false);
        applyOutline();
        closeEditor();
        openEditor(PICTURE_ID);
        openOutline();
        assertControls(1, 0, 4, false);
        closeEditor();
        openEditor("another-picture");
        openOutline();
        assertControls(2, 65, 0, false);
    }

    @Test public void newImportKeepsAppliedSettingsWhenAddedToLibrary() {
        openEditor(null);
        String id = (String) field("PictureId");
        openOutline();
        setControls(3, 70, 2, false);
        applyOutline();
        assertFalse(new PictureData().getListArray().containsKey(id));
        openOutline();
        assertControls(3, 70, 2, false);
        instrumentation.runOnMainSync(() -> {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            assertTrue(fragment.saveAllData());
        });
        closeEditor();
        openEditor(id);
        openOutline();
        assertControls(3, 70, 2, false);
    }

    private ArrayList<Uri> galleryItems() {
        ArrayList<Uri> result = new ArrayList<>();
        try (Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DISPLAY_NAME + " LIKE ?",
                new String[]{"FloatPicture_%.png"}, null)) {
            assertNotNull(cursor);
            while (cursor.moveToNext()) result.add(ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0)));
        }
        return result;
    }

    private void createExportPicture() {
        createPicture(PICTURE_ID);
        Bitmap source = Bitmap.createBitmap(1024, 96, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[1024 * 96];
        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 1024; x++) {
                int level = (x % 48) * 5;
                pixels[y * 1024 + x] = Color.rgb(level, y * 2, 255 - level);
            }
        }
        source.setPixels(pixels, 0, 1024, 0, 0, 1024, 96);
        assertTrue(IOMethods.replaceBitmap(source, 100, Config.DEFAULT_PICTURE_DIR + PICTURE_ID));
        source.recycle();
    }

    private void exportAndAssert(Bitmap source, int radius, int contrast, int color, boolean gray) throws Exception {
        Bitmap expected = ImageMethods.createOutline(source, radius, contrast, color, gray);
        assertNotNull(expected);
        instrumentation.runOnMainSync(() -> {
            assertTrue(dialog.findViewById(R.id.button_outline_export).performClick());
            assertFalse("Prevent duplicate exports", dialog.findViewById(R.id.button_outline_export).isEnabled());
        });
        await(() -> !dialog.isShowing());
        ArrayList<Uri> added = galleryItems();
        added.removeAll(originalGalleryItems);
        assertEquals("Exactly one exported image", 1, added.size());
        Uri uri = added.get(0);
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{
                MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.IS_PENDING}, null, null, null)) {
            assertNotNull(cursor);
            assertTrue(cursor.moveToFirst());
            assertTrue(cursor.getString(0).endsWith(".png"));
            assertEquals("image/png", cursor.getString(1));
            assertEquals("Pictures/FloatPicture/", cursor.getString(2));
            assertEquals("Visible in gallery", 0, cursor.getInt(3));
        }
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            Bitmap exported = BitmapFactory.decodeStream(input);
            assertNotNull(exported);
            assertEquals(source.getWidth(), exported.getWidth());
            assertEquals(source.getHeight(), exported.getHeight());
            assertTrue("Export must use the current controls", expected.sameAs(exported));
            exported.recycle();
        }
        instrumentation.runOnMainSync(() -> assertTrue("Current picture must also be updated",
                expected.sameAs((Bitmap) field("bitmap"))));
        expected.recycle();
    }

    @Test @SdkSuppress(minSdkVersion = 29)
    public void exportAppliesChangedControlsWithoutPressingApplyAndUsesFullSizePng() throws Exception {
        createExportPicture();
        openEditor(PICTURE_ID);
        Bitmap source = ImageMethods.getShowBitmap(context, PICTURE_ID);
        openOutline();
        setControls(4, 90, 0, false);
        exportAndAssert(source, 4, 90, ImageMethods.OUTLINE_RED, false);
        source.recycle();
        closeEditor();
        openEditor(PICTURE_ID);
        openOutline();
        assertControls(4, 90, 0, false);
    }

    @Test @SdkSuppress(minSdkVersion = 29)
    public void exportRegeneratesPreviouslyAppliedOutlineWithNewColorAndBackground() throws Exception {
        createExportPicture();
        openEditor(PICTURE_ID);
        openOutline();
        setControls(2, 35, 0, false);
        applyOutline();
        Bitmap source = ImageMethods.getOutlineSourceBitmap(PICTURE_ID);
        openOutline();
        setControls(6, 80, 2, true);
        exportAndAssert(source, 6, 80, ImageMethods.OUTLINE_BLUE, true);
        source.recycle();
        openOutline();
        assertControls(6, 80, 2, true);
    }

    @Test @SdkSuppress(minSdkVersion = 29)
    public void exportNewImportUsesChangedParametersAndCanThenSaveToLibrary() throws Exception {
        openEditor(null);
        String id = (String) field("PictureId");
        Bitmap source = ImageMethods.getShowBitmap(context, id);
        openOutline();
        setControls(4, 90, 3, false);
        exportAndAssert(source, 4, 90, ImageMethods.OUTLINE_CYAN, false);
        source.recycle();
        assertFalse(new PictureData().getListArray().containsKey(id));
        instrumentation.runOnMainSync(() -> assertTrue(fragment.saveAllData()));
        closeEditor();
        openEditor(id);
        openOutline();
        assertControls(4, 90, 3, false);
    }

    @Test @SdkSuppress(minSdkVersion = 29)
    public void failedApplicationDoesNotExportOrSaveChangedParameters() {
        createExportPicture();
        openEditor(PICTURE_ID);
        openOutline();
        setControls(4, 90, 0, false);
        String pictureDirectory = Config.DEFAULT_PICTURE_DIR;
        // Point writes at an existing file so creating the source directory fails.
        Config.DEFAULT_PICTURE_DIR = pictureDirectory + PICTURE_ID + "/";
        try {
            instrumentation.runOnMainSync(() -> dialog.findViewById(R.id.button_outline_export).performClick());
            await(() -> dialog.findViewById(R.id.button_outline_export).isEnabled());
            assertTrue(dialog.isShowing());
            assertEquals(originalGalleryItems, new HashSet<>(galleryItems()));
            PictureData data = new PictureData();
            data.setDataControl(PICTURE_ID);
            assertFalse(data.contains(Config.DATA_PICTURE_OUTLINE_RADIUS));
        } finally {
            Config.DEFAULT_PICTURE_DIR = pictureDirectory;
        }
    }

    @Test @SdkSuppress(minSdkVersion = 29)
    public void failedGalleryWriteCleansPendingImageAndAllowsRetry() throws Exception {
        createExportPicture();
        openEditor(PICTURE_ID);
        Bitmap expectedSource = ImageMethods.getShowBitmap(context, PICTURE_ID);
        Bitmap original = (Bitmap) field("bitmap");
        openOutline();
        setControls(4, 90, 0, false);
        boolean[] cleaned = {false};
        ContentProvider provider = new ContentProvider() {
            @Override public boolean onCreate() { return true; }
            @Override public Cursor query(Uri uri, String[] projection, String selection,
                                          String[] args, String order) { return null; }
            @Override public String getType(Uri uri) { return "image/png"; }
            @Override public Uri insert(Uri uri, ContentValues values) {
                assertEquals(Integer.valueOf(1), values.getAsInteger(MediaStore.Images.Media.IS_PENDING));
                return Uri.parse("content://outline-export-failure/image");
            }
            @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
                throw new FileNotFoundException("Simulated full storage");
            }
            @Override public int delete(Uri uri, String selection, String[] args) {
                cleaned[0] = true;
                return 1;
            }
            @Override public int update(Uri uri, ContentValues values, String selection, String[] args) {
                fail("Incomplete image must never be published");
                return 0;
            }
        };
        ProviderInfo info = new ProviderInfo();
        info.authority = "outline-export-failure";
        provider.attachInfo(context, info);
        ContentResolver resolver = ContentResolver.wrap(provider);
        Context failingContext = new ContextWrapper(context) {
            @Override public ContentResolver getContentResolver() { return resolver; }
        };
        Method apply = PictureSettingsFragment.class.getDeclaredMethod("applyOutline",
                Bitmap.class, int.class, int.class, int.class, boolean.class, boolean.class,
                Context.class, AlertDialog.class, ProgressBar.class, SeekBar.class, SeekBar.class,
                Spinner.class, CheckBox.class);
        apply.setAccessible(true);
        instrumentation.runOnMainSync(() -> dialog.findViewById(R.id.button_outline_export).setEnabled(false));
        apply.invoke(fragment, original, 4, 90, ImageMethods.OUTLINE_RED, false, true,
                failingContext, dialog, dialog.findViewById(R.id.progress_outline),
                dialog.findViewById(R.id.seek_outline_detail), dialog.findViewById(R.id.seek_outline_contrast),
                dialog.findViewById(R.id.spinner_outline_color), dialog.findViewById(R.id.check_outline_gray_background));
        await(() -> dialog.findViewById(R.id.button_outline_export).isEnabled());
        assertTrue(cleaned[0]);
        assertTrue(dialog.isShowing());
        assertFalse("Retry must retain the source bitmap", original.isRecycled());
        exportAndAssert(expectedSource, 4, 90, ImageMethods.OUTLINE_RED, false);
        expectedSource.recycle();
    }

    private interface Condition { boolean ready(); }

    private void await(Condition condition) {
        long deadline = SystemClock.uptimeMillis() + 15000;
        boolean[] ready = {false};
        while (!ready[0] && SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync(() -> ready[0] = condition.ready());
            if (!ready[0]) SystemClock.sleep(50);
        }
        assertTrue("Editor operation did not complete", ready[0]);
    }

    @After public void tearDown() {
        if (activity != null && !activity.isDestroyed()) closeEditor();
        if (testDirectory != null) {
            instrumentation.runOnMainSync(() -> ManageMethods.prepareForDataReload(context));
            Config.initialize(context);
            BackupArchive.deleteTree(testDirectory);
        }
        if (originalGalleryItems != null) {
            for (Uri uri : galleryItems()) {
                if (!originalGalleryItems.contains(uri)) context.getContentResolver().delete(uri, null, null);
            }
        }
    }
}
