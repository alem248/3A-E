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
  let chart;
  let activeController;
  let requestSequence = 0;

  regionSelect.addEventListener("change", () => {
    const region = regionSelect.value;
    const catalog = JSON.parse(document.querySelector("#zone-catalog")?.textContent || "[]");
    const available = catalog.filter(zone => zone.departamento === region);
    select.replaceChildren(new Option(available.length ? "Selecciona una zona…" : "Sin cobertura en esta región", ""));
    available.forEach(zone => select.add(new Option(zone.nombre, zone.id)));
    select.disabled = available.length === 0;
    if (available.length === 1) {
      select.value = String(available[0].id);
      select.dispatchEvent(new Event("change"));
    } else {
      requestSequence += 1;
      if (activeController) activeController.abort();
      if (chart) { chart.destroy(); chart = undefined; }
      alertBadge.classList.add("d-none");
      trendBadge.classList.add("hidden");
      status.textContent = available.length ? "Selecciona una zona para consultar sus datos." : "Esta región aún no cuenta con zonas conectadas a la API climática.";
    }
  });

  function showAlert(payload) {
    const threshold = Number(payload.umbralAlerta);
    const hasThreshold = Number.isFinite(threshold);
    const exceeds = hasThreshold && payload.puntos.some(point => Number(point.valor) >= threshold);
    const isActive = payload.alerta === true || exceeds;
    alertBadge.classList.toggle("d-none", !isActive);
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
          borderColor: "#0d6efd", backgroundColor: "rgba(13,110,253,.12)",
          borderWidth: 2, pointRadius: 2, fill: true, tension: .25,
        }],
      },
      options: {
        responsive: true, maintainAspectRatio: false, animation: false,
        interaction: { intersect: false, mode: "index" },
        scales: { y: { title: { display: Boolean(unit), text: unit } } },
        plugins: { legend: { display: false } },
      },
    });
    caption.textContent = `${payload.puntos.length} lecturas${unit ? ` · ${unit}` : ""}`;
    showAlert(payload);
  }

  select.addEventListener("change", async () => {
    const zoneId = select.value;
    requestSequence += 1;
    const thisRequest = requestSequence;
    if (activeController) activeController.abort();
    if (chart) { chart.destroy(); chart = undefined; }
    alertBadge.classList.add("d-none");
    trendBadge.classList.add("hidden");
    if (!zoneId) {
      status.textContent = "Selecciona una zona para consultar sus datos.";
      caption.textContent = "Selecciona una zona para consultar sus datos.";
      return;
    }

    activeController = new AbortController();
    status.textContent = "Consultando historial…";
    caption.textContent = select.selectedOptions[0].text;
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
      render(payload);
      if (trend?.patronEmpeoramiento === true) trendBadge.classList.remove("hidden");
      if (trend) trendCaption.textContent = `Tendencia: ${trend.patronEmpeoramiento ? "empeoramiento" : "sin empeoramiento"} · variación mínima ${trend.variacionMinima} °C por lectura`;
      else trendCaption.textContent = "No fue posible evaluar la tendencia.";
      status.textContent = payload.puntos.length ? "Historial actualizado." : "La zona no tiene lecturas en este período.";
    } catch (error) {
      if (error.name === "AbortError" || thisRequest !== requestSequence) return;
      status.textContent = error.message || "Error al consultar el historial.";
      alertBadge.classList.add("d-none");
    }
  });
  document.querySelector("#refresh-zones")?.addEventListener("click", () => window.location.reload());
})();
