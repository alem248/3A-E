(() => {
  const root = document.documentElement;
  const button = document.querySelector("[data-auth-theme-toggle]");
  const storageKey = "clima-admin-theme";

  const applyTheme = theme => {
    root.dataset.theme = theme;
    const isDark = theme === "dark";
    button.textContent = isDark ? "☀ Modo claro" : "☾ Modo oscuro";
    button.setAttribute("aria-pressed", String(isDark));
  };

  applyTheme(localStorage.getItem(storageKey) || "light");

  button.addEventListener("click", () => {
    const nextTheme = root.dataset.theme === "dark" ? "light" : "dark";
    localStorage.setItem(storageKey, nextTheme);
    applyTheme(nextTheme);
  });

  const notice = document.querySelector("[data-logout-notice]");
  if (notice) {
    const url = new URL(window.location.href);
    url.searchParams.delete("logged_out");
    window.history.replaceState({}, document.title, url);
  }
})();
