CREATE TABLE export_job_id_list (job_id bigint NOT NULL, stop_place_id bigint NOT NULL);

CREATE index exp_job_list_job ON export_job_id_list(job_id);
CREATE index exp_job_list_stop ON export_job_id_list(stop_place_id);
