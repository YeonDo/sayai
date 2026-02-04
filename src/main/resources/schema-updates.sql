ALTER TABLE ft_score_rotisserie ADD CONSTRAINT uk_game_player_round UNIQUE (fantasy_game_seq, player_id, round);
ALTER TABLE ft_players ADD CONSTRAINT chk_player_cost CHECK (cost >= 0);
