ALTER TABLE TESTTABLE DROP PRIMARY KEY;
ALTER TABLE TESTTABLE ADD CONSTRAINT TESTPK PRIMARY KEY(PK1,PK2);
DROP INDEX TESTTABLE_IDX1 ON TESTTABLE;
CREATE INDEX TESTINDEX ON TESTTABLE (AT2);
DROP INDEX TESTTABLE_IDX2 ON TESTTABLE;
CREATE UNIQUE INDEX TESTINDEX2 ON TESTTABLE (AT3);