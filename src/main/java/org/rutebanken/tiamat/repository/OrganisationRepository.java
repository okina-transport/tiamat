package org.rutebanken.tiamat.repository;

import org.rutebanken.tiamat.model.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganisationRepository extends JpaRepository<Organisation, Long>, EntityInVersionRepository<Organisation> {

}
