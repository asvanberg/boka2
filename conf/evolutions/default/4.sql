-- Copy table

# -- !Ups

CREATE TABLE returned_loans (
  identifier VARCHAR(255) NOT NULL PRIMARY KEY REFERENCES copy,
  borrower VARCHAR(255) NOT NULL,
  borrowed DATE NOT NULL,
  returned DATE NOT NULL
);

# -- !Downs

DROP TABLE returned_loans;
