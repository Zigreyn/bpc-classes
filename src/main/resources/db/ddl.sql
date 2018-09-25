CREATE TABLE customers (
  id         INTEGER PRIMARY KEY,
  name        VARCHAR(30),
  account_id  integer (10)
);

CREATE TABLE accounts (
  id         INTEGER PRIMARY KEY,
  account_number  integer (10),
  currency        VARCHAR(3),
  amount          integer (20)
);

CREATE TABLE currency_rates (
  id                 INTEGER PRIMARY KEY,
  currency_from      VARCHAR(3),
  currency_to        VARCHAR(3),
  multiplier        float (20)
);