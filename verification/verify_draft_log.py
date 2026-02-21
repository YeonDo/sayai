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

        # 2. Participants (Mock 10 participants)
        page.route("**/apis/v1/fantasy/games/100/participants", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='[{},{},{},{},{},{},{},{},{},{}]'
        ))

        # 3. Draft Logs (The main test)
        # We need enough logs to test round rollover
        # With 10 participants:
        # Pick 1 -> 1R - 1pick
        # Pick 10 -> 1R - 10pick
        # Pick 11 -> 2R - 1pick
        page.route("**/apis/v1/fantasy/games/100/logs?type=DRAFT", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='''[
                { "seq": 501, "playerName": "P1", "timestamp": "2023-10-27T10:00:00" },
                { "seq": 502, "playerName": "P2", "timestamp": "2023-10-27T10:01:00" },
                { "seq": 503, "playerName": "P3", "timestamp": "2023-10-27T10:02:00" },
                { "seq": 504, "playerName": "P4", "timestamp": "2023-10-27T10:03:00" },
                { "seq": 505, "playerName": "P5", "timestamp": "2023-10-27T10:04:00" },
                { "seq": 506, "playerName": "P6", "timestamp": "2023-10-27T10:05:00" },
                { "seq": 507, "playerName": "P7", "timestamp": "2023-10-27T10:06:00" },
                { "seq": 508, "playerName": "P8", "timestamp": "2023-10-27T10:07:00" },
                { "seq": 509, "playerName": "P9", "timestamp": "2023-10-27T10:08:00" },
                { "seq": 510, "playerName": "P10", "timestamp": "2023-10-27T10:09:00" },
                { "seq": 511, "playerName": "P11", "timestamp": "2023-10-27T10:10:00" }
            ]'''
        ))

        # Load file via localhost
        page.goto("http://localhost:8000/verification/mock_draft_log.html")

        # Wait for game selector to be populated and selected
        expect(page.locator("#gameSelector")).to_have_value("100")

        # Wait for table row to appear
        page.wait_for_selector("text=P1")

        # Verify content
        # Row 1 (Index 0): Should be 1R - 1pick
        row1_text = page.locator("#draftLogBody tr:nth-child(1) td:nth-child(1)").inner_text()
        print(f"Row 1: {row1_text}")
        assert "1R - 1pick (1픽)" in row1_text, f"Expected 1R - 1pick (1픽), got {row1_text}"

        # Row 10 (Index 9): Should be 1R - 10pick
        row10_text = page.locator("#draftLogBody tr:nth-child(10) td:nth-child(1)").inner_text()
        print(f"Row 10: {row10_text}")
        assert "1R - 10pick (10픽)" in row10_text, f"Expected 1R - 10pick (10픽), got {row10_text}"

        # Row 11 (Index 10): Should be 2R - 1pick
        row11_text = page.locator("#draftLogBody tr:nth-child(11) td:nth-child(1)").inner_text()
        print(f"Row 11: {row11_text}")
        assert "2R - 1pick (11픽)" in row11_text, f"Expected 2R - 1pick (11픽), got {row11_text}"

        # Screenshot
        page.screenshot(path="verification/verification_rounds.png")
        print("Verification successful, screenshot saved.")

        browser.close()

if __name__ == "__main__":
    run()
