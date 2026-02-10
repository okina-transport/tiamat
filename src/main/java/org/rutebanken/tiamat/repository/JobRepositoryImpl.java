package org.rutebanken.tiamat.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Hibernate;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class JobRepositoryImpl implements JobRepositoryCustom<Job> {

    private static final String ACTION = "action";
    private static final String SUBFOLDER = "subFolder";
    public static final String STATUS = "status";
    private static final String FILENAME = "fileName";

    @PersistenceContext
    private EntityManager em;

    public List<Job> findByReferential(String referential, JobStatus status) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Job> cq = cb.createQuery(Job.class);
        Root<Job> jobRoot = cq.from(Job.class);

        cq.select(jobRoot);

        Predicate referentialPredicate = cb.equal(jobRoot.get("referential"), referential);
        Predicate statusPredicate = cb.equal(jobRoot.get(STATUS), status);

        Predicate finalPredicate = cb.and(referentialPredicate, statusPredicate);
        cq.where(finalPredicate);

        return em.createQuery(cq).getResultList();
    }

    @Transactional
    public List<Job> findByTypesAndAction(List<JobType> types, JobAction jobAction) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Job> cq = cb.createQuery(Job.class);
        Root<Job> jobRoot = cq.from(Job.class);

        cq.select(jobRoot);

        Predicate typePredicate = jobRoot.get("type").in(types);
        Predicate statusPredicate = cb.equal(jobRoot.get(ACTION), jobAction);

        Predicate finalPredicate = cb.and(typePredicate, statusPredicate);
        cq.where(finalPredicate);
        cq.orderBy(cb.desc(jobRoot.get("id")));

        List<Job> results = em.createQuery(cq).setMaxResults(10).getResultList();

        if (CollectionUtils.isNotEmpty(results)){
            for (Job result : results) {
                Hibernate.initialize(result.getOperators());
            }
        }
        return results;

    }

    @Transactional()
    public Page<Job> findAllWithOperators(Specification<Job> combinedFilter, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Job> cq = cb.createQuery(Job.class);
        Root<Job> jobRoot = cq.from(Job.class);

        if (combinedFilter != null) {
            cq.where(combinedFilter.toPredicate(jobRoot, cq, cb));
        }

        TypedQuery<Job> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Job> results = query.getResultList();

        if (CollectionUtils.isNotEmpty(results)) {
            for (Job result : results) {
                Hibernate.initialize(result.getOperators());
            }
        }

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Job> countRoot = countQuery.from(Job.class);
        countQuery.select(cb.count(countRoot));
        if (combinedFilter != null) {
            countQuery.where(combinedFilter.toPredicate(countRoot, countQuery, cb));
        }
        Long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    public List<Job> findByReferentialAndAction(String referential, List<String> actions, JobStatus status) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Job> cq = cb.createQuery(Job.class);
        Root<Job> jobRoot = cq.from(Job.class);

        cq.select(jobRoot);

        Predicate referentialPredicate = cb.equal(jobRoot.get("referential"), referential);
        Predicate actionPredicate = jobRoot.get(ACTION).in(actions);
        Predicate statusPredicate = cb.equal(jobRoot.get(STATUS), status);

        Predicate finalPredicate = cb.and(referentialPredicate, actionPredicate, statusPredicate);
        cq.where(finalPredicate);

        return em.createQuery(cq).getResultList();
    }

    public List<Job> findAllExportBy(String referential, JobType jobType, JobStatus status) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Job> cq = cb.createQuery(Job.class);
        Root<Job> jobRoot = cq.from(Job.class);

        cq.select(jobRoot);

        Predicate referentialPredicate = cb.equal(jobRoot.get(SUBFOLDER), referential);
        Predicate actionPredicate = cb.equal(jobRoot.get(ACTION), JobAction.EXPORT);
        Predicate typePredicate = cb.equal(jobRoot.get("type"), jobType);
        Predicate finalPredicate;
        if (status != null) {
            Predicate statusPredicate = cb.equal(jobRoot.get(STATUS), status);
            finalPredicate = cb.and(referentialPredicate, actionPredicate, statusPredicate, typePredicate);
        } else {
            finalPredicate = cb.and(referentialPredicate, actionPredicate, typePredicate);
        }

        cq.where(finalPredicate).orderBy(cb.desc(jobRoot.get("id")));

        List<Job> results = em.createQuery(cq).getResultList();
        if (CollectionUtils.isNotEmpty(results)){
            for (Job result : results) {
                Hibernate.initialize(result.getOperators());
            }
        }

        return results;
    }


    public List<Job> findAllExportBy(String referential, JobType jobType, int maxSize) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Job> cq = cb.createQuery(Job.class);
        Root<Job> jobRoot = cq.from(Job.class);

        cq.select(jobRoot);

        Predicate referentialPredicate = cb.equal(jobRoot.get(SUBFOLDER), referential);
        Predicate actionPredicate = cb.equal(jobRoot.get(ACTION), JobAction.EXPORT);
        Predicate typePredicate = cb.equal(jobRoot.get("type"), jobType);
        Predicate finalPredicate = cb.and(referentialPredicate, actionPredicate, typePredicate);

        cq.where(finalPredicate).orderBy(cb.desc(jobRoot.get("id")));

        List<Job> results = em.createQuery(cq).setMaxResults(maxSize).getResultList();
        if (CollectionUtils.isNotEmpty(results)){
            for (Job result : results) {
                Hibernate.initialize(result.getOperators());
            }
        }

        return results;
    }

    public Job findBySubFolderLikeReferentialAndId(String subFolder, Long id) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Job> cq = cb.createQuery(Job.class);
        Root<Job> jobRoot = cq.from(Job.class);

        cq.select(jobRoot);

        Predicate referentialPredicate = cb.equal(jobRoot.get(SUBFOLDER), subFolder);
        Predicate idPredicate = jobRoot.get("id").in(id);

        Predicate finalPredicate = cb.and(referentialPredicate, idPredicate);
        cq.where(finalPredicate);

        return em.createQuery(cq).getSingleResult();
    }

    public Job findByFileNameAndSubFolder(String fileName, String subFolder) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Job> cq = cb.createQuery(Job.class);
        Root<Job> jobRoot = cq.from(Job.class);

        cq.select(jobRoot);

        Predicate fileNamePredicate = cb.equal(jobRoot.get(FILENAME), fileName);
        Predicate subFolderPredicate = cb.equal(jobRoot.get(SUBFOLDER), subFolder);
        Predicate finalPredicate = cb.and(fileNamePredicate, subFolderPredicate);
        cq.where(finalPredicate);

        List<Job> results = em.createQuery(cq).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public Job terminatedJob(String subFolder, Long id) {
        return findBySubFolderLikeReferentialAndId(subFolder, id);
    }
}
