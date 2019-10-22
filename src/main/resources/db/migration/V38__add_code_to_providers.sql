ALTER TABLE providers ADD COLUMN code CHARACTER VARYING(255) NOT NULL DEFAULT 'da_code';

UPDATE providers set code ='sqybus' WHERE name='SQYBUS';
UPDATE providers set code ='perrier' WHERE name='PERRIER';
UPDATE providers set code ='tvm' WHERE name='TVM';
UPDATE providers set code ='ceobus' WHERE name='CEOBUS';
UPDATE providers set code ='ctvmi' WHERE name='CTVMI';
UPDATE providers set code ='mobicite' WHERE name='MOBICITE';
UPDATE providers set code ='stile' WHERE name='STILE';
UPDATE providers set code ='timbus' WHERE name='TIMBUS';
UPDATE providers set code ='rd_brest' WHERE name='RD_BREST';
UPDATE providers set code ='rd_angers' WHERE name='RD_ANGERS';
UPDATE providers set code ='test' WHERE name='TEST';
