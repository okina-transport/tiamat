CREATE SEQUENCE IF NOT EXISTS value_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE SEQUENCE IF NOT EXISTS job_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


DO $$
DECLARE
v_max_seq bigint;
v_next bigint;
  v_sql  text;
BEGIN


EXECUTE format('SELECT COALESCE(MAX(id), 0) + 11 FROM job')
    INTO v_next;
v_sql := format('ALTER SEQUENCE %I RESTART WITH %s', 'job_seq', v_next);
EXECUTE v_sql;


EXECUTE format('SELECT COALESCE(MAX(id), 0) + 11 FROM value')
    INTO v_next;
v_sql := format('ALTER SEQUENCE %I RESTART WITH %s', 'value_seq', v_next);
EXECUTE v_sql;


END
$$;





