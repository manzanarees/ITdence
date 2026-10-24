CREATE TABLE IF NOT EXISTS usuario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    contrasena_hash TEXT NOT NULL,
    rol TEXT NOT NULL CHECK (rol IN ('cliente','tecnico','admin'))
);

CREATE TABLE IF NOT EXISTS categoria (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS ticket (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    titulo TEXT NOT NULL,
    descripcion TEXT,
    prioridad TEXT NOT NULL CHECK (prioridad IN ('baja','media','alta')),
    estado TEXT NOT NULL DEFAULT 'abierto' CHECK (estado IN ('abierto','en progreso', 'resuelto', 'cerrado')),
    categoria_id INTEGER NOT NULL REFERENCES categoria(id),
    creador_id INTEGER NOT NULL REFERENCES usuario(id),
    tecnico_asignado_id INTEGER REFERENCES usuario(id),
    fecha_creacion TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    fecha_actualizacion TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    fecha_limite TEXT,
    fecha_resolucion TEXT,
    valoracion INTEGER CHECK (valoracion BETWEEN 1 AND 5),
    reaperturas INTEGER NOT NULL DEFAULT 0,
    cancelado INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS comentario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_id INTEGER NOT NULL REFERENCES ticket(id),
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    texto TEXT NOT NULL,
    fecha TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    interno INTEGER NOT NULL DEFAULT 0
);


CREATE TABLE IF NOT EXISTS actividad (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_id INTEGER NOT NULL REFERENCES ticket(id),
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    tipo TEXT NOT NULL,
    detalle TEXT,
    fecha TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);


CREATE TABLE IF NOT EXISTS plantilla (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    titulo TEXT NOT NULL UNIQUE,
    texto TEXT NOT NULL
);


CREATE TABLE IF NOT EXISTS notificacion (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    ticket_id INTEGER NOT NULL REFERENCES ticket(id),
    texto TEXT NOT NULL,
    leida INTEGER NOT NULL DEFAULT 0,
    fecha TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);


CREATE TABLE IF NOT EXISTS adjunto (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_id INTEGER NOT NULL REFERENCES ticket(id),
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    nombre TEXT NOT NULL,
    archivo TEXT NOT NULL,
    tamano INTEGER NOT NULL DEFAULT 0,
    fecha TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);
