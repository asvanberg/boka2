CREATE TABLE ongoing_loans (
  identifier VARCHAR(255) NOT NULL PRIMARY KEY REFERENCES copy,
  borrower VARCHAR(255) NOT NULL,
  borrowed DATE NOT NULL
);
