CREATE TABLE providers
(
    id   BIGINT PRIMARY KEY,
    name CHARACTER VARYING(255) NOT NULL
);

INSERT INTO providers (id, name)
VALUES (1, 'SQYBUS');
INSERT INTO providers (id, name)
VALUES (1001, 'MOSAIC/SQYBUS');

INSERT INTO providers (id, name)
VALUES (2, 'PERRIER');
INSERT INTO providers (id, name)
VALUES (1002, 'MOSAIC/PERRIER');

INSERT INTO providers (id, name)
VALUES (3, 'TVM');
INSERT INTO providers (id, name)
VALUES (1003, 'MOSAIC/TVM');

INSERT INTO providers (id, name)
VALUES (4, 'CEOBUS');
INSERT INTO providers (id, name)
VALUES (1004, 'MOSAIC/CEOBUS');

INSERT INTO providers (id, name)
VALUES (5, 'CTVMI');
INSERT INTO providers (id, name)
VALUES (1005, 'MOSAIC/CTVMI');

INSERT INTO providers (id, name)
VALUES (6, 'MOBICITE');
INSERT INTO providers (id, name)
VALUES (1006, 'MOSAIC/MOBICITE');

INSERT INTO providers (id, name)
VALUES (7, 'STILE');
INSERT INTO providers (id, name)
VALUES (1007, 'MOSAIC/STILE');

INSERT INTO providers (id, name)
VALUES (8, 'TIMBUS');
INSERT INTO providers (id, name)
VALUES (1008, 'MOSAIC/TIMBUS');

INSERT INTO providers (id, name)
VALUES (9, 'RD_BREST');
INSERT INTO providers (id, name)
VALUES (1009, 'MOSAIC/RD_BREST');

INSERT INTO providers (id, name)
VALUES (10, 'RD_ANGERS');
INSERT INTO providers (id, name)
VALUES (1010, 'MOSAIC/RD_ANGERS');

INSERT INTO providers (id, name)
VALUES (11, 'TEST');
INSERT INTO providers (id, name)
VALUES (1011, 'MOSAIC/TEST');

ALTER TABLE stop_place ADD COLUMN provider_id LONG NOT NULL;
