package de.mydomain;

import org.jboss.logging.Logger;
import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.EmailException;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.*;
import org.keycloak.theme.Theme;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Properties;

public class CustomEventListenerProvider implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(CustomEventListenerProvider.class);

    private final KeycloakSession keycloakSession;
    private final RealmProvider realmProvider;

    public CustomEventListenerProvider(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
        this.realmProvider = keycloakSession.realms();
    }

    @Override
    public void onEvent(Event event) {

        if (EventType.REGISTER.equals(event.getType())) {

            RealmModel realm = realmProvider.getRealm(event.getRealmId());
            UserModel newUser = keycloakSession.users().getUserById(realm, event.getUserId());

            sendWelcomeMail(realm, newUser);

            /*
            logger.info("*************************************************************************");

            logger.infof("######### NEW [%s] EVENT", event.getType());

            RealmModel realm = model.getRealm(event.getRealmId());
            UserModel newUser = keycloakSession.users().getUserById(realm, event.getUserId());

            String emailPlainContent = "New user registration\n\n" +
                    "Email: " + newUser.getEmail() + "\n" +
                    "Username: " + newUser.getUsername() + "\n" +
                    "Client: " + event.getClientId();

            String emailHtmlContent = "<h1>New user registration</h1>" +
                    "<ul>" +
                    "<li>Email: " + newUser.getEmail() + "</li>" +
                    "<li>Username: " + newUser.getUsername() + "</li>" +
                    "<li>Client: " + event.getClientId() + "</li>" +
                    "</ul>";

            DefaultEmailSenderProvider senderProvider = new DefaultEmailSenderProvider(keycloakSession);

            try {
                senderProvider.send(
                        keycloakSession.getContext().getRealm().getSmtpConfig(),
                        "admin@example.com",
                        "Keycloak - New Registration",
                        emailPlainContent,
                        emailHtmlContent);

            } catch (EmailException e) {
                logger.error("Failed to send email", e);
            }

            logger.info("*************************************************************************");
            */
        }

    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
    }

    @Override
    public void close() {
    }

    private void sendWelcomeMail(RealmModel realmModel, UserModel userModel) {

        final String welcomeMailSubject = getMessagesFromKeycloakTheme(realmModel, userModel, "welcomeMailSubject");
        final String welcomeTextMail = getMessagesFromKeycloakTheme(realmModel, userModel, "welcomeMailBody");
        final String welcomeHtmlMail = getMessagesFromKeycloakTheme(realmModel, userModel, "welcomeMailBodyHtml");

        DefaultEmailSenderProvider senderProvider = new DefaultEmailSenderProvider(keycloakSession);
        try {
            senderProvider.send(realmModel.getSmtpConfig(),
                    userModel,
                    welcomeMailSubject,
                    welcomeTextMail,
                    welcomeHtmlMail);
        } catch (EmailException e) {
            logger.error("Error Sending Email.", e);
        }
    }

    private String getMessagesFromKeycloakTheme(RealmModel realmModel, UserModel userModel, String messageSource) {

        Theme theme = getTheme(realmModel, userModel);
        Locale locale = getLocale(userModel);

        try {
            Properties p = theme.getMessages(locale);
            return new MessageFormat(p.getProperty(messageSource, messageSource), locale).format(Collections.emptyList().toArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Theme getTheme(RealmModel realmModel, UserModel userModel) {
        // ThemeProvider tp = keycloakSession.getProvider(ThemeProvider.class, "extending");
        ThemeManager themeManager = keycloakSession.theme();
        try {
            return themeManager.getTheme(realmModel.getEmailTheme(), Theme.Type.EMAIL);
            // return tp.getTheme(realmModel.getEmailTheme(), Theme.Type.EMAIL);
        } catch (IOException e) {
            logger.error("Error retrieving email theme", e);
        }
        return null;
    }

    private Locale getLocale(UserModel userModel) {
        return keycloakSession.getContext().resolveLocale(userModel);
    }
}
