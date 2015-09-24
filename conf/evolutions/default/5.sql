-- Admin table

# -- !Ups

CREATE TABLE IF NOT EXISTS admins (
  subject VARCHAR(255) NOT NULL PRIMARY KEY
);

# -- !Downs

DROP TABLE IF EXISTS admins;