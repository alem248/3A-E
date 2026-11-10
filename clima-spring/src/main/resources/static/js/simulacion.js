const ZONAS = [
    { id: 1, nombre: "Tumbes" },
    { id: 2, nombre: "Piura Norte" },
    { id: 3, nombre: "Piura Sur (Paita)" },
    { id: 4, nombre: "Lambayeque" },
    { id: 5, nombre: "La Libertad Norte" },
    { id: 6, nombre: "La Libertad Sur (Chimbote)" }
];

const ESCENARIOS = {
    fuerte: {
        nombre: "Niño costero fuerte",
        nota: "TSM muy por encima de los umbrales en el norte: se esperan alertas rojas y naranjas.",
        lecturas: { 1: 29.5, 2: 29.0, 3: 28.0, 4: 27.0, 5: 25.0, 6: 25.0 }
    },
    moderado: {
        nombre: "Onda cálida moderada",
        nota: "Calentamiento leve: solo las zonas del extremo norte superan el umbral de vigilancia.",
        lecturas: { 1: 26.8, 2: 25.2, 3: 24.4, 4: 23.5, 5: 23.0, 6: 22.8 }
    },
    normal: {
        nombre: "Condiciones normales",
        nota: "Todas las lecturas por debajo del umbral: el escenario no debe generar alertas.",
        lecturas: { 1: 23.0, 2: 23.0, 3: 23.0, 4: 23.0, 5: 23.0, 6: 23.0 }
    }
};

const UBICACIONES = [
    { etiqueta: "Tumbes (dentro)", latitud: -3.62, longitud: -81.0 },
    { etiqueta: "Piura (dentro)", latitud: -4.35, longitud: -81.35 },
    { etiqueta: "Lima (fuera)", latitud: -12.05, longitud: -77.04 }
];

const selectorEscenario = document.getElementById("escenario");
const contenedorLecturas = document.getElementById("lecturas");
const contenedorUbicaciones = document.getElementById("ubicaciones");
const notaEscenario = document.getElementById("nota-escenario");
const botonSimular = document.getElementById("simular");
const estado = document.getElementById("estado");
const resultado = document.getElementById("resultado");
const tituloResultado = document.getElementById("titulo-resultado");
const chipVeredicto = document.getElementById("chip-veredicto");
const filasResultado = document.getElementById("filas-resultado");
const alertasEscenario = document.getElementById("alertas-escenario");
const metaReglas = document.getElementById("meta-reglas");
const metaZonas = document.getElementById("meta-zonas");
const metaUbicaciones = document.getElementById("meta-ubicaciones");

/** Nivel térmico de una lectura según los umbrales del dominio. */
function nivelDeLectura(temperatura) {
    if (temperatura >= 28) return "ROJO";
    if (temperatura >= 26) return "NARANJA";
    if (temperatura >= 24) return "AMARILLO";
    return "VERDE";
}

/** Normaliza nombres visibles ("Sin datos") a clases CSS ("SIN_DATOS"). */
function claseNivel(nivel) {
    if (!nivel) return "neutral";
    return nivel
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toUpperCase()
        .replace(/[^A-Z0-9]+/g, "_")
        .replace(/^_|_$/g, "");
}

function crearBadge(nivel, texto) {
    const badge = document.createElement("span");
    badge.className = `sim-badge ${claseNivel(nivel)}`;
    badge.textContent = texto ?? nivel ?? "—";
    return badge;
}

function crearOpciones() {
    Object.entries(ESCENARIOS).forEach(([clave, escenario]) => {
        const opcion = document.createElement("option");
        opcion.value = clave;
        opcion.textContent = escenario.nombre;
        selectorEscenario.appendChild(opcion);
    });
}

function pintarLecturas() {
    const escenario = ESCENARIOS[selectorEscenario.value];
    contenedorLecturas.innerHTML = "";

    ZONAS.forEach((zona) => {
        const temperatura = escenario.lecturas[zona.id];
        const lectura = document.createElement("span");
        lectura.className = "sim-reading";

        const punto = document.createElement("span");
        punto.className = `dot ${nivelDeLectura(temperatura)}`;
        punto.setAttribute("aria-hidden", "true");

        const texto = document.createElement("span");
        texto.textContent = `${zona.nombre} · ${temperatura.toFixed(1)} °C`;

        lectura.append(punto, texto);
        contenedorLecturas.appendChild(lectura);
    });

    notaEscenario.textContent = escenario.nota;
}

function pintarUbicaciones() {
    contenedorUbicaciones.innerHTML = "";

    UBICACIONES.forEach((ubicacion, indice) => {
        const tarjeta = document.createElement("div");
        tarjeta.className = "sim-location";
        tarjeta.dataset.indice = indice;

        tarjeta.appendChild(campoUbicacion("Etiqueta", "etiqueta", ubicacion.etiqueta, "text"));
        tarjeta.appendChild(campoUbicacion("Latitud", "latitud", ubicacion.latitud, "number", "0.0001"));
        tarjeta.appendChild(campoUbicacion("Longitud", "longitud", ubicacion.longitud, "number", "0.0001"));

        contenedorUbicaciones.appendChild(tarjeta);
    });
}

function campoUbicacion(etiqueta, campo, valor, tipo, paso) {
    const contenedor = document.createElement("label");
    contenedor.className = "sim-location-field";

    const titulo = document.createElement("span");
    titulo.textContent = etiqueta;

    const entrada = document.createElement("input");
    entrada.type = tipo;
    entrada.value = valor;
    entrada.dataset.campo = campo;
    if (paso) entrada.step = paso;

    contenedor.append(titulo, entrada);
    return contenedor;
}

function leerUbicaciones() {
    return Array.from(contenedorUbicaciones.querySelectorAll(".sim-location")).map((tarjeta) => ({
        etiqueta: tarjeta.querySelector('[data-campo="etiqueta"]').value,
        latitud: Number(tarjeta.querySelector('[data-campo="latitud"]').value),
        longitud: Number(tarjeta.querySelector('[data-campo="longitud"]').value)
    }));
}

function construirSolicitud() {
    const escenario = ESCENARIOS[selectorEscenario.value];

    return {
        nombre: escenario.nombre,
        lecturas: ZONAS.map((zona) => ({
            zonaId: zona.id,
            temperatura: escenario.lecturas[zona.id]
        })),
        ubicaciones: leerUbicaciones()
    };
}

function pintarResultado(datos) {
    const total = datos.ubicaciones.length;
    const conAlerta = datos.ubicaciones.filter((ubicacion) => ubicacion.alertaDisparada).length;
    const sinAlerta = total - conAlerta;

    tituloResultado.textContent = datos.escenario;
    chipVeredicto.textContent = sinAlerta > 0
        ? `${sinAlerta} ubicación(es) sin alerta fuera de zona crítica`
        : "Todas las ubicaciones verificadas reciben alerta";

    document.getElementById("kpi-alertas").textContent = datos.alertas.length;
    document.getElementById("kpi-ubicaciones").textContent = total;
    document.getElementById("kpi-con-alerta").textContent = conAlerta;
    document.getElementById("kpi-sin-alerta").textContent = sinAlerta;

    filasResultado.innerHTML = "";
    datos.ubicaciones.forEach((ubicacion) => {
        const fila = document.createElement("tr");
        fila.className = ubicacion.alertaDisparada ? "is-alert" : "is-safe";

        fila.appendChild(celda(ubicacion.etiqueta, "cell-strong"));
        fila.appendChild(celda(ubicacion.zona ?? "Fuera de zona"));
        fila.appendChild(celdaConBadge(ubicacion.nivelRiesgo));
        fila.appendChild(celda(ubicacion.zonaCritica ? "Sí" : "No"));
        fila.appendChild(celdaAlerta(ubicacion));
        fila.appendChild(celdaConBadge(ubicacion.nivelAlerta));
        fila.appendChild(celda(ubicacion.mensaje, "cell-message"));

        filasResultado.appendChild(fila);
    });

    alertasEscenario.innerHTML = "";
    if (datos.alertas.length === 0) {
        const vacio = document.createElement("li");
        vacio.className = "sim-empty";
        vacio.textContent = "El escenario no supera ningún umbral: no se generan alertas.";
        alertasEscenario.appendChild(vacio);
        return;
    }

    datos.alertas.forEach((alerta) => {
        const item = document.createElement("li");
        item.className = `sim-alert-item ${claseNivel(alerta.nivel)}`;

        const cuerpo = document.createElement("div");
        cuerpo.className = "sim-alert-body";

        const nombre = document.createElement("strong");
        nombre.textContent = `${alerta.zona} · ${alerta.nivel}`;

        const regla = document.createElement("span");
        regla.className = "sim-alert-rule";
        regla.textContent = `${alerta.regla} · TSM ${alerta.temperatura.toFixed(1)} °C`;

        const mensaje = document.createElement("p");
        mensaje.textContent = alerta.mensaje;

        cuerpo.append(nombre, regla, mensaje);
        item.append(cuerpo, crearBadge(alerta.nivel));
        alertasEscenario.appendChild(item);
    });
}

function celda(texto, clase) {
    const celdaHtml = document.createElement("td");
    celdaHtml.textContent = texto ?? "—";
    if (clase) celdaHtml.className = clase;
    return celdaHtml;
}

function celdaConBadge(nivel) {
    const celdaHtml = document.createElement("td");
    celdaHtml.appendChild(crearBadge(nivel));
    return celdaHtml;
}

function celdaAlerta(ubicacion) {
    const celdaHtml = document.createElement("td");
    celdaHtml.appendChild(ubicacion.alertaDisparada
        ? crearBadge(ubicacion.nivelAlerta)
        : crearBadge("neutral", "Sin alerta"));
    return celdaHtml;
}

async function cargarMetadatos() {
    metaZonas.textContent = ZONAS.length;
    metaUbicaciones.textContent = UBICACIONES.length;

    try {
        const respuesta = await fetch("/api/reglas");
        if (!respuesta.ok) return;
        const reglas = await respuesta.json();
        metaReglas.textContent = Array.isArray(reglas) ? reglas.length : "—";
    } catch {
        metaReglas.textContent = "—";
    }
}

async function simular() {
    botonSimular.disabled = true;
    estado.textContent = "Simulando escenario...";

    try {
        const respuesta = await fetch("/api/reglas/simular-escenario", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(construirSolicitud())
        });

        if (!respuesta.ok) {
            throw new Error(`El servidor respondió ${respuesta.status}`);
        }

        const datos = await respuesta.json();
        pintarResultado(datos);
        resultado.hidden = false;
        resultado.scrollIntoView({ behavior: "smooth", block: "start" });
        estado.textContent = "Simulación completada.";
    } catch (error) {
        estado.textContent = `No se pudo simular: ${error.message}`;
    } finally {
        botonSimular.disabled = false;
    }
}

crearOpciones();
pintarLecturas();
pintarUbicaciones();
cargarMetadatos();
selectorEscenario.addEventListener("change", pintarLecturas);
botonSimular.addEventListener("click", simular);
