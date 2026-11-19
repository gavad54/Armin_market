# ARMIN MARKET API — MVP

This folder contains the first independent API service for ARMIN MARKET.

## Implemented in this branch

- Buyer and seller account registration/login.
- Password hashing with PBKDF2-HMAC-SHA256 and per-account random salts.
- Signed, expiring bearer tokens.
- Buyer purchase-request creation and buyer-only request history.
- Seller marketplace view of open requests without buyer IDs or email addresses.
- Supplier quote submission with integer minor-unit prices and request-currency matching.
- Buyer-only quote comparison and quote selection.
- Role checks, tenant/owner checks on private request resources, audit events and SQLite foreign keys.
- No payment, escrow, inventory custody, binding contract, or transfer of funds.

## Run locally

From the repository root:

```bash
python -m venv .venv
. .venv/bin/activate
pip install -r backend/requirements.txt
export ARMIN_ENV=development
export ARMIN_TOKEN_SECRET="$(python -c 'import secrets; print(secrets.token_urlsafe(48))')"
export ARMIN_DATABASE_PATH=./backend/data/armin-market.sqlite
uvicorn backend.app.main:app --reload
```

OpenAPI documentation is served at `/docs` while running.

## Container

Set a private `ARMIN_TOKEN_SECRET` of at least 32 bytes, then from the repository root:

```bash
docker compose -f backend/docker-compose.yml up --build
```

The service runs as a non-root user and stores its SQLite file in a named volume. Back up the volume before upgrades. This compose file is a local/self-hosted starting point, not a production deployment approval.

## API flow

1. Register as `buyer` or `seller`.
2. Buyer creates a purchase request.
3. Seller browses open requests and submits one quote per request.
4. Buyer views quotes for their own request and selects one for follow-up.
5. Selection status is `QUOTE_SELECTED_PENDING_PARTIES`; it does not create a payment or binding settlement.

## Release blockers

This MVP is not yet a public commercial release. It still needs email verification, password reset, MFA, rate limiting/abuse controls, refresh/revocation, privacy/retention controls, PostgreSQL deployment, encrypted off-host backup and restore proof, monitoring, Android API integration, accessibility/UAT, and legal/provider review. The default SQLite store is suitable for isolated MVP testing, not a high-scale multi-instance deployment.
