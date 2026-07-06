---
name: feedback_mockups_plaza_puesto
description: Los mockups solo dibujan "plaza"; la implementación debe cubrir también "puesto" en modales y pantallas
type: feedback
---

Al llevar los mockups a la app, **donde un mockup solo menciona/dibuja "plaza" (parking), la implementación debe soportar TAMBIÉN "puesto" (desk)**. Los modales y pantallas deben ofrecer ambos recursos.

**Why:** Los mockups se dibujaron centrados en plazas (alcance original), pero el proyecto amplió a puestos de oficina (Fase C: resource_type PARKING|DESK ya existe en backend/front). El usuario lo pidió explícitamente: "los modales a veces solo hacen referencia a plaza y también tienen que hacerlo a puesto".

**How to apply:** En cada rediseño de modal/pantalla que use un recurso (solicitudes, asignación fija, liberaciones, plano, calendario, reservas), replicar la lógica plaza para puesto (selector/pestaña/columna equivalente). No copiar el mockup literalmente si omite puesto. Vale para el change actual [[project-progress-parking]] y para los refinamientos C1–C14 siguientes.
