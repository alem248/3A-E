(() => {
  const nombre = "clima-tema";
  const leerCookie = () => {
    const fila = document.cookie.split("; ").find(item => item.startsWith(nombre + "="));
    return fila ? fila.split("=")[1] : null;
  };
  const root = document.documentElement;
  root.dataset.theme = leerCookie() || "dark";
  document.addEventListener("submit", event => {
    if (event.target.matches(".global-actions form")) {
      const siguiente = root.dataset.theme === "dark" ? "light" : "dark";
      document.cookie = nombre + "=" + siguiente + "; path=/; max-age=31536000; SameSite=Lax";
    }
  });
})();
