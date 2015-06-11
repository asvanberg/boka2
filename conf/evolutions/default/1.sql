-- Products table

# -- !Ups

CREATE TABLE product (
  id SERIAL NOT NULL PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  description TEXT DEFAULT NULL
);

# -- !Downs

DROP TABLE product;
