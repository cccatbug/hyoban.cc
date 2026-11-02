-- Each trip leg's route, by the leg's key: a driving route between two places' points,
-- or with `train:` before it a rail line. The API fetches the ones a trip needs after a
-- write and marks deleted the ones no trip needs any more, so a moved place or a changed
-- trip is fetched again. Like the other tables, the record is kept as written.

CREATE TABLE routes (
  key TEXT PRIMARY KEY,
  rev INTEGER NOT NULL,
  deleted_at TEXT,
  record TEXT NOT NULL
);
