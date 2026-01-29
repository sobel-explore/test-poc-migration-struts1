package com.doctordoc.repository;

import com.doctordoc.entity.Bestellungen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Bestellungen (orders) entity operations.
 * Mapped to database schema from master_dump.sql.
 */
@Repository
public interface BestellungenRepository extends JpaRepository<Bestellungen, Long> {

    List<Bestellungen> findByKontoId(Long kontoId);

    Page<Bestellungen> findByKontoIdOrderByOrderdateDesc(Long kontoId, Pageable pageable);

    List<Bestellungen> findByBenutzerId(Long benutzerId);

    List<Bestellungen> findByKontoIdAndErledigt(Long kontoId, Boolean erledigt);

    List<Bestellungen> findByKontoIdAndState(Long kontoId, String state);

    Optional<Bestellungen> findByTrackingnrAndGbvnr(String trackingnr, String gbvnr);

    @Query("SELECT COUNT(b) FROM Bestellungen b WHERE b.kontoId = :kontoId " +
           "AND YEAR(b.orderdate) = YEAR(CURRENT_DATE)")
    long countOrdersThisYearForKonto(@Param("kontoId") Long kontoId);

    @Query("SELECT COUNT(b) FROM Bestellungen b WHERE b.benutzerId = :userId " +
           "AND b.kontoId = :kontoId " +
           "AND YEAR(b.orderdate) = YEAR(CURRENT_DATE)")
    long countOrdersPerUserThisYear(@Param("userId") Long userId, @Param("kontoId") Long kontoId);

    @Query("SELECT b FROM Bestellungen b WHERE b.kontoId = :kontoId " +
           "AND b.orderdate >= :startDate ORDER BY b.orderdate DESC")
    List<Bestellungen> findByKontoIdAndOrderdateAfter(@Param("kontoId") Long kontoId,
                                                       @Param("startDate") LocalDateTime startDate);

    @Query("SELECT b FROM Bestellungen b WHERE b.kontoId = :kontoId " +
           "AND (:state IS NULL OR b.state = :state) " +
           "ORDER BY b.orderdate DESC")
    Page<Bestellungen> findByKontoIdAndState(@Param("kontoId") Long kontoId,
                                              @Param("state") String state,
                                              Pageable pageable);

    @Query("SELECT b FROM Bestellungen b WHERE b.kontoId = :kontoId " +
           "AND (LOWER(b.artikeltitel) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(b.zeitschrift) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(b.autor) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR b.issn LIKE CONCAT('%', :search, '%'))")
    Page<Bestellungen> searchOrders(@Param("kontoId") Long kontoId,
                                    @Param("search") String search,
                                    Pageable pageable);
}
