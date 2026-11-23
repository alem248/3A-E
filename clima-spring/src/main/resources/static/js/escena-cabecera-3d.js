/*
 * Escena 3D de la cabecera: solo lluvia, sol, nubes y hojas que vuelan.
 * Se dibuja sobre la foto de fondo. Si no hay WebGL o la librería no carga,
 * se queda la versión SVG. Solo presentación.
 */
(() => {
    'use strict';

    const contenedor = document.querySelector('.cabecera-panel .escena');
    if (!contenedor) return;

    function hayWebGL() {
        try {
            const lienzo = document.createElement('canvas');
            return Boolean(lienzo.getContext('webgl2') || lienzo.getContext('webgl'));
        } catch (error) {
            return false;
        }
    }

    if (!hayWebGL()) return;

    import('https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.module.js')
        .then((THREE) => iniciar(THREE))
        .catch(() => { /* Sin librería: se conserva el SVG */ });

    function iniciar(THREE) {
        const cabecera = contenedor.parentElement;
        const reducirMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        const rnd = (a, b) => a + Math.random() * (b - a);
        const suave = (a, b, x) => {
            const t = Math.min(1, Math.max(0, (x - a) / (b - a)));
            return t * t * (3 - 2 * t);
        };

        // Render y cámara
        const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
        renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        contenedor.appendChild(renderer.domElement);
        cabecera.classList.add('es-3d');

        const escena = new THREE.Scene();
        const camara = new THREE.PerspectiveCamera(44, 2, 0.1, 200);
        camara.position.set(0, 3.4, 13);
        camara.lookAt(-0.5, 1.6, -6);

        const luzAmbiente = new THREE.HemisphereLight(0xbcd8ff, 0x2a3a22, 0.8);
        escena.add(luzAmbiente);
        const luzSol = new THREE.DirectionalLight(0xfff1c8, 1.0);
        luzSol.position.set(9, 8, -6);
        escena.add(luzSol);

        // Sol: resplandor que sale y se apaga con la lluvia
        const texturaSol = (() => {
            const lienzo = document.createElement('canvas');
            lienzo.width = 128;
            lienzo.height = 128;
            const g = lienzo.getContext('2d');
            const degradado = g.createRadialGradient(64, 64, 6, 64, 64, 64);
            degradado.addColorStop(0, 'rgba(255,240,180,1)');
            degradado.addColorStop(0.35, 'rgba(255,209,102,0.6)');
            degradado.addColorStop(1, 'rgba(255,209,102,0)');
            g.fillStyle = degradado;
            g.fillRect(0, 0, 128, 128);
            return new THREE.CanvasTexture(lienzo);
        })();
        const materialSol = new THREE.SpriteMaterial({ map: texturaSol, transparent: true, depthWrite: false, opacity: 0 });
        const sol = new THREE.Sprite(materialSol);
        sol.scale.set(9, 9, 1);
        sol.position.set(9, 0.5, -14);
        escena.add(sol);

        // Nubes de tormenta: sprites suaves que se oscurecen con la lluvia
        const texturaNube = (() => {
            const lienzo = document.createElement('canvas');
            lienzo.width = 128;
            lienzo.height = 128;
            const g = lienzo.getContext('2d');
            const degradado = g.createRadialGradient(64, 64, 4, 64, 64, 64);
            degradado.addColorStop(0, 'rgba(255,255,255,1)');
            degradado.addColorStop(0.5, 'rgba(255,255,255,0.55)');
            degradado.addColorStop(1, 'rgba(255,255,255,0)');
            g.fillStyle = degradado;
            g.fillRect(0, 0, 128, 128);
            for (let i = 0; i < 10; i++) {
                g.fillStyle = `rgba(255,255,255,${rnd(0.1, 0.35)})`;
                g.beginPath();
                g.arc(rnd(30, 98), rnd(30, 98), rnd(10, 26), 0, Math.PI * 2);
                g.fill();
            }
            return new THREE.CanvasTexture(lienzo);
        })();
        const colNubeDia = new THREE.Color('#f4f8ff');
        const colNubeTormenta = new THREE.Color('#2b3a52');
        const nubes = [];
        for (let i = 0; i < 30; i++) {
            const material = new THREE.SpriteMaterial({
                map: texturaNube,
                color: colNubeTormenta.clone(),
                transparent: true,
                depthWrite: false,
                opacity: rnd(0.7, 0.95),
            });
            const nube = new THREE.Sprite(material);
            nube.scale.set(rnd(10, 16), rnd(5, 7), 1);
            nube.position.set(rnd(-20, 20), rnd(6.5, 10), rnd(-8, -18));
            escena.add(nube);
            nubes.push(nube);
        }

        // Lluvia oblicua con el viento
        const N = 900;
        const posLluvia = new Float32Array(N * 6);
        const gotas = [];
        for (let i = 0; i < N; i++) {
            gotas.push({ x: rnd(-18, 18), y: rnd(0, 12), z: rnd(-14, 10), v: rnd(14, 20) });
        }
        const geoLluvia = new THREE.BufferGeometry();
        geoLluvia.setAttribute('position', new THREE.BufferAttribute(posLluvia, 3));
        const materialLluvia = new THREE.LineBasicMaterial({ color: 0xd8ecff, transparent: true, opacity: 0.7 });
        escena.add(new THREE.LineSegments(geoLluvia, materialLluvia));

        function actualizarLluvia(dt) {
            for (let i = 0; i < N; i++) {
                const g = gotas[i];
                g.y -= g.v * dt;
                g.x -= 2.4 * dt;
                if (g.y < 0) {
                    g.y = rnd(10, 12);
                    g.x = rnd(-18, 18);
                    g.z = rnd(-14, 10);
                }
                const k = i * 6;
                posLluvia[k] = g.x;
                posLluvia[k + 1] = g.y;
                posLluvia[k + 2] = g.z;
                posLluvia[k + 3] = g.x - 0.2;
                posLluvia[k + 4] = g.y + 0.6;
                posLluvia[k + 5] = g.z;
            }
            geoLluvia.attributes.position.needsUpdate = true;
        }

        // Hojas que vuelan con el viento, en todo el encabezado
        const materialHojaVuelo = new THREE.MeshStandardMaterial({ color: 0x3f9a3a, roughness: 0.7 });
        const hojasVuelo = [];
        for (let i = 0; i < 16; i++) {
            const hoja = new THREE.Mesh(new THREE.BoxGeometry(0.14, 0.02, 0.07), materialHojaVuelo);
            hoja.position.set(rnd(-12, 8), rnd(0.8, 6), rnd(-6, 4));
            hoja.userData = { v: rnd(1.5, 3), fase: rnd(0, 6) };
            escena.add(hoja);
            hojasVuelo.push(hoja);
        }

        function ajustar() {
            const w = contenedor.clientWidth;
            const h = contenedor.clientHeight;
            if (!w || !h) return;
            renderer.setSize(w, h, false);
            camara.aspect = w / h;
            camara.updateProjectionMatrix();
        }

        const reloj = new THREE.Clock();
        let t = 0;

        function cuadro() {
            const dt = Math.min(reloj.getDelta(), 0.05);
            t += dt;

            // Ciclo de 16 s: lluvia que cesa y sol que sale, como la versión SVG
            const c = 0.5 - 0.5 * Math.cos((Math.PI * 2 * t) / 16);
            const dia = suave(0.2, 0.8, c);
            const tormenta = 1 - dia;

            luzAmbiente.intensity = 0.4 + 0.5 * dia;
            luzSol.intensity = 0.25 + 0.9 * dia;
            materialSol.opacity = dia;
            sol.position.y = 0.5 + 6.5 * dia;
            materialLluvia.opacity = 0.7 * tormenta;
            nubes.forEach((nube) => nube.material.color.lerpColors(colNubeDia, colNubeTormenta, tormenta));

            if (!reducirMovimiento) {
                actualizarLluvia(dt);
                nubes.forEach((nube) => {
                    nube.position.x += dt * 0.35;
                    if (nube.position.x > 22) nube.position.x = -22;
                });
                hojasVuelo.forEach((hoja) => {
                    hoja.position.x += dt * hoja.userData.v;
                    hoja.position.y += Math.sin(t * 2 + hoja.userData.fase) * dt * 0.4 - dt * 0.12;
                    hoja.rotation.x += dt * 3;
                    if (hoja.position.x > 8 || hoja.position.y < 0.5) {
                        hoja.position.set(-12, rnd(1, 6), rnd(-6, 4));
                    }
                });
            }

            renderer.render(escena, camara);
            if (!reducirMovimiento) requestAnimationFrame(cuadro);
        }

        new ResizeObserver(ajustar).observe(contenedor);
        ajustar();
        cuadro();
    }
})();
