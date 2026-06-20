# BharatPOS Backend

A multi-tenant retail management backend built for small and mid-sized retail businesses. Handles billing, tax compliance, inventory, customer relationships, suppliers, and payments in one system.

Built with Spring Boot 3.3.5 and Java 17.

---

## What this is

Most retail software is either too simple to run a real business on, or too complex for a shop owner to actually use. BharatPOS is the backend for something in between, a system that handles real business logic (tax calculation, inventory audit trails, customer segmentation) while staying simple enough to integrate with a clean frontend.

This repository is the API layer. The frontend lives in a separate repository: [bharatpos-frontend](https://github.com/vkp000/bharatpos-frontend).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 |
| Database | PostgreSQL |
| Auth | JWT (access + refresh tokens) |
| Payments | Razorpay (orders, signature verification, webhooks) |
| Messaging | WhatsApp Business Cloud API (Meta) |
| Docs | springdoc-openapi (Swagger UI) |
| Build | Maven |
| Container | Docker (multi-stage build) |

---

## Core Modules

### Billing & Tax
- Sale processing with automatic tax calculation on inclusive pricing
- CGST/SGST split, configurable per product
- Sequential, collision-safe invoice numbering
- GST summary reports with automatic B2B / B2C invoice classification

### Inventory
- Real-time stock tracking, multi-store aware
- Every stock change is logged with quantity before/after and a reason (sale, purchase, adjustment, damage, expiry, return)
- Low-stock detection based on per-product reorder levels

### CRM & Loyalty
- Customer profiles keyed by phone number
- Automatic segmentation (new / loyal / champion) recalculated on every sale, based on visit count and spend
- Loyalty points engine, earned automatically per purchase
- WhatsApp-based lifecycle messaging: invoices, payment reminders, birthday offers, loyalty updates, broadcast campaigns — all async, none of it blocks the request thread

### Purchasing
- Purchase orders linked to suppliers
- Status lifecycle: `DRAFT → SENT → ACKNOWLEDGED → PARTIALLY_RECEIVED → RECEIVED`

### Payments
- Razorpay order creation and UPI QR generation
- HMAC-SHA256 signature verification on both payments and webhooks
- Graceful simulated mode when API keys aren't configured (useful for local dev/demo)

### Platform
- Multi-tenant from the ground up: `Tenant → Store → User`
- Registration auto-provisions a tenant, a default store, and an owner account in a single transaction
- Subscription plans with invoice/store/user limits
- JWT auth with short-lived access tokens and refresh token rotation
- Role-based access: `OWNER, MANAGER, CASHIER, STOCK_MANAGER, ACCOUNTANT`
- Tuned connection pool and a dedicated async thread pool, so WhatsApp/notification calls never block billing

---

## Project Structure

```
src/main/java/com/bharatpos/
├── controller/      # REST endpoints
├── service/         # Business logic
├── entity/          # JPA entities
├── repository/      # Spring Data repositories
├── dto/
│   ├── request/
│   └── response/
├── security/         # JWT filter, auth config
├── config/           # Async, security, OpenAPI config
├── enums/            # Role, SaleStatus, PaymentMode, POStatus
└── exception/        # Global exception handling
```

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- PostgreSQL running locally or accessible via connection string

### Run locally

```bash
./mvnw clean install
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080/api/v1`.

Swagger UI: `http://localhost:8080/swagger-ui.html`

### Run with Docker

```bash
docker build -t bharatpos-backend .
docker run -p 8080:8080 bharatpos-backend
```

### Environment Variables

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `JWT_SECRET` | Signing key for access/refresh tokens |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET` | Payment gateway (falls back to simulated mode if unset) |
| `WHATSAPP_TOKEN`, `WHATSAPP_PHONE_ID` | Meta WhatsApp Business API |

---

## API Overview

All endpoints are prefixed with `/api/v1`.

| Resource | Endpoints |
|---|---|
| Auth | `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/me` |
| Dashboard | `/dashboard` |
| Products | `/products`, `/products/search`, `/products/barcode/{code}` |
| Sales | `/sales`, `/sales/recent`, `/sales/summary`, `/sales/{id}` |
| Customers | `/customers`, `/customers/{id}` |
| Inventory | `/inventory`, `/inventory/low-stock`, `/inventory/adjust`, `/inventory/audit-log` |
| Suppliers | `/suppliers` |
| Stores | `/stores` |
| GST | `/gst/summary` |
| Payments | `/payments/create-order`, `/payments/verify`, `/payments/status/{id}` |
| WhatsApp | `/whatsapp/send-invoice`, `/whatsapp/send-reminder`, `/whatsapp/broadcast` |
| Subscription | `/subscription`, `/subscription/check-limit`, `/subscription/upgrade` |
| Admin | `/admin/stats`, `/admin/tenants` |

Full request/response schemas are available via Swagger UI once the app is running.

---

## Status

Actively developed. Core billing, inventory, CRM, GST compliance, and payments are functional. Next direction: integrating Spring AI to expose natural-language access to this data (e.g. "which products are about to run out") on top of the existing backend, rather than replacing it.

---

## Author

Vivek Prajapat — [@vkp_00](https://x.com/vkp_00)
