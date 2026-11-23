(() => {
  const app = document.querySelector("#history-app");
  if (!app) return;
  const select = document.querySelector("#zone-select");
  const regionSelect = document.querySelector("#region-select");
  const status = document.querySelector("#history-status");
  const caption = document.querySelector("#chart-caption");
  const alertBadge = document.querySelector("#alert-indicator");
  const alertText = document.querySelector("#alert-text");
  const trendBadge = document.querySelector("#trend-indicator");
  const trendCaption = document.querySelector("#trend-caption");
  const canvas = document.querySelector("#history-chart");
  const emptyBox = document.querySelector("#chart-empty");
  const emptyText = document.querySelector("#chart-empty-text");
  const MENSAJE_INICIAL = "Selecciona una región y una zona para ver sus lecturas.";
  let chart;
  let activeController;
  let requestSequence = 0;

  function mostrarVacio(texto) {
    emptyText.textContent = texto;
    emptyBox.classList.remove("hidden");
  }

  function ocultarVacio() {
    emptyBox.classList.add("hidden");
  }

  function limpiarGrafico() {
    if (chart) { chart.destroy(); chart = undefined; }
    alertBadge.classList.add("hidden");
    trendBadge.classList.add("hidden");
  }

  regionSelect.addEventListener("change", () => {
    const region = regionSelect.value;
    const catalog = JSON.parse(document.querySelector("#zone-catalog")?.textContent || "[]");
    const available = catalog.filter(zone => zone.departamento === region);
    select.replaceChildren(new Option(available.length ? "Selecciona una zona…" : "Sin cobertura en esta región", ""));
    available.forEach(zone => select.add(new Option(zone.nombre, zone.id)));
    select.disabled = available.length === 0;
    requestSequence += 1;
    if (activeController) activeController.abort();
    limpiarGrafico();
    if (available.length === 1) {
      select.value = String(available[0].id);
      select.dispatchEvent(new Event("change"));
    } else {
      const mensaje = available.length
        ? "Selecciona una zona para consultar sus datos."
        : "Esta región aún no cuenta con zonas conectadas a la API climática.";
      caption.textContent = mensaje;
      status.textContent = "";
      mostrarVacio(mensaje);
    }
  });

  function showAlert(payload) {
    const threshold = Number(payload.umbralAlerta);
    const hasThreshold = Number.isFinite(threshold);
    const exceeds = hasThreshold && payload.puntos.some(point => Number(point.valor) >= threshold);
    const isActive = payload.alerta === true || exceeds;
    alertBadge.classList.toggle("hidden", !isActive);
    if (isActive) {
      alertText.textContent = hasThreshold ? `Umbral alcanzado (${threshold})` : "Alerta activa";
    }
  }

  function render(payload) {
    if (chart) chart.destroy();
    const unit = typeof payload.unidad === "string" ? payload.unidad : "";
    chart = new Chart(canvas, {
      type: "line",
      data: {
        labels: payload.puntos.map(point => point.fecha),
        datasets: [{
          label: payload.zonaNombre || "Lectura",
          data: payload.puntos.map(point => Number(point.valor)),
          borderColor: "#38bdf8",
          backgroundColor: "rgba(56,189,248,.14)",
          pointBackgroundColor: "#38bdf8",
          borderWidth: 2, pointRadius: 2, fill: true, tension: .25,
        }],
      },
      options: {
        responsive: true, maintainAspectRatio: false, animation: false,
        interaction: { intersect: false, mode: "index" },
        scales: {
          x: { ticks: { color: "#9fb8d6", maxRotation: 0, autoSkip: true }, grid: { color: "#1f3d63" } },
          y: {
            title: { display: Boolean(unit), text: unit, color: "#9fb8d6" },
            ticks: { color: "#9fb8d6" },
            grid: { color: "#1f3d63" },
          },
        },
        plugins: {
          legend: { display: false },
          tooltip: { backgroundColor: "#0b1f3a", borderColor: "#1f3d63", borderWidth: 1 },
        },
      },
    });
    ocultarVacio();
    caption.textContent = `${payload.puntos.length} lecturas${unit ? ` · ${unit}` : ""}`;
    showAlert(payload);
  }

  select.addEventListener("change", async () => {
    const zoneId = select.value;
    requestSequence += 1;
    const thisRequest = requestSequence;
    if (activeController) activeController.abort();
    limpiarGrafico();
    if (!zoneId) {
      status.textContent = "";
      caption.textContent = "Selecciona una zona para consultar sus datos.";
      mostrarVacio(MENSAJE_INICIAL);
      return;
    }

    activeController = new AbortController();
    status.textContent = "Consultando historial…";
    caption.textContent = select.selectedOptions[0].text;
    mostrarVacio("Consultando historial…");
    const template = app.dataset.historyUrlTemplate;
    const url = template.replace("ZONE_ID", encodeURIComponent(zoneId));
    const trendUrl = app.dataset.trendUrlTemplate.replace("ZONE_ID", encodeURIComponent(zoneId));
    try {
      const [response, trendResponse] = await Promise.all([
        fetch(url, { headers: { Accept: "application/json" }, signal: activeController.signal }),
        fetch(trendUrl, { headers: { Accept: "application/json" }, signal: activeController.signal }),
      ]);
      const payload = await response.json();
      const trend = trendResponse.ok ? await trendResponse.json() : null;
      if (!response.ok) throw new Error(payload.error || "No se pudo cargar el historial.");
      if (thisRequest !== requestSequence) return;
      if (!Array.isArray(payload.puntos)) throw new Error("La respuesta de historial no tiene el formato esperado.");

      if (trend?.patronEmpeoramiento === true) trendBadge.classList.remove("hidden");
      if (trend) {
        trendCaption.textContent = `Tendencia: ${trend.patronEmpeoramiento ? "empeoramiento" : "sin empeoramiento"} · variación mínima ${trend.variacionMinima} °C por lectura`;
      } else {
        trendCaption.textContent = "No fue posible evaluar la tendencia.";
      }

      if (payload.puntos.length === 0) {
        status.textContent = "";
        mostrarVacio("La zona no tiene lecturas en este período.");
        return;
      }
      render(payload);
      status.textContent = "Historial actualizado.";
    } catch (error) {
      if (error.name === "AbortError" || thisRequest !== requestSequence) return;
      status.textContent = "";
      mostrarVacio(error.message || "Error al consultar el historial.");
    }
  });

  document.querySelector("#refresh-zones")?.addEventListener("click", () => window.location.reload());
})();
