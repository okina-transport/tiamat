
delete from quay_key_values where quay_id in (select id from quay where netex_id='NAQ:Quay:35973');
delete from stop_place_quays where quays_id in (select id from quay where netex_id='NAQ:Quay:35973');

delete from quay where netex_id='NAQ:Quay:35973';
