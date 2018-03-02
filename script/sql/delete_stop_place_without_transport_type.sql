delete from stop_place_quays where stop_place_id in (select id from stop_place where stop_place_type is null and parent_stop_place is false);
delete from stop_place_key_values where stop_place_id in (select id from stop_place where stop_place_type is null and parent_stop_place is false);
delete from stop_place_children where children_id in (select id from stop_place where stop_place_type is null and parent_stop_place is false);
delete from stop_place where stop_place_type is null and parent_stop_place is false;