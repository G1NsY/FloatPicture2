package tool.xfy9326.floatpicture.Methods;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/** Publishes a full-resolution PNG to Pictures/FloatPicture, preserving transparency. */
public final class GalleryExporter {
    private GalleryExporter() { }

    public static boolean savePng(Context context, Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return false;
        String name = "FloatPicture_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8) + ".png";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return saveMediaStore(context.getContentResolver(), bitmap, name);
        }
        return saveLegacy(context, bitmap, name);
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static boolean saveMediaStore(ContentResolver resolver, Bitmap bitmap, String name) {
        Uri uri = null;
        boolean published = false;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FloatPicture");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return false;
            try (OutputStream output = resolver.openOutputStream(uri)) {
                writePng(bitmap, output);
            }
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            published = resolver.update(uri, values, null, null) > 0;
            return published;
        } catch (IOException | RuntimeException exception) {
            return false;
        } finally {
            if (!published && uri != null) {
                try { resolver.delete(uri, null, null); }
                catch (RuntimeException ignored) { }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean saveLegacy(Context context, Bitmap bitmap, String name) {
        File file = null;
        boolean saved = false;
        boolean created = false;
        try {
            File directory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "FloatPicture");
            if (!directory.isDirectory() && !directory.mkdirs()) return false;
            file = new File(directory, name);
            if (!file.createNewFile()) return false;
            created = true;
            try (OutputStream output = new FileOutputStream(file)) {
                writePng(bitmap, output);
            }
            MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()},
                    new String[]{"image/png"}, null);
            saved = true;
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        } finally {
            if (!saved && created) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    private static void writePng(Bitmap bitmap, OutputStream output) throws IOException {
        if (output == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            throw new IOException("Could not write PNG");
        }
        output.flush();
    }
}
