CREATE TABLE providers
(
    id   BIGINT PRIMARY KEY,
    name CHARACTER VARYING(255) NOT NULL
);

INSERT INTO providers (id, name)
VALUES (1, 'SQYBUS');

INSERT INTO providers (id, name)
VALUES (2, 'PERRIER');

INSERT INTO providers (id, name)
VALUES (3, 'TVM');

INSERT INTO providers (id, name)
VALUES (4, 'CEOBUS');

INSERT INTO providers (id, name)
VALUES (5, 'CTVMI');

INSERT INTO providers (id, name)
VALUES (6, 'STILE');

INSERT INTO providers (id, name)
VALUES (7, 'TIMBUS');

INSERT INTO providers (id, name)
VALUES (8, 'RD_BREST');

INSERT INTO providers (id, name)
VALUES (9, 'RD_ANGERS');

INSERT INTO providers (id, name)
VALUES (10, 'TEST');

INSERT INTO providers (id, name)
VALUES (11, 'MOBICITEL40');

INSERT INTO providers (id, name)
VALUES (12, 'MOBICITE469');

INSERT INTO providers (id, name)
VALUES (13, 'CTVH');

INSERT INTO providers (id, name)
VALUES (14, 'RDLA');

INSERT INTO providers (id, name)
VALUES (15, 'SAINT_MALO');

INSERT INTO providers (id, name)
VALUES (16, 'VALENCIENNES');


ALTER TABLE stop_place ADD COLUMN provider_id BIGINT;
ALTER TABLE stop_place ADD CONSTRAINT fk_provider_id foreign key (provider_id) references providers;

UPDATE stop_place SET provider_id=10 WHERE provider_id IS NULL;

ALTER TABLE stop_place ALTER COLUMN provider_id SET NOT NULL;
