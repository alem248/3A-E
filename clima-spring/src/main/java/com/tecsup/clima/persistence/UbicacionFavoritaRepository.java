package com.tecsup.clima.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UbicacionFavoritaRepository extends JpaRepository<UbicacionFavoritaEntity, Integer> {

    /** Resumen anonimo: nombre de lugar y cuantos usuarios lo guardaron (sin identificar usuarios). */
    interface RankingFavorita {
        String getNombre();

        Long getVeces();
    }

    List<UbicacionFavoritaEntity> findByUsuarioOrderByIdAsc(UsuarioEntity usuario);

    Optional<UbicacionFavoritaEntity> findByIdAndUsuario(Integer id, UsuarioEntity usuario);

    @Query("select min(f.etiqueta) as nombre, count(f) as veces from UbicacionFavoritaEntity f "
            + "group by lower(trim(f.etiqueta)) order by count(f) desc, min(f.etiqueta) asc")
    List<RankingFavorita> rankingGlobal();
}
