create or replace function sync_id_generator_table() returns boolean
  LANGUAGE plpgsql
AS $$
declare

begin
    INSERT INTO id_generator(table_name,id_value)
    SELECT 'AccessibilityAssessment',existing_id FROM
        (SELECT DISTINCT split_part(netex_id,':',3)::BIGINT AS existing_id FROM accessibility_ASsessment )existing_acc
    WHERE not EXISTS (SELECT 1 FROM id_generator WHERE table_name = 'AccessibilityAssessment' AND id_value = existing_acc.existing_id);


    INSERT INTO id_generator(table_name,id_value)
    SELECT 'StopPlace',existing_id FROM
        (SELECT DISTINCT split_part(netex_id,':',3)::BIGINT AS existing_id FROM stop_place )existing_sp
    WHERE not EXISTS (SELECT 1 FROM id_generator WHERE table_name = 'StopPlace' AND id_value = existing_sp.existing_id);


    INSERT INTO id_generator(table_name,id_value)
    SELECT 'Quay',existing_id FROM
        (SELECT DISTINCT split_part(netex_id,':',3)::BIGINT AS existing_id FROM quay )existing_q
    WHERE not EXISTS (SELECT 1 FROM id_generator WHERE table_name = 'Quay' AND id_value = existing_q.existing_id);


    INSERT INTO id_generator(table_name,id_value)
    SELECT 'AccessibilityLimitation',existing_id FROM
        (SELECT DISTINCT split_part(netex_id,':',3)::BIGINT AS existing_id FROM accessibility_limitation )existing_al
    WHERE not EXISTS (SELECT 1 FROM id_generator WHERE table_name = 'AccessibilityLimitation' AND id_value = existing_al.existing_id);


    INSERT INTO id_generator(table_name,id_value)
    SELECT 'AlternativeName',existing_id FROM
        (SELECT DISTINCT split_part(netex_id,':',3)::BIGINT AS existing_id FROM alternative_name )existing_an
    WHERE not EXISTS (SELECT 1 FROM id_generator WHERE table_name = 'AlternativeName' AND id_value = existing_an.existing_id);


    INSERT INTO id_generator(table_name,id_value)
    SELECT 'AlternativeText',existing_id FROM
        (SELECT DISTINCT split_part(netex_id,':',3)::BIGINT AS existing_id FROM alternative_text WHERE netex_id IS NOT NULL )existing_at
    WHERE not EXISTS (SELECT 1 FROM id_generator WHERE table_name = 'AlternativeText' AND id_value = existing_at.existing_id);

return true;
end;
$$;
