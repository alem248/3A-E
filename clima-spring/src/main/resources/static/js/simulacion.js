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
        lecturas: { 1: 29.5, 2: 29.0, 3: 28.0, 4: 27.0, 5: 25.0, 6: 25.0 }
    },
    normal: {
        nombre: "Condiciones normales",
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
const cuerpoUbicaciones = document.getElementById("ubicaciones");
const botonSimular = document.getElementById("simular");
const estado = document.getElementById("estado");
const resultado = document.getElementById("resultado");
const resumen = document.getElementById("resumen");
const filasResultado = document.getElementById("filas-resultado");
const alertasEscenario = document.getElementById("alertas-escenario");

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
        const lectura = document.createElement("span");
        lectura.className = "lectura";
        lectura.textContent = `${zona.nombre}: ${escenario.lecturas[zona.id].toFixed(1)} °C`;
        contenedorLecturas.appendChild(lectura);
    });
}

function pintarUbicaciones() {
    UBICACIONES.forEach((ubicacion, indice) => {
        const fila = document.createElement("tr");

        const etiqueta = document.createElement("td");
        const entradaEtiqueta = document.createElement("input");
        entradaEtiqueta.value = ubicacion.etiqueta;
        entradaEtiqueta.dataset.campo = "etiqueta";
        etiqueta.appendChild(entradaEtiqueta);

        const latitud = document.createElement("td");
        const entradaLatitud = document.createElement("input");
        entradaLatitud.type = "number";
        entradaLatitud.step = "0.0001";
        entradaLatitud.value = ubicacion.latitud;
        entradaLatitud.dataset.campo = "latitud";
        latitud.appendChild(entradaLatitud);

        const longitud = document.createElement("td");
        const entradaLongitud = document.createElement("input");
        entradaLongitud.type = "number";
        entradaLongitud.step = "0.0001";
        entradaLongitud.value = ubicacion.longitud;
        entradaLongitud.dataset.campo = "longitud";
        longitud.appendChild(entradaLongitud);

        fila.dataset.indice = indice;
        fila.append(etiqueta, latitud, longitud);
        cuerpoUbicaciones.appendChild(fila);
    });
}

function leerUbicaciones() {
    return Array.from(cuerpoUbicaciones.querySelectorAll("tr")).map((fila) => ({
        etiqueta: fila.querySelector('[data-campo="etiqueta"]').value,
        latitud: Number(fila.querySelector('[data-campo="latitud"]').value),
        longitud: Number(fila.querySelector('[data-campo="longitud"]').value)
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
    const recibidas = datos.ubicaciones.filter((ubicacion) => ubicacion.alertaDisparada).length;
    resumen.textContent = `${datos.escenario}: ${datos.alertas.length} alerta(s) en el escenario, ` +
        `${recibidas} de ${datos.ubicaciones.length} ubicación(es) reciben alerta.`;

    filasResultado.innerHTML = "";
    datos.ubicaciones.forEach((ubicacion) => {
        const fila = document.createElement("tr");
        fila.className = ubicacion.alertaDisparada ? "alerta" : "ok";

        [
            ubicacion.etiqueta,
            ubicacion.zona ?? "—",
            ubicacion.nivelRiesgo ?? "—",
            ubicacion.zonaCritica ? "Sí" : "No",
            ubicacion.alertaDisparada ? "Sí" : "No",
            ubicacion.nivelAlerta ?? "—",
            ubicacion.mensaje
        ].forEach((valor) => {
            const celda = document.createElement("td");
            celda.textContent = valor;
            fila.appendChild(celda);
        });

        filasResultado.appendChild(fila);
    });

    alertasEscenario.innerHTML = "";
    if (datos.alertas.length === 0) {
        const item = document.createElement("li");
        item.textContent = "El escenario no supera ningún umbral.";
        alertasEscenario.appendChild(item);
        return;
    }

    datos.alertas.forEach((alerta) => {
        const item = document.createElement("li");
        item.textContent = `${alerta.zona} · ${alerta.regla} · ${alerta.nivel} · ${alerta.mensaje}`;
        alertasEscenario.appendChild(item);
    });
}

async function simular() {
    botonSimular.disabled = true;
    estado.textContent = "Simulando...";

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
selectorEscenario.addEventListener("change", pintarLecturas);
botonSimular.addEventListener("click", simular);
