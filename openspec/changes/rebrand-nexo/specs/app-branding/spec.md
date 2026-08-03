## ADDED Requirements

### Requirement: Nombre de producto de la aplicación
La aplicación SHALL usar **"Nexo"** como nombre de producto en todas las superficies visibles al usuario. El nombre de producto es distinto de la **empresa propietaria ("ALEATICA")**, que permanece inalterada.

#### Scenario: Título del navegador
- **WHEN** el usuario carga cualquier página de la aplicación
- **THEN** el `<title>` del documento es "Nexo" (a secas) y no contiene "ALEATICA"

#### Scenario: Aplicación instalable (PWA)
- **WHEN** el navegador lee `manifest.webmanifest`
- **THEN** `name`, `short_name` y `description` referencian "Nexo" como nombre de producto y no "ALEATICA"

### Requirement: Marca de producto consistente en todas las superficies
El nombre de producto "Nexo" SHALL aparecer de forma consistente en el lockup de marca de la app (sidebar, topbar y pantalla de acceso), en los emails transaccionales, en la documentación de la API y en el título de las notificaciones push.

#### Scenario: Lockup de marca en la interfaz
- **WHEN** se renderiza el shell autenticado o la pantalla de login
- **THEN** el subtítulo/lockup de marca muestra "Nexo" como nombre de producto
- **AND** el logotipo (imagen y su `alt`) se conserva el de ALEATICA de momento (estado transitorio aceptado)

#### Scenario: Email transaccional
- **WHEN** el sistema envía un email transaccional (alta, aprobación, rechazo, cancelación, reset de contraseña, etc.)
- **THEN** la referencia al **producto** en el cuerpo/cabecera es "Nexo"
- **AND** la dirección remitente y el dominio de correo NO cambian

#### Scenario: Documentación de la API
- **WHEN** se genera la especificación OpenAPI
- **THEN** el título/descripción del documento referencian "Nexo" como producto

#### Scenario: Notificación push
- **WHEN** el sistema emite una notificación push
- **THEN** el título mostrado usa "Nexo" como nombre de app

### Requirement: Separación entre nombre de producto y empresa
El renombrado SHALL limitar el cambio al **nombre de producto**. Los identificadores e infraestructura de la **empresa** NO se renombran.

#### Scenario: Identificador de código interno
- **WHEN** se revisa el código backend
- **THEN** el paquete Java `com.aleatica.parking` permanece sin cambios (no es superficie visible al usuario)

#### Scenario: Infraestructura de correo de la empresa
- **WHEN** se revisan dominios y remitentes de correo
- **THEN** `no-reply@parking.aleatica.com` y las direcciones `@aleatica.com` (semilla/tests) permanecen sin cambios

#### Scenario: Referencia a la empresa en el flujo corporativo
- **WHEN** un texto se refiere explícitamente a la **empresa** o a su landing corporativa (p. ej. la redirección SSO de Fase 2)
- **THEN** conserva "ALEATICA" (empresa), sin sustituirse por "Nexo" (producto)
