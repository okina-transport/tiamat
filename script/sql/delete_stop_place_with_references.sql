delete from stop_place_key_values where stop_place_id in (select id from stop_place where netex_id ='NAQ:StopPlace:25065');
delete from stop_place_quays where stop_place_id in (select id from stop_place where netex_id ='NAQ:StopPlace:25065');

delete from stop_place where netex_id ='NAQ:StopPlace:25065';
