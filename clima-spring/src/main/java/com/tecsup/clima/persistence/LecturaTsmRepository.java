package com.tecsup.clima.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de las lecturas historicas de TSM (trayectoria de temperatura).
 */
public interface LecturaTsmRepository extends JpaRepository<LecturaTsmEntity, Long> {
}
