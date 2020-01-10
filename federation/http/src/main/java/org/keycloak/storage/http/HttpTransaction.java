package org.keycloak.storage.http;

import org.keycloak.models.AbstractKeycloakTransaction;

public class HttpTransaction extends AbstractKeycloakTransaction {

    private final HttpConnector httpConnector;

    private final TxAwareHttpUserModelDelegate delegate;

    private boolean isEnlisted = false;

    public boolean isEnlisted() {
        return isEnlisted;
    }

    public void setEnlisted(boolean enlisted) {
        isEnlisted = enlisted;
    }

    public HttpTransaction(HttpConnector httpConnector, TxAwareHttpUserModelDelegate delegate) {
        this.httpConnector = httpConnector;
        this.delegate = delegate;
    }

    @Override
    protected void commitImpl() {
        if (delegate.isNotPersistedInHttpStorage()) {
            httpConnector.createUser(delegate.getRealmId(), delegate.getDelegatedUserModel(), delegate.isAdminTool());
            delegate.setPersistedInHttpStorage(true);
        } else {
            httpConnector.updateUser(delegate.getRealmId(), delegate.getDelegatedUserModel(), delegate.isAdminTool());
        }
    }

    @Override
    protected void rollbackImpl() {
    }

}
