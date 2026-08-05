# data-model.md — Canonical Data Model (SQL Server 2022)

> **Product:** parking — ALEATICA parking spaces & office desks.
> **Engine:** Microsoft SQL Server 2022 · **Persistence:** Spring Data JPA + Hibernate 6.5 · **Migrations:** Flyway 10.
> **Consumers:** `database-optimizer`, `backend-architect`.
> **Authority of names:** the README section *"Nomenclatura del código (ES → EN)"*. All tables, columns, enums and JPA classes use **English** identifiers. Business prose stays Spanish in the README; SQL is English/snake_case.
>
> **Scope note:** this model is re-synchronized with the **post-V12/V13/V15 schema**. It covers the parking core (Phase 1: the 9 live entities plus Spring Session) **and** the later increments that generalize the reservable reference: `generic-resource-refactor` (**V12**) turns `parking_space_id` into a polymorphic `resource_id BIGINT` + `resource_type VARCHAR(10)` on `fixed_assignments`, `requests` and `releases`; `desks` (**V13**) adds the second resource table and drops the single-table FKs; `floor-plan` (**V15**) adds the desk-pending filtered index. Office desks (`desks`) and the floor plan are therefore **in scope** here. `visitor_reservations` is the one reservable table **not** generalized — it still points to `parking_spaces` directly.

---

## 1. Conventions

| Aspect | Rule |
|--------|------|
| Schema | Single schema `dbo` (default; not re-created). |
| Table names | Plural, snake_case: `employees`, `parking_spaces`. |
| Column names | snake_case. |
| Primary keys | `id BIGINT IDENTITY(1,1)`, constraint `PK_<table>`. |
| Foreign keys | `<singular_table>_id`, explicit `FOREIGN KEY`, named `FK_<table>_<role-or-target>`. When a table has several FKs to the same target, the name carries the role (e.g. `FK_fixed_assignments_created_by`). |
| Booleans | `BIT NOT NULL DEFAULT 0` (named default constraint). |
| Timestamps | `DATETIME2(3)` with `DEFAULT SYSUTCDATETIME()` (UTC). |
| Dates (no time) | `DATE`. Date columns are named explicitly (`requested_date`, `release_date`, `reservation_date`) to avoid the reserved word `date`. |
| Enums | `VARCHAR(N)` + named `CHECK (col IN (...))`. Never free `VARCHAR`. |
| Short unicode text | `NVARCHAR(N)`. |
| ASCII-only values | `VARCHAR(N)` (enums, BCrypt hash). |
| Constraints / indexes | **Always explicitly named**, never anonymous. |

> **Naming deviation (documented):** the prompt suggested index names `IX_audit_log_timestamp` / `IX_login_log_timestamp`. In SQL Server `timestamp` is a deprecated synonym for `rowversion`, so the timestamp column is named `occurred_at` and the indexes are `IX_audit_log_occurred_at` / `IX_login_log_occurred_at`.

---

## 2. Entity-Relationship Diagram

> The current README ships no data-model ASCII diagram (it was removed when the README was reduced to a functional spec). The diagram below is authored from the entities and adds explicit cardinalities.

```
                         ┌────────────────────┐
                         │     employees      │
                         │  (Employee, live)  │
                         └─────────┬──────────┘
        actor / titular / creator  │  1
        ┌───────────────┬──────────┼───────────────┬──────────────┐
        │ N             │ N        │ N             │ N            │ N
        ▼               ▼          ▼               ▼              ▼
┌───────────────┐ ┌───────────┐ ┌──────────┐ ┌───────────┐ ┌───────────┐
│fixed_assign.. │ │ requests  │ │ releases │ │ visitors  │ │ login_log │
│(FixedAssign.) │ │ (Request) │ │ (Release)│ │ (Visitor) │ │ (LoginLog)│
└──────┬────────┘ └─────┬─────┘ └────┬─────┘ └─────┬─────┘ └───────────┘
   resource_id +   resource_id +  resource_id +    │ 1
   resource_type   resource_type  resource_type    ▼ N
   (polymorphic, no FK — ResourceResolverPort)  ┌──────────────────────┐
        │               │            │          │ visitor_reservations │
        └───────┬───────┴─────┬──────┘          │ (VisitorReservation) │
                ▼             ▼                  └──────────┬───────────┘
   ┌────────────────────┐ ┌────────────────────┐           │ N
   │   parking_spaces   │ │       desks        │           ▼
   │ (ParkingSpace)     │ │      (Desk)        │  parking_space_id (real FK)
   │ resource_type=     │ │ resource_type=     │──────────►│
   │   PARKING          │ │   DESK             │      parking_spaces
   └────────────────────┘ └────────────────────┘
      both implement BookableResource (domain interface)

  ┌───────────┐   ┌───────────────┐   ┌──────────────────────────────┐
  │ audit_log │   │ email_outbox  │   │ SPRING_SESSION /             │
  │(AuditLog) │   │ (EmailOutbox) │   │ SPRING_SESSION_ATTRIBUTES    │
  └───────────┘   │ (no FK)       │   │ (framework-managed)          │
                  └───────────────┘   └──────────────────────────────┘
```

**Cardinalities**
- `employees 1 — N fixed_assignments` (titular), and `1 — N` again as `created_by` / `revoked_by`.
- `fixed_assignments`, `requests` and `releases` reference a **reservable resource** via `resource_id` + `resource_type` (polymorphic, no FK): the resource is a `parking_spaces` row (`PARKING`) or a `desks` row (`DESK`). Both `ParkingSpace` and `Desk` implement the `BookableResource` domain interface.
- `employees 1 — N requests`; `resource 0..1 — N requests` (`resource_id` is NULL while a generic `PENDING` request has no assigned resource; a desk requested from the floor plan sets it at creation).
- `employees 1 — N releases`; `resource 1 — N releases`.
- `employees 1 — N visitors` (creator); `visitors 1 — N visitor_reservations`; `parking_spaces 1 — N visitor_reservations` (**real FK** — this table was not generalized).
- `employees 0..1 — N login_log` / `audit_log` (actor may be unknown/anonymous).
- `email_outbox` is standalone (no FK; `recipient` is a literal email address).

---

## 3. Tables

> The schema is **not** a single migration: each table ships in its own migration (see section 7). Spring Session is `V1`, the audit tables `V2`, `employees` `V4`, `parking_spaces` `V6`, `fixed_assignments` `V7`, `requests` `V8`, `releases` `V9`, `visitors`/`visitor_reservations` `V10`, `email_outbox` `V11`. The generic-resource refactor (`V12`), `desks` (`V13`) and the floor-plan desk index (`V15`) then evolve the reservable columns. **The DDL below is shown in its post-V12/V13/V15 form** (i.e. `resource_id` + `resource_type` on the three generalized tables); each table's supporting/performance indexes live inside its own migration, not in a separate performance-index migration.

### 3.1 `employees`
**Purpose:** corporate people with access to parking. Holds local credentials (Phase 1 / fallback) and role.

```sql
CREATE TABLE dbo.employees (
    id                       BIGINT IDENTITY(1,1) NOT NULL,
    first_name               NVARCHAR(100) NOT NULL,
    last_name                NVARCHAR(150) NOT NULL,
    login                    NVARCHAR(100) NOT NULL,
    email                    NVARCHAR(255) NOT NULL,
    password_hash            VARCHAR(72)   NULL,
    password_must_change     BIT NOT NULL CONSTRAINT DF_employees_password_must_change DEFAULT 0,
    department               NVARCHAR(100) NULL,
    mobile_phone             NVARCHAR(30)  NULL,
    license_plate            NVARCHAR(15)  NULL,
    is_corporate             BIT NOT NULL CONSTRAINT DF_employees_is_corporate DEFAULT 0,
    auth_origin              VARCHAR(10) NOT NULL CONSTRAINT DF_employees_auth_origin DEFAULT 'LOCAL',
    role                     VARCHAR(10) NOT NULL,
    enabled                  BIT NOT NULL CONSTRAINT DF_employees_enabled DEFAULT 1,
    active                   BIT NOT NULL CONSTRAINT DF_employees_active DEFAULT 1,
    email_notifications_enabled BIT NOT NULL CONSTRAINT DF_employees_email_notif DEFAULT 1,  -- V33: preferencia por empleado (push-notifications)
    push_notifications_enabled  BIT NOT NULL CONSTRAINT DF_employees_push_notif DEFAULT 1,   -- V33
    failed_login_attempts    INT NOT NULL CONSTRAINT DF_employees_failed_attempts DEFAULT 0,
    locked_until             DATETIME2(3) NULL,
    last_password_change_at  DATETIME2(3) NULL,
    created_at               DATETIME2(3) NOT NULL CONSTRAINT DF_employees_created_at DEFAULT SYSUTCDATETIME(),
    updated_at               DATETIME2(3) NULL,
    CONSTRAINT PK_employees PRIMARY KEY (id),
    CONSTRAINT CK_employees_role        CHECK (role IN ('ADMIN','EMPLOYEE')),
    CONSTRAINT CK_employees_auth_origin CHECK (auth_origin IN ('LOCAL','ENTRA_ID'))
);
GO
CREATE UNIQUE INDEX UX_employees_login ON dbo.employees(login);
GO
CREATE UNIQUE INDEX UX_employees_email ON dbo.employees(email);
GO
```

**Notes**
- `password_hash` is **nullable**: Phase-2 employees are created without local login (`NULL`); Phase-1 employees and fallback accounts carry a BCrypt hash (60 chars, ASCII → `VARCHAR(72)`).
- `is_corporate` = business attribute (has EntraID or not). `auth_origin` = how the account authenticates (`LOCAL` local password / `ENTRA_ID` via landing). They are independent: a corporate employee may still hold a `LOCAL` fallback password.
- `enabled` = account allowed to log in; `active` = not logically deleted. Logical delete only — rows are never physically removed.
- `failed_login_attempts` + `locked_until` implement the 5-attempt / 15-minute lockout (Phase 1).
- `last_password_change_at` drives the 90-day rotation (Phase 2 / fallback).

### 3.2 `parking_spaces`
**Purpose:** physical parking spaces, identified by an integer `number` (≥ 1000) from which the **floor** is derived (`floor = number / 1000`), active/inactive.

```sql
CREATE TABLE dbo.parking_spaces (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    number      INT NOT NULL,                                                      -- V19 (>= 1000)
    label       NVARCHAR(20) NOT NULL,
    active      BIT NOT NULL CONSTRAINT DF_parking_spaces_active DEFAULT 1,
    created_at  DATETIME2(3) NOT NULL CONSTRAINT DF_parking_spaces_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_parking_spaces PRIMARY KEY (id),
    CONSTRAINT CK_parking_spaces_number CHECK (number >= 1000)                     -- V19
);
GO
CREATE UNIQUE INDEX UX_parking_spaces_label ON dbo.parking_spaces(label);
GO
CREATE UNIQUE INDEX UX_parking_spaces_number ON dbo.parking_spaces(number);        -- V19
GO
```

**Notes**
- `number` (added in **V19**) is unique and human-facing; both `number` and the derived `label` (`CONVERT(NVARCHAR, number)`) are unique. Inactive spaces are never available for any date.
- **Floor is DERIVED, not persisted** (`floor = number / 1000`, integer division): `1001..1999 → floor 1`, `2001..2999 → floor 2`, etc. It is computed in the domain/DTO and exposed read-only; listing filters by floor via the number range `[floor*1000, floor*1000+999]` (change `parking-space-floors`, design §Decision 1).
- **V19 renumbering** rewrites the existing rows by `id` order (spacesPerFloor = 5) without touching `id`, so all polymorphic references (`fixed_assignments`/`requests`/`releases`) and the real FK from `visitor_reservations` keep resolving by `id`. The 25 seeded spaces become `1001-1005, 2001-2005, 3001-3005, 4001-4005, 5001-5005`.
- `desks` (§3.2b) is the sibling reservable table; since `generic-resource-refactor` (V12) both are addressed polymorphically through `resource_id` + `resource_type`. `ParkingSpace` implements the `BookableResource` domain interface with `resource_type = PARKING`.

### 3.2b `desks` (capability `init-desks`)
**Purpose:** office desks as the second reservable resource type (`ResourceType.DESK`). Sibling table of `parking_spaces` (not single-table inheritance): the `generic-resource-refactor` already supplies the `resource_type` discriminator, so a separate table keeps the desk-only columns (`number`, `category`, `coord_x`, `coord_y`) without nullable columns on `parking_spaces`. Created in migration **`V13__desks.sql`** (which also drops the three polymorphic FKs to `parking_spaces`); the 65 dev desks are seeded only under the `des` profile via `db/seed/dev/V14__seed_dev_desks.sql` (neutral coords), then positioned by `V16` (grid) and `V18` (real mockup coordinates).

```sql
CREATE TABLE dbo.desks (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    number      INT NOT NULL,
    category    VARCHAR(15) NOT NULL CONSTRAINT DF_desks_category DEFAULT 'STANDARD',
    coord_x     DECIMAL(5,2) NOT NULL CONSTRAINT DF_desks_coord_x DEFAULT 50,   -- percent of plan width
    coord_y     DECIMAL(5,2) NOT NULL CONSTRAINT DF_desks_coord_y DEFAULT 50,   -- percent of plan height
    active      BIT NOT NULL CONSTRAINT DF_desks_active DEFAULT 1,
    created_at  DATETIME2(3) NOT NULL CONSTRAINT DF_desks_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_desks PRIMARY KEY (id),
    CONSTRAINT CK_desks_number   CHECK (number BETWEEN 1 AND 65),
    CONSTRAINT CK_desks_category CHECK (category IN ('STANDARD','EXECUTIVE')),
    CONSTRAINT CK_desks_coord_x  CHECK (coord_x BETWEEN 0 AND 100),
    CONSTRAINT CK_desks_coord_y  CHECK (coord_y BETWEEN 0 AND 100)
);
GO
CREATE UNIQUE INDEX UX_desks_number ON dbo.desks(number);
GO
```

**Notes**
- `number` is unique and range-bounded 1-65: the reservable set is fixed and numbered.
- `category` `EXECUTIVE` is a visual distinction only — it does not change any reservation rule (released/requested exactly like `STANDARD`).
- `coord_x`/`coord_y` are percentages (0-100) of the plan image, independent of its resolution; fine positioning belongs to `floor-plan`.
- Desks reuse `fixed_assignments`, `requests`, `releases` and availability through `resource_id` + `resource_type = 'DESK'`. **V13 drops the single-table FKs to `parking_spaces`** on those three tables (a polymorphic `resource_id` cannot FK a single table); referential integrity of the resource moves to the application layer (`ResourceResolverPort#exists` per type). The filtered unique indexes (keyed on `resource_type`) are unchanged.
- **No visitor reservations for desks:** the availability calculation for `DESK` omits the `visitor_reservations` term.
- An employee may hold a `PARKING` and a `DESK` fixed assignment on the same weekday (the `UX_fixed_assignments_employee_day_active` index keys on `resource_type`).

### 3.3 `fixed_assignments`
**Purpose:** indefinite link between an employee and a parking space for a given weekday. Logically revoked, never deleted.

Created as `parking_space_id` in **`V7`**; generalized to `resource_id` + `resource_type` in **`V12`** (FK dropped for good in **`V13`**). Post-refactor form:

```sql
CREATE TABLE dbo.fixed_assignments (
    id                BIGINT IDENTITY(1,1) NOT NULL,
    resource_id       BIGINT NOT NULL,                 -- V12: was parking_space_id
    resource_type     VARCHAR(10) NOT NULL CONSTRAINT DF_fixed_assignments_resource_type DEFAULT 'PARKING',
    employee_id       BIGINT NOT NULL,
    day_of_week       TINYINT NOT NULL,
    active            BIT NOT NULL CONSTRAINT DF_fixed_assignments_active DEFAULT 1,
    created_by_id     BIGINT NOT NULL,
    created_at        DATETIME2(3) NOT NULL CONSTRAINT DF_fixed_assignments_created_at DEFAULT SYSUTCDATETIME(),
    revoked_by_id     BIGINT NULL,
    revoked_at        DATETIME2(3) NULL,
    CONSTRAINT PK_fixed_assignments PRIMARY KEY (id),
    CONSTRAINT CK_fixed_assignments_day_of_week   CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT CK_fixed_assignments_resource_type CHECK (resource_type IN ('PARKING','DESK')),
    -- No FK on resource_id: it is a polymorphic reference (parking_spaces or desks).
    -- V12 re-added FK_fixed_assignments_parking_spaces; V13 dropped it permanently.
    CONSTRAINT FK_fixed_assignments_employee   FOREIGN KEY (employee_id)   REFERENCES dbo.employees(id),
    CONSTRAINT FK_fixed_assignments_created_by FOREIGN KEY (created_by_id) REFERENCES dbo.employees(id),
    CONSTRAINT FK_fixed_assignments_revoked_by FOREIGN KEY (revoked_by_id) REFERENCES dbo.employees(id)
);
GO
CREATE UNIQUE INDEX UX_fixed_assignments_space_day_active
    ON dbo.fixed_assignments(resource_id, resource_type, day_of_week) WHERE active = 1;
GO
CREATE UNIQUE INDEX UX_fixed_assignments_employee_day_active
    ON dbo.fixed_assignments(employee_id, resource_type, day_of_week) WHERE active = 1;
GO
CREATE INDEX IX_fixed_assignments_employee_id_active
    ON dbo.fixed_assignments(employee_id, active);
GO
```

**Notes**
- `day_of_week` is **1-7** (1=Monday … 7=Sunday), enforced by CHECK.
- **Polymorphic reference (V12/V13):** `resource_id` + `resource_type` replaced `parking_space_id`. There is **no** foreign key on `resource_id`: it can point to `parking_spaces` (`PARKING`) or `desks` (`DESK`), and a single-table FK cannot express that. Referential integrity of the resource is enforced in the application layer via `ResourceResolverPort#exists(type, id)`. The named filtered indexes keep their original names (so the DataIntegrityViolation → 409 translation in `GlobalExceptionHandler` is unaffected) and now key on `resource_type` too — for the `PARKING`-only core the behaviour is identical.
- Revocation = `active=0` + `revoked_at` + `revoked_by_id`. The two filtered unique indexes guarantee that, **among active rows only**, a resource and an employee are each used at most once per weekday **per resource type** — past/revoked history is unaffected. An employee may hold a `PARKING` and a `DESK` fixed assignment on the same weekday (the employee index keys on `resource_type`).

### 3.4 `releases`
**Purpose:** marks a fixed-assigned space as available for one concrete date. Voluntary (by owner) or administrative (by admin).

Created as `parking_space_id` in **`V9`**; generalized to `resource_id` + `resource_type` in **`V12`** (FK dropped for good in **`V13`**). Post-refactor form:

```sql
CREATE TABLE dbo.releases (
    id                BIGINT IDENTITY(1,1) NOT NULL,
    resource_id       BIGINT NOT NULL,                 -- V12: was parking_space_id
    resource_type     VARCHAR(10) NOT NULL CONSTRAINT DF_releases_resource_type DEFAULT 'PARKING',
    employee_id       BIGINT NOT NULL,
    release_date      DATE NOT NULL,
    type              VARCHAR(15) NOT NULL,
    reason            NVARCHAR(500) NULL,
    released_by_id    BIGINT NOT NULL,
    created_at        DATETIME2(3) NOT NULL CONSTRAINT DF_releases_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_releases PRIMARY KEY (id),
    CONSTRAINT CK_releases_type          CHECK (type IN ('VOLUNTARY','ADMINISTRATIVE')),
    CONSTRAINT CK_releases_resource_type CHECK (resource_type IN ('PARKING','DESK')),
    -- No FK on resource_id: polymorphic reference; V12 re-added FK_releases_parking_spaces, V13 dropped it.
    CONSTRAINT FK_releases_employee    FOREIGN KEY (employee_id)    REFERENCES dbo.employees(id),
    CONSTRAINT FK_releases_released_by FOREIGN KEY (released_by_id) REFERENCES dbo.employees(id)
);
GO
-- Simple (non-filtered) unique index: cancellation is a physical delete, so a cancelled
-- row never blocks a new release of the same resource/date. Also serves the availability lookup.
CREATE UNIQUE INDEX UX_releases_space_date
    ON dbo.releases(resource_id, resource_type, release_date);
GO
CREATE INDEX IX_releases_employee_id ON dbo.releases(employee_id);
GO
```

**Notes**
- `employee_id` = the fixed-assignment owner whose resource is freed; `released_by_id` = who performed the release (the owner for `VOLUNTARY`, an admin for `ADMINISTRATIVE`).
- `reason` is required by business rule for `ADMINISTRATIVE` (enforced in the service layer, not by the schema, since `VOLUNTARY` allows NULL).
- **Polymorphic reference (V12/V13):** same as `fixed_assignments` — no FK on `resource_id`; integrity via `ResourceResolverPort`. `UX_releases_space_date` is a plain `UNIQUE` index (not filtered) keyed on `(resource_id, resource_type, release_date)`; a concurrent second release on the same resource/date violates it → `DataIntegrityViolation` → 409.
- A release row makes the resource available for `release_date` in the availability calculation.

### 3.5 `requests`
**Purpose:** an employee's point-in-time request for a parking space on a concrete date, with approve/reject lifecycle.

Created with `parking_space_id` in **`V8`**; generalized to `resource_id` + `resource_type` in **`V12`** (FK dropped for good in **`V13`**); the desk-pending filtered index added in **`V15`**. Post-refactor form:

```sql
CREATE TABLE dbo.requests (
    id                    BIGINT IDENTITY(1,1) NOT NULL,
    employee_id           BIGINT NOT NULL,
    requested_date        DATE NOT NULL,
    status                VARCHAR(10) NOT NULL CONSTRAINT DF_requests_status DEFAULT 'PENDING',
    resource_id           BIGINT NULL,                 -- V12: was parking_space_id (NULL while PENDING)
    resource_type         VARCHAR(10) NOT NULL CONSTRAINT DF_requests_resource_type DEFAULT 'PARKING',
    approval_note         NVARCHAR(500) NULL,
    rejection_reason_code VARCHAR(40) NULL,
    rejection_reason      NVARCHAR(500) NULL,
    resolved_by_id        BIGINT NULL,
    resolved_at           DATETIME2(3) NULL,
    created_at            DATETIME2(3) NOT NULL CONSTRAINT DF_requests_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_requests PRIMARY KEY (id),
    CONSTRAINT CK_requests_status        CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    CONSTRAINT CK_requests_resource_type CHECK (resource_type IN ('PARKING','DESK')),
    CONSTRAINT CK_requests_rejection_reason_code
        CHECK (rejection_reason_code IS NULL OR rejection_reason_code IN ('NO_AVAILABILITY','OUTSIDE_POLICY','OTHER')),
    -- No FK on resource_id: polymorphic reference; V12 re-added FK_requests_parking_spaces, V13 dropped it.
    CONSTRAINT FK_requests_employee    FOREIGN KEY (employee_id)    REFERENCES dbo.employees(id),
    CONSTRAINT FK_requests_resolved_by FOREIGN KEY (resolved_by_id) REFERENCES dbo.employees(id)
);
GO
-- One PENDING request per employee, resource type and date.
CREATE UNIQUE INDEX UX_requests_employee_date_pending
    ON dbo.requests(employee_id, resource_type, requested_date) WHERE status = 'PENDING';
GO
-- A resource can be APPROVED at most once per date (concurrency net between admins).
CREATE UNIQUE INDEX UX_requests_space_date_approved
    ON dbo.requests(resource_id, resource_type, requested_date) WHERE status = 'APPROVED';
GO
-- V15 (floor-plan): a desk requested from the plan fixes the desk at creation
-- (resource_id set, PENDING) so a second employee clicking the same desk/date gets a 409.
CREATE UNIQUE INDEX UX_requests_desk_date_pending
    ON dbo.requests(resource_id, requested_date)
    WHERE status = 'PENDING' AND resource_type = 'DESK' AND resource_id IS NOT NULL;
GO
-- Support indexes (created in V8).
CREATE INDEX IX_requests_status_created_at        ON dbo.requests(status, created_at);
CREATE INDEX IX_requests_employee_id_requested_date ON dbo.requests(employee_id, requested_date);
GO
```

**Notes**
- State machine: `PENDING` (`resource_id = NULL`, `resource_type = 'PARKING'`) → `APPROVED` (resource + `resolved_by_id` + `resolved_at` [+ optional `approval_note`]) | `REJECTED` (`rejection_reason_code` + resolver, optional free `rejection_reason`) | `CANCELLED` (by the employee while PENDING). Exception: a desk requested from the interactive floor plan is born with `resource_id` already set (`resource_type = 'DESK'`), so the clicked desk is held immediately — see `UX_requests_desk_date_pending` (V15).
- `approval_note` (added per UI): free note from the admin that travels in the approval email (mockup `05-modal-aprobar-solicitud`).
- **Rejection reason catalog** (added per UI): `rejection_reason_code` is a fixed catalog (`NO_AVAILABILITY`, `OUTSIDE_POLICY`, `OTHER`) shown translated in the UI; `rejection_reason` carries the free-text comment, **required when the code is `OTHER`** (≥ 5 chars) and optional otherwise. A rejection must always carry a code. ⚠️ The exact catalog values are a starting set, pendiente de confirmar con negocio.
- **Polymorphic reference (V12/V13):** no FK on `resource_id`; integrity via `ResourceResolverPort`. `UX_requests_employee_date_pending` now enforces **one `PENDING` request per employee, resource type and date**; `UX_requests_space_date_approved` enforces **one `APPROVED` request per resource and date**. Rejected/cancelled rows do not block new requests.

### 3.6 `visitors`
**Purpose:** reusable card for an external (non-employee) person. Has no app access.

```sql
CREATE TABLE dbo.visitors (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    first_name     NVARCHAR(100) NOT NULL,
    last_name      NVARCHAR(150) NOT NULL,
    national_id    NVARCHAR(20) NOT NULL,
    license_plate  NVARCHAR(15) NULL,
    company        NVARCHAR(150) NULL,
    usual_reason   NVARCHAR(255) NULL,
    created_by_id  BIGINT NOT NULL,
    created_at     DATETIME2(3) NOT NULL CONSTRAINT DF_visitors_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_visitors PRIMARY KEY (id),
    CONSTRAINT FK_visitors_created_by FOREIGN KEY (created_by_id) REFERENCES dbo.employees(id)
);
GO
CREATE UNIQUE INDEX UX_visitors_national_id ON dbo.visitors(national_id);
GO
```

**Notes**
- `national_id` (DNI/passport) is the natural unique key for reuse across visits; `created_by_id` is the admin who created the card.

### 3.7 `visitor_reservations`
**Purpose:** a concrete parking reservation for a visitor on a date. No approval flow — created directly by the admin.

```sql
CREATE TABLE dbo.visitor_reservations (
    id                BIGINT IDENTITY(1,1) NOT NULL,
    visitor_id        BIGINT NOT NULL,
    parking_space_id  BIGINT NOT NULL,
    reservation_date  DATE NOT NULL,
    notes             NVARCHAR(500) NULL,
    created_by_id     BIGINT NOT NULL,
    created_at        DATETIME2(3) NOT NULL CONSTRAINT DF_visitor_reservations_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_visitor_reservations PRIMARY KEY (id),
    CONSTRAINT FK_visitor_reservations_visitors       FOREIGN KEY (visitor_id)       REFERENCES dbo.visitors(id),
    CONSTRAINT FK_visitor_reservations_parking_spaces FOREIGN KEY (parking_space_id) REFERENCES dbo.parking_spaces(id),
    CONSTRAINT FK_visitor_reservations_created_by     FOREIGN KEY (created_by_id)    REFERENCES dbo.employees(id)
);
GO
-- Plain UNIQUE index (V10): one visitor reservation per space and date; also serves the
-- availability lookup (parking_space_id, reservation_date). Cancellation is a physical delete.
CREATE UNIQUE INDEX UX_visitor_reservations_space_date
    ON dbo.visitor_reservations(parking_space_id, reservation_date);
GO
CREATE INDEX IX_visitor_reservations_visitor_id
    ON dbo.visitor_reservations(visitor_id);
GO
```

**Notes**
- A reservation makes the space **unavailable** for `reservation_date` in the availability calculation. Visitor reservations apply to parking only (never desks): this table was **not** generalized by V12 — it still references `parking_spaces` directly with a real FK.

### 3.8 `audit_log`
**Purpose:** functional audit trail of admin/employee actions, populated by a Spring AOP aspect.

```sql
CREATE TABLE dbo.audit_log (
    id                 BIGINT IDENTITY(1,1) NOT NULL,
    actor_employee_id  BIGINT NULL,
    action             NVARCHAR(100) NOT NULL,
    entity_type        NVARCHAR(100) NOT NULL,
    entity_id          BIGINT NULL,
    details            NVARCHAR(MAX) NULL,
    occurred_at        DATETIME2(3) NOT NULL CONSTRAINT DF_audit_log_occurred_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_audit_log PRIMARY KEY (id),
    CONSTRAINT FK_audit_log_actor FOREIGN KEY (actor_employee_id) REFERENCES dbo.employees(id)
);
GO
```

**Notes**
- `actor_employee_id` is nullable for system-originated actions (e.g. scheduled purge).
- `details` is `NVARCHAR(MAX)` for a JSON snapshot of before/after where useful.

### 3.9 `login_log`
**Purpose:** every authentication attempt (Phase 1, Phase 2, fallback), kept separate from functional audit.

```sql
CREATE TABLE dbo.login_log (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    login_attempted  NVARCHAR(100) NOT NULL,
    employee_id      BIGINT NULL,
    result           VARCHAR(20) NOT NULL,
    phase            VARCHAR(10) NOT NULL,
    ip_address       NVARCHAR(45) NULL,
    user_agent       NVARCHAR(512) NULL,
    occurred_at      DATETIME2(3) NOT NULL CONSTRAINT DF_login_log_occurred_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_login_log PRIMARY KEY (id),
    CONSTRAINT CK_login_log_result CHECK (result IN ('OK','INVALID_CREDENTIALS','LOCKED','INACTIVE','NO_ACCESS','FALLBACK_OK')),
    CONSTRAINT CK_login_log_phase  CHECK (phase  IN ('PHASE_1','PHASE_2','FALLBACK')),
    CONSTRAINT FK_login_log_employee FOREIGN KEY (employee_id) REFERENCES dbo.employees(id)
);
GO
```

**Notes**
- `employee_id` is nullable: a failed attempt with an unknown login cannot resolve an employee.
- `login_attempted` stores the raw username tried (useful for `NO_ACCESS` / `INVALID_CREDENTIALS`).
- `ip_address` sized for IPv6 (45 chars).

### 3.10 `SPRING_SESSION` / `SPRING_SESSION_ATTRIBUTES`
**Purpose:** server-side HTTP session store (Spring Session JDBC) on SQL Server. **Do not redefine** — use the official schema shipped with the dependency.

- **Official path:** `org/springframework/session/jdbc/schema-sqlserver.sql` inside the `spring-session-jdbc` jar (Spring Session 3.3.x, governed by the Spring Boot 3.3 BOM).
- It is reproduced verbatim in migration `V2__spring_session_schema.sql` (section 7). It **must** match the Spring Session version on the classpath; copy it from the actual dependency rather than hand-editing.

### 3.11 `email_outbox`
**Purpose:** retry store for the cross-cutting `notifications` capability. A row is created **only** when the immediate SMTP send (in the `AFTER_COMMIT` phase of the originating event) fails; a scheduled job re-reads `PENDING` rows and retries them. Introduced by migration **`V11__email_outbox.sql`** (change `init-notifications`).

```sql
CREATE TABLE dbo.email_outbox (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    recipient        NVARCHAR(255) NOT NULL,
    subject          NVARCHAR(255) NOT NULL,
    body_html        NVARCHAR(MAX) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    attempts         INT NOT NULL CONSTRAINT DF_email_outbox_attempts DEFAULT 0,
    last_error       NVARCHAR(500) NULL,
    created_at       DATETIME2(3) NOT NULL,
    last_attempt_at  DATETIME2(3) NULL,
    sent_at          DATETIME2(3) NULL,
    CONSTRAINT PK_email_outbox PRIMARY KEY (id),
    CONSTRAINT CK_email_outbox_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);
GO

-- The retry job filters by status = 'PENDING'; this index serves that scan.
CREATE INDEX IX_email_outbox_status ON dbo.email_outbox(status);
GO
```

**Notes**
- The row stores the **already-rendered** message (`recipient`, `subject`, `body_html`) so the retry job re-sends without re-resolving recipients or re-rendering Thymeleaf templates.
- `status` machine is `PENDING → SENT | FAILED`. The job reads only `PENDING`, so a `SENT` row is never re-sent (idempotency); `FAILED` is reached when `attempts` hits the configured `parking.notifications.max-attempts` (5), stopping the retry loop (e.g. a recipient without a valid email).
- No FK: `recipient` is stored as the literal email address (needed verbatim to re-send), not an `employee_id`.
- `created_at` / `last_attempt_at` / `sent_at` are set in code via the injectable `ClockPort` (no DB default), so retry-window behaviour is testable with a fixed clock.
- **RGPD:** the row holds a recipient email + a rendered body (may contain personal data). It is transient (deleted on success/terminal failure by the app; a periodic purge of terminal rows can be added if volume warrants) and out of scope of the 2-year historical purge of section 9 unless later reclassified.

### 3.12 `system_settings`
**Purpose:** single-row (`id = 1`) global configuration for the whole installation. Introduced by migration **`V22__system_settings.sql`** (change `request-auto-assignment`) with the `approval_mode` parameter, then extended incrementally: **`V28`** adds `parking_address`, **`V29`** adds `weekend_reservable` (change `reservas-employee-admin-reassign`), and **`V30__system_settings_parking_coords.sql`** adds `parking_lat`/`parking_lng` (change `admin-improvements`, task 18). No FK to business tables; `updated_by_id` traces the last ADMIN who changed it.

```sql
CREATE TABLE dbo.system_settings (
    id                 TINYINT      NOT NULL CONSTRAINT DF_system_settings_id DEFAULT 1,
    approval_mode      VARCHAR(10)  NOT NULL CONSTRAINT DF_system_settings_mode DEFAULT 'MANUAL',
    parking_address    VARCHAR(500) NULL,                 -- V28: destino textual de "Ir al parking"
    parking_lat        DECIMAL(9,6) NULL,                 -- V30: latitud del punto exacto (mapa)
    parking_lng        DECIMAL(9,6) NULL,                 -- V30: longitud del punto exacto
    weekend_reservable BIT          NOT NULL CONSTRAINT DF_system_settings_weekend DEFAULT 0,  -- V29
    updated_by_id      BIGINT       NULL,                 -- último ADMIN que modificó (trazabilidad)
    updated_at         DATETIME2    NULL,
    CONSTRAINT PK_system_settings PRIMARY KEY (id),
    CONSTRAINT CK_system_settings_singleton CHECK (id = 1),
    CONSTRAINT CK_system_settings_mode CHECK (approval_mode IN ('MANUAL', 'AUTOMATIC'))
);
GO
```

**Notes**
- **Singleton:** `CHECK (id = 1)` garantiza una única fila de configuración global (no hay ajustes por departamento ni por empleado). Si la fila no existe aún, el dominio usa valores por defecto (`MANUAL`, sin dirección/coordenadas, sin fines de semana).
- `parking_lat`/`parking_lng` son `DECIMAL(9,6)` (precisión ~0.1 m; rango suficiente para lat `[-90, 90]` y lng `[-180, 180]`), nullable. En la entidad JPA se mapean como `BigDecimal`; el dominio y los DTOs los exponen como `Double` (JSON limpio para el mapa). Solo se persisten junto a una `parking_address` no vacía: borrar la dirección descarta las coordenadas.
- `weekend_reservable` gobierna si `POST /requests` acepta sábados/domingos (400 `WEEKEND_NOT_RESERVABLE` cuando es `0`).
- `approval_mode` es leíble por cualquier autenticado en `GET /settings/approval-mode`; `parking_address`+coordenadas en `GET /settings/parking-address`; el ajuste completo (con trazabilidad) solo por `ADMIN` en `GET /admin/settings`.

### 3.13 `push_subscription`
**Purpose:** Web Push subscription of an employee's browser/device (change `push-notifications`, migration **`V31__push_subscription.sql`**). Stores the push-service `endpoint` plus the client keys (`p256dh`/`auth`) needed to encrypt the VAPID payload. An employee may have several (multi-device); unique by `endpoint` (re-subscribe = upsert). Companion to the notification channel flags added by **`V32`** to `system_settings` (`email_notifications_enabled` / `push_notifications_enabled`) and by **`V33`** to `employees` (per-employee preferences).

```sql
CREATE TABLE dbo.push_subscription (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    employee_id  BIGINT        NOT NULL,
    endpoint     VARCHAR(1024) NOT NULL,   -- URL del push service (única)
    p256dh       VARCHAR(255)  NOT NULL,   -- clave pública del cliente (base64url)
    auth         VARCHAR(255)  NOT NULL,   -- secreto de autenticación (base64url)
    user_agent   NVARCHAR(255) NULL,       -- dispositivo/navegador (para que el usuario lo reconozca)
    created_at   DATETIME2(3)  NOT NULL CONSTRAINT DF_push_subscription_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_push_subscription PRIMARY KEY (id),
    CONSTRAINT UQ_push_subscription_endpoint UNIQUE (endpoint),
    CONSTRAINT FK_push_subscription_employee FOREIGN KEY (employee_id)
        REFERENCES dbo.employees(id) ON DELETE CASCADE
);
GO
CREATE INDEX IX_push_subscription_employee ON dbo.push_subscription(employee_id);
GO
```

**Notes**
- **Envío por canal push** = flag global `push_notifications_enabled` (system_settings) **AND** `push_notifications_enabled` del empleado **AND** que exista suscripción. El email es un canal independiente y sirve de fallback.
- **Limpieza:** una suscripción caducada (respuesta `404`/`410 Gone` del push service) se borra automáticamente al enviar; un `5xx` transitorio no la borra (best-effort, sin outbox en v1).
- **Ciclo de vida:** alta idempotente por `endpoint` (`POST /push/subscriptions`); baja por `endpoint` del propio usuario (`DELETE`); borrado en bloque al **desactivar** el empleado (el soft-delete no dispara el `ON DELETE CASCADE`, se hace en `EmployeeService`).
- **RGPD:** el `endpoint` es un identificador de dispositivo ligado al empleado; es estado vivo (no auditoría) y se borra al dar de baja, cerrar sesión y al desactivar/eliminar el empleado. Fuera del purgado histórico de 2 años.

---

## 4. Critical constraints (filtered indexes)

These partial-uniqueness indexes are consolidated here in their **post-V12/V13/V15 form** (each is created inside its table's migration, then rebuilt on `resource_id`/`resource_type` by V12; V15 adds the desk-pending one). English names per the nomenclature authority — the names are preserved verbatim across V12/V13 so the DataIntegrityViolation → 409 mapping in `GlobalExceptionHandler` keeps working:

```sql
-- A resource cannot be fixed-assigned to two employees on the same weekday (active rows only)   [V7 → V12]
CREATE UNIQUE INDEX UX_fixed_assignments_space_day_active
    ON dbo.fixed_assignments(resource_id, resource_type, day_of_week) WHERE active = 1;

-- An employee cannot hold two active fixed assignments of the same type on the same weekday      [V7 → V12]
CREATE UNIQUE INDEX UX_fixed_assignments_employee_day_active
    ON dbo.fixed_assignments(employee_id, resource_type, day_of_week) WHERE active = 1;

-- One PENDING request per employee, resource type and date                                       [V8 → V12]
CREATE UNIQUE INDEX UX_requests_employee_date_pending
    ON dbo.requests(employee_id, resource_type, requested_date) WHERE status = 'PENDING';

-- A resource can be APPROVED at most once per date (concurrency net)                              [V8 → V12]
CREATE UNIQUE INDEX UX_requests_space_date_approved
    ON dbo.requests(resource_id, resource_type, requested_date) WHERE status = 'APPROVED';

-- A desk requested from the floor plan is held at creation (one PENDING desk per date)            [V15]
CREATE UNIQUE INDEX UX_requests_desk_date_pending
    ON dbo.requests(resource_id, requested_date)
    WHERE status = 'PENDING' AND resource_type = 'DESK' AND resource_id IS NOT NULL;

-- One release per resource and date (plain UNIQUE — cancellation is a physical delete)            [V9 → V12]
CREATE UNIQUE INDEX UX_releases_space_date
    ON dbo.releases(resource_id, resource_type, release_date);

-- One visitor reservation per space and date (plain UNIQUE; parking only, not generalized)        [V10]
CREATE UNIQUE INDEX UX_visitor_reservations_space_date
    ON dbo.visitor_reservations(parking_space_id, reservation_date);

-- Unique national id among visitors                                                               [V10]
CREATE UNIQUE INDEX UX_visitors_national_id ON dbo.visitors(national_id);

-- Unique login and email among employees                                                          [V4]
CREATE UNIQUE INDEX UX_employees_login ON dbo.employees(login);
CREATE UNIQUE INDEX UX_employees_email ON dbo.employees(email);
```

> **Why filtered, not constraints:** `UNIQUE` constraints would forbid duplicate revoked/closed rows too. Filtered indexes restrict uniqueness to the *active* (`active = 1`) or *open* (`status = 'PENDING'`) subset, which is exactly the business rule. `UX_releases_space_date` and `UX_visitor_reservations_space_date` are **plain** `UNIQUE` (unfiltered): cancellation there is a physical delete, so a removed row never blocks a new one. `UX_employees_*`, `UX_visitors_national_id` and `UX_parking_spaces_label` are unfiltered (the columns are `NOT NULL` and globally unique).

---

## 5. Performance indexes

There is **no** single `V3__performance_indexes.sql`. `V3__infra_indexes.sql` only carries the two audit/login browsing indexes; every other support index lives inside its table's own migration (V7–V10). Each is justified by the query it serves.

```sql
-- Admin "pending requests" list, FIFO order: WHERE status='PENDING' ORDER BY created_at ASC   [V8]
CREATE INDEX IX_requests_status_created_at
    ON dbo.requests(status, created_at);

-- Employee "my requests": WHERE employee_id=? ORDER BY requested_date                          [V8]
CREATE INDEX IX_requests_employee_id_requested_date
    ON dbo.requests(employee_id, requested_date);

-- "My active fixed assignments" + availability checks per employee/weekday                     [V7]
CREATE INDEX IX_fixed_assignments_employee_id_active
    ON dbo.fixed_assignments(employee_id, active);

-- Employee "my releases": WHERE employee_id=?                                                   [V9]
CREATE INDEX IX_releases_employee_id
    ON dbo.releases(employee_id);

-- A visitor's reservations: WHERE visitor_id=?                                                  [V10]
CREATE INDEX IX_visitor_reservations_visitor_id
    ON dbo.visitor_reservations(visitor_id);

-- Audit browsing and the retention purge scan (occurred_at < cutoff)                            [V3]
CREATE INDEX IX_audit_log_occurred_at ON dbo.audit_log(occurred_at);

-- Login history browsing and retention purge scan                                               [V3]
CREATE INDEX IX_login_log_occurred_at ON dbo.login_log(occurred_at);
```

> **Availability lookups have no dedicated non-unique index.** The "is this resource released / reserved on date F?" queries are served by the plain `UNIQUE` indexes `UX_releases_space_date` (`resource_id, resource_type, release_date`) and `UX_visitor_reservations_space_date` (`parking_space_id, reservation_date`) — a unique index supersedes a separate non-unique one for that access path, which is why the earlier `IX_releases_parking_space_id_date` / `IX_visitor_reservations_parking_space_id_date` were dropped. The availability query for a date F combines all conditions (active resource, no active fixed assignment for that weekday unless released, no `APPROVED` request, and — for parking only — no visitor reservation). Consider an extra covering index on `requests(resource_id, resource_type, requested_date) INCLUDE(status)` once `database-optimizer` profiles real load (left as a tuning item, not added blindly).

---

## 6. Enums (CHECK constraints)

| Table | Column | Allowed values |
|-------|--------|----------------|
| `employees` | `role` | `ADMIN`, `EMPLOYEE` |
| `employees` | `auth_origin` | `LOCAL`, `ENTRA_ID` |
| `requests` | `status` | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` |
| `requests` | `rejection_reason_code` | `NO_AVAILABILITY`, `OUTSIDE_POLICY`, `OTHER` *(nullable; catálogo inicial ⚠️ a confirmar)* |
| `releases` | `type` | `VOLUNTARY`, `ADMINISTRATIVE` |
| `login_log` | `result` | `OK`, `INVALID_CREDENTIALS`, `LOCKED`, `INACTIVE`, `NO_ACCESS`, `FALLBACK_OK` |
| `login_log` | `phase` | `PHASE_1`, `PHASE_2`, `FALLBACK` |
| `email_outbox` | `status` | `PENDING`, `SENT`, `FAILED` |

> **Translation note (vs prompt2, Spanish):** `EMPLEADO→EMPLOYEE`, `PENDIENTE/APROBADA/RECHAZADA/CANCELADA→PENDING/APPROVED/REJECTED/CANCELLED`, `VOLUNTARIA/ADMINISTRATIVA→VOLUNTARY/ADMINISTRATIVE`, `CRED_INVALIDAS→INVALID_CREDENTIALS`, `BLOQUEADO→LOCKED`, `INACTIVO→INACTIVE`, `SIN_ACCESO→NO_ACCESS`, `FASE_1/FASE_2→PHASE_1/PHASE_2`. The generic `FAILED` from the README is split into the finer `INVALID_CREDENTIALS` / `LOCKED` per the prompt.

All implemented as named `CHECK (column IN (...))` constraints inside the `CREATE TABLE` statements in section 3.

---

## 7. Flyway migrations

Schema migrations live in `backend/src/main/resources/db/migration/`; **dev seeds** live in `backend/src/main/resources/db/seed/dev/` (profile `des` only). SQL Server dialect — `GO` is honored as a batch separator by both Flyway and `sqlcmd`. There is **no** `V1__initial_schema.sql`, `V3__performance_indexes.sql` or `V4__seed_bootstrap_admin.sql`: the schema is split one-table-per-migration and the admin is a dev seed (see below).

**Schema migrations (`db/migration/`):**

| Version | File | Content |
|---------|------|---------|
| V1 | `V1__spring_session_schema.sql` | **Verbatim** copy of `schema-sqlserver.sql` from `spring-session-jdbc` 3.3.5 (SQL Server sessions). |
| V2 | `V2__audit_and_login_log.sql` | `audit_log` + `login_log` (§3.8/§3.9); FKs to `employees` deferred to V4. |
| V3 | `V3__infra_indexes.sql` | `IX_audit_log_occurred_at` + `IX_login_log_occurred_at` only. |
| V4 | `V4__employees.sql` | `employees` (§3.1) + its unique indexes; adds the deferred audit/login FKs. |
| V6 | `V6__parking_spaces.sql` | `parking_spaces` (§3.2) + `UX_parking_spaces_label`. (V5 is a dev seed → gap in `db/migration`.) |
| V7 | `V7__fixed_assignments.sql` | `fixed_assignments` (§3.3) + its filtered/support indexes (`parking_space_id` form). |
| V8 | `V8__requests.sql` | `requests` (§3.5) + `UX_requests_employee_date_pending`, `UX_requests_space_date_approved`, support indexes. |
| V9 | `V9__releases.sql` | `releases` (§3.4) + `UX_releases_space_date` + `IX_releases_employee_id`. |
| V10 | `V10__visitors.sql` | `visitors` + `visitor_reservations` (§3.6/§3.7) + their unique/support indexes. |
| V11 | `V11__email_outbox.sql` | `email_outbox` (§3.11) + `IX_email_outbox_status`. |
| V12 | `V12__generic_resource_refactor.sql` | Generalizes `parking_space_id` → `resource_id` + `resource_type` on `fixed_assignments`, `requests`, `releases`; rebuilds their unique indexes; re-adds then keeps `FK_*_parking_spaces`. |
| V13 | `V13__desks.sql` | `desks` (§3.2b) + `UX_desks_number`; **drops** the three `FK_*_parking_spaces` (polymorphic `resource_id`). |
| V15 | `V15__floor_plan_desk_pending_index.sql` | `UX_requests_desk_date_pending` (filtered: PENDING + DESK + `resource_id IS NOT NULL`). (V14 is a dev seed → gap.) |
| V19 | `V19__parking_space_number.sql` | Adds `parking_spaces.number` (`CK_parking_spaces_number CHECK (number >= 1000)`) + `UX_parking_spaces_number`; renumbers existing rows by `id` (spacesPerFloor = 5) preserving `id`; floor is derived (`number/1000`, §3.2). (V16-V18 are dev seeds → gap.) |

**Dev seeds (`db/seed/dev/`, profile `des` only — never PRE/PRO):**

| Version | File | Content |
|---------|------|---------|
| V5 | `V5__seed_dev_admin.sql` | Idempotent local dev admin (`login=admin`, real BCrypt hash, `password_must_change=0`). |
| V14 | `V14__seed_dev_desks.sql` | 65 STANDARD desks at neutral (50,50) coords. |
| V16 | `V16__seed_dev_desk_coords.sql` | Redistributes the 65 desks over a deterministic 9-column grid. |
| V17 | `V17__seed_dev_employee.sql` | Idempotent local dev employee (`login=empleado`, real BCrypt hash, `password_must_change=0`). |
| V18 | `V18__seed_dev_desk_real_coords.sql` | Real per-desk `(x%,y%)` coordinates from the authoritative floor-plan mockup. |

> Flyway runs migrations and dev seeds under a single version timeline: in profile `des` both locations are on the classpath, so V5/V14/V16/V17/V18 interleave with the schema versions. In PRE/PRO only `db/migration` is loaded, leaving intentional gaps at V5, V14, (and no V16–V18).

**V1 — Spring Session (official schema, Spring Session 3.3.5)**

```sql
CREATE TABLE SPRING_SESSION (
    PRIMARY_ID            CHAR(36)     NOT NULL,
    SESSION_ID            CHAR(36)     NOT NULL,
    CREATION_TIME         BIGINT       NOT NULL,
    LAST_ACCESS_TIME      BIGINT       NOT NULL,
    MAX_INACTIVE_INTERVAL INT          NOT NULL,
    EXPIRY_TIME           BIGINT       NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);
GO
CREATE UNIQUE NONCLUSTERED INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
GO
CREATE NONCLUSTERED INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
GO
CREATE NONCLUSTERED INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);
GO
CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36)      NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200)  NOT NULL,
    ATTRIBUTE_BYTES    IMAGE         NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);
GO
```
> ⚠️ Verify this against the exact `spring-session-jdbc` version on the classpath and replace with that artifact's `schema-sqlserver.sql` if it differs. Do not hand-evolve it.

**V5 — dev admin seed** (`db/seed/dev/V5__seed_dev_admin.sql`, profile `des` only — **not** a production bootstrap). It is idempotent and carries a **real, already-generated** BCrypt(cost 12) hash, so it is runnable as-is. `password_must_change = 0` (it is a dev account, not the product of a reset). There is a sibling dev-employee seed (`V17`) with the same shape.

```sql
-- Idempotent local dev administrator (login=admin / password=Admin#Parking2026).
IF NOT EXISTS (SELECT 1 FROM dbo.employees WHERE login = N'admin')
BEGIN
    INSERT INTO dbo.employees
        (first_name, last_name, login, email, password_hash, password_must_change,
         is_corporate, auth_origin, role, enabled, active, last_password_change_at, created_at)
    VALUES
        (N'Dev', N'Admin', N'admin', N'admin.dev@aleatica.local',
         '$2a$12$xFfFx12kQw6hOFBT8E3NKe.GZO0pR5ncDvi3w/xKNdEIED66N4a/i', 0,
         0, 'LOCAL', 'ADMIN', 1, 1, SYSUTCDATETIME(), SYSUTCDATETIME());
END;
GO
```
> This seed loads only under the `des` profile (`db/seed/dev` is not on the PRE/PRO classpath). A production bootstrap admin, if needed, is a separate operational concern — it is **not** shipped as a migration here.

---

## 8. JPA entity mapping

| Table | JPA Entity | Repository |
|-------|------------|------------|
| `employees` | `Employee` | `EmployeeRepository` |
| `parking_spaces` | `ParkingSpace` (implements `BookableResource`) | `ParkingSpaceRepository` |
| `desks` | `Desk` (implements `BookableResource`) | `DeskRepository` |
| `fixed_assignments` | `FixedAssignment` | `FixedAssignmentRepository` |
| `releases` | `Release` | `ReleaseRepository` |
| `requests` | `Request` | `RequestRepository` |
| `visitors` | `Visitor` | `VisitorRepository` |
| `visitor_reservations` | `VisitorReservation` | `VisitorReservationRepository` |
| `email_outbox` | `EmailOutbox` | `EmailOutboxRepository` |
| `audit_log` | `AuditLog` | `AuditLogRepository` |
| `login_log` | `LoginLog` | `LoginLogRepository` |
| `SPRING_SESSION` / `SPRING_SESSION_ATTRIBUTES` | — (framework-managed, no JPA entity) | — |
| — (no table) | `BookableResource` (domain interface) | — |

**Mapping notes**
- `BookableResource` (`com.aleatica.parking.resource`) is a **non-persistent** domain interface — the polymorphic reservable abstraction over `resource_id` + `resource_type`. Materialized by `ParkingSpace` (`ResourceType.PARKING`) and `Desk` (`ResourceType.DESK`); resource existence per type is checked via `ResourceResolverPort`. `FixedAssignment`, `Request` and `Release` map `resource_id`/`resource_type` as plain columns (a `Long` id + a `@Enumerated(STRING) ResourceType`), not a JPA relationship, so there is no ORM-level FK.
- `Desk` uses its stable business key `number` for `equals`/`hashCode`; `coord_x`/`coord_y` map to `BigDecimal`.
- Enums map to `@Enumerated(EnumType.STRING)` with Java enums `Role`, `AuthOrigin`, `RequestStatus`, `ReleaseType`, `LoginResult`, `LoginPhase` — values identical to the CHECK lists.
- `Release` is a valid Java identifier (not a reserved word); no escaping needed.
- Disable Hibernate DDL auto (`spring.jpa.hibernate.ddl-auto=validate`); Flyway owns the schema. `validate` will catch entity/DDL drift at startup.
- Timestamps map to `Instant` (UTC); dates to `LocalDate`.

---

## 9. Retention policy (purge job)

A daily `@Scheduled` job deletes historical rows older than `parking.retention.years` (default 2). Live entities (`employees`, `parking_spaces`, `desks`, active `fixed_assignments`, `visitors`) are **never purged**.

| Table | Purge criterion | Frequency |
|-------|-----------------|-----------|
| `audit_log` | `occurred_at < cutoff` | daily |
| `login_log` | `occurred_at < cutoff` | daily |
| `requests` (closed) | `status <> 'PENDING' AND created_at < cutoff` | daily |
| `releases` | `release_date < cutoffDate` | daily |
| `visitor_reservations` | `reservation_date < cutoffDate` | daily |

Batched deletion to avoid long locks (`DELETE TOP (1000)` loop). Example for one table:

```sql
DECLARE @cutoff DATETIME2(3) = DATEADD(YEAR, -2, SYSUTCDATETIME());

WHILE 1 = 1
BEGIN
    DELETE TOP (1000) FROM dbo.audit_log WHERE occurred_at < @cutoff;
    IF @@ROWCOUNT < 1000 BREAK;
END
```

For date-based tables use `DECLARE @cutoffDate DATE = DATEADD(YEAR, -2, CAST(SYSUTCDATETIME() AS DATE));` and compare against `release_date` / `reservation_date`. The retention window is configurable per environment, so the job reads the cutoff from `parking.retention.years` rather than hard-coding `-2`.

---

## 10. Pendientes

1. ✅ **Dev admin seed resolved:** the local admin is `db/seed/dev/V5__seed_dev_admin.sql` — a **runnable** idempotent seed with a real BCrypt(cost 12) hash and `password_must_change = 0`, loaded only under the `des` profile. There is **no** production bootstrap admin migration; provisioning the real production admin is an operational task (out of this schema's scope).
2. ✅ **`spring-session-jdbc` version resolved:** governed by the Spring Boot 3.3.5 BOM → **Spring Session 3.3.5**. `V1__spring_session_schema.sql` is a verbatim copy of that artifact's `schema-sqlserver.sql`; re-copy from the jar if the version changes.
3. ✅ **`requests` resource discriminator resolved:** V12 added `resource_type` and rebuilt `UX_requests_employee_date_pending` as `(employee_id, resource_type, requested_date)`; V15 added `UX_requests_desk_date_pending` for floor-plan desk holds.
4. **`email` nullability:** assumed `NOT NULL` and unique. Confirm whether non-corporate employees may lack an email (would require a filtered unique index `WHERE email IS NOT NULL`).
5. **Covering index for availability** (`requests(resource_id, resource_type, requested_date) INCLUDE(status)`) — deferred to `database-optimizer` after profiling real load.
6. **`license_plate` uniqueness/format:** currently free `NVARCHAR(15)`, no uniqueness — confirm if a plate must be unique per employee.
7. **Production seed policy:** dev accounts (`V5` admin, `V17` employee) and dev desks (`V14`/`V16`/`V18`) live in `db/seed/dev` and load only under the `des` profile; confirm how PRE/PRO get their initial admin and reservable inventory (currently expected via the ADMIN CRUD on an empty schema).

---

> **Re-sync note:** this document was re-synchronized with the **actual schema post-V12 (generic-resource-refactor) / V13 (desks) / V15 (floor-plan desk index)**. The three generalized tables (`fixed_assignments`, `requests`, `releases`) carry `resource_id` + `resource_type` (no FK; polymorphic integrity via `ResourceResolverPort`); `desks`, `email_outbox` and the `BookableResource` domain interface are included; the migration/seed inventory matches `db/migration/*` and `db/seed/dev/*`.
