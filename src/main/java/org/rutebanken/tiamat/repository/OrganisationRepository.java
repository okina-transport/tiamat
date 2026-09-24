package org.rutebanken.tiamat.repository;

import org.rutebanken.tiamat.model.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganisationRepository extends JpaRepository<Organisation, Long>, EntityInVersionRepository<Organisation> {

    Optional<Organisation> findFirstByName(String name);

    @Query(value = "SELECT o.netex_id " +
            "FROM organisation o " +
            "INNER JOIN organisation_key_values okv ON okv.organisation_id = o.id " +
            "INNER JOIN value_items v ON okv.key_values_id = v.value_id " +
            "WHERE okv.key_values_key = :key " +
            "AND v.items = :value " +
            "LIMIT 1", nativeQuery = true)
    String findFirstNetexIdByKeyValue(@Param("key") String key, @Param("value") String value);
}
