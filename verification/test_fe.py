import json
from playwright.sync_api import Page, expect, sync_playwright

def test_ui(page: Page):

    # Intercept Auth
    def handle_auth_post(route):
        route.fulfill(
            status=200,
            content_type="application/json",
            body=json.dumps({"token": "fake-token", "playerId": 1})
        )

    def handle_auth_me(route):
        route.fulfill(
            status=200,
            content_type="application/json",
            body=json.dumps({"playerId": 1, "name": "Test User"})
        )

    def handle_games(route):
        route.fulfill(
            status=200,
            content_type="application/json",
            body=json.dumps([{"seq": 1, "title": "Test Game", "status": "ONGOING", "ruleType": "RULE_2"}])
        )

    def handle_details(route):
        route.fulfill(
            status=200,
            content_type="application/json",
            body=json.dumps({
                "seq": 1,
                "salaryCap": 100,
                "useFirstPickRule": True,
                "participants": [
                    {
                        "participantId": 1,
                        "preferredTeam": "Doosan",
                        "roster": [
                            {"seq": 1, "name": "Player 1", "team": "Doosan", "foreignerType": "NONE", "cost": 50, "position": "SP", "assignedPosition": "SP-1"},
                            {"seq": 2, "name": "Player 2", "team": "Doosan", "foreignerType": "NONE", "cost": 10, "position": "C", "assignedPosition": "C"}
                        ]
                    }
                ]
            })
        )

    def handle_my_picks(route):
        route.fulfill(
            status=200,
            content_type="application/json",
            body=json.dumps([
                {"seq": 1, "name": "Player 1", "team": "Doosan", "foreignerType": "NONE", "cost": 50, "position": "SP", "assignedPosition": "SP-1"},
                {"seq": 2, "name": "Player 2", "team": "Doosan", "foreignerType": "NONE", "cost": 10, "position": "C", "assignedPosition": "C"}
            ])
        )

    page.route("**/apis/v1/auth/login", handle_auth_post)
    page.route("**/apis/v1/auth/me*", handle_auth_me)
    page.route("**/apis/v1/fantasy/my-games*", handle_games)
    page.route("**/apis/v1/fantasy/games/*/details*", handle_details)
    page.route("**/apis/v1/fantasy/games/*/my-picks*", handle_my_picks)

    # set up cookie directly
    page.goto("http://localhost:8080/")

    page.context.add_cookies([
        {
            "name": "accessToken",
            "value": "fake-token",
            "domain": "localhost",
            "path": "/"
        }
    ])

    page.goto("http://localhost:8080/fantasy/trade")

    page.wait_for_timeout(2000)
    page.screenshot(path="verification/trade_discount.png")

    # test my-team
    page.goto("http://localhost:8080/fantasy/my-team")
    page.wait_for_timeout(2000)
    # Hover over the first player slot
    try:
        page.hover(".pitcher-slot.filled", timeout=2000)
        page.wait_for_timeout(500)
    except:
        pass
    page.screenshot(path="verification/myteam_discount.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            test_ui(page)
        finally:
            browser.close()
