-- Sauvegarde de la liste des noms des points d'arrêt avant modification dans une variable --

CREATE TEMP TABLE modifStopName (
  id     BIGINT,
  name   VARCHAR(100),
  modif  VARCHAR(100),
  status VARCHAR(100)
);

-- Insertion + transformation en minuscules dans la colonne modif --

INSERT INTO modifStopName
  SELECT
    id,
    name_value,
    LOWER(name_value)
  FROM stop_place SP
    INNER JOIN (
                 SELECT
                   netex_id,
                   MAX(version) AS maxVersion
                 FROM stop_place
                 GROUP BY netex_id) NV
      ON SP.netex_id = NV.netex_id
         AND SP.version = NV.maxVersion;

-- Majuscules première lettre de chaque mot --

UPDATE modifStopName
SET modif = initcap(modif);

-- Caractères spéciaux de fin --
-- Tiret --
UPDATE modifStopName
SET modif = SUBSTRING(modif, 0, length(modif))
WHERE SUBSTRING(modif, length(modif)) = '-';

-- Point --
UPDATE modifStopName
SET modif = SUBSTRING(modif, 0, length(modif))
WHERE SUBSTRING(modif, length(modif)) = '.';

-- Accents --
UPDATE modifStopName
SET modif = REPLACE(modif, 'College', 'Collège');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Collége', 'Collège');

UPDATE modifStopName
SET modif = REPLACE(modif, ' A ', ' à ');

UPDATE modifStopName
SET modif = REPLACE(modif, ' À ', ' à ');

-- Acronymes --
UPDATE modifStopName
SET modif = REPLACE(modif, 'Z.A', 'ZA');

UPDATE modifStopName
SET modif = REPLACE(modif, 'ZA.', 'ZA');

UPDATE modifStopName
SET modif = REPLACE(modif, 'ZAE.', 'ZAE');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Z.I.', 'ZI');

-- Articles en minuscule --
UPDATE modifStopName
SET modif = REPLACE(modif, ' De ', ' de ');

UPDATE modifStopName
SET modif = REPLACE(modif, ' Le ', ' le ');

UPDATE modifStopName
SET modif = REPLACE(modif, ' La ', ' la ');

UPDATE modifStopName
SET modif = REPLACE(modif, ' Du ', ' du ');

UPDATE modifStopName
SET modif = REPLACE(modif, ' Des ', ' des ');

UPDATE modifStopName
SET modif = REPLACE(modif, '- la ', '- LA ');

-- Abréviations --
UPDATE modifStopName
SET modif = REPLACE(modif, 'Rte ', 'Route ');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Lot.', 'Lotissement');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Imp. ', 'Impasse ');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Av.', 'Avenue');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Al.', 'Allée');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Pl.', 'Place');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Ch.', 'Chemin');

UPDATE modifStopName
SET modif = REPLACE(modif, 'St ', 'Saint ');

UPDATE modifStopName
SET modif = REPLACE(modif, 'St-', 'Saint-');

-- Apostrophes --
UPDATE modifStopName
SET modif = REPLACE(modif, ' D''', ' d''');

UPDATE modifStopName
SET modif = REPLACE(modif, ' L''', ' l''');

UPDATE modifStopName
SET modif = REPLACE(modif, '-L''', '-l''');

UPDATE modifStopName
SET modif = REPLACE(modif, ' D ', ' d''');

UPDATE modifStopName
SET modif = REPLACE(modif, '-D''', '-d''');

-- Nombres --
UPDATE modifStopName
SET modif = REPLACE(modif, 'Quatre', '4');

-- Termes particuliers --
UPDATE modifStopName
SET modif = REPLACE(modif, 'Sncf', 'SNCF');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Inra', 'INRA');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Irsa', 'IRSA');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Capc', 'CAPC');

UPDATE modifStopName
SET modif = REPLACE(modif, ' Lpi', ' LPI');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Ddass', 'DDASS');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Aft', 'AFT');

UPDATE modifStopName
SET modif = REPLACE(modif, ' Lep', ' LEP');

UPDATE modifStopName
SET modif = REPLACE(modif, ' Cfa', ' CFA');

UPDATE modifStopName
SET modif = REPLACE(modif, ' Hlm', ' HLM');

UPDATE modifStopName
SET modif = REPLACE(modif, ' Zae', ' ZAE');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Enap', ' ENAP');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Edf', 'EDF');

UPDATE modifStopName
SET modif = REPLACE(modif, 'Min', 'MIN');

-- On récupère les dernières versions des stops --

CREATE TEMP TABLE lastVersionStop AS (
  SELECT
    id,
    SP.netex_id,
    changed,
    created,
    from_date,
    to_date,
    version,
    version_comment,
    description_lang,
    description_value,
    name_lang,
    name_value,
    private_code_type,
    private_code_value,
    short_name_lang,
    short_name_value,
    centroid,
    all_areas_wheelchair_accessible,
    covered,
    parent_site_ref,
    parent_site_ref_version,
    air_submode,
    border_crossing,
    bus_submode,
    coach_submode,
    funicular_submode,
    metro_submode,
    public_code,
    rail_submode,
    stop_place_type,
    telecabin_submode,
    tram_submode,
    transport_mode,
    water_submode,
    weighting,
    polygon_id,
    accessibility_assessment_id,
    place_equipments_id,
    topographic_place_id,
    changed_by,
    parent_stop_place
  FROM stop_place SP
    INNER JOIN (
                 SELECT
                   netex_id,
                   MAX(version) AS maxVersion
                 FROM stop_place
                 GROUP BY netex_id) NV
      ON SP.netex_id = NV.netex_id
         AND SP.version = NV.maxVersion
);


CREATE TEMP TABLE lastVersionIdAccessibilityLimitation (
  id_value BIGINT
);

INSERT INTO lastVersionIdAccessibilityLimitation
  SELECT MAX(id_value)
  FROM id_generator
  WHERE table_name = 'AccessibilityLimitation';

-- Mise à jour ancien point d'arrêt et création d'une nouvelle version --

DO $$DECLARE
  modifStopNameList                  RECORD;
  _netex_id_accessibility_limitation BIGINT := (SELECT id_value
                                                FROM lastVersionIdAccessibilityLimitation);
BEGIN
  FOR modifStopNameList IN SELECT *
                           FROM modifStopName
                           WHERE name != modif
  LOOP

    _netex_id_accessibility_limitation := _netex_id_accessibility_limitation + 1;

    -- On valorise le to_date dans l'ancienne version du point d'arrêt pour définir une date d'expiration --

    UPDATE stop_place
    SET to_date = LOCALTIMESTAMP
    FROM modifStopName
    WHERE stop_place.id = modifStopNameList.id;

    -- On crée des valeurs dans les tables liés à la mise à jour du stop --

    INSERT INTO persistable_polygon
      SELECT MAX(id) + 1
      FROM persistable_polygon;

    INSERT INTO accessibility_assessment (id, netex_id, version, mobility_impaired_access)
      SELECT
        (SELECT MAX(id)
         FROM accessibility_assessment) + 1,
        ACE.netex_id,
        ACE.version + 1,
        mobility_impaired_access
      FROM accessibility_assessment ACE
        INNER JOIN (SELECT *
                    FROM lastVersionStop
                    WHERE lastVersionStop.id = modifStopNameList.id) LVS
          ON ACE.id = LVS.accessibility_assessment_id;

    INSERT INTO id_generator (table_name, id_value)
    VALUES ('AccessibilityLimitation', _netex_id_accessibility_limitation);

    INSERT INTO accessibility_limitation (id, netex_id, version, audible_signals_available, escalator_free_access, lift_free_access, step_free_access, wheelchair_access)
      SELECT
        (SELECT MAX(id)
         FROM accessibility_limitation) + 1,
        'NAQ:AccessibilityLimitation:' || CAST(_netex_id_accessibility_limitation AS VARCHAR),
        1,
        audible_signals_available,
        escalator_free_access,
        lift_free_access,
        step_free_access,
        wheelchair_access
      FROM accessibility_limitation AL
        JOIN accessibility_assessment_limitations ASL ON AL.id = ASL.limitations_id
        , lastVersionStop
      WHERE lastVersionStop.accessibility_assessment_id = AL.id AND lastVersionStop.id = modifStopNameList.id;


    INSERT INTO accessibility_assessment_limitations
      SELECT
        MAX(accessibility_assessment.id),
        MAX(accessibility_limitation.id)
      FROM accessibility_assessment, accessibility_limitation;


    INSERT INTO stop_place
      SELECT
        (SELECT MAX(id)
         FROM stop_place) + 1,
        lastVersionStop.netex_id,
        LOCALTIMESTAMP,
        lastVersionStop.created,
        LOCALTIMESTAMP,
        NULL,
        (lastVersionStop.version + 1),
        lastVersionStop.version_comment,
        lastVersionStop.description_lang,
        lastVersionStop.description_value,
        lastVersionStop.name_lang,
        modifStopNameList.modif,
        lastVersionStop.private_code_type,
        lastVersionStop.private_code_value,
        lastVersionStop.short_name_lang,
        lastVersionStop.short_name_value,
        lastVersionStop.centroid,
        lastVersionStop.all_areas_wheelchair_accessible,
        lastVersionStop.covered,
        lastVersionStop.parent_site_ref,
        lastVersionStop.parent_site_ref_version,
        lastVersionStop.air_submode,
        lastVersionStop.border_crossing,
        lastVersionStop.bus_submode,
        lastVersionStop.coach_submode,
        lastVersionStop.funicular_submode,
        lastVersionStop.metro_submode,
        lastVersionStop.public_code,
        lastVersionStop.rail_submode,
        lastVersionStop.stop_place_type,
        lastVersionStop.telecabin_submode,
        lastVersionStop.tram_submode,
        lastVersionStop.transport_mode,
        lastVersionStop.water_submode,
        lastVersionStop.weighting,
        (SELECT MAX(id)
         FROM persistable_polygon),
        (SELECT MAX(id)
         FROM accessibility_assessment),
        lastVersionStop.place_equipments_id,
        lastVersionStop.topographic_place_id,
        lastVersionStop.changed_by,
        lastVersionStop.parent_stop_place
      FROM lastVersionStop
      WHERE lastVersionStop.id = modifStopNameList.id;

    -- On fait la liaison du nouveau stop place avec les quais --

    INSERT INTO quay (id, netex_id, changed, version, description_lang, description_value, name_value, private_code_type, private_code_value, centroid, public_code, polygon_id, accessibility_assessment_id)
      SELECT
        (SELECT MAX(id)
         FROM quay) + 1,
        QU.netex_id,
        LOCALTIMESTAMP,
        QU.version + 1,
        QU.description_lang,
        QU.description_value,
        QU.name_value,
        QU.private_code_type,
        QU.private_code_value,
        QU.centroid,
        QU.public_code,
        QU.polygon_id,
        QU.accessibility_assessment_id
      FROM quay QU
        JOIN stop_place_quays SPQ ON QU.id = SPQ.quays_id
        , lastVersionStop
      WHERE lastVersionStop.id = stop_place_quays.stop_place_id AND lastVersionStop.id = modifStopNameList.id;

    -- On fait la liaison du nouveau stop place avec les clés-valeurs, value et value_items --

    INSERT INTO value
      SELECT MAX(id) + 1
      FROM value;

    INSERT INTO stop_place_key_values
      SELECT
        (SELECT MAX(id)
         FROM stop_place),
        (SELECT MAX(id)
         FROM value),
        key_values_key
      FROM stop_place_key_values, lastVersionStop
      WHERE lastVersionStop.id = stop_place_key_values.stop_place_id AND lastVersionStop.id = modifStopNameList.id;


    INSERT INTO value_items
      SELECT
        value_id,
        items
      FROM value_items VI INNER JOIN value VAL ON VI.value_id = VAL.id
        INNER JOIN stop_place_key_values SPKV ON SPKV.key_values_id = VAL.id
      WHERE SPKV.stop_place_id = modifStopNameList.id;

  END LOOP;
END$$;

-- Récupération du dernier ID alternativeName --

CREATE TEMP TABLE lastVersionIdAlternativeName (
  id_value BIGINT
);

INSERT INTO lastVersionIdAlternativeName
  SELECT MAX(id_value)
  FROM id_generator
  WHERE table_name = 'AlternativeName';


UPDATE lastVersionIdAlternativeName
SET id_value = 0
WHERE id_value IS NULL;

-- Ajout des noms d'origine dans les alternatives names et les ID créés dans la table IDGenerator pour éviter les conflits de création par la suite --

DO $$DECLARE
  modifStopNameList          RECORD;
  _netex_id_alternative_name BIGINT := (SELECT id_value
                                        FROM lastVersionIdAlternativeName);
BEGIN
  FOR modifStopNameList IN SELECT *
                           FROM modifStopName
                           WHERE name != modif
  LOOP
    -- Condition, on vérifie si l'alternative name n'a pas déjà été ajouté --
    IF (SELECT id
        FROM modifStopName
        WHERE name != modif AND id IN (SELECT stop_place_id
                                       FROM stop_place_alternative_names SPAN INNER JOIN (SELECT id
                                                                                          FROM alternative_name
                                                                                          WHERE name_type = 'OTHER') AN ON SPAN.alternative_names_id = AN.id
                                       WHERE SPAN.stop_place_id = modifStopNameList.id)) IS NULL
    THEN
      _netex_id_alternative_name := _netex_id_alternative_name + 1;
      IF (SELECT MAX(id)
          FROM alternative_name) IS NOT NULL
      THEN
        INSERT INTO alternative_name (id, netex_id, version, name_lang, name_value, name_type)
          SELECT
            (SELECT MAX(id) + 1
             FROM alternative_name),
            'NAQ:AlternativeName:' || CAST(_netex_id_alternative_name AS VARCHAR),
            1,
            'fra',
            modifStopNameList.name,
            'OTHER';
      ELSE
        INSERT INTO alternative_name (id, netex_id, version, name_lang, name_value, name_type)
          SELECT
            1,
            'NAQ:AlternativeName:' || CAST(_netex_id_alternative_name AS VARCHAR),
            1,
            'fra',
            modifStopNameList.name,
            'OTHER';
      END IF;

      INSERT INTO id_generator (table_name, id_value)
      VALUES ('AlternativeName', _netex_id_alternative_name);

      INSERT INTO stop_place_alternative_names
        SELECT
          modifStopNameList.id,
          MAX(id)
        FROM alternative_name;

      UPDATE modifStopName
      SET status = 'do'
      WHERE id = modifStopNameList.id;

    ELSE
      UPDATE modifStopName
      SET status = 'done'
      WHERE id = modifStopNameList.id;
    END IF;
  END LOOP;
END$$;

-- On met à jour la forme du nom d'origine ajouté dans les alternatives names --

UPDATE alternative_name
SET name_value = REPLACE(name_value, '("', '');

UPDATE alternative_name
SET name_value = REPLACE(name_value, '")', '');

-- On export par sécurité en csv le nom avant et après transformation --
SELECT *
FROM modifStopName
WHERE name != modif;





