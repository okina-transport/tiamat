package org.rutebanken.tiamat.security;

import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_EDIT_STOPS;

@Component
public class RolesChecker {

    public static final String KC_ROLE_PREFIX = "ROLE_";

    private final RoleAssignmentExtractor roleAssignmentExtractor;

    public RolesChecker(RoleAssignmentExtractor roleAssignmentExtractor) {
        this.roleAssignmentExtractor = roleAssignmentExtractor;
    }

    public boolean hasRoleEdit() {
        return hasRole(ROLE_EDIT_STOPS);
    }

    public boolean hasRole(String role) {
        boolean hasRoleInRoleAssignments = roleAssignmentExtractor.getRoleAssignmentsForUser()
                .stream()
                .anyMatch(roleAssignment -> roleAssignment.r.equals(role));

        boolean hasRoleInAuthorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals(KC_ROLE_PREFIX + role));

        return hasRoleInRoleAssignments || hasRoleInAuthorities;
    }

    public boolean isInClientGroup(Authentication auth, String clientGroup) {
        List<String> clients = roleAssignmentExtractor.getClientList(auth);
        return CollectionUtils.isNotEmpty(clients) && clients.stream().anyMatch(c -> c.equalsIgnoreCase(clientGroup));
    }

}
