CREATE TABLE IF NOT EXISTS kbo_hitter_stats (
  player_id BIGINT NOT NULL,
  season INT NOT NULL,
  ab INT,
  pa INT,
  hit INT,
  avg VARCHAR(10),
  hr INT,
  rbi INT,
  so INT,
  sb INT,
  PRIMARY KEY (player_id, season)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kbo_pitcher_stats (
  player_id BIGINT NOT NULL,
  season INT NOT NULL,
  outs INT,
  er INT,
  era VARCHAR(10),
  win INT,
  so INT,
  save INT,
  bb INT,
  phit INT,
  whip VARCHAR(10),
  PRIMARY KEY (player_id, season)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS app_posts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(50),
  title VARCHAR(255),
  content TEXT,
  created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
