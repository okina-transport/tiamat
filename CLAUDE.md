# CLAUDE.md

Consignes spécifiques au projet Tiamat (registre des arrêts, parkings et POI), fork du projet Tiamat d'Entur.

## Identifiants : ignorer les commentaires « NSR »

Tous les commentaires du code qui parlent d'ids « NSR » doivent être ignorés. Ces ids viennent du projet d'origine (Entur) qui a été forké : nous n'utilisons pas les ids de la même manière.

## Utilisation des ids dans ce projet

Exemple pour les points d'arrêt (StopPlace) et les quais (Quay), le principe est le même pour les autres objets (parkings, POI…) :

1. Un producteur envoie un objet avec son propre id, par exemple `PROV1:Quay:toto`.
2. Tiamat crée l'objet avec un `netex_id` généré, par exemple `MOBIITI:Quay:1234`.
3. L'id du producteur est conservé dans la clé `imported-id` de l'objet :
   - jointure de la table de l'objet avec sa table `xxx_key_values` (`quay` avec `quay_key_values`, `stop_place` avec `stop_place_key_values`, etc.) ;
   - filtrage avec `key_values_key = 'imported-id'`.

Les valeurs elles-mêmes sont dans `value_items` (`xxx_key_values.key_values_id` → `value_items.value_id`, colonne `items`). Exemple pour retrouver un quai à partir de l'id du producteur :

```sql
SELECT q.netex_id, q.version, vi.items AS imported_id
FROM quay q
JOIN quay_key_values qkv ON qkv.quay_id = q.id
JOIN value_items vi ON vi.value_id = qkv.key_values_id
WHERE qkv.key_values_key = 'imported-id'
  AND vi.items = :producerId;
```

Côté code :
- la clé est la constante `NetexIdMapper.ORIGINAL_ID_KEY` (`imported-id`) ;
- `DataManagedObjectStructure.getOriginalIds()` renvoie les ids producteur d'un objet ;
- à l'import NeTEx, `NetexIdMapper.toTiamatModel()` déplace l'id du producteur dans `imported-id`, sauf si son préfixe est déjà un préfixe valide de la stack (l'id est alors conservé comme `netex_id`).
