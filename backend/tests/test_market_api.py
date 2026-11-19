import pytest
from fastapi.testclient import TestClient

from backend.app.main import create_app


@pytest.fixture
def client(tmp_path):
    app = create_app(
        database_path=tmp_path / "armin-market-test.sqlite",
        token_secret="test-only-armin-market-secret-32-bytes-minimum",
    )
    with TestClient(app) as test_client:
        yield test_client


def register(client, role, email):
    response = client.post("/api/v1/auth/register", json={
        "email": email,
        "password": "secure-test-password-2026",
        "role": role,
    })
    assert response.status_code == 201, response.text
    return {"Authorization": "Bearer " + response.json()["access_token"]}


def create_request(client, headers):
    response = client.post("/api/v1/requests", headers=headers, json={
        "title": "درخواست پمپ صنعتی",
        "description": "پمپ آب صنعتی با مشخصات فنی پیوست",
        "category": "industrial-equipment",
        "quantity": "5",
        "unit": "عدد",
        "currency": "IRR",
        "delivery_city": "تهران",
        "needed_by": "2026-12-15",
    })
    assert response.status_code == 201, response.text
    return response.json()


def test_buyer_request_is_persisted_and_visible_to_sellers_without_private_identity(client):
    buyer = register(client, "buyer", "buyer-one@example.test")
    seller = register(client, "seller", "seller-one@example.test")
    created = create_request(client, buyer)

    market = client.get("/api/v1/market/requests", headers=seller)
    assert market.status_code == 200
    item = next(row for row in market.json()["items"] if row["id"] == created["id"])
    assert item["title"] == "درخواست پمپ صنعتی"
    assert "buyer_email" not in item
    assert "buyer_id" not in item


def test_seller_quote_is_private_to_buyer_and_buyer_can_select_without_payment(client):
    buyer = register(client, "buyer", "buyer-two@example.test")
    seller = register(client, "seller", "seller-two@example.test")
    request = create_request(client, buyer)

    quote = client.post(f"/api/v1/requests/{request['id']}/quotes", headers=seller, json={
        "unit_price_minor": 2500000,
        "currency": "IRR",
        "lead_time_days": 4,
        "valid_until": "2026-12-01",
        "note": "تحویل طبق مشخصات درخواست",
    })
    assert quote.status_code == 201, quote.text
    quote_id = quote.json()["id"]

    seller_cannot_list_all_quotes = client.get(
        f"/api/v1/requests/{request['id']}/quotes", headers=seller
    )
    assert seller_cannot_list_all_quotes.status_code == 403

    quotes = client.get(f"/api/v1/requests/{request['id']}/quotes", headers=buyer)
    assert quotes.status_code == 200
    assert quotes.json()["items"][0]["id"] == quote_id

    selected = client.post(
        f"/api/v1/requests/{request['id']}/select-quote",
        headers=buyer,
        json={"quote_id": quote_id},
    )
    assert selected.status_code == 200, selected.text
    assert selected.json()["status"] == "QUOTE_SELECTED_PENDING_PARTIES"
    assert selected.json()["payment_status"] == "NOT_APPLICABLE"


def test_buyer_cannot_read_another_buyers_request_or_quotes(client):
    buyer_a = register(client, "buyer", "buyer-a@example.test")
    buyer_b = register(client, "buyer", "buyer-b@example.test")
    seller = register(client, "seller", "seller-a@example.test")
    request = create_request(client, buyer_a)

    quote = client.post(f"/api/v1/requests/{request['id']}/quotes", headers=seller, json={
        "unit_price_minor": 1000,
        "currency": "IRR",
        "lead_time_days": 3,
        "valid_until": "2026-12-01",
        "note": "",
    })
    assert quote.status_code == 201

    assert client.get(f"/api/v1/requests/{request['id']}", headers=buyer_b).status_code == 404
    assert client.get(f"/api/v1/requests/{request['id']}/quotes", headers=buyer_b).status_code == 404


def test_roles_are_enforced_for_request_and_quote_operations(client):
    buyer = register(client, "buyer", "role-buyer@example.test")
    seller = register(client, "seller", "role-seller@example.test")
    request = create_request(client, buyer)

    seller_create = client.post("/api/v1/requests", headers=seller, json={
        "title": "درخواست نامجاز", "description": "شرح کامل درخواست", "category": "other",
        "quantity": "1", "unit": "عدد", "currency": "IRR",
        "delivery_city": "تهران", "needed_by": None,
    })
    assert seller_create.status_code == 403

    buyer_quote = client.post(f"/api/v1/requests/{request['id']}/quotes", headers=buyer, json={
        "unit_price_minor": 1000, "currency": "IRR", "lead_time_days": 1,
        "valid_until": "2026-12-01", "note": "",
    })
    assert buyer_quote.status_code == 403


def test_money_is_integer_minor_units_and_currency_must_match_request(client):
    buyer = register(client, "buyer", "money-buyer@example.test")
    seller = register(client, "seller", "money-seller@example.test")
    request = create_request(client, buyer)

    fractional = client.post(f"/api/v1/requests/{request['id']}/quotes", headers=seller, json={
        "unit_price_minor": 12.5, "currency": "IRR", "lead_time_days": 1,
        "valid_until": "2026-12-01", "note": "",
    })
    assert fractional.status_code == 422

    wrong_currency = client.post(f"/api/v1/requests/{request['id']}/quotes", headers=seller, json={
        "unit_price_minor": 1200, "currency": "USD", "lead_time_days": 1,
        "valid_until": "2026-12-01", "note": "",
    })
    assert wrong_currency.status_code == 422


def test_seller_can_submit_only_one_quote_per_request(client):
    buyer = register(client, "buyer", "unique-buyer@example.test")
    seller = register(client, "seller", "unique-seller@example.test")
    request = create_request(client, buyer)
    payload = {
        "unit_price_minor": 1200, "currency": "IRR", "lead_time_days": 1,
        "valid_until": "2026-12-01", "note": "",
    }
    assert client.post(f"/api/v1/requests/{request['id']}/quotes", headers=seller, json=payload).status_code == 201
    duplicate = client.post(f"/api/v1/requests/{request['id']}/quotes", headers=seller, json=payload)
    assert duplicate.status_code == 409


def test_protected_market_routes_reject_anonymous_users(client):
    assert client.get("/api/v1/market/requests").status_code == 401
    assert client.post("/api/v1/requests", json={}).status_code == 401
