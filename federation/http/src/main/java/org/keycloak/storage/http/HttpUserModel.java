package org.keycloak.storage.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import java.beans.ConstructorProperties;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(value = { "groups", "realmRoleMappings", "roleMappings", "groupsCount" })
public class HttpUserModel implements UserModel {

	private String id;

	private String username;

	private String password;

	private Long createdTimestamp;

	private boolean enabled;

	private Map<String, List<String>> attributes = new HashMap<>();

	private Set<String> requiredActions;

	private String email;

	private String firstName;

	private String lastName;

	private boolean emailVerified;

	@ConstructorProperties("id")
	public HttpUserModel(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public Long getCreatedTimestamp() {
		return createdTimestamp;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public Map<String, List<String>> getAttributes() {
		return attributes;
	}

	@Override
	public Set<String> getRequiredActions() {
		return requiredActions;
	}

	@Override
	public String getEmail() {
		return email;
	}

	@Override
	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String getFirstName() {
		return firstName;
	}

	@Override
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Override
	public String getLastName() {
		return lastName;
	}

	@Override
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public static final String GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE = "Groups and roles are not supported.";

	@Override
	public void setCreatedTimestamp(Long timestamp) {
		this.createdTimestamp = timestamp;
	}

	@Override
	public void setSingleAttribute(String name, String value) {
		attributes.put(name, Collections.singletonList(value));
	}

	@Override
	public void setAttribute(String name, List<String> values) {
		attributes.put(name, values);
	}

	@Override
	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	@Override
	public String getFirstAttribute(String name) {
		return attributes.containsKey(name) ? attributes.get(name).get(0) : null;
	}

	@Override
	public List<String> getAttribute(String name) {
		return attributes.getOrDefault(name, Collections.emptyList());
	}

	@Override
	public void addRequiredAction(String action) {
		requiredActions.add(action);
	}

	@Override
	public void removeRequiredAction(String action) {
		requiredActions.remove(action);
	}

	@Override
	public void addRequiredAction(RequiredAction action) {
		requiredActions.add(action.toString());
	}

	@Override
	public void removeRequiredAction(RequiredAction action) {
		requiredActions.remove(action.toString());
	}

	@Override
	public boolean isEmailVerified() {
		return emailVerified;
	}

	@Override
	public void setEmailVerified(boolean verified) {
		emailVerified = verified;
	}

	@Override
	public Set<GroupModel> getGroups() {
		throw new RuntimeException(GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE);
	}

	@Override
	public Set<GroupModel> getGroups(int first, int max) {
		throw new RuntimeException(GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE);
	}

	@Override
	public Set<GroupModel> getGroups(String search, int first, int max) {
		throw new RuntimeException(GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE);
	}

	@Override
	public long getGroupsCount() {
		throw new RuntimeException(GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE);
	}

	@Override
	public long getGroupsCountByNameContaining(String search) {
		throw new RuntimeException(GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE);
	}

	@Override
	public void joinGroup(GroupModel group) {
		throw new RuntimeException(GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE);
	}

	@Override
	public void leaveGroup(GroupModel group) {
		throw new RuntimeException(GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE);
	}

	@Override
	public boolean isMemberOf(GroupModel group) {
		throw new RuntimeException(GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE);
	}

	@Override
	public String getFederationLink() {
		// Not implemented
		return null;
	}

	@Override
	public void setFederationLink(String link) {
		// Not implemented
	}

	@Override
	public String getServiceAccountClientLink() {
		// Not implemented
		return null;
	}

	@Override
	public void setServiceAccountClientLink(String clientInternalId) {
		// Not implemented
	}

	@Override
	public Set<RoleModel> getRealmRoleMappings() {
		throw new RuntimeException(GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE);
	}

	@Override
	public Set<RoleModel> getClientRoleMappings(ClientModel app) {
		throw new RuntimeException(GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE);
	}

	@Override
	public boolean hasRole(RoleModel role) {
		throw new RuntimeException(GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE);
	}

	@Override
	public void grantRole(RoleModel role) {
		throw new RuntimeException(GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE);
	}

	@Override
	public Set<RoleModel> getRoleMappings() {
		throw new RuntimeException(GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE);
	}

	@Override
	public void deleteRoleMapping(RoleModel role) {
		throw new RuntimeException(GROUPS_AND_ROLES_ARE_NOT_SUPPORTED_BY_USER_SERVICE);
	}

	@Override
	public String toString() {
		return "HttpUserModel{" + "id='" + id + '\'' +
				", username='" + username + '\'' +
				", password='" + password + '\'' +
				", createdTimestamp=" + createdTimestamp +
				", enabled=" + enabled +
				", attributes=" + attributes +
				", requiredActions=" + requiredActions +
				", email='" + email + '\'' +
				", firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				", emailVerified=" + emailVerified +
				'}';
	}

	public void setRequiredActions(Set<String> requiredActions) {
		this.requiredActions = requiredActions;
	}
}
