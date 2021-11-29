CREATE OR REPLACE FUNCTION find_duplicate_imported_ids()
 RETURNS int
 LANGUAGE plpgsql
AS $function$
DECLARE
imported_id_record RECORD;
    netex_id_record RECORD;
    duplicate_record RECORD;
    nb_of_duplicate INT;
    tmp_parent_netex_id VARCHAR;

BEGIN

DROP TABLE tmp_duplicate_imported_ids;

CREATE TABLE tmp_duplicate_imported_ids (
                                            parent_netex_id varchar(255) NULL,
                                            netex_id varchar(255) NULL,
                                            imported_id varchar(255) NULL
);

FOR imported_id_record IN (SELECT DISTINCT vi.items AS items FROM (
					 SELECT DISTINCT vi_orig.items, vi_orig.value_id FROM value_items vi_orig
					  						   INNER JOIN quay_key_values qkv_orig  ON vi_orig.value_id = qkv_orig.key_values_id
					     						   WHERE qkv_orig.key_values_key ='imported-id'
	  								) vi
  					INNER JOIN quay_key_values qkv  ON  vi.value_id = qkv.key_values_id
  					INNER JOIN stop_place_quays spq ON  spq.quays_id = qkv.quay_id
  					INNER JOIN quay q ON  q.id = spq.quays_id
  					INNER JOIN stop_place sp ON  sp.id = spq.stop_place_id
  							WHERE qkv.key_values_key ='imported-id' AND sp.from_date < now()
  									AND (sp.to_date IS NULL OR sp.to_date > now())  										GROUP BY vi.items HAVING count(*) > 1
  							)
  LOOP


  		FOR netex_id_record IN (SELECT DISTINCT q.netex_id AS quay_netex_id FROM quay q INNER JOIN quay_key_values qkv  ON  q.id = qkv.quay_id
										  INNER JOIN value_items vi ON  vi.value_id = qkv.key_values_id
										 	WHERE qkv.key_values_key ='imported-id'
  											AND vi.items = imported_id_record.items)

		LOOP





			INSERT INTO tmp_duplicate_imported_ids ( netex_id,imported_id)
			VALUES (netex_id_record.quay_netex_id, imported_id_record.items);


END LOOP;
END LOOP;




FOR duplicate_record IN (SELECT * FROM tmp_duplicate_imported_ids)
  LOOP

SELECT sp.netex_id INTO tmp_parent_netex_id FROM stop_place sp INNER JOIN stop_place_quays spq ON sp.id = spq.stop_place_id
                                                               INNER JOIN quay q ON spq.quays_id = q.id
WHERE q.netex_id = duplicate_record.netex_id
  AND sp.from_date < now()
  AND (sp.to_date IS NULL OR sp.to_date > now());



IF (tmp_parent_netex_id IS NULL)
	 THEN
DELETE FROM tmp_duplicate_imported_ids WHERE netex_id = duplicate_record.netex_id AND imported_id = duplicate_record.imported_id;

ELSE


UPDATE tmp_duplicate_imported_ids SET parent_netex_id = tmp_parent_netex_id WHERE netex_id = duplicate_record.netex_id AND imported_id = duplicate_record.imported_id;

END IF;


END LOOP;



SELECT count(*) INTO nb_of_duplicate FROM tmp_duplicate_imported_ids;


RETURN nb_of_duplicate;
END;
$function$
;
