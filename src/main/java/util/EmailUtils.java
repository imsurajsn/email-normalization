package util;

import config.EmailConfiguration;
import factory.handler.EmailHandler;
import factory.handler.RemoveCharacterFromEmailHandler;
import factory.handler.ToLowerEmailHandler;
import factory.handler.TrimFromEmailHandler;
import model.enums.EmailProviderType;

import java.util.*;

/**
 * Util class of Email-Normalization library.
 */
public final class EmailUtils {

    /**
     * Private default constructor to avoid creating it object.
     */
    private EmailUtils() {

    }

    private static final ToLowerEmailHandler toLowerEmailHandler = new ToLowerEmailHandler();
    private static final RemoveCharacterFromEmailHandler removeCharacterFromEmailHandler = new RemoveCharacterFromEmailHandler("[.]");
    private static final TrimFromEmailHandler trimFromEmailHandler = new TrimFromEmailHandler("\\+");
    private static final Map<EmailProviderType, List<EmailHandler>>  emailProviderTypeToHandlerListMap = new HashMap<EmailProviderType, List<EmailHandler>>() {{
        put(EmailProviderType.GMAIL, new ArrayList<>(Arrays.asList(toLowerEmailHandler, removeCharacterFromEmailHandler, trimFromEmailHandler)));
        put(EmailProviderType.OUTLOOK, new ArrayList<>(Arrays.asList(toLowerEmailHandler)));
        put(EmailProviderType.AOL, new ArrayList<>(Arrays.asList(toLowerEmailHandler)));
        put(EmailProviderType.YAHOO, new ArrayList<>(Arrays.asList(toLowerEmailHandler)));
        put(EmailProviderType.ICLOUD, new ArrayList<>(Arrays.asList(toLowerEmailHandler)));
        put(EmailProviderType.ZOHO, new ArrayList<>(Arrays.asList(toLowerEmailHandler)));
        put(EmailProviderType.YANDEX, new ArrayList<>(Arrays.asList(toLowerEmailHandler)));
        put(EmailProviderType.TUTANOTA, new ArrayList<>(Arrays.asList(toLowerEmailHandler)));
        put(EmailProviderType.OTHER, new ArrayList<>(Arrays.asList(toLowerEmailHandler)));
    }};

    private static final Map<String, EmailProviderType> nameToEmailProviderType = new HashMap<String, EmailProviderType>() {{
        put("gmail", EmailProviderType.GMAIL);
        put("googlemail", EmailProviderType.GMAIL);
        put("outlook", EmailProviderType.OUTLOOK);
        put("aol", EmailProviderType.AOL);
        put("yahoo", EmailProviderType.YAHOO);
        put("yahoomail", EmailProviderType.YAHOO);
        put("icloud", EmailProviderType.ICLOUD);
        put("zoho", EmailProviderType.ZOHO);
        put("yandex", EmailProviderType.YANDEX);
        put("tutanota", EmailProviderType.TUTANOTA);
    }};

    /**
     * To fetch {@link  EmailProviderType} enum based on emailProvider string.
     * @param emailProvider - input string value.
     * @return - {@link  EmailProviderType} enum.
     */
    public static EmailProviderType getEmailProviderType(final String emailProvider) {
        EmailProviderType type = nameToEmailProviderType.get(emailProvider.toLowerCase());

        if (type == null) {
            type = EmailProviderType.OTHER;
        }

        return  type;
    }

    /**
     * Return the default config.
     * @return {@link EmailConfiguration} default object.
     */
    public static EmailConfiguration readDefaultConfig() {
        EmailConfiguration emailConfiguration = new EmailConfiguration();
        emailConfiguration.setEmailProviderTypeListMap(new HashMap<>(emailProviderTypeToHandlerListMap));

        return emailConfiguration;
    }

    /**
     * Generic function to test non-null and not-empty string
     * @param str - input string.
     * @return - true/false based on non-null and not-empty string.
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.equals("");
    }
}