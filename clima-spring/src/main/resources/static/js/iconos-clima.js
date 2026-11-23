// Iconos SVG del clima (trazo, sin imágenes externas). Solo presentación.
window.iconosClima = (() => {
    const RUTAS = {
        sol: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
        parcial: '<circle cx="9" cy="8" r="3.5"/><path d="M9 2.5V4M3.5 8H2M5.2 4.2l1 1M12.8 4.2l-1 1"/><path d="M9 20h8a3.5 3.5 0 0 0 0-7 5 5 0 0 0-9.6 1.5A2.7 2.7 0 0 0 9 20z"/>',
        nube: '<path d="M7 18a4 4 0 0 1 0-8 6 6 0 0 1 11.5 2A3.5 3.5 0 0 1 18 18z"/>',
        lluvia: '<path d="M7 14a4 4 0 0 1 0-8 6 6 0 0 1 11.5 2A3.5 3.5 0 0 1 18 14z"/><path d="M8 17l-1 3M12 17l-1 3M16 17l-1 3"/>',
        viento: '<path d="M3 8h11a3 3 0 1 0-3-3M3 12h15a3 3 0 1 1-3 3M3 16h7"/>',
        gota: '<path d="M12 3s6 6.2 6 10.5A6 6 0 0 1 6 13.5C6 9.2 12 3 12 3z"/>',
        alerta: '<path d="M12 3 2 20h20L12 3z"/><path d="M12 10v5M12 17.5v.5"/>',
        calor: '<path d="M14 14.8V5a2 2 0 0 0-4 0v9.8a4 4 0 1 0 4 0z"/>',
        pin: '<path d="M12 21s7-6.1 7-11a7 7 0 1 0-14 0c0 4.9 7 11 7 11Z"/><circle cx="12" cy="10" r="2.5"/>'
    };

    function svg(nombre, clase = '') {
        return `<svg class="icono-clima ${clase}" viewBox="0 0 24 24" fill="none" stroke="currentColor" ` +
            `stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${RUTAS[nombre]}</svg>`;
    }

    // Icono del día según la probabilidad de lluvia
    function pronostico(probabilidad) {
        if (probabilidad < 20) return svg('sol');
        if (probabilidad < 50) return svg('parcial');
        if (probabilidad < 70) return svg('nube');
        return svg('lluvia');
    }

    // Icono de cada dato del resumen de zona
    function para(titulo, principal) {
        const mapa = {
            'Región': 'pin',
            'Calor': 'calor',
            'Humedad': 'gota',
            'Viento': 'viento',
            'Lluvia': 'lluvia',
            'Sol': 'sol'
        };
        if (titulo === 'Fenómeno El Niño') {
            return ['Alto', 'Muy alto'].includes(principal) ? svg('alerta', 'zona-alerta') : svg('nube');
        }
        return svg(mapa[titulo] || 'nube');
    }

    return { svg, pronostico, para };
})();
