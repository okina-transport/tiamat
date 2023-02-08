create or replace function clean_orga(orga text) returns boolean
  LANGUAGE plpgsql
AS $$
declare
  l  record;
  l2 record;
  l3 record;
  c  integer;
begin
  -- suppression de toutes les clés valeurs imported-id à nettoyer
  delete
    from value_items
   where position(lower(orga || ':Quay') in lower(items)) = 1;

  delete
    from value_items
   where position(lower(orga || ':StopPlace') in lower(items)) = 1;

  delete
    from value_items
   where position(lower(orga || ':StopArea') in lower(items)) = 1;

	-- pour chaque stop_place qui n'ayant plus de clé valeurs en imported-id
  for l in (select id spid, netex_id spnid
              from stop_place sp
              left join stop_place_key_values spkv on spkv.stop_place_id = sp.id and spkv.key_values_key like 'imported-id'
              left join value_items vi             on vi.value_id = spkv.key_values_id
             where coalesce(sp.parent_stop_place, false) = false
             group by id, netex_id , parent_site_ref, sp.parent_stop_place
             having coalesce(string_agg(distinct vi.items, ' - '), '') like '')
  loop
    raise notice 'SP %', l;

    -- suppression des quais si ils ne sont pas partagés
	  for l2 in (select q.id qid, q.netex_id, items
                 from quay q
                 join stop_place_quays spq     on spq.quays_id = q.id
                 left join quay_key_values qkv on qkv.quay_id = q.id and qkv.key_values_key like 'imported-id'
                 left join value_items vi      on vi.value_id = qkv.key_values_id
                where spq.stop_place_id = l.spid)
    loop
      -- partagé = il reste des imported-id
      -- sinon
      if(coalesce(l2.items, '') like '') then
        -- on supprime les clés valeurs du quay
        for l3 in (select * from quay_key_values qkv where qkv.quay_id = l2.qid)
        loop
          delete from quay_key_values  where quay_id = l2.qid and key_values_id = l3.key_values_id;
          delete from value_items      where value_id = l3.key_values_id;
        end loop;
        --delete from quay_key_values  where quay_id = l2.qid;
        delete from stop_place_quays where quays_id = l2.qid;
        delete from quay             where id = l2.qid;
      end if;
    end loop;

    --  si le sp n'a plus de quai on supprime
    select count(*) into c
      from quay q
      join stop_place_quays spq on spq.quays_id = q.id
      join stop_place sp        on sp.id = spq.stop_place_id
     where sp.id = l.spid;
    if(c>0) then
      continue;
    end if;

    delete from stop_place_children   where children_id = l.spid;
    for l3 in (select * from stop_place_key_values spkv where spkv.stop_place_id  = l.spid)
    loop
      delete from stop_place_key_values  where stop_place_id = l.spid and key_values_id = l3.key_values_id;
      delete from value_items            where value_id = l3.key_values_id;
    end loop;
    delete from stop_place_key_values        where stop_place_id = l.spid;
    delete from stop_place_alternative_names where stop_place_id = l.spid;
    delete from stop_place                   where id            = l.spid;
  end loop;

  -- suppression des PEM sans enfant
  for l in (select sp.id spid, sp.netex_id, sp.parent_stop_place,
                   string_agg(q.netex_id, ' - ') qnids,
                   string_agg(distinct spc.children_id::TEXT, ' - ') childrenids
              from stop_place sp
              left join stop_place_quays spq    on spq.stop_place_id = sp.id
              left join quay q                  on q.id = spq.quays_id
              left join stop_place_children spc on spc.stop_place_id = sp.id
             where coalesce(sp.parent_stop_place, false) = true
             group by sp.id, sp.netex_id
            having coalesce(string_agg(distinct spc.children_id::TEXT, ' - '), '') like '')
  loop
    delete from stop_place_key_values        where stop_place_id = l.spid;
    delete from stop_place_alternative_names where stop_place_id = l.spid;
    delete from stop_place                   where id = l.spid;
  end loop;

  -- suppression des quais sans imported-id
  for l in (select q.id qid, q.netex_id,
                   string_agg(distinct qkv.key_values_key, ' - ') keys,
                   string_agg(distinct vi2.items, ' - ') importedid,
                   position('imported-id' in string_agg(distinct qkv.key_values_key, ' - '))
                   from quay q
                   left join quay_key_values qkv  on qkv.quay_id = q.id
                   left join quay_key_values qkv2 on qkv2.quay_id = q.id and qkv2.key_values_key like 'imported-id'
                   left join value_items vi2 on vi2.value_id = qkv2.key_values_id
                  group by q.id, q.netex_id
                  having (position('imported-id' in string_agg(distinct qkv.key_values_key, ' - ')) = 0
                         or
                         coalesce(string_agg(distinct vi2.items, ' - '), '') like ''))
  loop
    delete from quay_key_values  where quay_id  = l.qid;
    delete from stop_place_quays where quays_id = l.qid;
    delete from quay             where id       = l.qid;
  end loop;

  -- SP sans quay
  for l in (select sp.id spid, sp.netex_id
              from stop_place sp
              left join stop_place_quays spq on spq.stop_place_id = sp.id
             where spq.quays_id is null and sp.parent_stop_place = false)
  loop
    delete from stop_place_key_values        where stop_place_id = l.spid;
    delete from stop_place_children spc      where children_id   = l.spid;
    delete from stop_place_alternative_names where stop_place_id = l.spid;
    delete from stop_place                   where id            = l.spid;
  end loop;

  -- pem sans sp
  for l in (select sp.id spid, sp.netex_id, spc.children_id
              from stop_place sp
              left join stop_place_children spc on spc.stop_place_id = sp.id
             where sp.parent_stop_place = true and spc.children_id is null)
  loop
    -- delete from stop_place where id = l.spid;
    null;  -- @todo
  end loop;

--  raise exception 'boom';
return true;
end;
$$;