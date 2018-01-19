#!/usr/bin/env bash

# params :find_close_points.sh [host] [port]

ExtractPoints() {
    echo "Extracting points within $1 meters"
    psql -h localhost -p 5440 -U tiamat -t -A -F"," -c "
    WITH points AS (
        SELECT sp1.id AS point_id, sp1.netex_id AS origin_point_netex_id, sp2.netex_id AS close_point_netex_id, max(sp1.version) AS origin_point_version, max(sp2.version) AS close_point_version
        FROM stop_place sp1, stop_place sp2
        WHERE ST_DWithin(sp1.centroid,sp2.centroid,$1, false)
        AND sp1.centroid!='0101000020E610000000000000000000000000000000000000'
        AND sp1.netex_id != sp2.netex_id
        GROUP BY point_id, origin_point_netex_id, close_point_netex_id
    )
    SELECT origin_point_netex_id, close_point_netex_id, vi.items AS origin_point_imported_id
	FROM points
	LEFT JOIN stop_place_key_values  spkv on spkv.stop_place_id = points.point_id
	LEFT JOIN value_items vi on vi.value_id = spkv.key_values_id
	WHERE spkv.key_values_key='imported-id'
	ORDER by origin_point_imported_id, close_point_netex_id
    " > tiamat_close_points_$1.csv
}

ExtractPoints 5;
ExtractPoints 10;
ExtractPoints 15;
ExtractPoints 20;
ExtractPoints 30;
ExtractPoints 40;
ExtractPoints 50;

