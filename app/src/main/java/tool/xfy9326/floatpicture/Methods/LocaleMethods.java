package tool.xfy9326.floatpicture.Methods;

import android.app.Activity;
import android.app.Application;
import android.app.LocaleManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceManager;

import java.util.Locale;

import tool.xfy9326.floatpicture.Utils.Config;

/** Applies the user-selected interface language across AppCompat activities. */
public final class LocaleMethods {
    private LocaleMethods() {
    }

    /** Keeps root views aligned with the effective locale if Android leaves a stale direction. */
    public static void registerLayoutDirectionHandling(Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity,
                                          @Nullable Bundle savedInstanceState) {
                applyEffectiveLayoutDirection(activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                applyEffectiveLayoutDirection(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity,
                                                    @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }

    public static void applyEffectiveLayoutDirection(Activity activity) {
        LocaleListCompat locales = ConfigurationCompat.getLocales(
                activity.getResources().getConfiguration());
        if (locales.isEmpty()) {
            return;
        }
        Locale locale = locales.get(0);
        if (locale == null) {
            return;
        }
        int layoutDirection = TextUtils.getLayoutDirectionFromLocale(locale);
        View decorView = activity.getWindow().getDecorView();
        if (decorView.getLayoutDirection() != layoutDirection) {
            decorView.setLayoutDirection(layoutDirection);
            decorView.requestLayout();
        }
    }

    public static void applySavedLanguage(Context context) {
        String language = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Config.PREFERENCE_INTERFACE_LANGUAGE,
                Config.INTERFACE_LANGUAGE_SYSTEM);
        applyLanguage(context, language);
    }

    public static void applyLanguage(Context context, String language) {
        String languageTags = Config.INTERFACE_LANGUAGE_SYSTEM.equals(language)
                ? ""
                : language;

        // On Android 13+, the framework owns per-app locales. Using the supplied context is
        // important here: AppCompat 1.6 cannot find LocaleManager before an activity delegate
        // exists, so clearing an old override during application startup can otherwise be lost.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Api33Impl.setApplicationLocales(context, languageTags);
            return;
        }

        LocaleListCompat locales = LocaleListCompat.forLanguageTags(languageTags);
        // Let AppCompat perform its own equality check. Calling it even for an empty list also
        // initializes the pre-Android-13 requested locale state to "follow system".
        AppCompatDelegate.setApplicationLocales(locales);
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private static final class Api33Impl {
        private Api33Impl() {
        }

        static void setApplicationLocales(Context context, String languageTags) {
            LocaleManager localeManager = context.getSystemService(LocaleManager.class);
            LocaleList locales = LocaleList.forLanguageTags(languageTags);
            if (localeManager != null
                    && !localeManager.getApplicationLocales().equals(locales)) {
                int systemLayoutDirection = getSystemLayoutDirection(localeManager);
                localeManager.setApplicationLocales(locales);
                if (locales.isEmpty() && context instanceof Activity) {
                    applySystemLayoutDirection((Activity) context, systemLayoutDirection);
                }
            }
        }

        private static int getSystemLayoutDirection(LocaleManager localeManager) {
            LocaleList systemLocales = localeManager.getSystemLocales();
            return systemLocales.isEmpty()
                    ? android.view.View.LAYOUT_DIRECTION_LTR
                    : TextUtils.getLayoutDirectionFromLocale(systemLocales.get(0));
        }

        private static void applySystemLayoutDirection(Activity activity,
                                                       int systemLayoutDirection) {
            // Clearing the override can leave Configuration's direction bit stale even after
            // its locale list has returned to the system locale. Correct the current hierarchy;
            // lifecycle callbacks do the same for every recreated or newly opened activity.
            View decorView = activity.getWindow().getDecorView();
            decorView.setLayoutDirection(systemLayoutDirection);
            decorView.requestLayout();
        }
    }
}
