package org.keycloak.storage.http;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;


public class HttpUserStorageProviderFactory implements UserStorageProviderFactory<HttpUserStorageProvider> {
    private static final String PROVIDER_NAME = "http";

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public HttpUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new HttpUserStorageProvider("http://localhost:8080/v1/", session, model);
    }

}
