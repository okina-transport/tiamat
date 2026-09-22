package org.rutebanken.tiamat.repository;

import org.rutebanken.tiamat.model.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganisationRepository extends JpaRepository<Organisation, Long>, EntityInVersionRepository<Organisation> {

    Optional<Organisation> findByName(String name);
}
