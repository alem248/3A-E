/**
 * Calendario mensual del histórico. Cada dia se colorea segun su temperatura maxima.
 * Expone HistoricoCalendario.renderizar(contenedor, registros).
 */
const HistoricoCalendario = (() => {
    const NOMBRES_MES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
        'Julio', 'Agosto', 'Setiembre', 'Octubre', 'Noviembre', 'Diciembre'];
    const ENCABEZADOS_DIA = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

    const CATEGORIAS = [
        { clase: 'dia-sin-datos', nombre: 'Sin datos' },
        { clase: 'dia-fresco', nombre: 'Menos de 26 °C' },
        { clase: 'dia-templado', nombre: '26 a 28.9 °C' },
        { clase: 'dia-calido', nombre: '29 a 30.9 °C' },
        { clase: 'dia-muy-calido', nombre: '31 °C o más' }
    ];

    /** Devuelve la categoria de color segun la temperatura maxima (null = sin datos). */
    function categoriaTemperatura(temperaturaMaxC) {
        if (temperaturaMaxC === null || temperaturaMaxC === undefined) {
            return CATEGORIAS[0];
        }
        if (temperaturaMaxC < 26) {
            return CATEGORIAS[1];
        }
        if (temperaturaMaxC < 29) {
            return CATEGORIAS[2];
        }
        if (temperaturaMaxC < 31) {
            return CATEGORIAS[3];
        }
        return CATEGORIAS[4];
    }

    /** Agrupa registros por "AAAA-MM", en orden cronologico. */
    function agruparPorMes(registros) {
        const grupos = new Map();
        [...registros]
            .sort((a, b) => a.fecha.localeCompare(b.fecha))
            .forEach((registro) => {
                const clave = registro.fecha.slice(0, 7);
                if (!grupos.has(clave)) {
                    grupos.set(clave, []);
                }
                grupos.get(clave).push(registro);
            });
        return grupos;
    }

    /** Posicion de la columna (0 = lunes) del dia 1 del mes. */
    function columnaInicial(anio, mes) {
        return (new Date(anio, mes - 1, 1).getDay() + 6) % 7;
    }

    function crearLeyenda() {
        const leyenda = document.createElement('ul');
        leyenda.className = 'calendario-leyenda';
        CATEGORIAS.forEach((categoria) => {
            const item = document.createElement('li');
            const muestra = document.createElement('span');
            muestra.className = 'calendario-muestra ' + categoria.clase;
            muestra.setAttribute('aria-hidden', 'true');
            item.append(muestra, categoria.nombre);
            leyenda.append(item);
        });
        return leyenda;
    }

    function crearDia(registro) {
        const [, , dia] = registro.fecha.split('-');
        const celda = document.createElement('div');
        const categoria = categoriaTemperatura(registro.temperaturaMaxC);
        celda.className = 'dia ' + categoria.clase;
        celda.textContent = String(Number(dia));

        const partes = [registro.fecha];
        partes.push(registro.temperaturaMaxC === null ? 'máx sin dato' : `máx ${registro.temperaturaMaxC.toFixed(1)} °C`);
        partes.push(registro.temperaturaMinC === null ? 'mín sin dato' : `mín ${registro.temperaturaMinC.toFixed(1)} °C`);
        partes.push(registro.precipitacionMm === null ? 'lluvia sin dato' : `lluvia ${registro.precipitacionMm.toFixed(1)} mm`);
        celda.title = partes.join(' · ');
        celda.setAttribute('aria-label', partes.join(', '));
        return celda;
    }

    function crearMes(clave, registrosDelMes) {
        const [anio, mes] = clave.split('-').map(Number);
        const bloque = document.createElement('article');
        bloque.className = 'calendario-mes';

        const titulo = document.createElement('h3');
        titulo.textContent = `${NOMBRES_MES[mes - 1]} ${anio}`;

        const grilla = document.createElement('div');
        grilla.className = 'calendario-grilla';
        grilla.setAttribute('role', 'grid');
        grilla.setAttribute('aria-label', titulo.textContent);

        ENCABEZADOS_DIA.forEach((nombre) => {
            const cabecera = document.createElement('div');
            cabecera.className = 'calendario-cabecera';
            cabecera.textContent = nombre;
            grilla.append(cabecera);
        });

        for (let i = 0; i < columnaInicial(anio, mes); i++) {
            const vacio = document.createElement('div');
            vacio.className = 'dia dia-vacio';
            vacio.setAttribute('aria-hidden', 'true');
            grilla.append(vacio);
        }
        registrosDelMes.forEach((registro) => grilla.append(crearDia(registro)));

        bloque.append(titulo, grilla);
        return bloque;
    }

    function renderizar(contenedor, registros) {
        const meses = agruparPorMes(registros);
        const nodos = [crearLeyenda()];
        meses.forEach((registrosDelMes, clave) => nodos.push(crearMes(clave, registrosDelMes)));
        contenedor.replaceChildren(...nodos);
        contenedor.hidden = meses.size === 0;
    }

    return { categoriaTemperatura, agruparPorMes, columnaInicial, renderizar, CATEGORIAS };
})();
