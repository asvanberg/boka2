-- Copy table

# -- !Ups

CREATE TABLE copy (
  identifier VARCHAR(255) NOT NULL PRIMARY KEY,
  product_id BIGINT REFERENCES product (id),
  note TEXT DEFAULT NULL
);

# -- !Downs

DROP TABLE copy;
