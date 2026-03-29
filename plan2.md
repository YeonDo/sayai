1. Add `org.apache.poi:poi-ooxml:5.2.3` to `build.gradle` if not present. Let's check `build.gradle` first.
2. Create DTO `KboGameUploadRequest`? Since it's a multipart file upload with other parameters, we can use `@RequestParam` or a `@ModelAttribute` DTO.
3. Create `KboAdminController` under `com.sayai.kbo.controller` mapped to `/apis/v1/admin/kbo`.
   - Endpoint: `POST /game/upload`
   - Parameters: `season`, `home`, `away`, `homeScore`, `awayScore`, `result`, `MultipartFile file`.
4. Create `KboGameUploadService` under `com.sayai.kbo.service`.
   - Process `file.getInputStream()` with POI `XSSFWorkbook`.
   - Read Sheet 0 (away team) and Sheet 1 (home team).
   - For each sheet, find hitter rows and pitcher rows.
   - Hitters: Read "선수명" (C col, index 2), "타석" (M col, index 12), "타수" (N col, index 13), "안타" (O col, index 14), "타점" (P col, index 15), "득점" (Q col, index 16), "도루" (R col, index 17).
     - Actually, let me parse the image more carefully:
     - M (12): 타석
     - N (13): 타수
     - O (14): 안타
     - P (15): 타점
     - Q (16): 득점
     - R (17): 도루
     - Are there "삼진" and "홈런" columns? In the prompt: "kboHit 테이블에 데이터를 넣을때 타석이 0 인 선수의 데이터는 넣을 필요 없어." but `KboHit` has `so` and `hr`. If the Excel doesn't have them in the visible part, maybe they are in S and T? Let me just leave them as 0 if they don't exist, or try to read them from index 18 and 19? Or maybe I should read header names to find indices! That's safer. "선수명", "타석", "타수", "안타", "타점", "득점", "도루", "삼진", "홈런", "볼넷", "사구".
     - I will iterate over the header row to find column indices by name.
5. Save `KboGame` first (generating ID or providing it). Wait, the prompt says: "해당 ft_players 의 seq 를 player_id 로 삼아서 kboHit 테이블과 kboPitch 테이블을 업데이트 할거야." Does it mean we create a new `KboGame` record? The prompt says "input 으로는 시즌(ex. 2026), 홈팀명. 원정팀명, 홈팀 점수,. 원정팀 점수, 승리팀(result) , 경기 엑셀파일을 받을거야." So yes, create a new `KboGame` and save it, then save `KboHit` and `KboPitch`. But wait, how do we generate `game_idx`? Does `KboGame` have auto-increment? In `KboGame` entity: `@Id @Column(name = "game_idx") private Long id;`. It does NOT have `@GeneratedValue`. We need to generate it. Or maybe add `@GeneratedValue(strategy = GenerationType.IDENTITY)`? Let's check how `ft_games` or `game` handles it, or just use a max(id) + 1. Wait, I can just add `@GeneratedValue(strategy = GenerationType.IDENTITY)` to `KboGame.java`. Let me check if it works.
