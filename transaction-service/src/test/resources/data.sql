DELETE FROM accounts;
DELETE FROM users;

INSERT INTO users (id, "role") VALUES (1, 'ADMIN');
INSERT INTO users (id, "role") VALUES (2, 'PERSONAL');
INSERT INTO accounts (id, balance) VALUES (1, 2000.0);
