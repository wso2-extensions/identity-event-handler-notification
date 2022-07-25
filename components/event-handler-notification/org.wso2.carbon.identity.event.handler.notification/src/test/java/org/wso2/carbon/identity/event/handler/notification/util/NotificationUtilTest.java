package org.wso2.carbon.identity.event.handler.notification.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class NotificationUtilTest {

    // placeholders
    String ORGANIZATION_LOGO_DISPLAY_PLACEHOLDER = "organization.logo.display";
    String ORGANIZATION_LOGO_URL_PLACEHOLDER = "organization.logo.img";
    String ORGANIZATION_LOGO_ALT_TEXT_PLACEHOLDER = "organization.logo.altText";
    String ORGANIZATION_COPYRIGHT_TEXT_PLACEHOLDER = "organization.copyright.text";
    String ORGANIZATION_SUPPORT_EMAIL_PLACEHOLDER = "organization.support.mail";
    String ORGANIZATION_PRIMARY_COLOR_PLACEHOLDER = "organization.color.primary";
    String ORGANIZATION_BACKGROUND_COLOR_PLACEHOLDER = "organization.color.background";
    String ORGANIZATION_FONT_PLACEHOLDER = "organization.font";
    String ORGANIZATION_FONT_COLOR_PLACEHOLDER = "organization.font.color";
    String ORGANIZATION_BUTTON_FONT_COLOR_PLACEHOLDER = "organization.button.font.color";
    String ORGANIZATION_THEME_BACKGROUND_COLOR_PLACEHOLDER = "organization.theme.background.color";
    String ORGANIZATION_THEME_BORDER_COLOR_PLACEHOLDER = "organization.theme.border.color";

    // sample values for branding fallbacks
    String ORGANIZATION_COPYRIGHT_TEXT_FALLBACK = "Fallback Copyright Text";
    String ORGANIZATION_SUPPORT_EMAIL_FALLBACK = "fallback@support.com";
    String ORGANIZATION_PRIMARY_COLOR_FALLBACK = "#3CB371";
    String ORGANIZATION_BACKGROUND_COLOR_FALLBACK = "#94DBB4";
    String ORGANIZATION_FONT_FALLBACK = "Arial";
    String ORGANIZATION_FONT_COLOR_FALLBACK = "#353230";
    String ORGANIZATION_BUTTON_FONT_COLOR_FALLBACK = "#FFFFFF";
    String ORGANIZATION_LIGHT_BACKGROUND_COLOR_FALLBACK = "#F2F3F4";
    String ORGANIZATION_DARK_BACKGROUND_COLOR_FALLBACK = "#212F3D";
    String ORGANIZATION_LIGHT_BORDER_COLOR_FALLBACK = "#D5D8DC";
    String ORGANIZATION_DARK_BORDER_COLOR_FALLBACK = "#AEB6BF";

    // sample values for branding preferences
    String ORGANIZATION_LIGHT_LOGO_URL = "light.logo.url";
    String ORGANIZATION_DARK_LOGO_URL = "dark.logo.url";
    String ORGANIZATION_LIGHT_LOGO_ALT_TEXT = "light.logo.alt.text";
    String ORGANIZATION_DARK_LOGO_ALT_TEXT =  "dark.logo.alt.text";
    String ORGANIZATION_COPYRIGHT_TEXT = "Example Copyright Text";
    String ORGANIZATION_SUPPORT_EMAIL = "support@example.com";
    String ORGANIZATION_LIGHT_PRIMARY_COLOR = "#EE2B34";
    String ORGANIZATION_DARK_PRIMARY_COLOR = "#00529F";
    String ORGANIZATION_LIGHT_BACKGROUND_COLOR = "#CCE6FF";
    String ORGANIZATION_DARK_BACKGROUND_COLOR = "#CCCEEE";
    String ORGANIZATION_LIGHT_FONT = "Arial";
    String ORGANIZATION_DARK_FONT = "Arial Black";
    String ORGANIZATION_LIGHT_FONT_COLOR = "#000000";
    String ORGANIZATION_DARK_FONT_COLOR = "#FFFFFF";
    String ORGANIZATION_LIGHT_BUTTON_FONT_COLOR = "#F7F9F9";
    String ORGANIZATION_DARK_BUTTON_FONT_COLOR = "#FBFCFC";

    String LIGHT_THEME = "LIGHT";
    String DARK_THEME = "DARK";

    String BRANDING_ENABLED = "true";
    String BRANDING_DISABLED = "false";

    int CASE_1 = 1;
    int CASE_2 = 2;
    int CASE_3 = 3;
    int CASE_4 = 4;


    @DataProvider(name = "GetBrandingPreferenceDataProvider")
    public Object[][] provideTestData() {
        String brandingPreferencesStr1 = "{\"configs\":{\"isBrandingEnabled\":" + BRANDING_ENABLED + "},\"organizationDetails\":{\"copyrightText\":\"" + ORGANIZATION_COPYRIGHT_TEXT + "\",\"supportEmail\":\"" + ORGANIZATION_SUPPORT_EMAIL + "\"},\"theme\":{\"activeTheme\":\"" + LIGHT_THEME + "\",\"LIGHT\":{\"buttons\":{\"primary\":{\"base\":{\"font\":{\"color\":\"" + ORGANIZATION_LIGHT_BUTTON_FONT_COLOR + "\"}}}},\"colors\":{\"primary\":\"" + ORGANIZATION_LIGHT_PRIMARY_COLOR + "\"},\"images\":{\"logo\": {\"altText\": \"" + ORGANIZATION_LIGHT_LOGO_ALT_TEXT + "\",\"imgURL\": \"" + ORGANIZATION_LIGHT_LOGO_URL + "\"}},\"page\":{\"background\":{\"backgroundColor\":\"" + ORGANIZATION_LIGHT_BACKGROUND_COLOR + "\"},\"font\":{\"color\":\"" + ORGANIZATION_LIGHT_FONT_COLOR + "\"}},\"typography\":{\"font\":{\"fontFamily\":\"" + ORGANIZATION_LIGHT_FONT + "\"}}},\"DARK\":{\"buttons\":{\"primary\":{\"base\":{\"font\":{\"color\":\"" + ORGANIZATION_DARK_BUTTON_FONT_COLOR + "\"}}}},\"colors\":{\"primary\":\"" + ORGANIZATION_DARK_PRIMARY_COLOR + "\"},\"images\":{\"logo\": {\"altText\": \"" + ORGANIZATION_DARK_LOGO_ALT_TEXT + "\",\"imgURL\": \"" + ORGANIZATION_DARK_LOGO_URL + "\"}},\"page\":{\"background\":{\"backgroundColor\":\"" + ORGANIZATION_DARK_BACKGROUND_COLOR + "\"},\"font\":{\"color\":\"" + ORGANIZATION_DARK_FONT_COLOR + "\"}},\"typography\":{\"font\":{\"fontFamily\":\"" + ORGANIZATION_DARK_FONT + "\"}}}}}";
        String brandingPreferencesStr2 = "{\"configs\":{\"isBrandingEnabled\":" + BRANDING_ENABLED + "},\"organizationDetails\":{\"copyrightText\":\"" + ORGANIZATION_COPYRIGHT_TEXT + "\",\"supportEmail\":\"" + ORGANIZATION_SUPPORT_EMAIL + "\"},\"theme\":{\"activeTheme\":\"" + DARK_THEME + "\",\"LIGHT\":{\"buttons\":{\"primary\":{\"base\":{\"font\":{\"color\":\"" + ORGANIZATION_LIGHT_BUTTON_FONT_COLOR + "\"}}}},\"colors\":{\"primary\":\"" + ORGANIZATION_LIGHT_PRIMARY_COLOR + "\"},\"images\":{\"logo\": {\"altText\": \"" + ORGANIZATION_LIGHT_LOGO_ALT_TEXT + "\",\"imgURL\": \"" + ORGANIZATION_LIGHT_LOGO_URL + "\"}},\"page\":{\"background\":{\"backgroundColor\":\"" + ORGANIZATION_LIGHT_BACKGROUND_COLOR + "\"},\"font\":{\"color\":\"" + ORGANIZATION_LIGHT_FONT_COLOR + "\"}},\"typography\":{\"font\":{\"fontFamily\":\"" + ORGANIZATION_LIGHT_FONT + "\"}}},\"DARK\":{\"buttons\":{\"primary\":{\"base\":{\"font\":{\"color\":\"" + ORGANIZATION_DARK_BUTTON_FONT_COLOR + "\"}}}},\"colors\":{\"primary\":\"" + ORGANIZATION_DARK_PRIMARY_COLOR + "\"},\"images\":{\"logo\": {\"altText\": \"" + ORGANIZATION_DARK_LOGO_ALT_TEXT + "\",\"imgURL\": \"" + ORGANIZATION_DARK_LOGO_URL + "\"}},\"page\":{\"background\":{\"backgroundColor\":\"" + ORGANIZATION_DARK_BACKGROUND_COLOR + "\"},\"font\":{\"color\":\"" + ORGANIZATION_DARK_FONT_COLOR + "\"}},\"typography\":{\"font\":{\"fontFamily\":\"" + ORGANIZATION_DARK_FONT + "\"}}}}}";
        String brandingPreferencesStr3 = "{\"configs\":{\"isBrandingEnabled\":" + BRANDING_DISABLED + "},\"organizationDetails\":{\"copyrightText\":\"" + ORGANIZATION_COPYRIGHT_TEXT + "\",\"supportEmail\":\"" + ORGANIZATION_SUPPORT_EMAIL + "\"},\"theme\":{\"activeTheme\":\"" + LIGHT_THEME + "\",\"LIGHT\":{\"buttons\":{\"primary\":{\"base\":{\"font\":{\"color\":\"" + ORGANIZATION_LIGHT_BUTTON_FONT_COLOR + "\"}}}},\"colors\":{\"primary\":\"" + ORGANIZATION_LIGHT_PRIMARY_COLOR + "\"},\"images\":{\"logo\": {\"altText\": \"" + ORGANIZATION_LIGHT_LOGO_ALT_TEXT + "\",\"imgURL\": \"" + ORGANIZATION_LIGHT_LOGO_URL + "\"}},\"page\":{\"background\":{\"backgroundColor\":\"" + ORGANIZATION_LIGHT_BACKGROUND_COLOR + "\"},\"font\":{\"color\":\"" + ORGANIZATION_LIGHT_FONT_COLOR + "\"}},\"typography\":{\"font\":{\"fontFamily\":\"" + ORGANIZATION_LIGHT_FONT + "\"}}},\"DARK\":{\"buttons\":{\"primary\":{\"base\":{\"font\":{\"color\":\"" + ORGANIZATION_DARK_BUTTON_FONT_COLOR + "\"}}}},\"colors\":{\"primary\":\"" + ORGANIZATION_DARK_PRIMARY_COLOR + "\"},\"images\":{\"logo\": {\"altText\": \"" + ORGANIZATION_DARK_LOGO_ALT_TEXT + "\",\"imgURL\": \"" + ORGANIZATION_DARK_LOGO_URL + "\"}},\"page\":{\"background\":{\"backgroundColor\":\"" + ORGANIZATION_DARK_BACKGROUND_COLOR + "\"},\"font\":{\"color\":\"" + ORGANIZATION_DARK_FONT_COLOR + "\"}},\"typography\":{\"font\":{\"fontFamily\":\"" + ORGANIZATION_DARK_FONT + "\"}}}}}";

        JsonNode brandingPreferences1, brandingPreferences2, brandingPreferences3;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            brandingPreferences1 = objectMapper.readTree(brandingPreferencesStr1);
            brandingPreferences2 = objectMapper.readTree(brandingPreferencesStr2);
            brandingPreferences3 = objectMapper.readTree(brandingPreferencesStr3);
        } catch (JsonProcessingException e) {
            brandingPreferences1 = null;
            brandingPreferences2 = null;
            brandingPreferences3 = null;
        }

        Map<String, String> brandingFallbacks = new HashMap<>();
        brandingFallbacks.put("copyrightText", ORGANIZATION_COPYRIGHT_TEXT_FALLBACK);
        brandingFallbacks.put("supportMail", ORGANIZATION_SUPPORT_EMAIL_FALLBACK);
        brandingFallbacks.put("primaryColor", ORGANIZATION_PRIMARY_COLOR_FALLBACK);
        brandingFallbacks.put("backgroundColor", ORGANIZATION_BACKGROUND_COLOR_FALLBACK);
        brandingFallbacks.put("lightBackgroundColor", ORGANIZATION_LIGHT_BACKGROUND_COLOR_FALLBACK);
        brandingFallbacks.put("darkBackgroundColor", ORGANIZATION_DARK_BACKGROUND_COLOR_FALLBACK);
        brandingFallbacks.put("fontStyle", ORGANIZATION_FONT_FALLBACK);
        brandingFallbacks.put("fontColor", ORGANIZATION_FONT_COLOR_FALLBACK);
        brandingFallbacks.put("buttonFontColor", ORGANIZATION_BUTTON_FONT_COLOR_FALLBACK);
        brandingFallbacks.put("lightBorderColor", ORGANIZATION_LIGHT_BORDER_COLOR_FALLBACK);
        brandingFallbacks.put("darkBorderColor", ORGANIZATION_DARK_BORDER_COLOR_FALLBACK);

        return new Object[][] {
                {brandingPreferences1, brandingFallbacks, CASE_1},
                {brandingPreferences2, brandingFallbacks, CASE_2},
                {brandingPreferences3, brandingFallbacks, CASE_3},
                {null, brandingFallbacks, CASE_4}
        };
    }

    @Test
    public void testGetBrandingFallbacksFromConfigFile() {

    }

    @Test(dataProvider = "GetBrandingPreferenceDataProvider")
    public void testGetBrandingPreference(JsonNode brandingPreferences, Map<String, String> brandingFallback, int caseNo) {
        String logoDisplay = NotificationUtil.getBrandingPreference(ORGANIZATION_LOGO_DISPLAY_PLACEHOLDER, brandingPreferences, brandingFallback);
        String logoUrl = NotificationUtil.getBrandingPreference(ORGANIZATION_LOGO_URL_PLACEHOLDER, brandingPreferences, brandingFallback);
        String logoAltText = NotificationUtil.getBrandingPreference(ORGANIZATION_LOGO_ALT_TEXT_PLACEHOLDER, brandingPreferences, brandingFallback);
        String copyrightText = NotificationUtil.getBrandingPreference(ORGANIZATION_COPYRIGHT_TEXT_PLACEHOLDER, brandingPreferences, brandingFallback);
        String supportMail = NotificationUtil.getBrandingPreference(ORGANIZATION_SUPPORT_EMAIL_PLACEHOLDER, brandingPreferences, brandingFallback);
        String primaryColor = NotificationUtil.getBrandingPreference(ORGANIZATION_PRIMARY_COLOR_PLACEHOLDER, brandingPreferences, brandingFallback);
        String backgroundColor = NotificationUtil.getBrandingPreference(ORGANIZATION_BACKGROUND_COLOR_PLACEHOLDER, brandingPreferences, brandingFallback);
        String font = NotificationUtil.getBrandingPreference(ORGANIZATION_FONT_PLACEHOLDER, brandingPreferences, brandingFallback);
        String fontColor = NotificationUtil.getBrandingPreference(ORGANIZATION_FONT_COLOR_PLACEHOLDER, brandingPreferences, brandingFallback);
        String buttonFontColor = NotificationUtil.getBrandingPreference(ORGANIZATION_BUTTON_FONT_COLOR_PLACEHOLDER, brandingPreferences, brandingFallback);
        String themeBackgroundColor = NotificationUtil.getBrandingPreference(ORGANIZATION_THEME_BACKGROUND_COLOR_PLACEHOLDER, brandingPreferences, brandingFallback);
        String themeBorderColor = NotificationUtil.getBrandingPreference(ORGANIZATION_THEME_BORDER_COLOR_PLACEHOLDER, brandingPreferences, brandingFallback);

        if (caseNo == 1) {
            Assert.assertEquals(logoDisplay, "block" ,"This test is failing");
            Assert.assertEquals(logoUrl, ORGANIZATION_LIGHT_LOGO_URL);
            Assert.assertEquals(logoAltText, ORGANIZATION_LIGHT_LOGO_ALT_TEXT);
            Assert.assertEquals(copyrightText, ORGANIZATION_COPYRIGHT_TEXT);
            Assert.assertEquals(supportMail, ORGANIZATION_SUPPORT_EMAIL);
            Assert.assertEquals(primaryColor, ORGANIZATION_LIGHT_PRIMARY_COLOR);
            Assert.assertEquals(backgroundColor, ORGANIZATION_LIGHT_BACKGROUND_COLOR);
            Assert.assertEquals(font, ORGANIZATION_LIGHT_FONT);
            Assert.assertEquals(fontColor, ORGANIZATION_LIGHT_FONT_COLOR);
            Assert.assertEquals(buttonFontColor, ORGANIZATION_LIGHT_BUTTON_FONT_COLOR);
            Assert.assertEquals(themeBackgroundColor, ORGANIZATION_LIGHT_BACKGROUND_COLOR_FALLBACK);
            Assert.assertEquals(themeBorderColor, ORGANIZATION_LIGHT_BORDER_COLOR_FALLBACK);
        }

        if (caseNo == 2) {
            Assert.assertEquals(logoUrl, ORGANIZATION_DARK_LOGO_URL);
            Assert.assertEquals(logoAltText, ORGANIZATION_DARK_LOGO_ALT_TEXT);
            Assert.assertEquals(primaryColor, ORGANIZATION_DARK_PRIMARY_COLOR);
            Assert.assertEquals(backgroundColor, ORGANIZATION_DARK_BACKGROUND_COLOR);
            Assert.assertEquals(font, ORGANIZATION_DARK_FONT);
            Assert.assertEquals(fontColor, ORGANIZATION_DARK_FONT_COLOR);
            Assert.assertEquals(buttonFontColor, ORGANIZATION_DARK_BUTTON_FONT_COLOR);
            Assert.assertEquals(themeBackgroundColor, ORGANIZATION_DARK_BACKGROUND_COLOR_FALLBACK);
            Assert.assertEquals(themeBorderColor, ORGANIZATION_DARK_BORDER_COLOR_FALLBACK);
        }

        if (caseNo == 3) {
            Assert.assertEquals(logoDisplay, "none");
            Assert.assertEquals(logoUrl, "");
            Assert.assertEquals(logoAltText, "");
            Assert.assertEquals(copyrightText, ORGANIZATION_COPYRIGHT_TEXT_FALLBACK);
            Assert.assertEquals(supportMail, ORGANIZATION_SUPPORT_EMAIL_FALLBACK);
            Assert.assertEquals(primaryColor, ORGANIZATION_PRIMARY_COLOR_FALLBACK);
            Assert.assertEquals(backgroundColor, ORGANIZATION_BACKGROUND_COLOR_FALLBACK);
            Assert.assertEquals(font, ORGANIZATION_FONT_FALLBACK);
            Assert.assertEquals(fontColor, ORGANIZATION_FONT_COLOR_FALLBACK);
            Assert.assertEquals(buttonFontColor, ORGANIZATION_BUTTON_FONT_COLOR_FALLBACK);
            Assert.assertEquals(themeBackgroundColor, ORGANIZATION_LIGHT_BACKGROUND_COLOR_FALLBACK);
            Assert.assertEquals(themeBorderColor, ORGANIZATION_LIGHT_BORDER_COLOR_FALLBACK);
        }

        if (caseNo == 4) {
            Assert.assertEquals(logoDisplay, "none");
            Assert.assertEquals(logoUrl, "");
            Assert.assertEquals(logoAltText, "");
            Assert.assertEquals(copyrightText, ORGANIZATION_COPYRIGHT_TEXT_FALLBACK);
            Assert.assertEquals(supportMail, ORGANIZATION_SUPPORT_EMAIL_FALLBACK);
            Assert.assertEquals(primaryColor, ORGANIZATION_PRIMARY_COLOR_FALLBACK);
            Assert.assertEquals(backgroundColor, ORGANIZATION_BACKGROUND_COLOR_FALLBACK);
            Assert.assertEquals(font, ORGANIZATION_FONT_FALLBACK);
            Assert.assertEquals(fontColor, ORGANIZATION_FONT_COLOR_FALLBACK);
            Assert.assertEquals(buttonFontColor, ORGANIZATION_BUTTON_FONT_COLOR_FALLBACK);
            Assert.assertEquals(themeBackgroundColor, ORGANIZATION_LIGHT_BACKGROUND_COLOR_FALLBACK);
            Assert.assertEquals(themeBorderColor, ORGANIZATION_LIGHT_BORDER_COLOR_FALLBACK);
        }
    }
}
