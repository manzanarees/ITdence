# Análisis de aplicaciones similares y propuesta de funcionalidades

Documento de trabajo para el apartado "Estado del arte / análisis de la competencia" de la memoria y para decidir qué se añade a ITdence.

## 1. Aplicaciones revisadas

| Aplicación | Tipo | Qué es |
|---|---|---|
| ManageEngine ServiceDesk Plus | Comercial (servidor o nube) | Helpdesk ITIL completo. Es el de la primera captura (tablero de solicitudes). |
| Zendesk / Freshdesk / BoldDesk | Comerciales (SaaS) | Helpdesk de atención al cliente en la nube. |
| Odoo Helpdesk | Comercial (módulo de Odoo) | Tickets integrados con el resto del ERP. |
| GLPI | Código abierto | Tickets + inventario de equipos (CMDB). Necesita servidor web y base de datos. |
| osTicket | Código abierto | Tickets con portal de cliente. Necesita PHP y MySQL. |

Fuentes consultadas (son páginas de los propios fabricantes y comparadores, así que describen lo que dicen ofrecer, no pruebas propias):

- [ManageEngine: Request Kanban View](https://manageengine.com/products/service-desk-msp/help/adminguide/requests/kanban-view.html)
- [ManageEngine: Kanban Board for service desk](https://www.manageengine.com/products/service-desk/automation/kanban-board.html)
- [Tidio: ticketing system features](https://www.tidio.com/blog/ticketing-system-features/)
- [Advisory Apps: Helpdesk & Support Ticketing](https://advisoryapps.com/modules/helpdesk-ticketing)
- [Odoo Helpdesk](https://odoo.com/app/helpdesk)
- [Capterra: comparativa osTicket vs GLPI](https://www.capterra.es/compare/125118/126254/osticket/vs/glpi)
- Capturas de ejemplo: tablero Kanban de ServiceDesk Plus y la infografía "Anatomía de un ticket de soporte" (invgate.com).

## 2. Qué tienen todas

Esto es lo que se repite en casi todas las herramientas y por tanto lo que un usuario espera encontrar:

1. **Vista de tablero (Kanban)**: tarjetas en columnas agrupadas por estado, prioridad o técnico. Al arrastrar una tarjeta cambia ese campo.
2. **Vista de lista** con filtros, búsqueda, orden por columnas y elección de columnas.
3. **SLA (acuerdo de nivel de servicio)**: tiempo máximo de respuesta y de resolución según la prioridad, con aviso de "vence en…" y marca de "vencido".
4. **Historial de actividad** de cada ticket: quién lo creó, quién lo asignó, qué cambios de estado hubo y cuándo.
5. **Conversación con notas internas**: comentarios visibles para el cliente y notas privadas solo para el equipo técnico.
6. **Respuestas predefinidas** (plantillas) para contestar rápido a problemas típicos.
7. **Datos del ticket** (según la infografía): ID, estado, categoría, prioridad, tipo (incidencia o solicitud), descripción, solicitante, agente asignado y grupo o mesa de ayuda.
8. **Automatización**: asignación automática o reparto entre técnicos.
9. **Base de conocimiento**: artículos enlazados a los tickets.
10. **Informes y paneles** con métricas.
11. Otros: adjuntos, avisos por correo, encuesta de satisfacción, portal de autoservicio, inventario de equipos.

## 3. Qué tiene ITdence ahora

Estado a 16/10/2026. Todo lo marcado como hecho tiene tests (70 en total) salvo la parte visual y el arrastrar con el ratón.

| Funcionalidad | Estado |
|---|---|
| Tres roles con permisos (cliente, técnico, admin) | Hecho |
| Crear ticket con categoría y prioridad | Hecho |
| Lista de tickets con filtros, búsqueda y orden | Hecho |
| Tomar ticket, sin que dos técnicos se lo queden a la vez | Hecho |
| Cambio de estado con permisos (técnico solo los suyos) | Hecho |
| **Tablero Kanban** con arrastrar y soltar, agrupable por estado, prioridad o técnico | Hecho (A1) |
| **Historial de actividad** de cada ticket (creación, asignación, cambios de estado) | Hecho (A2) |
| **SLA por prioridad**: límite, "vence en…", "vencido hace…", % cumplido y vencidos en el panel | Hecho (A3) |
| **Notas internas** que el cliente no ve (filtradas en el servicio) | Hecho (A4) |
| **Respuestas predefinidas** con variables ({cliente}, {tecnico}, {ticket}), gestionadas por el admin | Hecho (A5) |
| Conversación con comentarios | Hecho |
| Panel del admin: contadores, gráfico por categoría, SLA | Hecho |
| Gestión de usuarios y categorías | Hecho |
| Contraseñas con BCrypt, modo oscuro, funciona sin internet | Hecho |
| Migración automática de la BD al actualizar la app | Hecho |
| Perfil (nombre y contraseña), cancelar ticket propio, adjuntar capturas | Hecho (Paquete D) |
| Tipo de ticket (incidencia / solicitud), CSV, estado "en espera" | No (Paquete B, líneas futuras) |

## 4. Propuesta

Se ha implementado el **Paquete A completo**. Sobre cómo se hizo cada cosa:

| # | Funcionalidad | Cómo se ha resuelto |
|---|---|---|
| A1 | Tablero Kanban | Clase `TableroKanban`. Al soltar una tarjeta se llama a `TicketServicio.moverEstado`, que reutiliza `tomarTicket` y `cambiarEstado`, así que valen los mismos permisos que en el detalle. Si un técnico arrastra un ticket sin asignar, se lo queda primero. Soltar en la misma columna no hace nada. Agrupado por prioridad o técnico es solo lectura (arrastrar ahí obligaría a cambiar prioridad o técnico, que tienen sus propias reglas). |
| A2 | Historial | Tabla `actividad`. Se escribe desde `TicketServicio`, nunca desde la vista, y si una acción se rechaza no queda nada. Los datos de ejemplo también pasan por los servicios. |
| A3 | SLA | Horas por prioridad en la clase `Sla` (alta 4 h, media 24 h, baja 72 h). El límite se guarda al crear el ticket (`fecha_limite`) y la fecha de resolución al resolverlo (`fecha_resolucion`), que se borra si se reabre. Un ticket resuelto a tiempo sigue contando como cumplido aunque pase el tiempo. |
| A4 | Notas internas | Columna `interno` en `comentario`. Se filtran en `ComentarioServicio.listarPorTicket(ticket, usuario)` (no en la pantalla), el servicio rechaza que un cliente cree una y una nota interna no cambia "Actualizado" para que el cliente no note nada. |
| A5 | Respuestas predefinidas | Tabla `plantilla`. El técnico elige una en el detalle y se rellena la caja de comentario con las variables sustituidas; **nunca se envía sola**. Solo el admin las gestiona. |

### Paquete B (pendiente, para líneas futuras)

Tipo de ticket (incidencia o solicitud), exportar a CSV y estado "en espera" (este último obliga a recrear el `CHECK` de la tabla ticket, porque SQLite no permite modificarlo).

### Fuera de alcance (justificado en la memoria)

Avisos por correo (necesitan servidor SMTP), portal web, inventario de equipos (CMDB), base de conocimiento, automatización con reglas y asistente de IA.

## 5. Qué puede aportar ITdence de distinto (para la memoria, con honestidad)

Conviene no afirmar que "las apps grandes no tienen" estas funciones: ServiceDesk Plus, Zendesk o Freshdesk las tienen casi todas, y de forma mucho más completa. Lo que sí se puede defender con datos de este proyecto:

1. **Cero instalación de servidor**: GLPI y osTicket necesitan servidor web, PHP y MySQL; ServiceDesk Plus y Zendesk son de pago y necesitan servidor o nube. ITdence es una aplicación de escritorio de unos 50 MB con la BD en un fichero, y funciona sin internet.
2. **Enfocada y fácil de aprender**: las herramientas grandes tienen decenas de módulos (cambios, problemas, activos, compras…). ITdence se centra solo en el ciclo del ticket, con una interfaz con tres roles claros.
3. **Reparto sin pisarse**: el técnico "toma" el ticket y el sistema evita que dos lo cojan a la vez (comprobado con tests).
4. **Todo lo básico de un helpdesk profesional en una app pequeña**: tablero, SLA, historial y notas internas, con código y base de datos que se pueden leer en un fin de semana (útil como proyecto educativo y como base para ampliar).
5. **Gratuita y de código abierto** (repositorio público).

La comparación en la memoria puede ser una tabla "Característica × aplicación" con esas filas, marcando con sinceridad lo que ITdence no cubre.

## 6. Cambios en la base de datos (hechos)

- `ticket`: columnas `fecha_limite` y `fecha_resolucion`.
- `comentario`: columna `interno` (0 o 1).
- Tablas nuevas: `actividad` y `plantilla`.

`schema.sql` usa `CREATE TABLE IF NOT EXISTS`, así que una base de datos creada con una versión anterior no recibe las columnas nuevas y la app fallaba al leerlas. Se resolvió con `ConexionBD.migrar`, que en cada arranque comprueba con `PRAGMA table_info` si falta cada columna y la añade con `ALTER TABLE`. Además rellena los tickets antiguos: calcula su límite de SLA con las horas de su prioridad, usa su última actualización como fecha de resolución si ya estaban resueltos y les crea la línea de historial "creó el ticket". Se puede ejecutar varias veces sin duplicar nada. Está probado con un test (`MigracionTest`) y arrancando la app contra una copia de la base de datos real de la versión anterior.

## 7. Limitaciones que conviene decir en la memoria

- El **SLA cuenta horas naturales** (24 h al día, también fines de semana). Las herramientas profesionales permiten calendarios laborales y pausar el reloj; aquí no.
- El **SLA no se actualiza solo** mientras la pantalla está abierta: se recalcula al abrir o recargar la pantalla.
- Si un ticket resuelto se **reabre**, mantiene su límite original (probablemente ya vencido).
- El historial y el cambio de estado se guardan en **dos operaciones separadas** (no en una transacción); si la app se cerrara justo en medio podría faltar una línea de historial.
- El arrastrar y soltar del tablero está probado en su lógica (los permisos y los cambios de estado, con tests), pero no tiene test automático del gesto con el ratón.
- Hay una única conexión a la base de datos y la aplicación es de un solo puesto: no es multiusuario en red.

## 8. Segunda revisión: qué hacen otras herramientas que a ITdence le faltaba (Paquete C)

Búsqueda del 16/10/2026 en la documentación y comunidades de Help Scout, Freshdesk, Zendesk, Jira Service Management y Linear (herramienta de tickets de desarrollo, muy citada por su experiencia de uso). Son fuentes de los propios fabricantes, foros y blogs, así que describen lo que ofrecen, no pruebas propias.

| Qué se vio | Fuente | ¿Lo tiene ITdence? | Qué se hace |
|---|---|---|---|
| **Paleta de comandos / buscador** abierto con Ctrl+K para saltar a cualquier cosa | [Help Scout](https://docs.helpscout.com/article/419-keyboard-shortcuts), [Linear](https://linear.app/enablement/guide/navigating-linear) | No | **C3**: buscador global con Ctrl+K |
| **Atajos de teclado** con hoja de ayuda ("?") | [Freshdesk](https://support.freshdesk.com/support/solutions/articles/186758-keyboard-shortcuts-in-freshdesk), [Linear](https://linear.app/changelog/2021-03-25-keyboard-shortcuts-help) | No | C3 (Ctrl+K y Esc) |
| **El cliente puede reabrir** un ticket resuelto; el problema es hacerlo automático al comentar, porque un simple "gracias" lo reabre | [Atlassian Community](https://community.atlassian.com/forums/Jira-Service-Management/Customer-responses-to-resolved-ticket-is-not-re-opening/qaq-p/1544264) | No | **C1**: al resolverse, el cliente decide "Sí, cerrar" o "No, reabrir" (con motivo), nunca automático |
| **Encuesta de satisfacción (CSAT)**, que conviene pedir cuando el ticket ya está cerrado de verdad y no al resolverlo, porque puede reabrirse | [Atlassian (JSDCLOUD-4016)](https://jira.atlassian.com/browse/JSDCLOUD-4016) | No | **C1**: la valoración se pide al cerrar |
| **Métricas** que importan: tiempo de resolución, CSAT, antigüedad del trabajo pendiente y reaperturas, mejor por técnico/categoría que una sola cifra global | [Capterra](https://www.capterra.com/resources/help-desk-kpis/), [apps365](https://www.apps365.com/blog/helpdesk-dashboard/) | Parcial (solo SLA) | **C4**: tiempo medio de resolución, satisfacción y reaperturas en el resumen del admin |
| **Avisos** cuando algo cambia en tu ticket (normalmente por correo) | varias | No (y sin correo no hay forma de enterarse) | **C2**: campana con avisos dentro de la app |
| El admin **asigna o reasigna** | habitual | No (el admin solo puede cambiar el estado) | **C6**: asignar técnico desde el detalle |

### Estado del Paquete C (implementado el 16/10/2026)

| # | Funcionalidad | Cómo se ha resuelto |
|---|---|---|
| C1 | Cierre con el cliente | El ticket resuelto muestra al cliente "¿Se ha resuelto tu problema?". "Sí" lo cierra y pide una valoración de 1 a 5 estrellas; "No" lo reabre y exige un motivo, que queda como comentario del cliente. Nunca se reabre por escribir un comentario. Un ticket reabierto vuelve a "en progreso" (o a "abierto" si no tenía técnico) y se cuenta la reapertura. |
| C2 | Avisos dentro de la app | Tabla `notificacion`. Los servicios avisan cuando un ticket urgente se crea (a todos los técnicos), se toma o se asigna, cambia de estado, recibe un comentario público, se cierra o se reabre. Nunca se avisa a quien hace la acción y las notas internas no avisan a nadie. La campana de la barra lateral muestra los avisos sin leer, se actualiza sola cada 8 segundos y abrir un ticket da por leídos sus avisos. |
| C3 | Buscador global | Ctrl+K (o el botón de la barra lateral). Busca por número, título, descripción, categoría y nombre del creador o del técnico; el cliente solo entre los suyos. Flechas y Enter para abrir, Esc para cerrar. |
| C4 | Métricas y gráficas | Resumen del admin con donut de estados, cumplimiento del SLA, calidad del servicio (valoración media, tiempo medio de resolución, reaperturas), gráfica de actividad de los últimos 14 días, tickets por categoría y la lista de los que requieren atención. |
| C6 | Asignar y reasignar | El admin elige un técnico en el detalle del ticket; queda en el historial ("asignó…" o "reasignó… de A a B") y avisa al técnico y al cliente. |

### Lo que puede aportar ITdence frente a ellas

- **Avisos dentro de la app sin servidor de correo**: las herramientas grandes dependen del email para avisar; en una app de escritorio local, la campana resuelve el mismo problema sin configurar nada.
- **Reabrir con motivo obligatorio**: evita el fallo de "el gracias reabre el ticket", que se cita en foros de Jira, sin necesidad de reglas de automatización.
- **Valoración al cerrar**, no al resolver: es la recomendación que se desprende del propio error que reconoce Atlassian en su registro de incidencias.

### Lo que sigue fuera de alcance
Correo electrónico, informes históricos por periodo, métricas de primera respuesta (haría falta registrar la primera contestación), portal web, correo y automatización con reglas.

### Limitaciones añadidas por el Paquete C

- Los avisos se **consultan cada 8 segundos**, no llegan al instante. Dos ventanas de la aplicación sobre la misma base de datos sí se ven entre sí, pero no es multiusuario en red.
- Los avisos de un técnico por ticket urgente llegan a **todos** los técnicos, aunque otro ya lo haya tomado.
- El buscador filtra en memoria sobre todos los tickets: perfecto para cientos o pocos miles, no pensado para decenas de miles.
- La valoración es solo una puntuación de 1 a 5, sin comentario escrito.
- No se mide el **tiempo de primera respuesta**, que es una de las métricas más citadas, porque habría que guardar cuándo contesta el técnico por primera vez.

## 9. Más funcionalidades para el cliente (Paquete D, 16/10/2026)

El cliente solo podía crear, mirar y comentar. Se le añadió lo que tienen sus equivalentes en otras herramientas:

| # | Funcionalidad | Cómo se ha resuelto |
|---|---|---|
| D1 | Mi perfil | Ventana desde la tarjeta de usuario de la barra lateral: cambiar nombre y contraseña (pide la actual y repetir la nueva). Disponible para todos los roles. |
| D2 | Cancelar ticket | El cliente creador puede cancelar un ticket abierto o en progreso, con confirmación. Columna `cancelado`; el ticket deja de contar para el SLA y queda en el historial. |
| D3 | Adjuntar capturas | Imágenes (png, jpg, gif, bmp) de hasta 5 MB, 6 por ticket, al crear el ticket o desde el detalle. Miniaturas y visor ampliado. Los ficheros viven en `data/adjuntos/` y la BD guarda solo la ruta. |
| D4 | Aviso de tickets por confirmar | Franja destacada en la pantalla del cliente con el número de tickets resueltos que esperan su respuesta y un botón que filtra la lista. Se oculta cuando no hay ninguno. |

### Limitaciones del Paquete D

- Solo se admiten **imágenes**; no PDF ni logs de texto, aunque a un técnico le serviría.
- Los adjuntos **no se pueden borrar** una vez subidos.
- Los ficheros están en una carpeta junto a la base de datos: para hacer una copia de seguridad hay que copiar ambas (`data/itdence.db` y `data/adjuntos/`).
- No se comprueba el contenido real del fichero, solo su extensión y su tamaño (se lee como imagen al mostrarlo).
