DELETE FROM parking_capacity WHERE id NOT IN (
    SELECT spaces_id FROM parking_properties_spaces
);