(() => {
  const key = "clima-admin-theme";
  const root = document.documentElement;
  root.dataset.theme = localStorage.getItem(key) || "light";
  document.addEventListener("submit", event => {
    if (event.target.matches(".global-actions form")) {
      const next = root.dataset.theme === "dark" ? "light" : "dark";
      localStorage.setItem(key, next);
    }
  });
})();
