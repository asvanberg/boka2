-- Change borrower from string (username) to long (id)

# -- !Ups

ALTER TABLE ongoing_loans ALTER COLUMN borrower TYPE BIGINT;
ALTER TABLE returned_loans ALTER COLUMN borrower TYPE BIGINT;

# -- !Downs

ALTER TABLE ongoing_loans ALTER COLUMN borrower TYPE VARCHAR(255);
ALTER TABLE returned_loans ALTER COLUMN borrower TYPE VARCHAR(255);
