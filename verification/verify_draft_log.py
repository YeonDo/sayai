from playwright.sync_api import sync_playwright, expect
import time

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # Mock APIs
        # 1. My Games
        page.route("**/apis/v1/fantasy/my-games?userId=1", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='[{"seq": 100, "title": "Test Game"}]'
        ))

        # 2. Draft Logs (The main test)
        page.route("**/apis/v1/fantasy/games/100/logs?type=DRAFT", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='''[
                {
                    "seq": 501,
                    "playerName": "Ohtani",
                    "playerTeam": "LAD",
                    "playerPosition": "SP, DH",
                    "participantName": "Team A",
                    "actionType": "DRAFT_PICK",
                    "details": "Pick #1",
                    "timestamp": "2023-10-27T10:00:00"
                },
                {
                    "seq": 502,
                    "playerName": "Judge",
                    "playerTeam": "NYY",
                    "playerPosition": "RF",
                    "participantName": "Team B",
                    "actionType": "DRAFT_PICK",
                    "details": "Pick #2",
                    "timestamp": "2023-10-27T10:05:00"
                }
            ]'''
        ))

        # Load file via localhost
        page.goto("http://localhost:8000/verification/mock_draft_log.html")

        # Wait for game selector to be populated and selected
        # Using expect to wait
        expect(page.locator("#gameSelector")).to_have_value("100")

        # It should auto-load the draft log due to logic
        # Wait for table row to appear
        # We look for the first row's first cell to contain "501"
        page.wait_for_selector("text=Ohtani")

        # Verify content
        row1_text = page.locator("#draftLogBody tr:nth-child(1)").inner_text()
        print(f"Row 1: {row1_text}")

        assert "501" in row1_text, "Seq 501 not found"
        assert "Ohtani" in row1_text, "Ohtani not found"
        assert "LAD" in row1_text, "LAD not found"
        assert "Team A" in row1_text, "Team A not found"

        row2_text = page.locator("#draftLogBody tr:nth-child(2)").inner_text()
        print(f"Row 2: {row2_text}")

        assert "502" in row2_text, "Seq 502 not found"
        assert "Judge" in row2_text, "Judge not found"

        # Screenshot
        page.screenshot(path="verification/verification.png")
        print("Verification successful, screenshot saved.")

        browser.close()

if __name__ == "__main__":
    run()
