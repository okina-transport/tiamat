CREATE INDEX IF NOT EXISTS idx_quay_place_equipments_id ON quay (place_equipments_id);
CREATE INDEX IF NOT EXISTS idx_stop_place_place_equipments_id ON stop_place (place_equipments_id);
CREATE INDEX IF NOT EXISTS idx_parking_place_equipments_id ON parking (place_equipments_id);
CREATE INDEX IF NOT EXISTS idx_access_space_place_equipments_id ON access_space (place_equipments_id);
CREATE INDEX IF NOT EXISTS idx_boarding_position_place_equipments_id ON boarding_position (place_equipments_id);
CREATE INDEX IF NOT EXISTS idx_parking_area_place_equipments_id ON parking_area (place_equipments_id);
CREATE INDEX IF NOT EXISTS idx_point_of_interest_place_equipments_id ON point_of_interest (place_equipments_id);
CREATE INDEX IF NOT EXISTS idx_parking_bay_place_equipments_id ON parking_bay (place_equipments_id);
CREATE INDEX IF NOT EXISTS idx_parking_entrance_place_equipments_id ON parking_entrance (place_equipments_id);
CREATE INDEX IF NOT EXISTS idx_installed_equipment_version_structure_installed_equipment_place_equipments_id ON installed_equipment_version_structure_installed_equipment (place_equipment_id);

DELETE FROM installed_equipment_version_structure_installed_equipment
WHERE installed_equipment_id IN (
    SELECT ievs.id
    FROM installed_equipment_version_structure ievs
    WHERE NOT EXISTS (SELECT 1 FROM parking             p  WHERE p.place_equipments_id   = ievs.id)
      AND NOT EXISTS (SELECT 1 FROM access_space        a  WHERE a.place_equipments_id   = ievs.id)
      AND NOT EXISTS (SELECT 1 FROM boarding_position   b  WHERE b.place_equipments_id   = ievs.id)
      AND NOT EXISTS (SELECT 1 FROM parking_area        pa WHERE pa.place_equipments_id  = ievs.id)
      AND NOT EXISTS (SELECT 1 FROM quay                q  WHERE q.place_equipments_id   = ievs.id)
      AND NOT EXISTS (SELECT 1 FROM stop_place          sp WHERE sp.place_equipments_id  = ievs.id)
      AND NOT EXISTS (SELECT 1 FROM point_of_interest   poi WHERE poi.place_equipments_id = ievs.id)
      AND NOT EXISTS (SELECT 1 FROM parking_bay         pb WHERE pb.place_equipments_id  = ievs.id)
      AND NOT EXISTS (SELECT 1 FROM parking_entrance    pe WHERE pe.place_equipments_id  = ievs.id)
      AND NOT EXISTS (SELECT 1 FROM installed_equipment_version_structure_installed_equipment ie
                      WHERE ie.place_equipment_id = ievs.id)
);

DELETE FROM installed_equipment_version_structure ievs
WHERE NOT EXISTS (SELECT 1 FROM parking             p  WHERE p.place_equipments_id   = ievs.id)
  AND NOT EXISTS (SELECT 1 FROM access_space        a  WHERE a.place_equipments_id   = ievs.id)
  AND NOT EXISTS (SELECT 1 FROM boarding_position   b  WHERE b.place_equipments_id   = ievs.id)
  AND NOT EXISTS (SELECT 1 FROM parking_area        pa WHERE pa.place_equipments_id  = ievs.id)
  AND NOT EXISTS (SELECT 1 FROM quay                q  WHERE q.place_equipments_id   = ievs.id)
  AND NOT EXISTS (SELECT 1 FROM stop_place          sp WHERE sp.place_equipments_id  = ievs.id)
  AND NOT EXISTS (SELECT 1 FROM point_of_interest   poi WHERE poi.place_equipments_id = ievs.id)
  AND NOT EXISTS (SELECT 1 FROM parking_bay         pb WHERE pb.place_equipments_id  = ievs.id)
  AND NOT EXISTS (SELECT 1 FROM parking_entrance    pe WHERE pe.place_equipments_id  = ievs.id)
  AND NOT EXISTS (SELECT 1 FROM installed_equipment_version_structure_installed_equipment ie
                  WHERE ie.place_equipment_id = ievs.id);



