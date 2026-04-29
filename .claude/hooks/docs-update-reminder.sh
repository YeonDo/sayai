#!/bin/bash
# PostToolUse hook: Java 소스 변경 시 관련 .claude/docs 업데이트 알림

INPUT=$(cat)

# Extract file_path from tool_input (python3 → python 순으로 시도)
FILE_PATH=$(echo "$INPUT" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(data.get('tool_input', {}).get('file_path', ''))
except:
    print('')
" 2>/dev/null)

# python3 실패 시 python으로 재시도
if [ -z "$FILE_PATH" ]; then
  FILE_PATH=$(echo "$INPUT" | python -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(data.get('tool_input', {}).get('file_path', ''))
except:
    print('')
" 2>/dev/null)
fi

[ -z "$FILE_PATH" ] && exit 0

# Java 소스 파일만 검사
echo "$FILE_PATH" | grep -qE '\.java$' || exit 0

# 테스트 파일 제외
echo "$FILE_PATH" | grep -qiE 'Test\.java$' && exit 0

FILENAME=$(basename "$FILE_PATH" .java)
DOCS=()

case "$FILENAME" in
  *FantasyRosterService*)
    DOCS+=("system/trade-system.md" "system/waiver-system.md" "apis/fantasy-roster.md") ;;
  *FantasyDraftService*)
    DOCS+=("system/draft-system.md" "apis/fantasy-draft.md") ;;
  *FantasyScoringService*)
    DOCS+=("system/rotisserie-scoring.md") ;;
  *FantasyGameService*)
    DOCS+=("apis/fantasy-game.md" "system/draft-system.md") ;;
  *FantasyRankingService*)
    DOCS+=("apis/fantasy-ranking.md") ;;
  *DraftScheduler*)
    DOCS+=("system/draft-system.md") ;;
  *WaiverScheduler*)
    DOCS+=("system/waiver-system.md") ;;
  *FcmService*)
    DOCS+=("apis/fcm.md") ;;
  *FcmController*)
    DOCS+=("apis/fcm.md") ;;
  *Rule*Validator*)
    DOCS+=("system/draft-system.md") ;;
  *Scoring*Strategy*|*ScoringStrategy*)
    DOCS+=("system/rotisserie-scoring.md") ;;
  *Controller*)
    echo "$FILENAME" | grep -qi "Draft"   && DOCS+=("apis/fantasy-draft.md")
    echo "$FILENAME" | grep -qi "Roster"  && DOCS+=("apis/fantasy-roster.md")
    echo "$FILENAME" | grep -qi "Game"    && DOCS+=("apis/fantasy-game.md")
    echo "$FILENAME" | grep -qi "Ranking" && DOCS+=("apis/fantasy-ranking.md")
    echo "$FILENAME" | grep -qi "Auth"    && DOCS+=("apis/auth.md")
    echo "$FILENAME" | grep -qi "Player"  && DOCS+=("apis/player-stats.md")
    [ ${#DOCS[@]} -eq 0 ] && DOCS+=("apis/") ;;
  *Service*|*Scheduler*)
    DOCS+=("system/") ;;
esac

if [ ${#DOCS[@]} -gt 0 ]; then
  echo "[DOCS HOOK] $FILENAME 변경 감지 → 아래 문서를 확인하고 필요시 업데이트하세요:"
  for doc in "${DOCS[@]}"; do
    echo "  .claude/docs/$doc"
  done
fi
