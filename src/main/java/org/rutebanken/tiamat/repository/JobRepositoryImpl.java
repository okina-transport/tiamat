package org.rutebanken.tiamat.repository;

import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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

        return em.createQuery(cq).setMaxResults(10).getResultList();

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

        return em.createQuery(cq).getResultList();
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

        return em.createQuery(cq).setMaxResults(maxSize).getResultList();
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
