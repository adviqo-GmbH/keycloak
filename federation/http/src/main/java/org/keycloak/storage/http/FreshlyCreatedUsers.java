package org.keycloak.storage.http;

import org.keycloak.models.KeycloakSession;

import java.util.Optional;

/**
 * All new users who are still not persisted to http storage shall remain in current KeycloakSession. This way,
 * if infinispan calls getUserById for the user that hasn't been persisted yet, this class will return the user as
 * it was already persisted.
 */
public class FreshlyCreatedUsers {

    private final KeycloakSession session;

    public FreshlyCreatedUsers(KeycloakSession session) {
        this.session = session;
    }

    private static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private static String usernameKey(String username) {
        return "username:" + username;
    }

    private static String emailKey(String email) {
        return "email:" + email;
    }

    private static String idKey(String id) {
        return "id:" + id;
    }

    public Optional<TxAwareHttpUserModelDelegate> getFreshlyCreatedUserByUsername(String username) {
        return Optional.ofNullable(session.getAttribute(usernameKey(username), TxAwareHttpUserModelDelegate.class));
    }

    public Optional<TxAwareHttpUserModelDelegate> getFreshlyCreatedUserById(String id) {
        return Optional.ofNullable(session.getAttribute(idKey(id), TxAwareHttpUserModelDelegate.class));
    }

    public Optional<TxAwareHttpUserModelDelegate> getFreshlyCreatedUserByEmail(String email) {
        return Optional.ofNullable(session.getAttribute(emailKey(email), TxAwareHttpUserModelDelegate.class));
    }

    public void saveInSession(TxAwareHttpUserModelDelegate userModel) {
        String username = userModel.getUsername();
        if (isNotBlank(username)) {
            session.setAttribute(usernameKey(username), userModel);
        }
        String email = userModel.getEmail();
        if (isNotBlank(email)) {
            session.setAttribute(emailKey(email), userModel);
        }
        String id = userModel.getId();
        if (isNotBlank(id)) {
            session.setAttribute(idKey(id), userModel);
        }
    }

    public void removeFromSession(TxAwareHttpUserModelDelegate userModel) {
        String username = userModel.getUsername();
        if (isNotBlank(username)) {
            session.removeAttribute(usernameKey(username));
        }
        String email = userModel.getEmail();
        if (isNotBlank(email)) {
            session.removeAttribute(emailKey(email));
        }
        String id = userModel.getId();
        if (isNotBlank(id)) {
            session.removeAttribute(idKey(id));
        }
    }

}
