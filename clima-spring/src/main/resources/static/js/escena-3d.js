/*
 * Escena 3D de la barra lateral (Three.js): tormenta, río, mar con espuma, casas,
 * palmeras con viento, muñeco con paraguas. Si no hay WebGL o la librería no carga,
 * se queda la versión 2.5D. Solo presentación.
 */
(() => {
    'use strict';

    const contenedor = document.querySelector('.escena-lateral');
    if (!contenedor) return;

    function hayWebGL() {
        try {
            const lienzoPrueba = document.createElement('canvas');
            return Boolean(lienzoPrueba.getContext('webgl2') || lienzoPrueba.getContext('webgl'));
        } catch (error) {
            return false;
        }
    }

    if (!hayWebGL()) return;

    import('https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.module.js')
        .then((THREE) => iniciar(THREE))
        .catch(() => { /* Sin librería: se conserva la versión 2.5D */ });

    function iniciar(THREE) {
        const reducirMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        const rnd = (a, b) => a + Math.random() * (b - a);
        const suave = (a, b, x) => {
            const t = Math.min(1, Math.max(0, (x - a) / (b - a)));
            return t * t * (3 - 2 * t);
        };

        // Línea central del río (x, z)
        const RIO = [[-0.6, -16], [-0.2, -10], [-0.7, -5], [-0.1, -0.5], [0.9, 2.5], [1.4, 5.5]];

        function alturaBase(x, z) {
            const lejos = suave(-2, -18, z);
            const orilla = suave(1.2, 4.2, z);
            let h = 0.4 + 1.6 * lejos + 0.22 * Math.sin(x * 0.9 + z * 0.35) * Math.cos(z * 0.45) + 0.12 * Math.sin(x * 2.3 - z * 1.7);
            h = h * (1 - orilla) - orilla * 0.9;
            return h;
        }

        function distanciaSegmento(x, z, ax, az, bx, bz) {
            const vx = bx - ax;
            const vz = bz - az;
            const t = Math.max(0, Math.min(1, ((x - ax) * vx + (z - az) * vz) / (vx * vx + vz * vz)));
            return Math.hypot(ax + vx * t - x, az + vz * t - z);
        }

        function distanciaRio(x, z) {
            let d = Infinity;
            for (let i = 0; i < RIO.length - 1; i++) {
                d = Math.min(d, distanciaSegmento(x, z, RIO[i][0], RIO[i][1], RIO[i + 1][0], RIO[i + 1][1]));
            }
            return d;
        }

        function alturaTerreno(x, z) {
            return alturaBase(x, z) - 0.5 * Math.exp(-(distanciaRio(x, z) ** 2) / 0.35);
        }

        // Render, escena y cámara
        const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
        renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        renderer.shadowMap.enabled = true;
        renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        contenedor.appendChild(renderer.domElement);
        contenedor.classList.add('es-3d');

        const escena = new THREE.Scene();
        escena.fog = new THREE.Fog(0x2a3d55, 20, 50);
        const camara = new THREE.PerspectiveCamera(40, 1, 0.1, 200);

        escena.add(new THREE.HemisphereLight(0x9fb8dc, 0x1f3a2a, 0.75));
        const sol = new THREE.DirectionalLight(0xbcd4ff, 1.0);
        sol.position.set(-6, 16, 10);
        sol.castShadow = true;
        sol.shadow.mapSize.set(1024, 1024);
        Object.assign(sol.shadow.camera, { left: -9, right: 9, top: 14, bottom: -14, near: 1, far: 70 });
        escena.add(sol);

        function sombrear(objeto) {
            objeto.traverse((hijo) => {
                if (hijo.isMesh) {
                    hijo.castShadow = true;
                    hijo.receiveShadow = true;
                }
            });
            return objeto;
        }

        // Terreno: verde en las colinas, barro junto al río y arena en la orilla
        const geoTerreno = new THREE.PlaneGeometry(14, 44, 70, 120);
        geoTerreno.rotateX(-Math.PI / 2);
        geoTerreno.translate(0, 0, -2);
        const posTerreno = geoTerreno.attributes.position;
        const colores = new Float32Array(posTerreno.count * 3);
        const verde = new THREE.Color('#4a9a48');
        const verdeClaro = new THREE.Color('#6cbc64');
        const verdeLejano = new THREE.Color('#2f6b3e');
        const arena = new THREE.Color('#c9a06a');
        const barro = new THREE.Color('#6b4a2b');
        const tmp = new THREE.Color();
        for (let i = 0; i < posTerreno.count; i++) {
            const x = posTerreno.getX(i);
            const z = posTerreno.getZ(i);
            const h = alturaTerreno(x, z);
            posTerreno.setY(i, h);
            if (h < 0.25) tmp.copy(arena);
            else if (h < 0.8) tmp.copy(verdeClaro);
            else tmp.copy(verde);
            tmp.lerp(verdeLejano, suave(-6, -16, z) * 0.7);
            tmp.lerp(barro, (1 - suave(0.4, 1.4, distanciaRio(x, z))) * 0.8);
            colores.set([tmp.r, tmp.g, tmp.b], i * 3);
        }
        geoTerreno.setAttribute('color', new THREE.BufferAttribute(colores, 3));
        geoTerreno.computeVertexNormals();
        const terreno = new THREE.Mesh(geoTerreno, new THREE.MeshStandardMaterial({ vertexColors: true, roughness: 0.9 }));
        terreno.receiveShadow = true;
        escena.add(terreno);

        // Mar: oleaje con espuma blanca en las crestas
        const geoMar = new THREE.PlaneGeometry(16, 30, 56, 60);
        geoMar.rotateX(-Math.PI / 2);
        geoMar.translate(0, 0, 14);
        const posMar = geoMar.attributes.position;
        const baseMar = Float32Array.from(posMar.array);
        const colMar = new Float32Array(posMar.count * 3);
        const colorMar = new THREE.Color('#0f5f7a');
        const colorEspuma = new THREE.Color('#e8f7ff');
        const colTmp = new THREE.Color();
        geoMar.setAttribute('color', new THREE.BufferAttribute(colMar, 3));
        const mar = new THREE.Mesh(geoMar, new THREE.MeshStandardMaterial({ vertexColors: true, roughness: 0.3, metalness: 0.1 }));
        mar.receiveShadow = true;
        escena.add(mar);

        // Río con franjas de corriente que avanzan
        const lienzoRio = document.createElement('canvas');
        lienzoRio.width = 256;
        lienzoRio.height = 64;
        const gRio = lienzoRio.getContext('2d');
        gRio.fillStyle = '#2b7fb8';
        gRio.fillRect(0, 0, 256, 64);
        for (let i = 0; i < 9; i++) {
            gRio.fillStyle = `rgba(230, 248, 255, ${rnd(0.2, 0.6)})`;
            gRio.fillRect(rnd(0, 256), rnd(0, 60), rnd(14, 46), rnd(2, 5));
        }
        const texturaRio = new THREE.CanvasTexture(lienzoRio);
        texturaRio.wrapS = THREE.RepeatWrapping;
        texturaRio.wrapT = THREE.RepeatWrapping;
        texturaRio.repeat.set(5, 1);
        const curvaRio = new THREE.CatmullRomCurve3(
            RIO.map(([x, z]) => new THREE.Vector3(x, alturaTerreno(x, z) + 0.14, z))
        );
        const rio = new THREE.Mesh(
            new THREE.TubeGeometry(curvaRio, 90, 0.42, 10, false),
            new THREE.MeshStandardMaterial({ color: 0x3a8fc9, map: texturaRio, roughness: 0.15, metalness: 0.1 })
        );
        escena.add(rio);

        // Casas de madera con ventanas iluminadas
        const materialVentana = new THREE.MeshStandardMaterial({ color: 0xffd27a, emissive: 0xffb84d, emissiveIntensity: 1.4 });
        const materialPuerta = new THREE.MeshStandardMaterial({ color: 0x5a3a22, roughness: 0.8 });
        function crearCasa(x, z, pared, techo) {
            const grupo = new THREE.Group();
            const cuerpo = new THREE.Mesh(new THREE.BoxGeometry(0.9, 0.7, 0.8), new THREE.MeshStandardMaterial({ color: pared, roughness: 0.8 }));
            cuerpo.position.y = 0.35;
            const tejado = new THREE.Mesh(new THREE.ConeGeometry(0.8, 0.6, 4), new THREE.MeshStandardMaterial({ color: techo, roughness: 0.7 }));
            tejado.rotation.y = Math.PI / 4;
            tejado.position.y = 1.0;
            const puerta = new THREE.Mesh(new THREE.BoxGeometry(0.18, 0.34, 0.02), materialPuerta);
            puerta.position.set(0, 0.17, 0.41);
            const ventana = new THREE.Mesh(new THREE.BoxGeometry(0.16, 0.16, 0.02), materialVentana);
            ventana.position.set(0.25, 0.45, 0.41);
            grupo.add(cuerpo, tejado, puerta, ventana);
            grupo.position.set(x, alturaTerreno(x, z), z);
            escena.add(sombrear(grupo));
        }
        crearCasa(-2.2, -8, 0x6b4a33, 0xb5452e);
        crearCasa(0.4, -10.5, 0x7a5a3c, 0xc0553a);
        crearCasa(2.3, -7, 0x5f4330, 0x9e3f2a);

        // Arbustos en grupos de varios tonos de verde
        const verdes = [0x2f7f3d, 0x3c9a48, 0x256b33];
        function crearArbusto(x, z) {
            const grupo = new THREE.Group();
            for (let i = 0; i < 4; i++) {
                const esfera = new THREE.Mesh(
                    new THREE.SphereGeometry(rnd(0.22, 0.38), 10, 8),
                    new THREE.MeshStandardMaterial({ color: verdes[i % verdes.length], roughness: 0.9 })
                );
                esfera.position.set(rnd(-0.3, 0.3), rnd(0.1, 0.3), rnd(-0.2, 0.2));
                esfera.scale.y = 0.7;
                grupo.add(esfera);
            }
            grupo.position.set(x, alturaTerreno(x, z), z);
            escena.add(sombrear(grupo));
        }
        [[-1.2, -6.5], [1.2, -8.8], [-3.2, -9.5], [2.9, -4.5], [-3.4, -1.5], [-0.4, -4.2]].forEach(([x, z]) => crearArbusto(x, z));

        // Palmeras: el tronco se dobla con el viento y las hojas caen en arco
        const palmas = [];
        const materialHoja = new THREE.MeshStandardMaterial({ color: 0x2f9a4d, roughness: 0.6, side: THREE.DoubleSide });
        const materialTronco = new THREE.MeshStandardMaterial({ color: 0x6e4c32, roughness: 0.9 });
        function crearPalmera(x, z, inclinacion, velocidad, fuerza) {
            const base = new THREE.Group();
            base.position.set(x, alturaTerreno(x, z), z);
            const curva = new THREE.CatmullRomCurve3([
                new THREE.Vector3(0, 0, 0),
                new THREE.Vector3(0.25 * inclinacion, 1.6, 0),
                new THREE.Vector3(0.6 * inclinacion, 3.1, 0),
            ]);
            const tronco = new THREE.Mesh(new THREE.TubeGeometry(curva, 16, 0.12, 8), materialTronco);
            const copa = new THREE.Group();
            copa.position.set(0.6 * inclinacion, 3.1, 0);
            for (let k = 0; k < 9; k++) {
                const hoja = new THREE.Mesh(new THREE.ConeGeometry(0.26, 2.0, 4), materialHoja);
                hoja.position.set(0.9, -0.45, 0);
                hoja.rotation.z = -Math.PI / 2 - 0.7;
                hoja.scale.z = 0.45;
                const brazo = new THREE.Group();
                brazo.rotation.y = (k / 9) * Math.PI * 2;
                brazo.add(hoja);
                copa.add(brazo);
            }
            base.add(tronco, copa);
            escena.add(sombrear(base));
            palmas.push({ base, velocidad, fuerza, fase: x * 1.7 });
        }
        crearPalmera(-2.9, -4, -0.6, 1.3, 0.16);
        crearPalmera(2.6, -2.5, 0.5, 1.1, 0.18);
        crearPalmera(3.2, -9, 1.0, 2.1, 0.3);

        // Hojas que vuelan con el viento
        const matHojaVuelo = new THREE.MeshStandardMaterial({ color: 0x3f9a3a, roughness: 0.7 });
        const hojasVuelo = [];
        for (let i = 0; i < 36; i++) {
            const hoja = new THREE.Mesh(new THREE.BoxGeometry(0.14, 0.02, 0.07), matHojaVuelo);
            hoja.position.set(rnd(-5, 5), rnd(0.5, 4), rnd(-14, 4));
            hoja.userData = { v: rnd(1.5, 3), fase: rnd(0, 6) };
            escena.add(hoja);
            hojasVuelo.push(hoja);
        }

        // Muñeco con paraguas azul e impermeable amarillo
        const materialChubasquero = new THREE.MeshStandardMaterial({ color: 0xf5c518, roughness: 0.6 });
        const materialPantalon = new THREE.MeshStandardMaterial({ color: 0x1f2937, roughness: 0.8 });
        const materialBota = new THREE.MeshStandardMaterial({ color: 0xd64541, roughness: 0.6 });
        const materialPiel = new THREE.MeshStandardMaterial({ color: 0xf2c6a0, roughness: 0.7 });
        const materialPelo = new THREE.MeshStandardMaterial({ color: 0x3b2a1d, roughness: 0.9 });
        const materialParaguas = new THREE.MeshStandardMaterial({ color: 0x1d5fd8, roughness: 0.5, side: THREE.DoubleSide });
        const posFigura = { x: -1.9, z: 1.2 };
        const figura = new THREE.Group();
        figura.position.set(posFigura.x, alturaTerreno(posFigura.x, posFigura.z), posFigura.z);
        escena.add(sombrear(figura));

        const torso = new THREE.Mesh(new THREE.CylinderGeometry(0.22, 0.27, 0.8, 12), materialChubasquero);
        torso.position.y = 1.15;
        const cabeza = new THREE.Mesh(new THREE.SphereGeometry(0.19, 14, 10), materialPiel);
        cabeza.position.y = 1.78;
        const pelo = new THREE.Mesh(new THREE.SphereGeometry(0.2, 14, 8, 0, Math.PI * 2, 0, Math.PI / 2), materialPelo);
        pelo.position.y = 1.8;
        figura.add(torso, cabeza, pelo);

        function crearPierna(lado) {
            const cadera = new THREE.Group();
            cadera.position.set(0.12 * lado, 0.72, 0);
            const pierna = new THREE.Mesh(new THREE.CylinderGeometry(0.075, 0.075, 0.62, 8), materialPantalon);
            pierna.position.y = -0.31;
            const bota = new THREE.Mesh(new THREE.BoxGeometry(0.18, 0.12, 0.28), materialBota);
            bota.position.set(0, -0.68, 0.05);
            cadera.add(pierna, bota);
            figura.add(cadera);
            return cadera;
        }

        function crearBrazo(lado) {
            const hombro = new THREE.Group();
            hombro.position.set(0.27 * lado, 1.45, 0);
            const brazo = new THREE.Mesh(new THREE.CylinderGeometry(0.07, 0.07, 0.6, 8), materialChubasquero);
            brazo.position.y = -0.3;
            hombro.add(brazo);
            figura.add(hombro);
            return hombro;
        }

        const piernaIzq = crearPierna(-1);
        const piernaDer = crearPierna(1);
        const brazoIzq = crearBrazo(-1);
        crearBrazo(1);

        const paraguas = new THREE.Group();
        const mango = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.03, 1.3, 6), materialPantalon);
        mango.position.y = 0.65;
        const dosel = new THREE.Mesh(new THREE.SphereGeometry(0.66, 16, 8, 0, Math.PI * 2, 0, Math.PI / 2), materialParaguas);
        dosel.position.y = 1.3;
        paraguas.add(mango, dosel);
        paraguas.position.set(0.3, 0.8, 0.1);
        paraguas.rotation.z = 0.3;
        figura.add(paraguas);

        // Nubes de tormenta: sprites de textura suave apilados
        const texturaNube = (() => {
            const lienzoNube = document.createElement('canvas');
            lienzoNube.width = 128;
            lienzoNube.height = 128;
            const g = lienzoNube.getContext('2d');
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
            return new THREE.CanvasTexture(lienzoNube);
        })();

        const nubes = [];
        for (let i = 0; i < 46; i++) {
            const tono = rnd(0.14, 0.3);
            const material = new THREE.SpriteMaterial({
                map: texturaNube,
                color: new THREE.Color().setHSL(0.6, 0.3, tono),
                transparent: true,
                depthWrite: false,
                opacity: rnd(0.7, 0.95),
            });
            const nube = new THREE.Sprite(material);
            nube.scale.set(rnd(7, 12), rnd(4, 7), 1);
            nube.position.set(rnd(-10, 10), rnd(7, 12), rnd(-8, -26));
            escena.add(nube);
            nubes.push(nube);
        }

        // Lluvia densa y oblicua
        const N = 380;
        const geoLluvia = new THREE.BufferGeometry();
        const posLluvia = new Float32Array(N * 6);
        const gotas = [];
        for (let i = 0; i < N; i++) {
            gotas.push({ x: rnd(-6, 6), y: rnd(0, 24), z: rnd(-26, 10), v: rnd(16, 24) });
        }
        geoLluvia.setAttribute('position', new THREE.BufferAttribute(posLluvia, 3));
        escena.add(new THREE.LineSegments(
            geoLluvia,
            new THREE.LineBasicMaterial({ color: 0xd8ecff, transparent: true, opacity: 0.45 })
        ));

        function actualizarLluvia(dt) {
            for (let i = 0; i < N; i++) {
                const g = gotas[i];
                g.y -= g.v * dt;
                g.x -= 2.2 * dt;
                if (g.y < 0) {
                    g.y = rnd(18, 24);
                    g.x = rnd(-6, 6);
                    g.z = rnd(-26, 10);
                }
                const k = i * 6;
                posLluvia[k] = g.x;
                posLluvia[k + 1] = g.y;
                posLluvia[k + 2] = g.z;
                posLluvia[k + 3] = g.x - 0.18;
                posLluvia[k + 4] = g.y + 0.9;
                posLluvia[k + 5] = g.z;
            }
            geoLluvia.attributes.position.needsUpdate = true;
        }

        // Oleaje con espuma y marea
        function actualizarMar(t) {
            for (let i = 0; i < posMar.count; i++) {
                const x = baseMar[i * 3];
                const z = baseMar[i * 3 + 2];
                const y = Math.sin(x * 0.9 + t * 1.4) * 0.18 + Math.cos(z * 0.7 - t * 1.1) * 0.14 + Math.sin((x + z) * 1.7 + t * 2.3) * 0.06;
                posMar.setY(i, y);
                colTmp.copy(colorMar).lerp(colorEspuma, suave(0.1, 0.28, y));
                colMar.set([colTmp.r, colTmp.g, colTmp.b], i * 3);
            }
            posMar.needsUpdate = true;
            geoMar.attributes.color.needsUpdate = true;
            mar.position.y = Math.sin(t * 0.45) * 0.22;
        }

        // Interacción: el cursor mueve la cámara; sin cursor hay un vaivén suave
        let dentro = false;
        let objetivoX = 0;
        let objetivoY = 0;
        let actualX = 0;
        let actualY = 0;
        contenedor.addEventListener('pointermove', (evento) => {
            const rect = contenedor.getBoundingClientRect();
            objetivoX = ((evento.clientX - rect.left) / rect.width - 0.5) * 2;
            objetivoY = ((evento.clientY - rect.top) / rect.height - 0.5) * 2;
            dentro = true;
        });
        contenedor.addEventListener('pointerleave', () => { dentro = false; });

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

            if (!dentro) {
                objetivoX = Math.sin(t * 0.3) * 0.5;
                objetivoY = Math.sin(t * 0.22) * 0.25;
            }
            actualX += (objetivoX - actualX) * 0.05;
            actualY += (objetivoY - actualY) * 0.05;
            camara.position.set(actualX * 1.6, 9 + actualY * 0.8, 26);
            camara.lookAt(0, 2, -8);

            if (!reducirMovimiento) {
                actualizarMar(t);
                actualizarLluvia(dt);
                texturaRio.offset.x -= dt * 0.8;
                palmas.forEach((p) => {
                    const rafaga = 0.5 + 0.5 * Math.sin(t * 0.35 + p.fase * 0.3);
                    p.base.rotation.z = (Math.sin(t * p.velocidad + p.fase) * 0.7 + 0.3 * rafaga) * p.fuerza;
                });
                nubes.forEach((nube) => {
                    nube.position.x += dt * 0.25;
                    if (nube.position.x > 10) nube.position.x = -10;
                });
                hojasVuelo.forEach((hoja) => {
                    hoja.position.x += dt * hoja.userData.v;
                    hoja.position.y += Math.sin(t * 2 + hoja.userData.fase) * dt * 0.4 - dt * 0.12;
                    hoja.rotation.x += dt * 3;
                    if (hoja.position.x > 5 || hoja.position.y < 0.2) {
                        hoja.position.set(-5, rnd(1, 4), rnd(-14, 4));
                    }
                });
                const paso = Math.sin(t * 6);
                piernaIzq.rotation.x = paso * 0.5;
                piernaDer.rotation.x = -paso * 0.5;
                brazoIzq.rotation.x = -paso * 0.4;
                figura.position.y = alturaTerreno(posFigura.x, posFigura.z) + Math.abs(paso) * 0.04;
            }

            renderer.render(escena, camara);
            if (!reducirMovimiento) requestAnimationFrame(cuadro);
        }

        new ResizeObserver(ajustar).observe(contenedor);
        ajustar();
        cuadro();
    }
})();
