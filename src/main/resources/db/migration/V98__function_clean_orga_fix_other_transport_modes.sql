create or replace function clean_orga_delete_quay(p_id bigint) returns void
  LANGUAGE plpgsql
AS $$
begin
  with d as (delete from quay_key_values where quay_id = p_id returning key_values_id)
  insert into clean_orga_orphan select 'value', key_values_id from d;
  with d as (delete from quay_alternative_names where quay_id = p_id returning alternative_names_id)
  insert into clean_orga_orphan select 'alternative_name', alternative_names_id from d;
  with d as (delete from quay_equipment_places where quay_id = p_id returning equipment_places_id)
  insert into clean_orga_orphan select 'equipment_place', equipment_places_id from d;
  with d as (delete from quay_boarding_positions where quay_id = p_id returning boarding_positions_id)
  insert into clean_orga_orphan select 'boarding_position', boarding_positions_id from d;
  with d as (delete from quay_check_constraints where quay_id = p_id returning check_constraints_id)
  insert into clean_orga_orphan select 'check_constraint', check_constraints_id from d;
  delete from stop_place_quays where quays_id = p_id;

  with d as (delete from quay where id = p_id
             returning netex_id, accessibility_assessment_id, place_equipments_id, polygon_id),
       o as (insert into clean_orga_orphan
             select v.kind, v.id
               from d, lateral (values ('accessibility_assessment', d.accessibility_assessment_id),
                                       ('installed_equipment_version_structure', d.place_equipments_id),
                                       ('persistable_polygon', d.polygon_id)) v(kind, id)
              where v.id is not null)
  insert into clean_orga_deleted select 'quay', netex_id from d;
end;
$$;

-- suppression d'un stop place (et de ses éventuels quais restants) et de ses tables de liaison
create or replace function clean_orga_delete_stop_place(p_id bigint) returns void
  LANGUAGE plpgsql
AS $$
declare
  q record;
begin
  for q in (select quays_id from stop_place_quays where stop_place_id = p_id)
  loop
    perform clean_orga_delete_quay(q.quays_id);
  end loop;

  delete from stop_place_children where children_id = p_id or stop_place_id = p_id;
  with d as (delete from stop_place_key_values where stop_place_id = p_id returning key_values_id)
  insert into clean_orga_orphan select 'value', key_values_id from d;
  with d as (delete from stop_place_alternative_names where stop_place_id = p_id returning alternative_names_id)
  insert into clean_orga_orphan select 'alternative_name', alternative_names_id from d;
  with d as (delete from stop_place_tariff_zones where stop_place_id = p_id returning tariff_zones_id)
  insert into clean_orga_orphan select 'tariff_zone_ref', tariff_zones_id from d;
  with d as (delete from stop_place_equipment_places where stop_place_id = p_id returning equipment_places_id)
  insert into clean_orga_orphan select 'equipment_place', equipment_places_id from d;
  with d as (delete from stop_place_access_spaces where stop_place_id = p_id returning access_spaces_id)
  insert into clean_orga_orphan select 'access_space', access_spaces_id from d;
  delete from stop_place_other_transport_modes where stop_place_id = p_id;
  delete from stop_place_adjacent_sites        where stop_place_id = p_id;
  update point_of_interest set stop_place_id = null where stop_place_id = p_id;

  with d as (delete from stop_place where id = p_id
             returning netex_id, accessibility_assessment_id, place_equipments_id, polygon_id),
       o as (insert into clean_orga_orphan
             select v.kind, v.id
               from d, lateral (values ('accessibility_assessment', d.accessibility_assessment_id),
                                       ('installed_equipment_version_structure', d.place_equipments_id),
                                       ('persistable_polygon', d.polygon_id)) v(kind, id)
              where v.id is not null)
  insert into clean_orga_deleted select 'stop_place', netex_id from d;
end;
$$;

create or replace function clean_orga_keep_referenced(p_kind text, p_cols text[]) returns void
  LANGUAGE plpgsql
AS $$
declare
  r record;
begin
  for r in (select c.table_name, c.column_name
              from information_schema.columns c
              join information_schema.tables t on t.table_schema = c.table_schema and t.table_name = c.table_name
             where c.table_schema = 'public'
               and t.table_type = 'BASE TABLE'
               and c.column_name = any(p_cols)
               and c.table_name not like replace(p_kind, '_', '\_') || '\_%')
  loop
    execute format('delete from clean_orga_orphan o where o.kind = %L and exists (select 1 from %I x where x.%I = o.id)',
                   p_kind, r.table_name, r.column_name);
  end loop;
end;
$$;

-- purge des objets détachés qui ne sont plus référencés (parents avant enfants)
create or replace function clean_orga_purge_orphans() returns void
  LANGUAGE plpgsql
AS $$
declare
  k text;
  n integer;
begin
  analyze clean_orga_orphan;

  -- access_space et boarding_position : mêmes tables enfants
  foreach k in array array['access_space', 'boarding_position']
  loop
    perform clean_orga_keep_referenced(k, array[k || 's_id']);
    execute format('with d as (delete from %I where %I in (select id from clean_orga_orphan where kind = %L) returning alternative_names_id)
                    insert into clean_orga_orphan select ''alternative_name'', alternative_names_id from d',
                   k || '_alternative_names', k || '_id', k);
    execute format('with d as (delete from %I where %I in (select id from clean_orga_orphan where kind = %L) returning check_constraints_id)
                    insert into clean_orga_orphan select ''check_constraint'', check_constraints_id from d',
                   k || '_check_constraints', k || '_id', k);
    execute format('with d as (delete from %I where %I in (select id from clean_orga_orphan where kind = %L) returning equipment_places_id)
                    insert into clean_orga_orphan select ''equipment_place'', equipment_places_id from d',
                   k || '_equipment_places', k || '_id', k);
    execute format('with d as (delete from %I where %I in (select id from clean_orga_orphan where kind = %L) returning key_values_id)
                    insert into clean_orga_orphan select ''value'', key_values_id from d',
                   k || '_key_values', k || '_id', k);
    execute format('with d as (delete from %I where id in (select id from clean_orga_orphan where kind = %L)
                               returning accessibility_assessment_id, place_equipments_id, polygon_id)
                    insert into clean_orga_orphan
                    select v.kind, v.id
                      from d, lateral (values (''accessibility_assessment'', d.accessibility_assessment_id),
                                              (''installed_equipment_version_structure'', d.place_equipments_id),
                                              (''persistable_polygon'', d.polygon_id)) v(kind, id)
                     where v.id is not null',
                   k, k);
  end loop;

  -- equipment_place
  perform clean_orga_keep_referenced('equipment_place', array['equipment_places_id']);
  with d as (delete from equipment_place_equipment_positions
              where equipment_place_id in (select id from clean_orga_orphan where kind = 'equipment_place')
             returning equipment_positions_id)
  insert into clean_orga_orphan select 'equipment_position', equipment_positions_id from d;
  with d as (delete from equipment_place_key_values
              where equipment_place_id in (select id from clean_orga_orphan where kind = 'equipment_place')
             returning key_values_id)
  insert into clean_orga_orphan select 'value', key_values_id from d;
  with d as (delete from equipment_place where id in (select id from clean_orga_orphan where kind = 'equipment_place')
             returning polygon_id)
  insert into clean_orga_orphan select 'persistable_polygon', polygon_id from d where polygon_id is not null;

  -- equipment_position
  perform clean_orga_keep_referenced('equipment_position', array['equipment_positions_id']);
  with d as (delete from equipment_position_key_values
              where equipment_position_id in (select id from clean_orga_orphan where kind = 'equipment_position')
             returning key_values_id)
  insert into clean_orga_orphan select 'value', key_values_id from d;
  delete from equipment_position where id in (select id from clean_orga_orphan where kind = 'equipment_position');

  -- check_constraint
  perform clean_orga_keep_referenced('check_constraint', array['check_constraints_id']);
  with d as (delete from check_constraint_key_values
              where check_constraint_id in (select id from clean_orga_orphan where kind = 'check_constraint')
             returning key_values_id)
  insert into clean_orga_orphan select 'value', key_values_id from d;
  delete from check_constraint where id in (select id from clean_orga_orphan where kind = 'check_constraint');

  -- accessibility_assessment puis accessibility_limitation
  perform clean_orga_keep_referenced('accessibility_assessment', array['accessibility_assessment_id']);
  with d as (delete from accessibility_assessment_limitations
              where accessibility_assessment_id in (select id from clean_orga_orphan where kind = 'accessibility_assessment')
             returning limitations_id)
  insert into clean_orga_orphan select 'accessibility_limitation', limitations_id from d;
  delete from accessibility_assessment where id in (select id from clean_orga_orphan where kind = 'accessibility_assessment');

  perform clean_orga_keep_referenced('accessibility_limitation', array['limitations_id']);
  delete from accessibility_limitation where id in (select id from clean_orga_orphan where kind = 'accessibility_limitation');

  -- installed_equipment_version_structure : un équipement de lieu contient des équipements installés (même table)
  loop
    perform clean_orga_keep_referenced('installed_equipment_version_structure', array['place_equipments_id']);
    -- encore contenu par un équipement de lieu qui n'est pas purgé
    delete from clean_orga_orphan o
     where o.kind = 'installed_equipment_version_structure'
       and exists (select 1
                     from installed_equipment_version_structure_installed_equipment l
                    where l.installed_equipment_id = o.id
                      and l.place_equipment_id not in (select id from clean_orga_orphan
                                                        where kind = 'installed_equipment_version_structure'));
    with d as (delete from installed_equipment_version_structure_installed_equipment
                where place_equipment_id in (select id from clean_orga_orphan where kind = 'installed_equipment_version_structure')
               returning installed_equipment_id)
    insert into clean_orga_orphan select 'installed_equipment_next', installed_equipment_id from d;
    delete from installed_equipment_version_structure
     where id in (select id from clean_orga_orphan where kind = 'installed_equipment_version_structure');
    delete from clean_orga_orphan where kind = 'installed_equipment_version_structure';
    update clean_orga_orphan set kind = 'installed_equipment_version_structure' where kind = 'installed_equipment_next';
    get diagnostics n = row_count;
    exit when n = 0;
  end loop;

  -- objets sans enfant
  perform clean_orga_keep_referenced('tariff_zone_ref', array['tariff_zones_id']);
  delete from tariff_zone_ref where id in (select id from clean_orga_orphan where kind = 'tariff_zone_ref');

  perform clean_orga_keep_referenced('alternative_name', array['alternative_names_id']);
  delete from alternative_name where id in (select id from clean_orga_orphan where kind = 'alternative_name');

  perform clean_orga_keep_referenced('persistable_polygon', array['polygon_id']);
  delete from persistable_polygon where id in (select id from clean_orga_orphan where kind = 'persistable_polygon');

  perform clean_orga_keep_referenced('value', array['key_values_id']);
  delete from value_items where value_id in (select id from clean_orga_orphan where kind = 'value');
  delete from value       where id       in (select id from clean_orga_orphan where kind = 'value');
end;
$$;

-- nettoyage des références NeTEx (par netex_id) vers les stop places / quais dont il ne reste plus aucune version
create or replace function clean_orga_clean_netex_refs() returns void
  LANGUAGE plpgsql
AS $$
begin
  -- une autre version existe encore : la référence reste valide
  delete from clean_orga_deleted d
   where (d.kind = 'stop_place' and exists (select 1 from stop_place sp where sp.netex_id = d.netex_id))
      or (d.kind = 'quay'       and exists (select 1 from quay q       where q.netex_id  = d.netex_id));
  analyze clean_orga_deleted;

  update stop_place        set parent_site_ref = null, parent_site_ref_version = null
   where parent_site_ref in (select netex_id from clean_orga_deleted where kind = 'stop_place');
  update parking           set parent_site_ref = null, parent_site_ref_version = null
   where parent_site_ref in (select netex_id from clean_orga_deleted where kind = 'stop_place');
  update point_of_interest set parent_site_ref = null, parent_site_ref_version = null
   where parent_site_ref in (select netex_id from clean_orga_deleted where kind = 'stop_place');

  delete from group_of_stop_places_members where ref in (select netex_id from clean_orga_deleted where kind = 'stop_place');
  delete from stop_place_adjacent_sites    where ref in (select netex_id from clean_orga_deleted where kind = 'stop_place');
  delete from parking_adjacent_sites       where ref in (select netex_id from clean_orga_deleted where kind = 'stop_place');
  delete from tag                          where netex_reference in (select netex_id from clean_orga_deleted);
end;
$$;

create or replace function clean_orga(orga text) returns boolean
  LANGUAGE plpgsql
AS $$
declare
  l  record;
  l2 record;
begin
  create temp table if not exists clean_orga_orphan  (kind text, id bigint) on commit drop;
  create temp table if not exists clean_orga_deleted (kind text, netex_id text) on commit drop;
  truncate clean_orga_orphan, clean_orga_deleted;

  -- suppression de toutes les clés valeurs imported-id à nettoyer
  delete
    from value_items
   where position(lower(orga || ':Quay')      in lower(items)) = 1
      or position(lower(orga || ':StopPlace') in lower(items)) = 1
      or position(lower(orga || ':StopArea')  in lower(items)) = 1;

  -- pour chaque stop_place qui n'a plus de clé valeurs en imported-id
  for l in (select sp.id spid
              from stop_place sp
             where not sp.parent_stop_place
               and not exists (select 1
                                 from stop_place_key_values spkv
                                 join value_items vi on vi.value_id = spkv.key_values_id
                                where spkv.stop_place_id = sp.id
                                  and spkv.key_values_key = 'imported-id'
                                  and vi.items <> ''))
  loop
    -- suppression des quais s'ils ne sont pas partagés (partagé = il reste des imported-id)
    for l2 in (select spq.quays_id qid
                 from stop_place_quays spq
                where spq.stop_place_id = l.spid
                  and not exists (select 1
                                    from quay_key_values qkv
                                    join value_items vi on vi.value_id = qkv.key_values_id
                                   where qkv.quay_id = spq.quays_id
                                     and qkv.key_values_key = 'imported-id'
                                     and vi.items <> ''))
    loop
      perform clean_orga_delete_quay(l2.qid);
    end loop;

    -- si le sp n'a plus de quai on supprime
    if not exists (select 1 from stop_place_quays spq where spq.stop_place_id = l.spid) then
      perform clean_orga_delete_stop_place(l.spid);
    end if;
  end loop;

  -- suppression des quais sans imported-id
  for l in (select q.id qid
              from quay q
             where not exists (select 1
                                 from quay_key_values qkv
                                 join value_items vi on vi.value_id = qkv.key_values_id
                                where qkv.quay_id = q.id
                                  and qkv.key_values_key = 'imported-id'
                                  and vi.items <> ''))
  loop
    perform clean_orga_delete_quay(l.qid);
  end loop;

  -- SP sans quay
  for l in (select sp.id spid
              from stop_place sp
             where not sp.parent_stop_place
               and not exists (select 1 from stop_place_quays spq where spq.stop_place_id = sp.id))
  loop
    perform clean_orga_delete_stop_place(l.spid);
  end loop;

  -- suppression des PEM sans enfant (en dernier, pour prendre en compte les enfants supprimés ci-dessus)
  for l in (select sp.id spid
              from stop_place sp
             where sp.parent_stop_place
               and not exists (select 1 from stop_place_children spc where spc.stop_place_id = sp.id))
  loop
    perform clean_orga_delete_stop_place(l.spid);
  end loop;

  perform clean_orga_clean_netex_refs();
  perform clean_orga_purge_orphans();

  return true;
end;
$$;
