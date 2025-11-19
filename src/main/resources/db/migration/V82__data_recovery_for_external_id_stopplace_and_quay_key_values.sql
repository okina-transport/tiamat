CREATE OR REPLACE PROCEDURE pa_create_all_external_refs()
LANGUAGE plpgsql
AS $$
DECLARE
rec RECORD;
    v_external_ref_value TEXT;
    v_new_value_id INT8;
    v_current_max_id INT8;
BEGIN
    LOCK TABLE public.value IN ACCESS EXCLUSIVE MODE;
SELECT COALESCE(MAX(id), 0) INTO v_current_max_id FROM public.value;

    -- -----------------------------------
    -- === STOP_PLACE ===
    -- -----------------------------------
FOR rec IN
    SELECT
        DISTINCT ON (sp.id)
        sp.id AS entity_id,
        replace(vi.items, '##3A##', ':') AS imported_id_value
    FROM
        public.stop_place sp
        JOIN
        public.stop_place_key_values spkv ON sp.id = spkv.stop_place_id
        JOIN
        public.value v ON spkv.key_values_id = v.id
        JOIN
        public.value_items vi ON v.id = vi.value_id
    WHERE
        spkv.key_values_key = 'imported-id'
      AND vi.items IS NOT NULL
      AND NOT EXISTS (
        SELECT 1
        FROM public.stop_place_key_values spkv_check
        WHERE spkv_check.stop_place_id = sp.id
      AND spkv_check.key_values_key = 'external-ref'
        )
        LOOP
            v_external_ref_value := split_part(rec.imported_id_value, ':', 3);

            IF v_external_ref_value IS NOT NULL AND v_external_ref_value != '' THEN
                v_current_max_id := v_current_max_id + 1;
                v_new_value_id := v_current_max_id;

                INSERT INTO public.value (id) VALUES (v_new_value_id);
                INSERT INTO public.value_items (value_id, items) VALUES (v_new_value_id, v_external_ref_value);
                INSERT INTO public.stop_place_key_values (stop_place_id, key_values_id, key_values_key)
                VALUES (rec.entity_id, v_new_value_id, 'external-ref');
            END IF;
        END LOOP;

    -- -----------------------------------
    -- === QUAY ===
    -- -----------------------------------
FOR rec IN
    SELECT
        DISTINCT ON (q.id)
        q.id AS entity_id,
        replace(vi.items, '##3A##', ':') AS imported_id_value
    FROM
        public.quay q
        JOIN
        public.quay_key_values qkv ON q.id = qkv.quay_id
        JOIN
        public.value v ON qkv.key_values_id = v.id
        JOIN
        public.value_items vi ON v.id = vi.value_id
    WHERE
        qkv.key_values_key = 'imported-id'
      AND vi.items IS NOT NULL
      AND NOT EXISTS (
        SELECT 1
        FROM public.quay_key_values qkv_check
        WHERE qkv_check.quay_id = q.id
      AND qkv_check.key_values_key = 'external-ref'
        )
        LOOP
            v_external_ref_value := split_part(rec.imported_id_value, ':', 3);

            IF v_external_ref_value IS NOT NULL AND v_external_ref_value != '' THEN
                v_current_max_id := v_current_max_id + 1;
                v_new_value_id := v_current_max_id;

                INSERT INTO public.value (id) VALUES (v_new_value_id);
                INSERT INTO public.value_items (value_id, items) VALUES (v_new_value_id, v_external_ref_value);
                INSERT INTO public.quay_key_values (quay_id, key_values_id, key_values_key)
                VALUES (rec.entity_id, v_new_value_id, 'external-ref');
            END IF;
        END LOOP;

END;
$$;


call pa_create_all_external_refs();