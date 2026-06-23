# Prompt 1

Rol: Eres un experimentado Arquitecto de DataBase Senior en el diseño de Bases de datos..

Objetivo: Diseño de modelo de datos completo.Analiza toda la documentación disponible @prompts/descripcion/descripcion.md y @readme.md y, basándote sólo y exclusivamente en esta información, diseña un modelo de datos completo que incluya entidades, atributos (nombre y tipo) y relaciones. Usa el estándar Diagramas Entidad-Relación (ER). La nomenclatura de entidades y atributos deben estar en ingles.

Outputs a generar: actualiza el @readme.md genera la documentación relativa al punto 4 Modelo de datos e incluye los diagramas ER necesarios en formato Mermaid

No inventes lo que no sepas. Si necesitas solucionar dudas, pregúntame antes de realizar nada.

# Respuesta Prompt 1
Antes de diseñar el modelo, tengo **3 dudas concretas** que afectan directamente a la estructura de las tablas y que no están respondidas en la documentación:

---

### ❓ Preguntas previas

**1. Precio de la reserva**
La descripción menciona pagos e ingresos pero nunca define un modelo de precios. ¿Cómo funciona?

- **Opción A** — Precio fijo global (ej. 10€/hora) configurado por el administrador. Todas las reservas cuestan lo mismo.
- **Opción B** — Precio variable según duración (ej. 10€/h → 1h=10€, 1.5h=15€, 2h=20€...).
- **Opción C** — El link de Redsys ya lleva el importe embebido y el sistema solo registra si está pagado o no, sin almacenar el importe.

> Esto afecta a las tablas `PAYMENTS` y `SYSTEM_CONFIG` y al registro de ingresos del dashboard.
Respuesta: Opción B. Precio Variable según duración. El administrador marca el precio/hora. y se multiplica por la horas de la reserva.
---

**2. Vinculación de Telegram**
Para enviar mensajes directos a un usuario (OTP, confirmación de pago, notificaciones), Telegram requiere el `chat_id` del usuario, que es distinto al número de teléfono.

- **Opción A** — El usuario se vincula en su perfil haciendo `/start` al bot y el sistema almacena su `telegram_chat_id`.
- **Opción B** — Solo se usa el número de teléfono (que en Telegram puede buscarse si el usuario lo tiene público, pero no es fiable).
- **Opción C** — No hay mensajes directos a usuarios individuales por Telegram; solo se publica en el grupo.

> Esto determina si añadimos el campo `telegram_chat_id` a la tabla `USERS`.
Respuesta: Opción B. El usuario debe tener su telegram bien configurado.
---

**3. Historial de notificaciones**
La descripción menciona envío de notificaciones (email, Telegram) pero no especifica si se debe almacenar un registro de cada notificación enviada (aparte del log de auditoría).

- **Opción A** — Solo el log de auditoría. Una entrada de auditoría por cada notificación enviada.
- **Opción B** — Tabla `NOTIFICATION_LOG` independiente con estado de entrega (enviado/fallido), canal y destinatario.

> Opción B tiene más valor para el administrador (puede ver si una notificación llegó) pero añade complejidad.
Respuesta: Opción B. 
---

Con las respuestas a estas 3 preguntas genero el modelo de datos completo sin inventar nada. ¿Me las confirmas?

# Prompt 2 
Respuesta 1: Opción B. Precio Variable según duración. El administrador marca el precio/hora. y se multiplica por la horas de la reserva.
Respuesta 2: Opción B. El usuario debe tener su telegram bien configurado.
Respuesta 3:  Opción B. 

# Respuesta prompt 2 

✅ Sección 4 generada. Aquí el resumen completo:

---

### 📊 Modelo de Datos — Resumen

#### **8 entidades** diseñadas exclusivamente a partir de la documentación

| Entidad | Campos | Decisión clave |
|---|---|---|
| `USERS` | 11 | `phone` como identificador Telegram; roles `ADMIN`/`USER` |
| `RESERVATIONS` | 11 | Estados: `CONFIRMED` → `PAID`/`CANCELLED`/`PENDING_PAYMENT` |
| `PARTICIPANTS` | 9 | `user_id = NULL` para jugadores externos; máx. 4 por reserva |
| `PAYMENTS` | 12 | `amount = price_per_hour × (duration_minutes/60)`; `UNIQUE` por reserva |
| `OTP_CODES` | 7 | TTL 10 min; 3 tipos: reserva, cancelación, reset password |
| `AUDIT_LOG` | 9 | Inmutable; cubre WEB, TELEGRAM y acciones SYSTEM |
| `NOTIFICATION_LOG` | 12 | 3 canales: `EMAIL`, `TELEGRAM_DIRECT`, `TELEGRAM_GROUP`; estado `SENT`/`FAILED` |
| `SYSTEM_CONFIG` | 15 | Singleton (`id=1`); `price_per_hour` configurable; credenciales cifradas AES-256 |

#### **2 diagramas ER Mermaid**
- **4.2** — Modelo core: `USERS ↔ RESERVATIONS ↔ PARTICIPANTS ↔ PAYMENTS`
- **4.3** — Modelo transversal: `OTP_CODES`, `AUDIT_LOG`, `NOTIFICATION_LOG`, `SYSTEM_CONFIG`

#### **17 índices** definidos con justificación de rendimiento por tabla

#### Decisiones derivadas de las respuestas confirmadas:
- `price_per_hour` en `SYSTEM_CONFIG` → multiplicado por horas en `PAYMENTS.amount`
- `phone` en `USERS` → identificador para envío por Telegram (no se almacena `telegram_chat_id`)
- `NOTIFICATION_LOG` con estado de entrega → el admin puede ver notificaciones fallidas

# Prompt 3

Rol: Eres un experimentado Arquitecto de DataBase Senior en el diseño de Bases de datos..

Objetivo: Analiza toda la documentación disponible @prompts/descripcion/descripcion.md y @readme.md y, basándote sólo y exclusivamente en esta información, y analiza cuales serán las consultas más frecuentes y  optimízalo para:
- Índices para búsquedas frecuentes de disponibilidad
- Constraints para integridad de datos de negocio
- Particionado si es necesario para grandes volúmenes
- Estrategia de caching para consultas repetitivas


Outputs a generar: actualiza el @readme.md genera la documentación relativa al punto 4 Modelo de datos, indica las consultas más frecuentes actualiza lo necesario.

No inventes lo que no sepas. Si necesitas solucionar dudas, pregúntame antes de realizar nada.

# Respuesta Prompt 3

Tengo todo lo necesario. Voy a analizar el modelo completo antes de escribir nada.

**Análisis de consultas frecuentes identificadas:**

1. **Disponibilidad** (crítica + concurrencia): overlap check `WHERE reservation_date = ? AND status != 'CANCELLED' AND ranges overlap`
2. **Calendario semanal** (dashboard): `WHERE reservation_date BETWEEN today AND today+7`
3. **Reservas incompletas** (unirse): `GROUP BY r.id HAVING COUNT(participants) < 4`
4. **Historial del usuario**: `WHERE owner_id = ? ORDER BY reservation_date DESC`
5. **Pagos pendientes del usuario**: JOIN reservations+payments `WHERE status='PENDING'`
6. **Validación OTP**: `WHERE user_id=? AND type=? AND used=false AND expires_at > NOW()`
7. **SYSTEM_CONFIG**: singleton leído en cada operación de precio y config
8. **Autenticación**: `WHERE login=? AND status='ACTIVE'`

Ahora reescribo 4.6 y añado 4.7–4.10:
Todo en orden. Resumen de lo añadido a la Sección 4:

---

### ✅ Cambios aplicados a la Sección 4

**4.6 Estrategia de Índices** — completamente reescrita

| Antes | Ahora |
|---|---|
| 17 índices simples en tabla plana | 21 índices con tipo, columnas, condición parcial y consulta cubierta |
| Sin índice de solapamiento | **`excl_res_no_overlap` GiST exclusion** (prevención de doble reserva a nivel BD) |
| Sin índices compuestos reales | 8 índices compuestos alineados con patrones de consulta reales |
| Sin índices parciales | 5 índices parciales (solo filas relevantes) |

**4.7 Consultas Frecuentes** *(nuevo)* — 8 queries con SQL completo, índice activo y tiempo esperado

**4.8 Constraints de Integridad** *(nuevo)* — DDL completo con:
- `CHECK` de duraciones válidas (60/90/120/150/180 min)
- `CHECK` de coherencia `end_time = start_time + duration`
- `CHECK` de inicio en :00 o :30
- `CHECK` formato OTP 6 dígitos (`code ~ '^\d{6}$'`)
- `CHECK` jugador externo XOR registrado
- `CHECK` singleton de `SYSTEM_CONFIG (id = 1)`
- **`EXCLUDE USING gist`** para prevención de solapamiento (requiere `btree_gist`)

**4.9 Particionado** *(nuevo)* — Análisis de volumen: **no necesario en v1.0** (< 4.000 reservas en 5 años). Plan documentado para cuando `AUDIT_LOG` supere 500.000 filas

**4.10 Caché** *(nuevo)* — Spring Cache + Caffeine con 4 cachés definidas: `system-config` (sin TTL, invalidación explícita), `available-slots` (30s), `weekly-calendar` (30s), `user-profile` (5 min). Código Java de anotaciones incluido. Justificación de qué NO se cachea (OTP, pagos en curso)