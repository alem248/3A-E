# Prototipos de alertas térmicas

La interfaz toma la temperatura superficial del mar (TSM) por zona y aplica los umbrales existentes: vigilancia desde 24 °C, riesgo alto desde 26 °C y crítico desde 28 °C.

## Baja fidelidad: estructura

Prototipo navegable: [wireframe de alertas](prototipos/baja.html). Separa zona, temperatura, nivel y hora con cajas y bordes; el detalle desplegable enumera los estados de carga, normalidad y falta de lectura.

```text
+------------------------------------------------------------+
| ALERTAS TÉRMICAS                              [N alertas]  |
| Temperatura observada y nivel de riesgo por zona costera   |
+------------------------------------------------------------+
| | Zona / departamento | Temperatura | Riesgo | Actualizado |
| | Piura Norte          | 26.4 °C     | Naranja| 10:30       |
| | Tumbes               | 24.8 °C     | Amarillo| 10:30      |
+------------------------------------------------------------+
| Umbrales: vigilancia 24 °C · alto 26 °C · crítico 28 °C   |
```

Prioriza jerarquía, lectura rápida y contenido mínimo. En móvil, cada fila se reorganiza en una tarjeta.

## Alta fidelidad: interfaz implementada

Maqueta visual independiente: [prototipo de alta fidelidad](prototipos/alta.html). La pantalla principal implementada mantiene el mismo orden visual, con datos en vivo y ajustes para móvil.

La sección **Alertas térmicas** en la pantalla principal aplica la estructura con tarjetas, color asociado al nivel, temperatura destacada, nombre y departamento, descripción del riesgo y hora de lectura. Incluye estados sin alertas, carga inicial y zonas sin lectura; el color se acompaña siempre de texto para que el nivel no dependa solo de la percepción del color. La sección es adaptable a móvil y anuncia actualizaciones a tecnologías de asistencia.

## Criterios de aceptación

- La temperatura se muestra en °C con un decimal, usando el valor recibido.
- El nivel y su descripción corresponden a `clasificarTemperatura`.
- Las lecturas normales no generan una alerta activa.
- Los datos ausentes se identifican expresamente y no se convierten en 0 °C.
- Los umbrales se mantienen visibles para dar contexto al riesgo.
