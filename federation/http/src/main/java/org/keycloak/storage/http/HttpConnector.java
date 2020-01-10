package org.keycloak.storage.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Connector that sends http requests to externalized user management service
 */
public class HttpConnector {

    private static final Logger logger = Logger.getLogger(HttpConnector.class);

    private static final ObjectMapper OBJECT_MAPPER;

    private static final ResteasyJackson2Provider JACKSON_PROVIDER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JACKSON_PROVIDER = new ResteasyJackson2Provider() {
        };
        JACKSON_PROVIDER.setMapper(OBJECT_MAPPER);
    }

    private static final String IS_MANUAL_SET_UP_QUERY_PARAM = "is_manual_set_up";

    public static final String MASKED_PASSWORD = "**masked password**";

    private final WebTarget realmTarget;

    private final WebTarget usersTarget;


    public HttpConnector(String targetUri) {
        realmTarget = new ResteasyClientBuilder()
                .register(JACKSON_PROVIDER, 100)
                .build()
                .target(targetUri)
                .path("realms/{realmId}");
        usersTarget = realmTarget.path("users");
    }

    /**
     * Helper method to build endpoint url for users resource
     *
     * @param realmId realm in which users are stored
     * @return request builder
     */
    private WebTarget usersEndpoint(String realmId) {
        return usersTarget.resolveTemplate("realmId", realmId);
    }

    /**
     * Helper method to build endpoint url for realm resource
     *
     * @param realmId id of the realm
     * @return request builder
     */
    private WebTarget realmEndpoint(String realmId) {
        return realmTarget.resolveTemplate("realmId", realmId);
    }

    /**
     * Helper method to build endpoint url for user resource
     *
     * @param realmId realm in which users are stored
     * @return request builder
     */
    private WebTarget userEndpoint(String realmId, String userId) {
        return usersTarget.path(userId)
                .resolveTemplate("realmId", realmId);
    }

    public Optional<HttpUserModel> getUserByExternalId(String realmId, String externalId) {
        Response resolvedUser = userEndpoint(realmId, externalId)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        if (isSuccessful(resolvedUser)) {
            final Optional<HttpUserModel> result = Optional.of(resolvedUser.readEntity(HttpUserModel.class));
            logger.tracef("getUserByExternalId(%s, %s) = %s", realmId, externalId, result);
            return result;
        }
        logger.tracef("getUserByExternalId(%s, %s) = empty", realmId, externalId);
        return Optional.empty();
    }

    public Optional<HttpUserModel> getUserByUsername(String realmId, String username) {
        logger.tracef("getUserByUsername(%s, %s)", realmId, username);
        Response resolvedUser = usersEndpoint(realmId)
                .queryParam("username", username)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        if (isSuccessful(resolvedUser)) {
            List<HttpUserModel> users = resolvedUser.readEntity(new GenericType<List<HttpUserModel>>() {
            });
            return Optional.ofNullable(users.isEmpty() ? null : users.get(0));
        }
        return Optional.empty();
    }

    private boolean isSuccessful(Response resolvedUser) {
        return resolvedUser.getStatus() == 200 && resolvedUser.hasEntity();
    }

    public Optional<HttpUserModel> getUserByEmail(String realmId, String email) {
        logger.tracef("getUserByEmail(%s, %s)", realmId, email);
        Response resolvedUser = usersEndpoint(realmId)
                .queryParam("email", email)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        if (isSuccessful(resolvedUser)) {
            List<HttpUserModel> users = resolvedUser.readEntity(new GenericType<List<HttpUserModel>>() {
            });
            return Optional.ofNullable(users.isEmpty() ? null : users.get(0));
        }
        return Optional.empty();
    }

    public Optional<Integer> getUsersCount(String realmId) {
        logger.tracef("getUsersCount(%s)", realmId);
        final Response response = realmEndpoint(realmId)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        if (isSuccessful(response)) {
            return Optional.of(response.readEntity(Integer.class));
        }
        return Optional.empty();
    }

    /**
     * Helper method to build search methods on users resource
     *
     * @param realmId               realm within which users exist
     * @param offset                common parameter for each search method for offset-based pagination
     * @param limit                 common parameter for each search method for offset-based pagination
     * @param appendQueryParameters function that adds additional queryParameters, used by
     *                              search methods
     * @return list of {@linkplain HttpUserModel} that satisfy criteria
     */
    private List<HttpUserModel> getUsersTemplate(String realmId, int offset, int limit,
                                                 Function<WebTarget, WebTarget> appendQueryParameters) {
        final WebTarget usersEndpointWithAdditionalQueryParameters = appendQueryParameters
                .apply(usersEndpoint(realmId));

        final Response response = usersEndpointWithAdditionalQueryParameters
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        if (isSuccessful(response)) {
            return response.readEntity(new GenericType<List<HttpUserModel>>() {
            });
        } else if (response.getStatusInfo().toEnum() == Response.Status.BAD_REQUEST) {
            throw new RuntimeException(response.readEntity(String.class));
        }
        logger.errorf("getUsersTemplate(%s, %s, %s, %s}) = %s",
                realmId, offset, limit, "appendQueryParameters", response);
        return Collections.emptyList();
    }

    public List<HttpUserModel> getUsers(String realmId, int offset, int limit) {
        logger.tracef("getUsers(%s, %s, %s)", realmId, offset, limit);
        final List<HttpUserModel> result = getUsersTemplate(realmId, offset, limit, Function.identity());
        logListOfUserModel(result);
        return result;
    }

    private void logListOfUserModel(List<HttpUserModel> result) {
        logger.tracef("list of user models: %s", result);
    }

    public List<HttpUserModel> searchForUser(String realmId, String search, int offset, int limit) {
        logger.tracef("searchForUser(%s, %s, %d, %d)", realmId, search, offset, limit);
        final List<HttpUserModel> result = getUsersTemplate(realmId, offset, limit,
                target -> target.queryParam("search", search));
        logListOfUserModel(result);
        return result;
    }

    public List<HttpUserModel> searchForUserByParams(String realmId, Map<String, String> params, int offset,
                                                     int limit) {
        logger.tracef("searchForUserByParams(%s, %s, %d, %d)", realmId, params, offset, limit);
        final Function<WebTarget, WebTarget> appendQueryParametersToTarget = target -> {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                target = target.queryParam(entry.getKey(), entry.getValue());
            }
            return target;
        };
        final List<HttpUserModel> result = getUsersTemplate(realmId, offset, limit, appendQueryParametersToTarget);
        logListOfUserModel(result);
        return result;
    }

    public Optional<Boolean> isConfiguredPasswordForExternalId(String realmId, String externalId) {
        logger.tracef("isConfiguredPasswordForExternalId(%s, %s)", new Object[]{realmId, externalId});
        final Response response = userEndpoint(realmId, externalId)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        if (response.hasEntity()) {
            return Optional.of(response.readEntity(Map.class).containsKey("password"));
        } else {
            return Optional.empty();
        }
    }

    public HttpUserModel createUser(String realmId, HttpUserModel user, boolean isManualSetUp) {
        logger.tracef("createUser(%s, %s)", realmId, user);

        Response response = usersEndpoint(realmId)
                .queryParam(IS_MANUAL_SET_UP_QUERY_PARAM, isManualSetUp)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(user));

        if (isSuccessful(response)) {
            return response.readEntity(HttpUserModel.class);
        } else {
            String error = response.readEntity(String.class);
            logger.errorf("Response was not OK: %s", error);
            throw new RuntimeException("Creating user in http storage has failed");
        }
    }

    /**
     * Verify non-null password for a given user
     *
     * @param realmId     realm within which user exists
     * @param externalId  user service id
     * @param rawPassword password from UI that needs to be verified. If it is null then
     *                    this method returns false
     * @return true - if is valid, false otherwise
     */
    public boolean verifyPassword(String realmId, String externalId, String rawPassword) {
        if (rawPassword == null) {
            logger.tracef("verifyPassword(%s, %s, null) = false", realmId, externalId);
            return false;
        }

        final class VerifyPasswordEvent {

            final String password;

            public VerifyPasswordEvent(String password) {
                this.password = password;
            }
        }

        final Response response = userEndpoint(realmId, externalId).path("valid_password")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(new VerifyPasswordEvent(rawPassword)));
        if (isSuccessful(response)) {
            boolean matches = response.readEntity(Boolean.class);
            logger.tracef("verifyPassword(%s, %s, %s) = %s", realmId, externalId, MASKED_PASSWORD, matches);
            return matches;
        }
        logger.tracef("verifyPassword(%s, %s, %s) = false", realmId, externalId, MASKED_PASSWORD);
        return false;
    }

    public boolean removeUserByExternalId(String realmId, String externalId) {
        boolean success = userEndpoint(realmId, externalId)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete()
                .getStatus() == 200;
        logger.tracef("removeUserByExternalId(%s, %s) = %s", realmId, externalId, success);
        return success;
    }

    public void updateUser(String realmId, HttpUserModel updatedUserModel, boolean isManualSetUp) {
        logger.tracef("updateUser(%s, %s)", realmId, updatedUserModel);

        final Response response = userEndpoint(realmId, updatedUserModel.getId())
                .queryParam(IS_MANUAL_SET_UP_QUERY_PARAM, isManualSetUp)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(updatedUserModel));
        if (response.getStatus() != 200) {
            throw new RuntimeException("Couldn't update user. Return status: " + response.getStatus());
        }
    }

}
