package org.rutebanken.tiamat.auth;

import com.vividsolutions.jts.geom.Polygon;
import org.rutebanken.helper.organisation.ReflectionAuthorizationService;
import org.rutebanken.helper.organisation.RoleAssignment;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.rutebanken.tiamat.auth.check.AuthorizationCheckFactory;
import org.rutebanken.tiamat.model.EntityStructure;
import org.rutebanken.tiamat.model.TopographicPlace;
import org.rutebanken.tiamat.repository.TopographicPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.rutebanken.helper.organisation.AuthorizationConstants.ENTITY_CLASSIFIER_ALL_TYPES;
import static org.rutebanken.helper.organisation.AuthorizationConstants.ENTITY_TYPE;

@Service
public class GenericAuthorizationService implements AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(GenericAuthorizationService.class);

    @Value("${authorization.enabled:true}")
    protected boolean authorizationEnabled;

    @Value("${administrative.zone.id.prefix:KVE:TopographicPlace:}")
    protected String administrativeZoneIdPrefix;

    @Autowired
    protected TopographicPlaceRepository topographicPlaceRepository;

    @Autowired
    private AuthorizationCheckFactory authorizationCheckFactory;

    @Autowired
    private RoleAssignmentExtractor roleAssignmentExtractor;

    @Override
    public void assertAuthorized(String requiredRole, Collection<? extends EntityStructure> entities) {

        final boolean allowed = isAuthorized(requiredRole, entities);
        if (!allowed) {
            throw new AccessDeniedException("Insufficient privileges for operation");
        }
    }

    @Override
    public void assertAuthorized(String requiredRole, EntityStructure... entities) {
        assertAuthorized(requiredRole, Arrays.asList(entities));
    }

    @Override
    public boolean isAuthorized(String requiredRole, EntityStructure... entities) {
        return isAuthorized(requiredRole, Arrays.asList(entities));
    }

    @Override
    public Set<String> getRelevantRolesForEntity(EntityStructure entityStructure) {
        return roleAssignmentExtractor.getRoleAssignmentsForUser().stream()
                       .filter(roleAssignment -> roleAssignment.getEntityClassifications().get(ENTITY_TYPE).stream()
                            .anyMatch(entityTypeString -> entityTypeString.toLowerCase().equals(entityStructure.getClass().getSimpleName().toLowerCase())
                                    || entityTypeString.contains(ENTITY_CLASSIFIER_ALL_TYPES)))
                       .map(roleAssignment -> roleAssignment.getRole())
                       .collect(Collectors.toSet());
    }

    @Override
    public boolean isAuthorized(String requiredRole, Collection<? extends EntityStructure> entities) {
        if (!authorizationEnabled) {
            return true;
        }

        List<RoleAssignment> relevantRoles = roleAssignmentExtractor.getRoleAssignmentsForUser().stream().filter(ra -> requiredRole.equals(ra.r)).collect(toList());

        boolean allowed = true;
        for (EntityStructure entity : entities) {
            allowed &= entity == null ||
                               relevantRoles.stream()
                                       .anyMatch(roleAssignment -> isAuthorizationForEntity(entity, roleAssignment, requiredRole));

        }
        return allowed;
    }

    protected boolean isAuthorizationForEntity(EntityStructure entity, RoleAssignment roleAssignment, String requiredRole) {

        ReflectionAuthorizationService reflectionAuthorizationService = new ReflectionAuthorizationService() {
            @Override
            public boolean entityAllowedInAdministrativeZone(RoleAssignment roleAssignment, Object entity) {
                Polygon administrativeZone = null;
                if (roleAssignment.z != null) {
                    TopographicPlace tp = topographicPlaceRepository.findFirstByNetexIdOrderByVersionDesc(administrativeZoneIdPrefix + roleAssignment.z);
                    if (tp == null) {
                        logger.warn("RoleAssignment contains unknown adminZone reference:" + roleAssignment.z + " . Will not allow authorization");
                        return false;
                    }
                    administrativeZone = tp.getPolygon();
                }
                // TODO check polygon
            return true;
            }
        };
        return reflectionAuthorizationService.authorized(roleAssignment, entity, requiredRole);
    }


}
