package org.rutebanken.tiamat.security;

import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_EDIT_STOPS;

@Component
public class RolesChecker {

    public static final String KC_ROLE_PREFIX = "ROLE_";

    @Autowired
    private RoleAssignmentExtractor roleAssignmentExtractor;

    public boolean hasRoleEdit() {
        boolean hasRoleInRoleAssignments = roleAssignmentExtractor.getRoleAssignmentsForUser()
                .stream()
                .anyMatch(roleAssignment -> roleAssignment.r.equals(ROLE_EDIT_STOPS));

        boolean hasRoleInAuthorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals(KC_ROLE_PREFIX + ROLE_EDIT_STOPS));

        return hasRoleInRoleAssignments || hasRoleInAuthorities;
    }
}
