package tool.xfy9326.floatpicture;

import android.app.Instrumentation;
import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import tool.xfy9326.floatpicture.Activities.AboutActivity;
import tool.xfy9326.floatpicture.Methods.LocaleMethods;
import tool.xfy9326.floatpicture.Utils.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class LocaleLayoutDirectionTest {
    @Test
    public void returningToSystemLanguageRestoresSystemLayoutDirection() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Locale systemLocale = getSystemLocale(instrumentation.getTargetContext());
        assertNotNull(systemLocale);
        int expectedSystemDirection = TextUtils.getLayoutDirectionFromLocale(systemLocale);
        String oppositeLanguage = expectedSystemDirection == View.LAYOUT_DIRECTION_RTL
                ? "en"
                : "ar";
        int expectedOppositeDirection = expectedSystemDirection == View.LAYOUT_DIRECTION_RTL
                ? View.LAYOUT_DIRECTION_LTR
                : View.LAYOUT_DIRECTION_RTL;
        AtomicReference<String> previousLanguage = new AtomicReference<>(
                Config.INTERFACE_LANGUAGE_SYSTEM);
        AtomicReference<String> previousPreference = new AtomicReference<>(
                Config.INTERFACE_LANGUAGE_SYSTEM);

        try (ActivityScenario<AboutActivity> scenario =
                     ActivityScenario.launch(AboutActivity.class)) {
            scenario.onActivity(activity -> {
                LocaleListCompat previous = AppCompatDelegate.getApplicationLocales();
                if (!previous.isEmpty()) {
                    previousLanguage.set(previous.toLanguageTags());
                }
                previousPreference.set(PreferenceManager.getDefaultSharedPreferences(activity)
                        .getString(Config.PREFERENCE_INTERFACE_LANGUAGE,
                                Config.INTERFACE_LANGUAGE_SYSTEM));
                PreferenceManager.getDefaultSharedPreferences(activity).edit()
                        .putString(Config.PREFERENCE_INTERFACE_LANGUAGE, oppositeLanguage)
                        .commit();
                LocaleMethods.applyLanguage(activity, oppositeLanguage);
            });
            waitForLocaleChange(instrumentation);
            scenario.onActivity(activity -> assertDirections(
                    activity.getResources().getConfiguration(),
                    activity.getWindow().getDecorView(),
                    expectedOppositeDirection));

            scenario.onActivity(activity -> {
                PreferenceManager.getDefaultSharedPreferences(activity).edit()
                        .putString(Config.PREFERENCE_INTERFACE_LANGUAGE,
                                Config.INTERFACE_LANGUAGE_SYSTEM)
                        .commit();
                LocaleMethods.applyLanguage(activity, Config.INTERFACE_LANGUAGE_SYSTEM);
            });
            waitForLocaleChange(instrumentation);
            scenario.onActivity(activity -> assertDirections(
                    activity.getResources().getConfiguration(),
                    activity.getWindow().getDecorView(),
                    expectedSystemDirection));
        } finally {
            try (ActivityScenario<AboutActivity> scenario =
                         ActivityScenario.launch(AboutActivity.class)) {
                scenario.onActivity(activity -> {
                    PreferenceManager.getDefaultSharedPreferences(activity).edit()
                            .putString(Config.PREFERENCE_INTERFACE_LANGUAGE,
                                    previousPreference.get())
                            .commit();
                    LocaleMethods.applyLanguage(activity, previousLanguage.get());
                });
                waitForLocaleChange(instrumentation);
            }
        }
    }

    private static void assertDirections(Configuration configuration, View decorView,
                                         int expectedDirection) {
        String details = "activityLocales="
                + ConfigurationCompat.getLocales(configuration).toLanguageTags()
                + ", configurationDirection=" + configuration.getLayoutDirection()
                + ", decorDirection=" + decorView.getLayoutDirection();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleManager localeManager = decorView.getContext()
                    .getSystemService(LocaleManager.class);
            if (localeManager != null) {
                details += ", applicationLocales=" + localeManager.getApplicationLocales()
                        .toLanguageTags()
                        + ", systemLocales=" + localeManager.getSystemLocales().toLanguageTags();
            }
        }
        Locale effectiveLocale = ConfigurationCompat.getLocales(configuration).get(0);
        assertNotNull(effectiveLocale);
        assertEquals(details, expectedDirection,
                TextUtils.getLayoutDirectionFromLocale(effectiveLocale));
        assertEquals(details, expectedDirection, decorView.getLayoutDirection());
        View content = decorView.findViewById(R.id.layout_about_content);
        assertNotNull(content);
        assertEquals(details, expectedDirection, content.getLayoutDirection());
    }

    private static Locale getSystemLocale(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Api33Impl.getSystemLocale(context);
        }
        return ConfigurationCompat.getLocales(
                Resources.getSystem().getConfiguration()).get(0);
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private static final class Api33Impl {
        private Api33Impl() {
        }

        static Locale getSystemLocale(Context context) {
            LocaleManager localeManager = context.getSystemService(LocaleManager.class);
            assertNotNull(localeManager);
            LocaleList systemLocales = localeManager.getSystemLocales();
            return systemLocales.isEmpty() ? null : systemLocales.get(0);
        }
    }

    private static void waitForLocaleChange(Instrumentation instrumentation) {
        instrumentation.waitForIdleSync();
        SystemClock.sleep(1000);
        instrumentation.waitForIdleSync();
    }
}
