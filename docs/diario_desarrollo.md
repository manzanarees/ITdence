# Diario de desarrollo – ITdence

Registro de lo que se ha ido haciendo, los problemas que han salido y cómo se han resuelto.
Sirve de base para el apartado "Diario de desarrollo" y para "Problemas encontrados" de la memoria.

---

## 08/09/2026 – Elección del proyecto y planificación

- Elijo la Línea 2 (desarrollo de aplicación). El tema viene de mis prácticas en Sixt, donde vi cómo se gestionaban las incidencias con una herramienta de ticketing, y de que es el perfil de soporte IT que me interesa.
- La primera idea de nombre fue *IncidenciasIT*.
- Decido hacerla de escritorio (JavaFX) y no web ni móvil: es más realista para un helpdesk interno y no necesita servidor.
- Defino los tres roles (cliente, técnico y admin) y las funcionalidades mínimas de cada uno.

## 09–10/09/2026 – Diagramas y mockups

- Diagrama de casos de uso en draw.io.
- Diagrama E‑R en dbdiagram.io, con 4 tablas: usuario, categoria, ticket y comentario.
- Wireframes de las 5 pantallas: login, cliente, técnico, admin y detalle de ticket. De ahí sale también la paleta de colores (acento teal #2f6a72 y un color para cada estado y prioridad).

## 10/09/2026 – Proyecto Maven + JavaFX

- Creo el proyecto Maven en IntelliJ con JavaFX 21 y SQLite.
- **Problema:** al ejecutar salía el error *"main method must return void"* aunque el `main` estaba bien. **Causa:** tenía puesto el JDK 25, que era muy nuevo y daba problemas con el lanzador de JavaFX. **Solución:** cambiar el SDK del proyecto a Java 21 LTS (Temurin).
- **Problema:** el botón Run de IntelliJ no cogía bien los módulos de JavaFX. **Solución:** ejecutar siempre con `mvn javafx:run`.
- **Problema:** `schema.sql` daba errores de sintaxis en SQLite. **Causa:** el teclado en español metía `´` (tilde) en vez de la comilla simple `'` en los CHECK. **Solución:** revisar el fichero y cambiar esas comillas.
- Cambio el nombre del proyecto a **ITdence** ("IT" + "incidence"). Renombro el paquete `com.incidenciasit` a `com.itdence` con el refactor de IntelliJ.
- Borro la clase `Main` que deja la plantilla de IntelliJ en `org.example`.

## 13/09/2026 – Clases del modelo

- Hago `Usuario`, `Categoria`, `Ticket` y `Comentario`.
- **Problema:** *"invalid method declaration; return type required"*. **Causa:** la clase se llamaba `usuario` (minúscula) y el constructor `Usuario`. Java no los reconoce como iguales. **Solución:** clase, fichero y constructor con el mismo nombre exacto.
- Otros errores típicos de este día: un setter sin parámetro, una llave de cierre que faltaba (*"reached end of file while parsing"*) y un método repetido (`setEstado` dos veces).
- **Problema:** después de corregir, seguía saliendo el error antiguo. **Causa:** clases compiladas viejas en `target/`. **Solución:** `mvn clean` o *Rebuild Project*.

## 16/09/2026 – Login y contraseñas con BCrypt

- `ConexionBD` como singleton. Crea la base de datos a partir de `schema.sql` la primera vez.
- `UsuarioDAO`, `PasswordUtil` (BCrypt con salt) y `AuthServicio.login`.
- **Problema:** el primer login funcionaba, pero el siguiente acceso a la BD fallaba porque la conexión estaba cerrada. **Causa:** metía `Connection con = ConexionBD.getConexion()` dentro del `try-with-resources`, así que al acabar el `try` se cerraba la conexión compartida del singleton. **Solución:** declarar la conexión fuera del `try` y meter dentro solo el `PreparedStatement` y el `ResultSet`. Desde entonces todos los DAO siguen ese patrón.
- El usuario admin de prueba se crea al arrancar si no existe.

## 19–21/09/2026 – Formulario de crear ticket (cliente)

- `CategoriaDAO` y `TicketDAO.crear`. Categorías iniciales: Hardware, Software, Red y Cuenta.
- `Categoria.toString()` devuelve el nombre para que el `ComboBox` no enseñe `com.itdence.modelo.Categoria@1a2b3c`.
- Saco el login a un método `crearEscenaLogin` y el panel del cliente a `crearEscenaCliente`.
- **Problemas al reorganizar `App.java`:** llamaba a `crearEscenaPorRol(stage)` en vez de `crearEscenaLogin(stage)`, tenía un if/else con la lógica en la rama equivocada, faltaba el `grid.add(emailField, …)` y el campo de email no se veía, y `TicketDAO` estaba creado en el paquete equivocado (`com.itdence` en vez de `com.itdence.dao`). Lo arreglé moviéndolo con el refactor *Move*.

## 29/09/2026 – Resto de pantallas, estilos y pruebas

En esta sesión trabajé con un asistente de IA (Claude) como apoyo para revisar el código y terminar las pantallas que faltaban. Los fallos que salieron:

- **Bug importante: los tickets no se guardaban.** En `TicketDAO.crear` rellenaba los parámetros del `PreparedStatement` pero **nunca llamaba a `executeUpdate()`**. La pantalla ponía "Ticket creado correctamente" y no se guardaba nada. No se había notado porque todavía no existía la pantalla "Mis tickets". **Solución:** añadir `stmt.executeUpdate()`. Además, ahora `crear` devuelve el id generado (`RETURN_GENERATED_KEYS`) para poder enseñar "Ticket #0006 creado".
- **Claves foráneas que no se comprobaban.** SQLite trae las foreign keys **desactivadas por defecto**, así que se podía meter un ticket con una categoría que no existía. **Solución:** ejecutar `PRAGMA foreign_keys = ON` justo al abrir la conexión.
- **Horas desfasadas.** `datetime('now')` en SQLite devuelve la hora UTC, así que salían 2 horas menos. **Solución:** usar `datetime('now', 'localtime')` en los DEFAULT y en los UPDATE.
- **La carpeta `data/` no existía al clonar el repo.** SQLite no crea carpetas, solo el fichero, así que la app petaba al arrancar en un equipo nuevo. **Solución:** `mkdirs()` antes de conectar. Además se ignora `data/*.db` en Git.
- Renombro la BD de `incidenciasit.db` a `itdence.db` para que cuadre con el nombre nuevo (la vieja solo tenía datos de prueba).
- **Nombres en vez de ids.** En las tablas salía "categoría 1", "creador 2"... **Solución:** una consulta base en `TicketDAO` con `JOIN` a categoría y usuario, y `LEFT JOIN` para el técnico (porque puede ser `null` si nadie lo ha cogido).
- **Dos técnicos cogiendo el mismo ticket.** Si dos técnicos pulsaban "Tomar ticket" a la vez, el segundo se lo quitaba al primero. **Solución:** el `UPDATE` lleva `WHERE id = ? AND tecnico_asignado_id IS NULL` y se mira cuántas filas ha cambiado. Si es 0, se avisa de que otro técnico lo ha cogido antes.
- **Resumen del admin sin todos los estados.** `COUNT(*) ... GROUP BY estado` no devuelve los estados que tienen 0 tickets, y la tarjeta de "Cerrado" se quedaba vacía. **Solución:** rellenar antes un `LinkedHashMap` con los 4 estados a 0.
- **`App.java` era demasiado grande** (login, cliente, estilos y datos de prueba en la misma clase). Lo reparto en el paquete `controlador` (una clase por pantalla más `Navegacion`, `ControladorBase` y `Componentes`) y paso los datos de prueba a `servicio/DatosIniciales`. Así se cumple la arquitectura de 4 capas que planteé al principio.
- **Validaciones repartidas por las vistas.** Las muevo a la capa de servicio con una excepción propia (`ValidacionException`). Las vistas solo enseñan el mensaje en rojo.
- **CSS: el logo "IT" salía negro y "Cerrar sesión" parecía un botón.** Las reglas generales `.label` y `.button` estaban más abajo en el CSS y pisaban a `.logo-texto` y `.link-cerrar-sesion`, porque tienen la misma especificidad. **Solución:** selectores más específicos (`.logo .logo-texto`, `.button.link-cerrar-sesion`).
- **Las barras de las tarjetas del resumen no se veían** con `ProgressBar` y CSS personalizado. **Solución:** hacer la barra a mano con un `StackPane` (fondo gris) y un `Region` dentro, cuyo ancho va enlazado (`bind`) al porcentaje.
- **Fondo gris dentro del `TextArea`.** El `TextArea` lleva dentro un `ScrollPane` con su propio fondo. **Solución:** poner transparentes `.text-area .scroll-pane` y `.viewport`.
- **Al cambiar de escena la ventana se quedaba del tamaño del login.** **Solución:** `stage.sizeToScene()` y `centerOnScreen()` después de `setScene`.
- **Pruebas unitarias.** Añado JUnit 5 con 38 tests de la capa de servicio. Para no tocar la BD real, la ruta de la base de datos se puede cambiar con la propiedad `-Ditdence.bd`, y en los tests apunta a `target/test-data`.
- **Problema con los tests:** *"illegal character: '﻿'"*. **Causa:** un fichero guardado desde PowerShell 5 con `Set-Content -Encoding utf8`, que añade el BOM al principio. **Solución:** guardar en UTF-8 sin BOM.
- Creo el usuario técnico de prueba (`tecnico@itdence.com`) y unos tickets de ejemplo para que al abrir la app no salga todo vacío.

### Rediseño visual (tarde del 29/09)

Con la app ya funcionando rehíce la interfaz para que fuera más agradable de usar:

- **Logo y colores de marca.** Añadí el logo definitivo (anillo con un ticket y una "i", en degradado morado → azul). El acento teal de los mockups (#2f6a72) no pegaba con el logo, así que el acento pasa a ser el degradado de la marca (#A855F7 → #885CF6 → #3B82F6). Los colores de estado y prioridad se mantienen, porque tienen significado.
  - **Problema:** en el PNG `app-icon-square` del paquete de marca el ticket sale descentrado (en el SVG el rectángulo va de x=198 a x=393 y su centro no coincide con el del círculo, 256). Uso la versión `icon-transparent`, que sí está centrada.
- **Tipografías de los mockups** (Archivo, Public Sans e IBM Plex Mono) metidas en `resources/fonts` y cargadas con `Font.loadFont` al arrancar.
  - **Problema:** JavaFX registra los pesos intermedios como familias distintas ("Archivo ExtraBold", "Public Sans SemiBold") y no como pesos de la misma familia, así que `-fx-font-weight: 800` no funciona. **Solución:** usar el nombre de familia completo en el CSS.
- **Barra lateral** con menú, contadores, tarjeta del usuario y cerrar sesión, en lugar de la barra superior.
- **Modo oscuro.** Todos los colores del CSS pasan a ser *looked-up colors* definidos en `.root`, y `.root.oscuro` los redefine. Al pulsar el botón se añade o quita la clase `oscuro` en todas las ventanas abiertas.
- **Iconos** con `SVGPath` (trazados de Material Icons), sin imágenes ni librerías externas. Se colorean desde el CSS con `-fx-fill`.
- **Animaciones** (clase `Animaciones`): entrada de las pantallas con fundido y desplazamiento, números que cuentan hacia arriba, barras que crecen, sacudida del formulario si el login falla, tarjetas que se elevan al pasar el ratón y ventanas modales que aparecen con zoom.
- **Notificaciones tipo toast** en vez de los labels verdes y rojos. Aparecen abajo a la derecha y se van solas.
- **Diálogo de confirmación propio.** El `Alert` de JavaFX no seguía el estilo ni el modo oscuro.
- **Detalle del ticket:** línea de progreso de los estados (abierto → en progreso → resuelto → cerrado) y comentarios tipo chat (los tuyos a la derecha). Se puede enviar con Ctrl+Enter.
- **Gráfico de tickets por categoría** en el resumen del admin, con una consulta nueva (`LEFT JOIN` + `GROUP BY` para que salgan también las categorías con 0 tickets).
- **Problemas de maquetación** que salieron al revisar las capturas: los textos de los filtros y de los botones del formulario de usuarios se cortaban con "...", y las tarjetas de estadísticas tenían anchos distintos según la longitud del título. Se arregló acortando los textos de los filtros, poniendo los botones en dos filas y dando a todas las tarjetas el mismo ancho base (`prefWidth`) para que el `HBox` reparta el espacio igual.
- **Problema:** `mvn clean` fallaba con *"Failed to delete target\classes\img\icon-64.png"* porque la app seguía abierta y tenía el fichero en uso. **Solución:** cerrar la app antes de hacer `clean`.
- **Problema:** con el CSS de JavaFX no se puede poner un desenfoque (solo existen `dropshadow` e `innershadow`), así que el efecto `GaussianBlur` de las burbujas del login se pone desde el código.

### Segunda vuelta al login y detalles finales

- El panel izquierdo del login con manchas de color difuminadas no quedaba bien (se veía sucio). Lo cambio por un fondo oscuro con dos brillos suaves (`radial-gradient`) y, encima, una **vista previa de la app**: tres tarjetas de tickets un poco giradas que flotan despacio y un aviso de "Ticket resuelto". Probé también una cuadrícula de puntitos de fondo, pero la quité porque recargaba demasiado.
  - **Problema:** las tarjetas eran casi transparentes y al solaparse se veían los bordes de las de detrás. **Solución:** hacerlas casi opacas.
  - **Problema:** el titular salía cortado con "..." porque no cabía todo de alto. **Solución:** ventana de login un poco más alta y `setMinHeight(USE_PREF_SIZE)` en los textos para que no se encojan.
- Tercera versión del panel del login, **estilo terminal** (encaja con una app de soporte IT). Es una ventana de terminal con los tres circulitos de colores donde se van tecleando, letra a letra, comandos de ejemplo que cuentan la vida de un ticket: se crea, lo toma un técnico y se resuelve. Al terminar vuelve a empezar. Al "pulsar Enter" el comando se colorea (opciones en cian, ids en amarillo, textos entre comillas en verde) y las respuestas `[OK]` salen en verde. Los textos del panel van en IBM Plex Mono. Está en la clase `TerminalAnimada`, que usa un `Timeline` con un `KeyFrame` por letra y un `TextFlow` para mezclar colores en la misma línea. El cursor es un `Region` que parpadea con un `FadeTransition`.
  - Para no dejar la animación funcionando cuando ya se ha iniciado sesión, antes de cada línea se comprueba si la ventana de la terminal sigue visible. **Problema:** justo al crear la pantalla todavía no está puesta en la ventana y la comprobación daba `false`, así que no llegaba a arrancar. **Solución:** empezar con un pequeño retraso.
- **Pantalla de bienvenida** al arrancar: el logo aparece con zoom, luego el nombre y el lema, y pasa al login con un fundido.
  - **Problema:** la app petaba al arrancar con *"Control point coordinates must all be in range [0,1]"*. Había usado `Interpolator.SPLINE(0.2, 0.9, 0.3, 1.2)` para que el logo "rebotara", pero JavaFX solo admite valores entre 0 y 1. **Solución:** usar `EASE_OUT`.
- Fecha de hoy encima del título de cada pantalla ("MARTES, 29 DE SEPTIEMBRE").
- Ventana principal más ancha (1300 px) para que no se corten las columnas. Si la pantalla es más pequeña (por ejemplo un portátil de 1366×768), la ventana se maximiza sola.
- Raya de color a la izquierda de la fila de la tabla que tiene el ratón encima. Se hace con dos fondos e *insets*, así el contenido no se mueve.
- Los botones principales brillan un poco más al pasar el ratón.

### Decisiones de diseño tomadas este día
- Al **tomar** un ticket abierto, pasa automáticamente a *en progreso* (así el cliente ve que alguien lo está mirando).
- El técnico solo puede cambiar el estado de **sus** tickets; el admin, de cualquiera. El cliente nunca.
- No se puede comentar en un ticket **cerrado**.
- No se puede borrar un usuario con tickets o comentarios, ni una categoría con tickets, para no perder el historial. El admin tampoco puede borrarse a sí mismo ni quitarse el rol.
- El detalle del ticket se abre con **doble clic** y no con un clic, porque en la bandeja del técnico la fila tiene el botón "Tomar ticket" y así no se abre el detalle sin querer al pulsarlo. Además, es lo habitual en las aplicaciones de escritorio.

## 16/10/2026 – Funcionalidades de un helpdesk de verdad

Después de revisar cómo son las herramientas de este tipo (ver `docs/analisis_funcional.md`: ServiceDesk Plus, Zendesk, Freshdesk, Odoo, GLPI y osTicket), vi que a ITdence le faltaba lo que casi todas tienen. Añado cinco funcionalidades, una por una (compilar, pasar tests y hacer un commit antes de pasar a la siguiente). Esto fue lo que salió:

**Antes de tocar nada: la migración de la base de datos.** `schema.sql` usa `CREATE TABLE IF NOT EXISTS`, y eso no añade columnas a una tabla que ya existe. Con la BD de la versión anterior la app habría fallado nada más arrancar al leer las columnas nuevas. Hice `ConexionBD.migrar`, que comprueba con `PRAGMA table_info` si falta cada columna y la añade con `ALTER TABLE`, y rellena los tickets antiguos. En SQLite una columna `NOT NULL` nueva necesita un `DEFAULT`, por eso `interno` lleva `DEFAULT 0` y las fechas del SLA son opcionales. Los tests siempre crean una BD nueva, así que nunca pasarían por ese camino: añadí `MigracionTest` (crea una BD con el esquema viejo y comprueba que no se pierden datos y que se puede migrar dos veces) y además arranqué la app contra una copia de mi BD real de antes.

1. **Notas internas.** Comentarios que solo ven técnicos y admin. El filtro está en `ComentarioServicio` y no en la pantalla (si solo se ocultaran en la vista, otra pantalla futura podría enseñarlas por error). El servicio también rechaza que un cliente cree una, y una nota interna no cambia la fecha de "Actualizado", porque si no el cliente notaría que ha pasado algo.
2. **Historial de actividad.** Tabla `actividad` que se escribe desde `TicketServicio`. **Problema:** los datos de ejemplo llamaban directamente a los DAO y se saltaban el servicio, así que sus tickets salían sin historial. Los paso por los servicios. **Problema:** un admin que solo había cambiado estados no tenía tickets ni comentarios, así que se podía "borrar", pero el historial apuntaba a él y SQLite fallaba con un error feo. Ahora `UsuarioServicio.eliminar` también cuenta la actividad y da un mensaje claro. Los tickets antiguos reciben su línea de "creó el ticket" en la migración.
3. **SLA por prioridad** (alta 4 h, media 24 h, baja 72 h). Clase `Sla` con solo cálculos, para probarla con horas fijas. **Decisiones:** guardar `fecha_resolucion` y no usar `fecha_actualizacion` (cambia con cada comentario); si se reabre un ticket resuelto, esa fecha se borra y el límite original se mantiene; un ticket resuelto a tiempo sigue contando como cumplido aunque pase el tiempo. Los datos de ejemplo incluyen tickets "antiguos" (con `ponerAntiguedad`) para que se vea alguno vencido sin tener que esperar horas.
   - **Problema:** los textos de las etiquetas ("Vence en 21 h 59 min") se cortaban en las tablas. **Solución:** de 10 horas en adelante se quitan los minutos, y se reparten a mano los anchos de las columnas.
   - **Problema:** los títulos de las tarjetas del resumen se cortaban ("SLA ven..."). **Solución:** títulos más cortos ("Vencidos", "Cumplido"), el significado completo en un tooltip y menos relleno.
4. **Tablero Kanban** con arrastrar y soltar. Al soltar una tarjeta se llama a `TicketServicio.moverEstado`, que reutiliza `tomarTicket` y `cambiarEstado`, así que no hay permisos duplicados. **Decisiones:** soltar en la misma columna no hace nada (no es un error); si un técnico arrastra un ticket sin asignar, primero se lo queda; agrupado por prioridad o técnico es de solo lectura, porque arrastrar ahí obligaría a cambiar prioridad o técnico, que tienen sus propias reglas.
   - **Problema:** las etiquetas de las tarjetas se salían por la derecha cuando no cabían. **Solución:** `FlowPane` en vez de `HBox`, para que salten de línea.
   - Para no reconstruir las columnas en mitad del evento de soltar, el refresco va con `Platform.runLater`.
5. **Respuestas predefinidas** con variables (`{cliente}`, `{tecnico}`, `{ticket}`). El técnico elige una y se rellena la caja de comentario; **nunca se envía sola**. Solo el admin las gestiona (comprobado en el servicio).
   - **Problema:** `ComboBox` no tiene `setGraphic` (error de compilación); lo quité.

**Otros problemas de esta sesión:**
- En el detalle, la descripción salía recortada por arriba: la caja de comentario coge el foco al abrir y el scroll bajaba hasta ella. **Solución:** colocar el scroll arriba al abrir la ventana.
- Con el historial y el SLA el detalle ya no cabía en la ventana; ahora es más alta (sin pasarse de la pantalla) y la columna derecha tiene scroll.
- Tests: pasamos de 38 a 70.

**Limitaciones que dejo apuntadas para la memoria:** el SLA cuenta horas naturales (sin calendario laboral), no se actualiza solo mientras la pantalla está abierta, y el arrastrar con el ratón no tiene test automático del gesto (sí de la lógica que hay debajo).

### Segunda revisión de diseño (16/10/2026, con la guía `frontend-design`)

Repasé las pantallas con una guía de diseño que señala los "tics de plantilla" más comunes. Lo que encontré en ITdence y lo que cambié:

- **Etiquetas en mayúsculas y en fuente monoespaciada** encima de todo (campos, cabeceras de tabla, la fecha). Eran difíciles de leer y no aportaban información. Ahora van en minúsculas con la fuente de texto, y la monoespaciada se queda solo para los ids (`#0001`) y los números.
- **Texto con puntos medios** ("#0001 · Hardware"): ahora el id va en mono y la categoría al lado, en texto normal, sin separador.
- **Seis tarjetas iguales en el resumen del admin**: sustituidas por un panel con una **barra repartida por estados** (se ve de un vistazo cuánto trabajo hay en cada estado) y un panel aparte con el **cumplimiento del SLA** (porcentaje, vencidos y a punto de vencer, en color solo cuando hay algo preocupante).
- **Efecto de elevación al pasar el ratón en tarjetas que no se pulsan**: ahora solo lo llevan las que sí se pulsan (los filtros del cliente y las tarjetas del tablero).
- Se mantiene la paleta y el degradado de la marca, porque vienen del logo.
- Nota: la guía `taste-skill` que también quise usar no estaba instalada en mi sesión, así que solo se aplicó esta.

### Tercera pasada de diseño: mismo estilo en toda la app (16/10/2026)

Tras ver el tablero Kanban con columnas de borde completo y contadores sin relleno, decidí aplicar ese mismo estilo a todas las pantallas para que la app sea coherente:

- **Esquinas menos redondeadas**: los radios de 10, 12 y 14 px pasan a 8 px en tarjetas, botones, campos, paneles y burbujas de comentario (los círculos de avatares e iconos no se tocan).
- **Etiquetas sin relleno** (estado, prioridad, SLA, rol y los contadores del menú lateral): solo el punto de color y el texto. Se leen igual y recargan menos.
- **Las "tarjetas de estadísticas" iguales** (cuatro en la vista del cliente, cuatro en la del técnico) se sustituyen por un único **panel de métricas** con los números separados por rayitas (`PanelMetricas`). En la vista del cliente, pulsar un estado filtra la lista y el seleccionado queda subrayado con su color. La clase `TarjetaEstadistica` y su CSS ya no se usaban y se eliminaron.
- Al final el resumen del admin, el del técnico y el del cliente comparten el mismo lenguaje: paneles planos, números grandes y color solo donde significa algo (estado, prioridad, SLA).

## 16/10/2026 – Segunda ronda de mejoras (investigando otras herramientas)

Con la ayuda del asistente de IA busqué cómo resuelven otras herramientas (Help Scout, Freshdesk, Zendesk, Jira Service Management, Linear) cosas que ITdence no tenía, y lo comparé con lo que ya hay (todo en `docs/analisis_funcional.md`, apartado 8). Implementé cinco mejoras, una por una con sus tests y su commit:

1. **El admin asigna y reasigna técnicos** (antes solo podía cambiar estados). Queda en el historial.
2. **Cierre con el cliente.** Lo más curioso de la investigación: en Jira, hacer que el ticket se reabra solo cuando el cliente comenta provoca que un simple "gracias" lo reabra, y que la encuesta de satisfacción puede mandarse al resolver aunque luego se reabra. Lo hice distinto: al resolverse, el cliente decide "cerrar" (con valoración) o "reabrir" (con motivo obligatorio). Nada es automático.
   - **Problema:** al reabrir se escribe un comentario del cliente con el motivo, y eso mandaba al técnico dos avisos ("ha comentado" y "ha reabierto"). Solución: el comentario de la reapertura se guarda sin aviso propio.
3. **Avisos dentro de la app** (campana). Sin servidor de correo, el cliente no tenía forma de enterarse de que su ticket había cambiado.
   - **Problema:** al borrar un técnico que no había hecho nada pero tenía avisos de tickets urgentes, SQLite fallaba por la clave foránea. Solución: los avisos se borran con el usuario (son pasajeros) y hay un test.
   - **Problema:** en la lista de avisos, el "hace 5 min" se cortaba con "..." cuando el texto del aviso era largo. Solución: darle al texto de la hora su ancho mínimo.
4. **Buscador global** con Ctrl+K y un botón en la barra lateral.
   - **Problema:** una expresión regular escrita con una barra invertida mal escapada impedía compilar; la cambié por `[0-9]+`.
5. **Métricas de calidad** en el resumen del admin (valoración media, tiempo medio de resolución y reaperturas).

Los datos de ejemplo incluyen ahora un ticket cerrado con valoración de 5 estrellas, uno reabierto y uno resuelto a la espera de que el cliente decida, para que se vea todo sin tener que crearlo a mano. Tests: de 70 a 97.

### Gráficas del resumen rehechas y cabecera centrada (16/10/2026)

- **Cabecera de la barra lateral descentrada.** El texto "ITdence" quedaba unos 6 px más arriba que la campana y el logo. Lo medí en lugar de adivinar (imprimiendo la posición de cada elemento): el contenedor del texto estaba alineado por la **línea base** y rellenaba toda la altura, así que el texto se pegaba arriba. Solución: alinearlo por el centro. Es la misma función que usa el login, así que también se corrige allí.
- **Las gráficas que había eran barras dibujadas a mano** (una barra repartida por estados y barras con regiones para las categorías): bonitas pero sin ejes ni valores, y no se podía leer nada exacto. Las sustituí por gráficas de verdad:
  - **Actividad de los últimos 14 días**: `LineChart` de JavaFX con eje de fechas, eje de números enteros, cuadrícula y un punto por día (creados y resueltos), con tooltip al pasar el ratón.
  - **Tickets por categoría**: `BarChart` horizontal con el número al final de cada barra.
  - **Estado de los tickets**: donut con el total en el centro y, al lado, cada estado con su número y porcentaje. JavaFX no trae un gráfico de donut (el `PieChart` no tiene agujero), así que son cuatro arcos con trazo grueso sobre un anillo; los colores salen del CSS, así que funciona en modo oscuro.
  - **Problema:** una línea oscura junto a las barras no se iba con ningún estilo de eje. Era la "línea del cero" del gráfico (\`chart-vertical-zero-line\`), no el eje.
  - **Problema:** la categoría con más tickets salía abajo; el eje vertical pinta la primera categoría al fondo, así que se añaden en orden inverso.
- Para que las gráficas tuvieran historia, los datos de ejemplo incluyen 14 tickets cerrados de los últimos 13 días (con valoraciones y algún SLA incumplido, por eso el cumplimiento sale al 88 % y no al 100 %). Sus avisos se crean ya como leídos para no inundar la campana.
- La tabla del resumen deja de ser "todos los tickets" (para eso está el tablero y el buscador) y pasa a ser **"Requieren atención"**: los de plazo vencido, a punto de vencer o sin técnico. Como ya no cabe todo en una pantalla, el resumen tiene scroll.
- Nota: para ver los datos de ejemplo nuevos hay que borrar \`data/itdence.db\` con la app cerrada, porque los datos de ejemplo solo se cargan cuando la base de datos está vacía.

### Más funcionalidades para el cliente (16/10/2026)

Al ver la app con ojos de cliente me di cuenta de que solo podía crear tickets, mirarlos y comentar. Es el rol que más usa la gente de la empresa, así que le añadí tres cosas:

1. **Mi perfil.** Pulsando la tarjeta del usuario en la barra lateral (ya valía para todos los roles) se abre una ventana para cambiar el nombre y la contraseña. La contraseña pide la actual, la nueva (mínimo 6 caracteres) y repetirla; las reglas están en `UsuarioServicio`, no en la pantalla, y tienen sus tests.
2. **Cancelar un ticket propio.** Si el cliente se equivoca o el problema se arregla solo, puede cancelarlo mientras nadie lo haya resuelto. Se guarda con una columna `cancelado` (no con un estado nuevo, porque cambiar el `CHECK` de `estado` obliga a recrear la tabla en SQLite). Solo el creador puede cancelar, y solo mientras esté abierto o en progreso. Un ticket cancelado deja de contar para el SLA ("Cancelado" en gris) y pide confirmación.
3. **Adjuntar capturas de pantalla.** Es lo que más ayuda al técnico y lo que traen todas las herramientas profesionales.
   - Se pueden añadir al crear el ticket (botón "Adjuntar capturas", con una etiqueta por imagen y una x para quitarla) o después desde el detalle ("Añadir captura").
   - Reglas en `AdjuntoServicio`: solo imágenes (png, jpg, jpeg, gif, bmp), máximo 5 MB y 6 por ticket; solo pueden adjuntar el creador del ticket y los técnicos/admin, y no en tickets cerrados.
   - Los ficheros se copian a `data/adjuntos/` con un nombre único y en la base de datos solo se guarda la ruta y el nombre original (tabla `adjunto`). La carpeta está en `.gitignore`.
   - En el detalle salen miniaturas; al pulsar una se abre un visor en grande. Cada adjunto queda en el historial ("adjuntó la imagen...").
   - Los datos de ejemplo incluyen una captura generada por código en el ticket de la VPN (dibujada con `BufferedImage`, para no meter un fichero binario en el repositorio).
   - **Problema:** la ruta de los adjuntos en los tests escribía en la carpeta real. Solución: propiedad `itdence.adjuntos`, que el pom apunta a `target/test-data` (igual que ya se hacía con la base de datos).
   - **Problema:** si una captura falla al crear el ticket (por ejemplo, pesa 8 MB), no se puede perder el ticket entero. El ticket se crea igualmente y la pantalla muestra un aviso por cada imagen rechazada con el motivo.
   - **Problema:** al borrar un usuario, los adjuntos que había subido bloqueaban el borrado por la clave foránea; ahora el borrado comprueba también los adjuntos y lo impide con un mensaje claro, igual que con tickets y comentarios.
- **Puntos de la contraseña demasiado grandes.** El punto (•) que dibuja JavaFX en los campos de contraseña salía enorme con la tipografía de la app. Solución en el CSS: una tipografía y un tamaño propios para `.password-field` (Segoe UI, 11 px), de modo que los puntos quedan discretos. Como el campo cambia de tipografía, hubo que fijar la misma altura mínima que el de texto para que los dos campos del login no quedaran desiguales.
- Tests: de 97 a 115.
- Nota: para ver el adjunto de ejemplo hay que borrar `data/itdence.db` con la app cerrada.

### Cuarta pasada de diseño: centrados que no se veían a simple vista (16/10/2026)

Revisando las capturas pantalla por pantalla encontré dos descentrados que no había notado:

- **Texto de dos líneas en las tablas** (título + #0001 en los tickets, nombre + email en los usuarios, título + inicio en las respuestas rápidas): se quedaba pegado al borde de arriba de la fila mientras el resto de celdas (prioridad, estado, avatar) estaban centradas. Lo medí con el programa de medidas: la caja vertical (`VBox`) ocupaba toda la celda (56 px) pero alineaba su contenido arriba, que es el valor por defecto de `VBox`. Solución: `Componentes.centrado(VBox)`, que la alinea al centro, usado en las tres tablas.
- **Filas de "Detalles" del ticket con espaciado desigual**: las filas con avatar (creado por, asignado a) eran más altas que las de texto, así que los huecos eran de 69, 62 y 55 px. Ahora todas las filas tienen la misma altura mínima y alineación vertical centrada con `RowConstraints`. Las filas opcionales (valoración, reabierto, cancelado) se numeran con `getRowCount()` para que no queden huecos vacíos; mi primer intento reservaba siempre siete filas y dejaba un hueco sobrante abajo.
- La ventana "Mi perfil" tenía unos 100 px vacíos al final; se redujo su altura.

### Ventana pequeña: tamaño mínimo y resumen del admin (16/10/2026)

Hasta ahora solo había mirado la app con la ventana grande. Fotografié las pantallas principales con el tamaño mínimo que permitía (960 x 600) y salieron varios problemas:

- Resumen del admin: la fila de tarjetas era más ancha que la pantalla, así que la tarjeta de calidad y la de categorías quedaban cortadas por la derecha y la leyenda del donut salía como "...".
- Bandeja del técnico: textos truncados ("Client...", "Toma...", "Sin ...").
- Tablero: el desplegable "Agrupar por" salía vacío.

Con la barra lateral de 264 px, 960 px no deja sitio al contenido. Solución en dos partes:

1. **Tamaño mínimo de ventana 1180 x 700** (antes 960 x 600). Sigue siendo menos que una pantalla de 1366 x 768, que es lo más pequeño que se usa hoy en día.
2. **Resumen más compacto**: donut de 148 px (antes 160), tarjetas de SLA y calidad de 230 px que pueden encogerse hasta 190, y las etiquetas de la leyenda no se truncan nunca. También acorté dos títulos que se cortaban: "Cumplimiento SLA" y "Últimos 14 días".

Lo que aprendí: el `ScrollPane` con `setFitToWidth(true)` no ajusta el contenido si su ancho **mínimo** es mayor que el visible; en ese caso lo recorta sin avisar. Por eso la fila de gráficas de abajo también se salía, aunque su problema era la de arriba.

- **Fechas del eje de la gráfica de actividad.** Con las 14 fechas completas (3/10, 4/10...) el eje las giraba 90 grados para que cupieran y no se leían bien. Ahora el eje muestra solo el número del día, con el mes en el primero y cuando empieza un mes nuevo ("3 oct, 4, 5... 1 nov, 2..."); la fecha completa sigue saliendo en el tooltip al pasar el ratón. Además se fija `setTickLabelRotation(0)`.
- **Columnas de las tablas.** A 1180 px se cortaban el nombre del técnico ("Técnico de pr..."), el nombre del cliente y el texto del SLA ("Vence en 3 h 58 mi"). Se ensancharon las columnas de persona (160 → 205) y de SLA (150 → 190) a costa de la de título (330 → 290) y de las de prioridad y estado (125 → 112). Un título muy largo sigue abreviándose con "...", que es lo esperado.
- **Entrada escalonada del resumen.** Al elegir "Resumen", las cinco tarjetas (estado, SLA, calidad, actividad y categorías) aparecen una detrás de otra con 70 ms de diferencia en vez de todas a la vez. Reutiliza `Animaciones.escalonada`. Compilando se me olvidó el `import java.util.List`; ese fichero no usaba nada de `java.util` y el script que añadía imports solo buscaba una línea `java.util` ya existente.

### Foco visible al navegar con teclado (16/10/2026)

Al tabular por la app no se veía qué botón tenía el foco: el CSS propio de los botones había quitado el aro que trae JavaFX. Lo difícil: en JavaFX la pseudo-clase `:focused` también se activa al **hacer clic**, así que un `.button:focused` normal dejaba un aro morado tras cada clic, que queda raro. JavaFX no trae `:focus-visible` como los navegadores. Solución: en `Tema.aplicar` se escucha la escena; al pulsar **Tab** se añade la clase `teclado` a la raíz y al pulsar el **ratón** se quita. El CSS (`.root.teclado .button:focused`...) dibuja el aro solo con esa clase. Lo comprobé simulando un Tab y un clic con eventos de JavaFX y mirando las dos capturas: con Tab sale el aro morado alrededor de "Tomar ticket"; tras el clic desaparece.

### Aviso de "tickets esperando tu confirmación" en la vista del cliente (16/10/2026)

Con el cierre por parte del cliente (ver más arriba) había un hueco: el cliente solo se enteraba de que un ticket estaba resuelto si miraba la lista o la campana. Ahora, si tiene tickets en estado "resuelto", arriba de su pantalla sale una franja con el borde en el color de la marca: "Tienes 1 ticket esperando tu confirmación" y un botón "Verlos" que filtra la lista a los resueltos. Cuando no hay ninguno la franja desaparece sin dejar hueco (`setManaged(false)`, para que el espaciado de la columna no cuente un elemento oculto), y aparece con la misma animación de entrada que el resto. Se calcula en `refrescar()`, así que se actualiza sola con el sondeo de avisos. Es la acción pendiente más importante del cliente, por eso va la primera y con más peso visual que las tarjetas de números.

- **Ventanas modales limitadas a la pantalla.** `Componentes.crearVentanaModal` recorta el ancho y el alto de la ventana al área visible de la pantalla (menos un margen). Antes cada ventana llevaba su tamaño fijo y, en un portátil de 1366 x 768, un modal de 800 px de alto se salía por abajo; solo el detalle del ticket lo tenía en cuenta.
- **Visor de imágenes con un bloque blanco debajo.** Al abrir una captura pequeña, el fondo gris del visor terminaba justo bajo la imagen y el resto de la ventana quedaba en blanco. Lo vi al fotografiar el visor con un programa de pruebas que rellena el formulario por reflexión (los chips de captura solo se añaden desde el selector de ficheros del sistema, que no se puede automatizar). Solución: `ScrollPane.setFitToHeight(true)`, así el fondo ocupa siempre todo el alto visible y la imagen queda centrada; si la imagen es más alta que la ventana, sigue habiendo scroll.
