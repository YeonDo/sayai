1. **Create KBO Models**
   - Create `KboLigue.java` in `src/main/java/com/sayai/record/kbo/model/`.
   - Create `KboGame.java` in `src/main/java/com/sayai/record/kbo/model/`.
   - Create `KboHit.java` in `src/main/java/com/sayai/record/kbo/model/`.
   - Create `KboPitch.java` in `src/main/java/com/sayai/record/kbo/model/`.
   - Make sure their fields represent KBO records and use similar properties as the original models but mapped to `kbo_*` tables.
   - Link `KboHit` and `KboPitch` to a `KboPlayer` if necessary, or just use the existing `Player` model as players might be shared.

2. **Create Repositories**
   - Create `KboLigueRepository.java`, `KboGameRepository.java`, `KboHitRepository.java`, `KboPitchRepository.java` in `src/main/java/com/sayai/record/kbo/repository/`.
   - Add necessary queries for calculating player statistics.

3. **Create DTOs**
   - Create DTOs if necessary to represent the responses, or reuse the existing ones (`PlayerRecord`, `PlayerDto`, `PitcherDto`) to ensure API output consistency.

4. **Create KBO Service**
   - Create `KboHitService.java`, `KboPitchService.java`, `KboPlayerService.java` to handle data retrieval.
   - Calculate stats (Batting average, ERA, etc.) as the original services do, but specifically from the `Kbo*` tables.
   - Missing data should be explicitly set to `null` instead of default 0s as specified by the prompt: "없는 데이터의 경우 null 로 처리해줘".

5. **Create KBO Controller**
   - Create `KboPlayerController.java` mapped to `/apis/v1/kboplayer`.
   - Implement the same paths as in `PlayerController`:
     - `GET /{id}`
     - `GET /all`
     - `GET /hitter/all`
     - `GET /pitcher/all`
     - `GET /hitter/{playerId}`
     - `GET /pitcher/{playerId}`
     - `GET /hitter/{playerId}/period`
   - Route logic to use the new KBO services.

6. **Pre-commit Steps**
   - Ensure pre-commit verification and testing runs.
