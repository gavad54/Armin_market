from __future__ import annotations

import base64
import binascii
import hashlib
import hmac
import json
import os
import re
import secrets
import sqlite3
import time
import uuid
from contextlib import contextmanager
from datetime import date, datetime, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Literal

from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel, Field, StrictInt, field_validator

CURRENCY_PATTERN = r"^[A-Z]{3}$"
PASSWORD_ITERATIONS = 260_000
TOKEN_TTL_SECONDS = 12 * 60 * 60


class RegisterInput(BaseModel):
    email: str = Field(min_length=6, max_length=254)
    password: str = Field(min_length=12, max_length=256)
    role: Literal["buyer", "seller"]

    @field_validator("email")
    @classmethod
    def normalize_email(cls, value: str) -> str:
        normalized = value.strip().casefold()
        if not re.fullmatch(r"[^\s@]+@[^\s@]+\.[^\s@]+", normalized):
            raise ValueError("A valid email address is required.")
        return normalized


class LoginInput(BaseModel):
    email: str = Field(min_length=6, max_length=254)
    password: str = Field(min_length=1, max_length=256)

    @field_validator("email")
    @classmethod
    def normalize_email(cls, value: str) -> str:
        return value.strip().casefold()


class RequestInput(BaseModel):
    title: str = Field(min_length=4, max_length=140)
    description: str = Field(min_length=4, max_length=4000)
    category: str = Field(min_length=2, max_length=80)
    quantity: str = Field(min_length=1, max_length=40, pattern=r"^\d+(\.\d{1,3})?$")
    unit: str = Field(min_length=1, max_length=32)
    currency: str = Field(pattern=CURRENCY_PATTERN)
    delivery_city: str = Field(min_length=2, max_length=100)
    needed_by: date | None = None

    @field_validator("quantity")
    @classmethod
    def positive_quantity(cls, value: str) -> str:
        try:
            if Decimal(value) <= 0:
                raise ValueError("Quantity must be greater than zero.")
        except InvalidOperation:
            raise ValueError("Quantity must be a valid decimal string.")
        return value


class QuoteInput(BaseModel):
    unit_price_minor: StrictInt = Field(gt=0, le=9_000_000_000_000_000)
    currency: str = Field(pattern=CURRENCY_PATTERN)
    lead_time_days: StrictInt = Field(ge=0, le=3650)
    valid_until: date
    note: str = Field(default="", max_length=1000)


class SelectQuoteInput(BaseModel):
    quote_id: str = Field(min_length=20, max_length=40)


def _b64encode(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).decode("ascii").rstrip("=")


def _b64decode(value: str) -> bytes:
    return base64.urlsafe_b64decode(value + "=" * (-len(value) % 4))


def _password_hash(password: str, salt: bytes | None = None) -> str:
    salt = salt or secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, PASSWORD_ITERATIONS)
    return "pbkdf2_sha256$" + str(PASSWORD_ITERATIONS) + "$" + _b64encode(salt) + "$" + _b64encode(digest)


def _password_matches(password: str, encoded: str) -> bool:
    try:
        algorithm, rounds, salt_text, digest_text = encoded.split("$", 3)
        if algorithm != "pbkdf2_sha256":
            return False
        digest = hashlib.pbkdf2_hmac(
            "sha256", password.encode("utf-8"), _b64decode(salt_text), int(rounds)
        )
        return hmac.compare_digest(digest, _b64decode(digest_text))
    except (ValueError, TypeError):
        return False


def _utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def _create_token(user: sqlite3.Row, secret: bytes) -> str:
    payload = {"sub": user["id"], "role": user["role"], "exp": int(time.time()) + TOKEN_TTL_SECONDS}
    encoded = _b64encode(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
    signature = _b64encode(hmac.new(secret, encoded.encode("ascii"), hashlib.sha256).digest())
    return encoded + "." + signature


def create_app(database_path: str | Path | None = None, token_secret: str | bytes | None = None) -> FastAPI:
    db_path = Path(database_path or os.getenv("ARMIN_DATABASE_PATH", "backend/data/armin-market.sqlite"))
    db_path.parent.mkdir(parents=True, exist_ok=True)

    is_production = os.getenv("ARMIN_ENV", "development").casefold() == "production"
    secret_value = token_secret or os.getenv("ARMIN_TOKEN_SECRET")
    if secret_value is None:
        if is_production:
            raise RuntimeError("ARMIN_TOKEN_SECRET must be configured in production.")
        secret_value = secrets.token_urlsafe(48)
    secret = secret_value.encode("utf-8") if isinstance(secret_value, str) else secret_value
    if len(secret) < 32:
        raise RuntimeError("The token signing secret must contain at least 32 bytes.")

    @contextmanager
    def connection():
        conn = sqlite3.connect(str(db_path), timeout=15)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA foreign_keys=ON")
        conn.execute("PRAGMA busy_timeout=15000")
        try:
            yield conn
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            conn.close()

    def initialize_database() -> None:
        with connection() as conn:
            conn.executescript("""
                PRAGMA journal_mode=WAL;
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    email TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL CHECK(role IN ('buyer','seller')),
                    created_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS purchase_requests (
                    id TEXT PRIMARY KEY,
                    buyer_id TEXT NOT NULL REFERENCES users(id),
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    category TEXT NOT NULL,
                    quantity TEXT NOT NULL,
                    unit TEXT NOT NULL,
                    currency TEXT NOT NULL,
                    delivery_city TEXT NOT NULL,
                    needed_by TEXT,
                    status TEXT NOT NULL CHECK(status IN ('OPEN','QUOTE_SELECTED_PENDING_PARTIES','CLOSED','CANCELLED')),
                    selected_quote_id TEXT,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                );
                CREATE INDEX IF NOT EXISTS idx_requests_status_created
                    ON purchase_requests(status, created_at DESC);
                CREATE INDEX IF NOT EXISTS idx_requests_buyer
                    ON purchase_requests(buyer_id, created_at DESC);
                CREATE TABLE IF NOT EXISTS supplier_quotes (
                    id TEXT PRIMARY KEY,
                    request_id TEXT NOT NULL REFERENCES purchase_requests(id),
                    seller_id TEXT NOT NULL REFERENCES users(id),
                    unit_price_minor INTEGER NOT NULL CHECK(unit_price_minor > 0),
                    currency TEXT NOT NULL,
                    lead_time_days INTEGER NOT NULL CHECK(lead_time_days >= 0),
                    valid_until TEXT NOT NULL,
                    note TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL CHECK(status IN ('SUBMITTED','SELECTED','NOT_SELECTED','WITHDRAWN')),
                    created_at TEXT NOT NULL,
                    UNIQUE(request_id, seller_id)
                );
                CREATE INDEX IF NOT EXISTS idx_quotes_request
                    ON supplier_quotes(request_id, created_at);
                CREATE TABLE IF NOT EXISTS audit_events (
                    id TEXT PRIMARY KEY,
                    actor_user_id TEXT NOT NULL REFERENCES users(id),
                    action TEXT NOT NULL,
                    entity_type TEXT NOT NULL,
                    entity_id TEXT NOT NULL,
                    details_json TEXT NOT NULL DEFAULT '{}',
                    created_at TEXT NOT NULL
                );
            """)

    initialize_database()
    app = FastAPI(
        title="ARMIN MARKET API",
        version="0.1.0",
        docs_url=None if is_production else "/docs",
        redoc_url=None if is_production else "/redoc",
        openapi_url=None if is_production else "/openapi.json",
    )
    app.state.database_path = str(db_path)

    @app.middleware("http")
    async def security_headers(request, call_next):
        response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["Referrer-Policy"] = "no-referrer"
        response.headers["Cache-Control"] = "no-store"
        return response

    def current_user(authorization: str | None = Header(default=None)):
        if not authorization or not authorization.startswith("Bearer "):
            raise HTTPException(401, "Authentication required.")
        token = authorization[7:].strip()
        try:
            encoded, supplied_signature = token.split(".", 1)
            expected_signature = _b64encode(
                hmac.new(secret, encoded.encode("ascii"), hashlib.sha256).digest()
            )
            if not hmac.compare_digest(supplied_signature, expected_signature):
                raise ValueError("Invalid signature")
            payload = json.loads(_b64decode(encoded))
            if not isinstance(payload.get("exp"), int) or payload["exp"] <= int(time.time()):
                raise ValueError("Expired token")
            user_id = payload["sub"]
        except (ValueError, TypeError, KeyError, AttributeError, binascii.Error, json.JSONDecodeError):
            raise HTTPException(401, "Invalid or expired authentication token.")

        with connection() as conn:
            user = conn.execute(
                "SELECT id,email,role,created_at FROM users WHERE id=?", (user_id,)
            ).fetchone()
        if user is None:
            raise HTTPException(401, "Account is no longer available.")
        return dict(user)

    def require_role(user: dict, role: str) -> None:
        if user["role"] != role:
            raise HTTPException(403, "This operation is not available for this account role.")

    def audit(conn, actor_id: str, action: str, entity_type: str, entity_id: str, details=None):
        conn.execute(
            """INSERT INTO audit_events(id,actor_user_id,action,entity_type,entity_id,details_json,created_at)
               VALUES(?,?,?,?,?,?,?)""",
            (
                str(uuid.uuid4()), actor_id, action, entity_type, entity_id,
                json.dumps(details or {}, ensure_ascii=False, separators=(",", ":")), _utc_now()
            ),
        )

    def request_view(row: sqlite3.Row) -> dict:
        return {
            "id": row["id"], "title": row["title"], "description": row["description"],
            "category": row["category"], "quantity": row["quantity"], "unit": row["unit"],
            "currency": row["currency"], "delivery_city": row["delivery_city"],
            "needed_by": row["needed_by"], "status": row["status"], "created_at": row["created_at"],
        }

    def quote_view(row: sqlite3.Row) -> dict:
        return {
            "id": row["id"], "request_id": row["request_id"], "seller_id": row["seller_id"],
            "unit_price_minor": row["unit_price_minor"], "currency": row["currency"],
            "lead_time_days": row["lead_time_days"], "valid_until": row["valid_until"],
            "note": row["note"], "status": row["status"], "created_at": row["created_at"],
        }

    @app.get("/health")
    def health():
        with connection() as conn:
            conn.execute("SELECT 1").fetchone()
        return {"status": "ok", "service": "armin-market-api"}

    @app.post("/api/v1/auth/register", status_code=201)
    def register(body: RegisterInput):
        user_id = str(uuid.uuid4())
        created_at = _utc_now()
        encoded_password = _password_hash(body.password)
        try:
            with connection() as conn:
                conn.execute(
                    "INSERT INTO users(id,email,password_hash,role,created_at) VALUES(?,?,?,?,?)",
                    (user_id, body.email, encoded_password, body.role, created_at),
                )
                user = conn.execute(
                    "SELECT id,email,role,created_at FROM users WHERE id=?", (user_id,)
                ).fetchone()
        except sqlite3.IntegrityError:
            raise HTTPException(409, "An account with this email already exists.")
        return {
            "access_token": _create_token(user, secret),
            "token_type": "bearer",  # nosec B105 - protocol label, not a credential
            "user": {"id": user["id"], "email": user["email"], "role": user["role"]},
        }

    @app.post("/api/v1/auth/login")
    def login(body: LoginInput):
        with connection() as conn:
            user = conn.execute(
                "SELECT id,email,password_hash,role,created_at FROM users WHERE email=?",
                (body.email,),
            ).fetchone()
        if user is None or not _password_matches(body.password, user["password_hash"]):
            raise HTTPException(401, "Email or password is incorrect.")
        return {
            "access_token": _create_token(user, secret),
            "token_type": "bearer",
            "user": {"id": user["id"], "email": user["email"], "role": user["role"]},
        }

    @app.post("/api/v1/requests", status_code=201)
    def create_request(body: RequestInput, user=Depends(current_user)):
        require_role(user, "buyer")
        if body.needed_by is not None and body.needed_by < date.today():
            raise HTTPException(422, "Requested delivery date must be today or later.")
        request_id = str(uuid.uuid4())
        now = _utc_now()
        with connection() as conn:
            conn.execute(
                """INSERT INTO purchase_requests(
                    id,buyer_id,title,description,category,quantity,unit,currency,
                    delivery_city,needed_by,status,created_at,updated_at
                ) VALUES(?,?,?,?,?,?,?,?,?,?,'OPEN',?,?)""",
                (
                    request_id, user["id"], body.title.strip(), body.description.strip(),
                    body.category.strip(), body.quantity, body.unit.strip(), body.currency,
                    body.delivery_city.strip(), body.needed_by.isoformat() if body.needed_by else None,
                    now, now,
                ),
            )
            audit(conn, user["id"], "buyer_request.created", "purchase_request", request_id)
            row = conn.execute("SELECT * FROM purchase_requests WHERE id=?", (request_id,)).fetchone()
        return request_view(row)

    @app.get("/api/v1/requests")
    def list_my_requests(user=Depends(current_user)):
        require_role(user, "buyer")
        with connection() as conn:
            rows = conn.execute(
                "SELECT * FROM purchase_requests WHERE buyer_id=? ORDER BY created_at DESC",
                (user["id"],),
            ).fetchall()
        return {"items": [request_view(row) for row in rows]}

    @app.get("/api/v1/requests/{request_id}")
    def get_my_request(request_id: str, user=Depends(current_user)):
        require_role(user, "buyer")
        with connection() as conn:
            row = conn.execute(
                "SELECT * FROM purchase_requests WHERE id=? AND buyer_id=?",
                (request_id, user["id"]),
            ).fetchone()
        if row is None:
            raise HTTPException(404, "Request not found.")
        return request_view(row)

    @app.get("/api/v1/market/requests")
    def browse_requests(user=Depends(current_user)):
        require_role(user, "seller")
        with connection() as conn:
            rows = conn.execute(
                "SELECT * FROM purchase_requests WHERE status='OPEN' ORDER BY created_at DESC LIMIT 100"
            ).fetchall()
        return {"items": [request_view(row) for row in rows]}

    @app.post("/api/v1/requests/{request_id}/quotes", status_code=201)
    def submit_quote(request_id: str, body: QuoteInput, user=Depends(current_user)):
        require_role(user, "seller")
        if body.valid_until < date.today():
            raise HTTPException(422, "Quote validity date must be today or later.")
        with connection() as conn:
            request_row = conn.execute(
                "SELECT * FROM purchase_requests WHERE id=?", (request_id,)
            ).fetchone()
            if request_row is None or request_row["status"] != "OPEN":
                raise HTTPException(404, "Open request not found.")
            if request_row["currency"] != body.currency:
                raise HTTPException(422, "Quote currency must match the buyer request currency.")
            quote_id = str(uuid.uuid4())
            try:
                conn.execute(
                    """INSERT INTO supplier_quotes(
                        id,request_id,seller_id,unit_price_minor,currency,lead_time_days,
                        valid_until,note,status,created_at
                    ) VALUES(?,?,?,?,?,?,?,?,?,?)""",
                    (
                        quote_id, request_id, user["id"], body.unit_price_minor,
                        body.currency, body.lead_time_days, body.valid_until.isoformat(),
                        body.note.strip(), "SUBMITTED", _utc_now(),
                    ),
                )
            except sqlite3.IntegrityError:
                raise HTTPException(409, "This seller has already submitted a quote for this request.")
            audit(conn, user["id"], "supplier_quote.submitted", "supplier_quote", quote_id,
                  {"request_id": request_id})
            row = conn.execute("SELECT * FROM supplier_quotes WHERE id=?", (quote_id,)).fetchone()
        return quote_view(row)

    @app.get("/api/v1/requests/{request_id}/quotes")
    def list_quotes(request_id: str, user=Depends(current_user)):
        require_role(user, "buyer")
        with connection() as conn:
            request_row = conn.execute(
                "SELECT id FROM purchase_requests WHERE id=? AND buyer_id=?",
                (request_id, user["id"]),
            ).fetchone()
            if request_row is None:
                raise HTTPException(404, "Request not found.")
            rows = conn.execute(
                """SELECT * FROM supplier_quotes WHERE request_id=?
                   ORDER BY unit_price_minor ASC, lead_time_days ASC, created_at ASC""",
                (request_id,),
            ).fetchall()
        return {"items": [quote_view(row) for row in rows]}

    @app.post("/api/v1/requests/{request_id}/select-quote")
    def select_quote(request_id: str, body: SelectQuoteInput, user=Depends(current_user)):
        require_role(user, "buyer")
        with connection() as conn:
            request_row = conn.execute(
                "SELECT * FROM purchase_requests WHERE id=? AND buyer_id=?",
                (request_id, user["id"]),
            ).fetchone()
            if request_row is None:
                raise HTTPException(404, "Request not found.")
            if request_row["status"] != "OPEN":
                raise HTTPException(409, "This request is no longer open for quote selection.")
            quote_row = conn.execute(
                "SELECT * FROM supplier_quotes WHERE id=? AND request_id=?",
                (body.quote_id, request_id),
            ).fetchone()
            if quote_row is None or quote_row["status"] != "SUBMITTED":
                raise HTTPException(404, "Submitted quote not found.")
            if quote_row["valid_until"] < date.today().isoformat():
                raise HTTPException(409, "This quote has expired.")
            updated = conn.execute(
                """UPDATE purchase_requests
                   SET status='QUOTE_SELECTED_PENDING_PARTIES',selected_quote_id=?,updated_at=?
                   WHERE id=? AND buyer_id=? AND status='OPEN'""",
                (body.quote_id, _utc_now(), request_id, user["id"]),
            )
            if updated.rowcount != 1:
                raise HTTPException(409, "The request changed while selecting a quote; reload and try again.")
            conn.execute(
                "UPDATE supplier_quotes SET status=CASE WHEN id=? THEN 'SELECTED' ELSE 'NOT_SELECTED' END WHERE request_id=?",
                (body.quote_id, request_id),
            )
            audit(conn, user["id"], "buyer_quote.selected", "purchase_request", request_id,
                  {"quote_id": body.quote_id})
        return {
            "request_id": request_id,
            "quote_id": body.quote_id,
            "status": "QUOTE_SELECTED_PENDING_PARTIES",
            "payment_status": "NOT_APPLICABLE",
            "message": "Quote selected for follow-up. No payment or binding transfer was initiated.",
        }

    return app


app = create_app()
