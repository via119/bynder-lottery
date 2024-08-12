CREATE TABLE participant (
  id SERIAL PRIMARY KEY,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL
);

CREATE TABLE lottery (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL
);

CREATE TABLE entry (
  id SERIAL PRIMARY KEY,
  entry_time TIMESTAMPTZ NOT NULL,
  participant_id INT NOT NULL,
  lottery_id INT NOT NULL,
  FOREIGN KEY (lottery_id) REFERENCES lottery(id),
  FOREIGN KEY (participant_id) REFERENCES participant(id)
);

CREATE TABLE winner (
  id SERIAL PRIMARY KEY,
  win_time TIMESTAMPTZ NOT NULL,
  participant_id INT NOT NULL,
  lottery_id INT NOT NULL,
  FOREIGN KEY (lottery_id) REFERENCES lottery(id),
  FOREIGN KEY (participant_id) REFERENCES participant(id)
);

INSERT INTO lottery (name) VALUES ('first and only lottery');