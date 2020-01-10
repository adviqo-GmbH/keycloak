package org.keycloak.storage.http;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.AbstractKeycloakTransaction.TransactionState;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Delegation pattern. Used to delegate managing roles and groups to Keycloak federated storage and entity data such as
 * username, email, etc. to http storage
 */
public class TxAwareHttpUserModelDelegate extends AbstractUserAdapterFederatedStorage {

    private static final Logger logger = Logger.getLogger(TxAwareHttpUserModelDelegate.class);

    /**
     * Is this delegate represents persisted entity in http storage?
     */
    private boolean isPersistedInHttpStorage;

    /**
     * User model that keeps the data
     */
    private final HttpUserModel httpUserModel;

    /**
     * Http transaction that updates http storage at one moment
     */
    private final HttpTransaction httpTransaction;

    public static TxAwareHttpUserModelDelegate createForNewUser(KeycloakSession session,
                                                                RealmModel realm,
                                                                ComponentModel storageProviderModel,
                                                                HttpUserModel httpUserModel,
                                                                HttpConnector httpConnector) {
        TxAwareHttpUserModelDelegate delegate = new TxAwareHttpUserModelDelegate(session, realm, storageProviderModel, httpUserModel, httpConnector);
        delegate.isPersistedInHttpStorage = false;
        return delegate;
    }

    public static TxAwareHttpUserModelDelegate createForExistingUser(KeycloakSession session,
                                                                     RealmModel realm,
                                                                     ComponentModel storageProviderModel,
                                                                     HttpUserModel userModel,
                                                                     HttpConnector httpConnector) {
        TxAwareHttpUserModelDelegate delegate = new TxAwareHttpUserModelDelegate(session, realm, storageProviderModel, userModel, httpConnector);
        delegate.isPersistedInHttpStorage = true;
        return delegate;
    }

    private TxAwareHttpUserModelDelegate(KeycloakSession session,
                                         RealmModel realm,
                                         ComponentModel storageProviderModel,
                                         HttpUserModel httpUserModel,
                                         HttpConnector httpConnector) {
        super(session, realm, storageProviderModel);
        this.httpUserModel = httpUserModel;
        httpTransaction = new HttpTransaction(httpConnector, this);
    }

    public void ensureTransactionEnlisted() {
        if (TransactionState.NOT_STARTED.equals(httpTransaction.getState()) && !httpTransaction.isEnlisted()) {
            session.getTransactionManager().enlistAfterCompletion(httpTransaction);
            httpTransaction.setEnlisted(true);
        }
    }

    public boolean isAdminTool() {
        return session.getContext().getUri().getDelegate().getPath().startsWith("/admin/realms/");
    }

    public boolean isNotPersistedInHttpStorage() {
        return !isPersistedInHttpStorage;
    }

    public void setPersistedInHttpStorage(boolean persistedInHttpStorage) {
        isPersistedInHttpStorage = persistedInHttpStorage;
    }

    public HttpUserModel getDelegatedUserModel() {
        return httpUserModel;
    }

    public String getRealmId() {
        return realm.getId();
    }

    @Override
    public String getUsername() {
        return httpUserModel.getUsername();
    }

    @Override
    public void setUsername(String username) {
        logger.tracef("setUsername(%s)", username);
        if (Objects.equals(httpUserModel.getUsername(), username)) {
            return;
        }
        httpUserModel.setUsername(username);
        ensureTransactionEnlisted();
    }

    @Override
    public String getEmail() {
        return httpUserModel.getEmail();
    }

    @Override
    public void setEmail(String email) {
        logger.tracef("setEmail(%s)", email);
        if (Objects.equals(httpUserModel.getEmail(), email)) {
            return;
        }
        httpUserModel.setEmail(email);
        ensureTransactionEnlisted();
    }

    @Override
    public String getFirstName() {
        return httpUserModel.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        logger.tracef("setFirstName(%s)", firstName);
        if (Objects.equals(httpUserModel.getFirstName(), firstName)) {
            return;
        }
        httpUserModel.setFirstName(firstName);
        ensureTransactionEnlisted();
    }

    @Override
    public String getLastName() {
        return httpUserModel.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        logger.tracef("setLastName(%s)", lastName);
        if (Objects.equals(httpUserModel.getLastName(), lastName)) {
            return;
        }
        httpUserModel.setLastName(lastName);
        ensureTransactionEnlisted();
    }

    @Override
    public boolean isEmailVerified() {
        return httpUserModel.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        logger.tracef("setEmailVerified(%s)", verified);
        if (httpUserModel.isEmailVerified() == verified) {
            return;
        }
        httpUserModel.setEmailVerified(verified);
        ensureTransactionEnlisted();
    }

    @Override
    public boolean isEnabled() {
        return httpUserModel.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        logger.tracef("setEnabled(%s)", enabled);
        if (httpUserModel.isEnabled() == enabled) {
            return;
        }
        httpUserModel.setEnabled(enabled);
        ensureTransactionEnlisted();
    }

    @Override
    public Long getCreatedTimestamp() {
        return httpUserModel.getCreatedTimestamp();
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        logger.tracef("setCreatedTimestamp(%s)", timestamp);
        if (Objects.equals(httpUserModel.getCreatedTimestamp(), timestamp)) {
            return;
        }
        httpUserModel.setCreatedTimestamp(timestamp);
        ensureTransactionEnlisted();
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        logger.tracef("setSingleAttribute(%s, %s)", name, value);
        if (Objects.equals(httpUserModel.getFirstAttribute(name), value)) {
            return;
        }
        httpUserModel.setSingleAttribute(name, value);
        ensureTransactionEnlisted();
    }

    @Override
    public void removeAttribute(String name) {
        logger.tracef("removeAttribute(%s)", name);
        httpUserModel.removeAttribute(name);
        ensureTransactionEnlisted();
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        logger.tracef("setAttribute(%s, %s)", name, values);
        if (httpUserModel.getAttribute(name).equals(values)) {
            return;
        }
        httpUserModel.setAttribute(name, values);
        ensureTransactionEnlisted();
    }

    @Override
    public String getFirstAttribute(String name) {
        logger.tracef("getFirstAttribute(%s)", name);
        return httpUserModel.getFirstAttribute(name);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        logger.tracef("getAttributes()");
        return httpUserModel.getAttributes();
    }

    @Override
    public List<String> getAttribute(String name) {
        logger.tracef("getAttribute(%s)", name);
        return httpUserModel.getAttribute(name);
    }

    @Override
    public String getId() {
        if (storageId == null) {
            storageId = new StorageId(storageProviderModel.getId(), httpUserModel.getId());
        }
        return storageId.getId();
    }
}
