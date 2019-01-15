-- Requête pour retrouver des fusions de points d'arrêt --
-- version = 2 car après fusion une version est créé --
-- Changer les préfixes 1 et 2 par les préfixes de votre choix --

SELECT * FROM public.stop_place
WHERE version = 2
AND id IN (
  SELECT stop_place_id FROM public.stop_place_key_values WHERE key_values_id IN (
    SELECT id FROM public.value WHERE id IN (
      SELECT value_id FROM public.value_items WHERE (items LIKE '1') AND (items LIKE '2')
      )
    )
  )