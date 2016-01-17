-- TEXT in H2 is CLOB which is not coercible to String
-- VARCHAR is identical to TEXT in PostgreSQL

ALTER TABLE product ALTER COLUMN description TYPE VARCHAR;
ALTER TABLE copy ALTER COLUMN note TYPE VARCHAR;