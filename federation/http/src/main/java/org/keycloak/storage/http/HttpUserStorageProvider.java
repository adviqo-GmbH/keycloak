package org.keycloak.storage.http;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Custom HttpUserStorageProvider. Makes possible to store users in externalized user management service.
 */
public class HttpUserStorageProvider implements UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator,
        CredentialInputUpdater,
        UserRegistrationProvider,
        UserQueryProvider {

    private final HttpConnector httpConnector;

    private final KeycloakSession session;

    private final ComponentModel model;

    private final FreshlyCreatedUsers freshlyCreatedUsers;

    HttpUserStorageProvider(String targetUri, KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.freshlyCreatedUsers = new FreshlyCreatedUsers(session);
        this.model = model;
        this.httpConnector = new HttpConnector(targetUri);
    }

    // UserLookupProvider methods

    @Override
    public TxAwareHttpUserModelDelegate getUserByUsername(String username, RealmModel realm) {
        Supplier<TxAwareHttpUserModelDelegate> remoteCall = () ->
                httpConnector.getUserByUsername(realm.getId(), username)
                        .map(user -> TxAwareHttpUserModelDelegate.createForExistingUser(session, realm, model, user, httpConnector))
                        .orElse(null);
        return freshlyCreatedUsers.getFreshlyCreatedUserByUsername(username).orElseGet(remoteCall);
    }

    @Override
    public TxAwareHttpUserModelDelegate getUserById(String id, RealmModel realm) {
        Supplier<TxAwareHttpUserModelDelegate> remoteCall = () ->
                httpConnector.getUserByExternalId(realm.getId(), StorageId.externalId(id))
                        .map(user -> TxAwareHttpUserModelDelegate.createForExistingUser(session, realm, model, user, httpConnector))
                        .orElseThrow(() -> new RuntimeException("User is not found by external id = " + StorageId.externalId(id)));

        return freshlyCreatedUsers.getFreshlyCreatedUserById(id).orElseGet(remoteCall);
    }

    @Override
    public TxAwareHttpUserModelDelegate getUserByEmail(String email, RealmModel realm) {
        Supplier<TxAwareHttpUserModelDelegate> remoteCall = () ->
                httpConnector.getUserByEmail(realm.getId(), email)
                        .map(user -> TxAwareHttpUserModelDelegate.createForExistingUser(session, realm, model, user, httpConnector))
                        .orElse(null);
        return freshlyCreatedUsers.getFreshlyCreatedUserByEmail(email).orElseGet(remoteCall);
    }

    // UserQueryProvider methods

    @Override
    public int getUsersCount(RealmModel realm) {
        return httpConnector.getUsersCount(realm.getId())
                .orElseThrow(() -> new RuntimeException("No users count could be retrieved"));
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return httpConnector.getUsers(realm.getId(), 0, Integer.MAX_VALUE).stream()
                .map(user -> TxAwareHttpUserModelDelegate.createForExistingUser(session, realm, model, user, httpConnector))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int offset, int limit) {
        return httpConnector.getUsers(realm.getId(), offset, limit).stream()
                .map(user -> TxAwareHttpUserModelDelegate.createForExistingUser(session, realm, model, user, httpConnector))
                .collect(Collectors.toList());
    }

    // UserQueryProvider method implementations

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int offset, int limit) {
        return httpConnector.searchForUser(realm.getId(), search, offset, limit).stream()
                .map(user -> TxAwareHttpUserModelDelegate.createForExistingUser(session, realm, model, user, httpConnector))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        return searchForUser(params, realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int offset, int limit) {
        return httpConnector.searchForUserByParams(realm.getId(), params, offset, limit).stream()
                .map(user -> TxAwareHttpUserModelDelegate.createForExistingUser(session, realm, model, user, httpConnector))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int offset, int limit) {
        final Map<String, String> singleParam = Collections.singletonMap("group", group.getName());
        return httpConnector.searchForUserByParams(realm.getId(), singleParam, offset, limit).stream()
                .map(user -> TxAwareHttpUserModelDelegate.createForExistingUser(session, realm, model, user, httpConnector))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        final Map<String, String> singleParam = Collections.singletonMap("group", group.getName());
        return httpConnector.searchForUserByParams(realm.getId(), singleParam, 0, Integer.MAX_VALUE).stream()
                .map(user -> TxAwareHttpUserModelDelegate.createForExistingUser(session, realm, model, user, httpConnector))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        final Map<String, String> singleParam = Collections.singletonMap(attrName, attrValue);
        return httpConnector.searchForUserByParams(realm.getId(), singleParam, 0, Integer.MAX_VALUE).stream()
                .map(user -> TxAwareHttpUserModelDelegate.createForExistingUser(session, realm, model, user, httpConnector))
                .collect(Collectors.toList());
    }

    // UserRegistrationProvider method implementations

    @Override
    public TxAwareHttpUserModelDelegate addUser(RealmModel realm, String username) {
        HttpUserModel user = new HttpUserModel(KeycloakModelUtils.generateId());
        user.setUsername(username);
        user.setCreatedTimestamp(System.currentTimeMillis());
        TxAwareHttpUserModelDelegate delegate = TxAwareHttpUserModelDelegate.createForNewUser(session, realm, model, user, httpConnector);
        freshlyCreatedUsers.saveInSession(delegate);
        return delegate;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        if (!(user instanceof TxAwareHttpUserModelDelegate)) {
            throw new RuntimeException("Cannot process anything else except TxAwareHttpUserModelDelegate");
        }
        freshlyCreatedUsers.removeFromSession((TxAwareHttpUserModelDelegate)user);
        return httpConnector.removeUserByExternalId(realm.getId(), StorageId.externalId(user.getId()));
    }

    // CredentialInputValidator methods

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) {
            return false;
        }
        return httpConnector.isConfiguredPasswordForExternalId(realm.getId(), StorageId.externalId(user.getId()))
                .orElseThrow(() -> new RuntimeException(
                        "Couldn't check is password set for a user with id = " + user.getId()));
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
            return false;
        }
        UserCredentialModel cred = (UserCredentialModel) input;
        String rawPassword = cred.getChallengeResponse();
        Optional<TxAwareHttpUserModelDelegate> freshlyCreatedUserById = freshlyCreatedUsers.getFreshlyCreatedUserById(user.getId());
        if (freshlyCreatedUserById.isPresent()) {
            throw new RuntimeException();
        }
        return httpConnector.verifyPassword(realm.getId(), StorageId.externalId(user.getId()), rawPassword);
    }

    // CredentialInputUpdater methods

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) {
            return false;
        }
        if (!PasswordCredentialModel.TYPE.equals(input.getType())) {
            return false;
        }
        if (!(user instanceof TxAwareHttpUserModelDelegate)) {
            throw new RuntimeException();
        }
        UserCredentialModel cred = (UserCredentialModel) input;
        TxAwareHttpUserModelDelegate delegate = (TxAwareHttpUserModelDelegate) user;
        delegate.getDelegatedUserModel().setPassword(cred.getChallengeResponse());
        delegate.ensureTransactionEnlisted();
        return true;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        // is not supported
        throw new RuntimeException();
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        // is not supported
        return Collections.emptySet();
    }

    @Override
    public void close() {
        // noop
    }

}
