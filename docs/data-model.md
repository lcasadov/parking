# data-model.md — Canonical Data Model (SQL Server 2022)

> **Product:** parking — ALEATICA parking spaces & office desks.
> **Engine:** Microsoft SQL Server 2022 · **Persistence:** Spring Data JPA + Hibernate 6.5 · **Migrations:** Flyway 10.
> **Consumers:** `database-optimizer`, `backend-architect`.
> **Authority of names:** the README section *"Nomenclatura del código (ES → EN)"*. All tables, columns, enums and JPA classes use **English** identifiers. Business prose stays Spanish in the README; SQL is English/snake_case.
>
> **Scope note:** this model covers the **parking core (MVP, Phase 1)**: the 9 live entities plus Spring Session. **Office desks (`desks`) and the floor plan are out of scope here** — they arrive in the later `generic-resource-refactor` + `desks` increments, which generalize `parking_space_id` into a polymorphic `resource_id`. Until then every reservable FK points to `parking_spaces`.

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
       │ N              │ 0..1       │ N           │ 1
       ▼                ▼            ▼             ▼ N
┌────────────────────────────────────────┐ ┌──────────────────────┐
│            parking_spaces               │ │ visitor_reservations │
│          (ParkingSpace, live)           │◄┤(VisitorReservation)  │
└─────────────────────────────────────────┘ └──────────────────────┘

           ┌───────────┐         ┌──────────────────────────────┐
           │ audit_log │         │ SPRING_SESSION /             │
           │(AuditLog) │         │ SPRING_SESSION_ATTRIBUTES    │
           └───────────┘         │ (framework-managed)          │
                                 └──────────────────────────────┘
```

**Cardinalities**
- `employees 1 — N fixed_assignments` (titular), and `1 — N` again as `created_by` / `revoked_by`.
- `parking_spaces 1 — N fixed_assignments`.
- `employees 1 — N requests`; `parking_spaces 0..1 — N requests` (`parking_space_id` is NULL while `PENDING`).
- `employees 1 — N releases`; `parking_spaces 1 — N releases`.
- `employees 1 — N visitors` (creator); `visitors 1 — N visitor_reservations`; `parking_spaces 1 — N visitor_reservations`.
- `employees 0..1 — N login_log` / `audit_log` (actor may be unknown/anonymous).

---

## 3. Tables

> All `CREATE TABLE` and unique/filtered indexes below belong to migration **`V1__initial_schema.sql`** (section 7). Performance indexes (section 5) belong to **`V3`**.

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
**Purpose:** physical parking spaces, identified by a label (e.g. `P-08`), active/inactive.

```sql
CREATE TABLE dbo.parking_spaces (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    label       NVARCHAR(20) NOT NULL,
    active      BIT NOT NULL CONSTRAINT DF_parking_spaces_active DEFAULT 1,
    created_at  DATETIME2(3) NOT NULL CONSTRAINT DF_parking_spaces_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_parking_spaces PRIMARY KEY (id)
);
GO
CREATE UNIQUE INDEX UX_parking_spaces_label ON dbo.parking_spaces(label);
GO
```

**Notes**
- `label` is unique and human-facing. Inactive spaces are never available for any date.
- Future `desks` will be a sibling table; the `generic-resource-refactor` introduces a shared `resource_id` abstraction.

### 3.3 `fixed_assignments`
**Purpose:** indefinite link between an employee and a parking space for a given weekday. Logically revoked, never deleted.

```sql
CREATE TABLE dbo.fixed_assignments (
    id                BIGINT IDENTITY(1,1) NOT NULL,
    parking_space_id  BIGINT NOT NULL,
    employee_id       BIGINT NOT NULL,
    day_of_week       TINYINT NOT NULL,
    active            BIT NOT NULL CONSTRAINT DF_fixed_assignments_active DEFAULT 1,
    created_by_id     BIGINT NOT NULL,
    created_at        DATETIME2(3) NOT NULL CONSTRAINT DF_fixed_assignments_created_at DEFAULT SYSUTCDATETIME(),
    revoked_by_id     BIGINT NULL,
    revoked_at        DATETIME2(3) NULL,
    CONSTRAINT PK_fixed_assignments PRIMARY KEY (id),
    CONSTRAINT CK_fixed_assignments_day_of_week CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT FK_fixed_assignments_parking_spaces FOREIGN KEY (parking_space_id) REFERENCES dbo.parking_spaces(id),
    CONSTRAINT FK_fixed_assignments_employee       FOREIGN KEY (employee_id)      REFERENCES dbo.employees(id),
    CONSTRAINT FK_fixed_assignments_created_by     FOREIGN KEY (created_by_id)    REFERENCES dbo.employees(id),
    CONSTRAINT FK_fixed_assignments_revoked_by     FOREIGN KEY (revoked_by_id)    REFERENCES dbo.employees(id)
);
GO
CREATE UNIQUE INDEX UX_fixed_assignments_space_day_active
    ON dbo.fixed_assignments(parking_space_id, day_of_week) WHERE active = 1;
GO
CREATE UNIQUE INDEX UX_fixed_assignments_employee_day_active
    ON dbo.fixed_assignments(employee_id, day_of_week) WHERE active = 1;
GO
```

**Notes**
- `day_of_week` is **1-7** (1=Monday … 7=Sunday), enforced by CHECK.
- Revocation = `active=0` + `revoked_at` + `revoked_by_id`. The two filtered unique indexes guarantee that, **among active rows only**, a space and an employee are each used at most once per weekday — past/revoked history is unaffected.

### 3.4 `releases`
**Purpose:** marks a fixed-assigned space as available for one concrete date. Voluntary (by owner) or administrative (by admin).

```sql
CREATE TABLE dbo.releases (
    id                BIGINT IDENTITY(1,1) NOT NULL,
    parking_space_id  BIGINT NOT NULL,
    employee_id       BIGINT NOT NULL,
    release_date      DATE NOT NULL,
    type              VARCHAR(15) NOT NULL,
    reason            NVARCHAR(500) NULL,
    released_by_id    BIGINT NOT NULL,
    created_at        DATETIME2(3) NOT NULL CONSTRAINT DF_releases_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_releases PRIMARY KEY (id),
    CONSTRAINT CK_releases_type CHECK (type IN ('VOLUNTARY','ADMINISTRATIVE')),
    CONSTRAINT FK_releases_parking_spaces FOREIGN KEY (parking_space_id) REFERENCES dbo.parking_spaces(id),
    CONSTRAINT FK_releases_employee       FOREIGN KEY (employee_id)      REFERENCES dbo.employees(id),
    CONSTRAINT FK_releases_released_by     FOREIGN KEY (released_by_id)   REFERENCES dbo.employees(id)
);
GO
```

**Notes**
- `employee_id` = the fixed-assignment owner whose space is freed; `released_by_id` = who performed the release (the owner for `VOLUNTARY`, an admin for `ADMINISTRATIVE`).
- `reason` is required by business rule for `ADMINISTRATIVE` (enforced in the service layer, not by the schema, since `VOLUNTARY` allows NULL).
- A release row makes the space available for `release_date` in the availability calculation.

### 3.5 `requests`
**Purpose:** an employee's point-in-time request for a parking space on a concrete date, with approve/reject lifecycle.

```sql
CREATE TABLE dbo.requests (
    id                    BIGINT IDENTITY(1,1) NOT NULL,
    employee_id           BIGINT NOT NULL,
    requested_date        DATE NOT NULL,
    status                VARCHAR(10) NOT NULL CONSTRAINT DF_requests_status DEFAULT 'PENDING',
    parking_space_id      BIGINT NULL,
    approval_note         NVARCHAR(500) NULL,
    rejection_reason_code VARCHAR(40) NULL,
    rejection_reason      NVARCHAR(500) NULL,
    resolved_by_id        BIGINT NULL,
    resolved_at           DATETIME2(3) NULL,
    created_at            DATETIME2(3) NOT NULL CONSTRAINT DF_requests_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_requests PRIMARY KEY (id),
    CONSTRAINT CK_requests_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    CONSTRAINT CK_requests_rejection_reason_code
        CHECK (rejection_reason_code IS NULL OR rejection_reason_code IN ('NO_AVAILABILITY','OUTSIDE_POLICY','OTHER')),
    CONSTRAINT FK_requests_employee       FOREIGN KEY (employee_id)      REFERENCES dbo.employees(id),
    CONSTRAINT FK_requests_parking_spaces FOREIGN KEY (parking_space_id) REFERENCES dbo.parking_spaces(id),
    CONSTRAINT FK_requests_resolved_by     FOREIGN KEY (resolved_by_id)   REFERENCES dbo.employees(id)
);
GO
CREATE UNIQUE INDEX UX_requests_employee_date_pending
    ON dbo.requests(employee_id, requested_date) WHERE status = 'PENDING';
GO
```

**Notes**
- State machine: `PENDING` (`parking_space_id = NULL`) → `APPROVED` (space + `resolved_by_id` + `resolved_at` [+ optional `approval_note`]) | `REJECTED` (`rejection_reason_code` + resolver, optional free `rejection_reason`) | `CANCELLED` (by the employee while PENDING).
- `approval_note` (added per UI): free note from the admin that travels in the approval email (mockup `05-modal-aprobar-solicitud`).
- **Rejection reason catalog** (added per UI): `rejection_reason_code` is a fixed catalog (`NO_AVAILABILITY`, `OUTSIDE_POLICY`, `OTHER`) shown translated in the UI; `rejection_reason` carries the free-text comment, **required when the code is `OTHER`** (≥ 5 chars) and optional otherwise. A rejection must always carry a code. ⚠️ The exact catalog values are a starting set, pendiente de confirmar con negocio.
- The filtered unique index enforces **one `PENDING` request per employee per date**; rejected/cancelled rows do not block new requests.
- Scope note: in this parking-only core a request targets a parking space. When `desks` arrive, a `resource_type` discriminator joins this uniqueness key (employee + date + resource type).

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
```

**Notes**
- A reservation makes the space **unavailable** for `reservation_date` in the availability calculation. Visitor reservations apply to parking only (never desks).

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

---

## 4. Critical constraints (filtered indexes)

These partial-uniqueness indexes are created in `V1` (shown inline above; consolidated here). English names per the nomenclature authority:

```sql
-- A space cannot be fixed-assigned to two employees on the same weekday (active rows only)
CREATE UNIQUE INDEX UX_fixed_assignments_space_day_active
    ON dbo.fixed_assignments(parking_space_id, day_of_week) WHERE active = 1;

-- An employee cannot hold two active fixed assignments on the same weekday
CREATE UNIQUE INDEX UX_fixed_assignments_employee_day_active
    ON dbo.fixed_assignments(employee_id, day_of_week) WHERE active = 1;

-- One PENDING request per employee per date
CREATE UNIQUE INDEX UX_requests_employee_date_pending
    ON dbo.requests(employee_id, requested_date) WHERE status = 'PENDING';

-- Unique national id among visitors
CREATE UNIQUE INDEX UX_visitors_national_id ON dbo.visitors(national_id);

-- Unique login and email among employees
CREATE UNIQUE INDEX UX_employees_login ON dbo.employees(login);
CREATE UNIQUE INDEX UX_employees_email ON dbo.employees(email);
```

> **Why filtered, not constraints:** `UNIQUE` constraints would forbid duplicate revoked/closed rows too. Filtered indexes restrict uniqueness to the *active* (`active = 1`) or *open* (`status = 'PENDING'`) subset, which is exactly the business rule. `UX_employees_*`, `UX_visitors_national_id` and `UX_parking_spaces_label` are unfiltered (the columns are `NOT NULL` and globally unique).

---

## 5. Performance indexes

Created in `V3__performance_indexes.sql`. Each is justified by the query it serves.

```sql
-- Admin "pending requests" list, FIFO order: WHERE status='PENDING' ORDER BY created_at ASC
CREATE INDEX IX_requests_status_created_at
    ON dbo.requests(status, created_at);

-- Employee "my requests": WHERE employee_id=? ORDER BY requested_date
CREATE INDEX IX_requests_employee_id_requested_date
    ON dbo.requests(employee_id, requested_date);

-- "My active fixed assignments" + availability checks per employee/weekday
CREATE INDEX IX_fixed_assignments_employee_id_active
    ON dbo.fixed_assignments(employee_id, active);

-- Availability: is this space released on date F? WHERE parking_space_id=? AND release_date=?
CREATE INDEX IX_releases_parking_space_id_date
    ON dbo.releases(parking_space_id, release_date);

-- Availability: is this space reserved for a visitor on date F?
CREATE INDEX IX_visitor_reservations_parking_space_id_date
    ON dbo.visitor_reservations(parking_space_id, reservation_date);

-- Audit browsing and the retention purge scan (occurred_at < cutoff)
CREATE INDEX IX_audit_log_occurred_at ON dbo.audit_log(occurred_at);

-- Login history browsing and retention purge scan
CREATE INDEX IX_login_log_occurred_at ON dbo.login_log(occurred_at);
```

> The availability query for a date F combines all four conditions (active space, no active fixed assignment for that weekday unless released, no `APPROVED` request, no visitor reservation). `IX_requests_status_created_at` plus the per-space release/reservation indexes cover the hot paths; consider an extra covering index on `requests(parking_space_id, requested_date) INCLUDE(status)` once `database-optimizer` profiles real load (left as a tuning item, not added blindly).

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

> **Translation note (vs prompt2, Spanish):** `EMPLEADO→EMPLOYEE`, `PENDIENTE/APROBADA/RECHAZADA/CANCELADA→PENDING/APPROVED/REJECTED/CANCELLED`, `VOLUNTARIA/ADMINISTRATIVA→VOLUNTARY/ADMINISTRATIVE`, `CRED_INVALIDAS→INVALID_CREDENTIALS`, `BLOQUEADO→LOCKED`, `INACTIVO→INACTIVE`, `SIN_ACCESO→NO_ACCESS`, `FASE_1/FASE_2→PHASE_1/PHASE_2`. The generic `FAILED` from the README is split into the finer `INVALID_CREDENTIALS` / `LOCKED` per the prompt.

All implemented as named `CHECK (column IN (...))` constraints inside the `CREATE TABLE` statements in section 3.

---

## 7. Flyway migrations

Location: `backend/src/main/resources/db/migration/`. SQL Server dialect — `GO` is honored as a batch separator by both Flyway and `sqlcmd`.

| Version | File | Content |
|---------|------|---------|
| V1 | `V1__initial_schema.sql` | All live tables (3.1–3.9) + their CHECK constraints + the filtered/unique indexes of section 4. |
| V2 | `V2__spring_session_schema.sql` | **Verbatim** copy of the official `schema-sqlserver.sql` from `spring-session-jdbc` (Phase-1 sessions live in SQL Server too). |
| V3 | `V3__performance_indexes.sql` | All indexes of section 5. |
| V4 | `V4__seed_bootstrap_admin.sql` | Single bootstrap admin for Phase 1. |

**V2 — Spring Session (official schema, Spring Session 3.3.x)**

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

**V4 — bootstrap admin** (the only seed authored via Flyway; functional seed data otherwise lives in `database/seed/`)

```sql
-- Single bootstrap administrator for Phase 1.
-- password_hash MUST be a real BCrypt(cost 12) hash — see Pendientes (section 10).
INSERT INTO dbo.employees
    (first_name, last_name, login, email, password_hash, password_must_change,
     is_corporate, auth_origin, role, enabled, active, last_password_change_at, created_at)
VALUES
    (N'Bootstrap', N'Admin', N'admin', N'admin@aleatica.local',
     '_[pendiente — generar BCrypt cost 12]_', 1,
     0, 'LOCAL', 'ADMIN', 1, 1, SYSUTCDATETIME(), SYSUTCDATETIME());
GO
```
> This file is **not executable as-is**: the `password_hash` placeholder must be replaced by a generated BCrypt hash before running. `password_must_change = 1` forces a change on first login.

---

## 8. JPA entity mapping

| Table | JPA Entity | Repository |
|-------|------------|------------|
| `employees` | `Employee` | `EmployeeRepository` |
| `parking_spaces` | `ParkingSpace` | `ParkingSpaceRepository` |
| `fixed_assignments` | `FixedAssignment` | `FixedAssignmentRepository` |
| `releases` | `Release` | `ReleaseRepository` |
| `requests` | `Request` | `RequestRepository` |
| `visitors` | `Visitor` | `VisitorRepository` |
| `visitor_reservations` | `VisitorReservation` | `VisitorReservationRepository` |
| `audit_log` | `AuditLog` | `AuditLogRepository` |
| `login_log` | `LoginLog` | `LoginLogRepository` |
| `SPRING_SESSION` / `SPRING_SESSION_ATTRIBUTES` | — (framework-managed, no JPA entity) | — |

**Mapping notes**
- Enums map to `@Enumerated(EnumType.STRING)` with Java enums `Role`, `AuthOrigin`, `RequestStatus`, `ReleaseType`, `LoginResult`, `LoginPhase` — values identical to the CHECK lists.
- `Release` is a valid Java identifier (not a reserved word); no escaping needed.
- Disable Hibernate DDL auto (`spring.jpa.hibernate.ddl-auto=validate`); Flyway owns the schema. `validate` will catch entity/DDL drift at startup.
- Timestamps map to `Instant` (UTC); dates to `LocalDate`.

---

## 9. Retention policy (purge job)

A daily `@Scheduled` job deletes historical rows older than `parking.retention.years` (default 2). Live entities (`employees`, `parking_spaces`, active `fixed_assignments`, `visitors`) are **never purged**.

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

1. **BCrypt hash** for the bootstrap admin in `V4` (cost 12) — the migration is not runnable until provided.
2. **Bootstrap admin contact data:** real `email` (placeholder `admin@aleatica.local`) — confirm with the customer.
3. ✅ **`spring-session-jdbc` version resolved:** governed by the Spring Boot 3.3 BOM → **Spring Session 3.3.x**. Copy the verbatim `schema-sqlserver.sql` from that exact artifact for `V2` (the included script targets Spring Session 3.x; confirm byte-for-byte against the resolved 3.3.x jar).
4. **`requests` resource discriminator:** the `PENDING`-uniqueness key is `(employee_id, requested_date)` for the parking-only core. When `desks` land, decide whether to add `resource_type` to the table and to `UX_requests_employee_date_pending`.
5. **`email` nullability:** assumed `NOT NULL` and unique. Confirm whether non-corporate employees may lack an email (would require a filtered unique index `WHERE email IS NOT NULL`).
6. **Covering index for availability** (`requests(parking_space_id, requested_date) INCLUDE(status)`) — deferred to `database-optimizer` after profiling real load.
7. **`license_plate` uniqueness/format:** currently free `NVARCHAR(15)`, no uniqueness — confirm if a plate must be unique per employee.
8. **Seed placement policy:** bootstrap admin is delivered as Flyway `V4`; confirm this over `database/seed/` (the document contract separates schema from seed, but section 7 of the brief requested the admin as a migration).
