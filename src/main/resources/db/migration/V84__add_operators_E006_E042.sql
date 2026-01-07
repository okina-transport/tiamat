
CREATE TABLE job_operators (
                               job_id BIGINT NOT NULL,
                               operator VARCHAR(100) NOT NULL,
                               CONSTRAINT uk_job_operator UNIQUE (job_id, operator),
                               CONSTRAINT fk_job_operator_job FOREIGN KEY (job_id) REFERENCES job(id)
);
