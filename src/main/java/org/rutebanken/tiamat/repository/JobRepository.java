/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.repository;


import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.service.job.JobPurgeCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface JobRepository extends PagingAndSortingRepository<Job, Long>, JobRepositoryCustom<Job>, JpaRepository<Job, Long>,
        JpaSpecificationExecutor<Job> {

    List<Job> findByTypesAndAction(List<JobType> types, JobAction jobAction);

    List<Job> findByTypeAndStatus(JobType type, JobStatus status);

    @Query("select j from Job j left join fetch j.operators where j.id in :ids order by j.started desc")
    List<Job> findJobsWithOperatorsFetching(List<Long> ids);

    @Query("select new org.rutebanken.tiamat.service.job.JobPurgeCandidate(j.id, j.subFolder, j.type, j.action, j.fileName, coalesce(j.finished, j.started)) " +
            "from Job j where j.status in :statuses")
    List<JobPurgeCandidate> findPurgeCandidatesByStatusIn(@Param("statuses") Collection<JobStatus> statuses);

    @Query("select new org.rutebanken.tiamat.service.job.JobPurgeCandidate(j.id, j.subFolder, j.type, j.action, j.fileName, coalesce(j.finished, j.started)) " +
            "from Job j where j.status is null or j.status not in :statuses")
    List<JobPurgeCandidate> findPurgeCandidatesByStatusNotIn(@Param("statuses") Collection<JobStatus> statuses);

    @Modifying
    @Query(value = "delete from job_operators where job_id in :ids", nativeQuery = true)
    int deleteOperatorsByJobIdIn(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query(value = "delete from job_id_list where job_id in :ids", nativeQuery = true)
    int deleteJobIdListByJobIdIn(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query("delete from Job j where j.id in :ids")
    int deleteByIdIn(@Param("ids") Collection<Long> ids);

}
