package com.doctordoc.repository;

import com.doctordoc.entity.Lieferant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Lieferant (supplier) entity operations.
 * Mapped to database schema from master_dump.sql.
 */
@Repository
public interface LieferantRepository extends JpaRepository<Lieferant, Long> {

    List<Lieferant> findByKontoIdIsNullOrderByLieferantAsc();

    List<Lieferant> findByKontoIdOrderByLieferantAsc(Long kontoId);

    List<Lieferant> findByCountryCode(String countryCode);

    List<Lieferant> findByAllgemeinTrueOrKontoIdOrderByLieferantAsc(Long kontoId);

    List<Lieferant> findByAllgemeinTrueOrKontoId(Long kontoId);
}
