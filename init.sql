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
  id SERIAL,
  entry_time TIMESTAMPTZ NOT NULL,
  participant_id INT NOT NULL,
  lottery_id INT NOT NULL,
  FOREIGN KEY (lottery_id) REFERENCES lottery(id),
  FOREIGN KEY (participant_id) REFERENCES participant(id)
);

CREATE TABLE winner (
  id SERIAL PRIMARY KEY,
  win_date DATE NOT NULL,
  lottery_id INT NOT NULL,
  entry_id INT NOT NULL
);

INSERT INTO lottery (name) VALUES ('first and only lottery');

SELECT create_hypertable('entry', 'entry_time', chunk_time_interval => INTERVAL '1 day');