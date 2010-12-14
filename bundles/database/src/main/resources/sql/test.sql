-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS test (
  ID BIGINT IDENTITY
);

INSERT INTO test VALUES (1),(2);

--//@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS test;
