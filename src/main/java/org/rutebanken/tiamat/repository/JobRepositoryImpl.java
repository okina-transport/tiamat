package org.rutebanken.tiamat.repository;

import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

public class JobRepositoryImpl implements JobRepositoryCustom<Job> {

    @PersistenceContext
    private EntityManager em;

    public List<Job> findByReferential(String referential, JobStatus status) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Job> cq = cb.createQuery(Job.class);
        Root<Job> jobRoot = cq.from(Job.class);

        cq.select(jobRoot);

        Predicate referentialPredicate = cb.equal(jobRoot.get("referential"), referential);
        Predicate statusPredicate = cb.equal(jobRoot.get("status"), status);

        Predicate finalPredicate = cb.and(referentialPredicate, statusPredicate);
        cq.where(finalPredicate);

        return em.createQuery(cq).getResultList();
    }

    public List<Job> findByReferentialAndAction(String referential, List<String> actions, JobStatus status) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Job> cq = cb.createQuery(Job.class);
        Root<Job> jobRoot = cq.from(Job.class);

        cq.select(jobRoot);

        Predicate referentialPredicate = cb.equal(jobRoot.get("referential"), referential);
        Predicate actionPredicate = jobRoot.get("action").in(actions);
        Predicate statusPredicate = cb.equal(jobRoot.get("status"), status);

        Predicate finalPredicate = cb.and(referentialPredicate, actionPredicate, statusPredicate);
        cq.where(finalPredicate);

        return em.createQuery(cq).getResultList();
    }

    public Job findByReferentialAndId(String referential, Long id) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Job> cq = cb.createQuery(Job.class);
        Root<Job> jobRoot = cq.from(Job.class);

        cq.select(jobRoot);

        Predicate referentialPredicate = cb.equal(jobRoot.get("referential"), referential);
        Predicate idPredicate = jobRoot.get("id").in(id);

        Predicate finalPredicate = cb.and(referentialPredicate, idPredicate);
        cq.where(finalPredicate);

        return em.createQuery(cq).getSingleResult();
    }

    public Job terminatedJob(String referential, Long id) {
        return findByReferentialAndId(referential, id);
    }
}
