CREATE OR REPLACE FUNCTION delete_poi_for_classification(classificationName text) RETURNS boolean
 LANGUAGE plpgsql
AS $function$
DECLARE
poi_record RECORD;
    netex_id_record RECORD;
    duplicate_record RECORD;
    nb_of_duplicate INT;
    tmp_parent_netex_id VARCHAR;

BEGIN

   CREATE TEMPORARY TABLE t_poi_id ON COMMIT DROP
            AS SELECT point_of_interest_id FROM point_of_interest_classifications WHERE classifications_id IN
                     (SELECT poi_c1.id FROM point_of_interest_classification poi_c1 WHERE poi_c1.parent_id in
                             ( SELECT poi_c2.id FROM point_of_interest_classification poi_c2 WHERE poi_c2.parent_id IS NULL AND poi_c2.name_value = classificationName));



    CREATE TEMPORARY TABLE t_key_id ON COMMIT DROP
    AS SELECT key_values_id FROM point_of_interest_key_values WHERE point_of_interest_id IN (SELECT point_of_interest_id FROM t_poi_id);





    DELETE FROM point_of_interest_classifications WHERE point_of_interest_id IN (SELECT point_of_interest_id FROM t_poi_id);
    DELETE FROM point_of_interest_key_values WHERE point_of_interest_id IN (SELECT point_of_interest_id FROM t_poi_id);
    DELETE FROM value_items WHERE value_id IN (SELECT key_values_id FROM t_key_id);
    DELETE FROM point_of_interest WHERE id IN (SELECT point_of_interest_id FROM t_poi_id);
    DELETE FROM point_of_interest_facility_set fs WHERE NOT EXISTS ( SELECT 1 FROM point_of_interest poi WHERE poi.point_of_interest_facility_set_id = fs.id);



RETURN TRUE;
END;
$function$
;

CREATE OR REPLACE FUNCTION delete_poi_except_classification(classificationName text) RETURNS boolean
 LANGUAGE plpgsql
AS $function$
DECLARE
poi_record RECORD;
    netex_id_record RECORD;
    duplicate_record RECORD;
    nb_of_duplicate INT;
    tmp_parent_netex_id VARCHAR;

BEGIN

   CREATE TEMPORARY TABLE t_poi_id ON COMMIT DROP
AS SELECT point_of_interest_id FROM point_of_interest_classifications WHERE classifications_id IN
                                                                            (SELECT poi_c1.id FROM point_of_interest_classification poi_c1 WHERE poi_c1.parent_id in
                                                                    ( SELECT poi_c2.id FROM point_of_interest_classification poi_c2 WHERE poi_c2.parent_id IS NULL AND poi_c2.name_value <> classificationName));



CREATE TEMPORARY TABLE t_key_id ON COMMIT DROP
AS SELECT key_values_id FROM point_of_interest_key_values WHERE point_of_interest_id IN (SELECT point_of_interest_id FROM t_poi_id);





DELETE FROM point_of_interest_classifications WHERE point_of_interest_id IN (SELECT point_of_interest_id FROM t_poi_id);
DELETE FROM point_of_interest_key_values WHERE point_of_interest_id IN (SELECT point_of_interest_id FROM t_poi_id);
DELETE FROM value_items WHERE value_id IN (SELECT key_values_id FROM t_key_id);
DELETE FROM point_of_interest WHERE id IN (SELECT point_of_interest_id FROM t_poi_id);
DELETE FROM point_of_interest_facility_set fs WHERE NOT EXISTS ( SELECT 1 FROM point_of_interest poi WHERE poi.point_of_interest_facility_set_id = fs.id);



RETURN TRUE;
END;
$function$
;
