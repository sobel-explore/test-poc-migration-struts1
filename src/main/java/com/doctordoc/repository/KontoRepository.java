package com.doctordoc.repository;

import com.doctordoc.entity.Konto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Konto entity operations.
 * Replaces JDBC operations from the original Konto.java
 */
@Repository
public interface KontoRepository extends JpaRepository<Konto, Long> {

    List<Konto> findAllByOrderByKontotypDescBibliotheksnameAsc();

    List<Konto> findByKontotypGreaterThanOrderByKontotypDescBibliotheksnameAsc(int kontotyp);

    List<Konto> findByKontostatus(boolean status);

    List<Konto> findByFaxnoIsNotNull();

    @Query("SELECT k FROM Konto k WHERE k.expdate < :expireDate")
    List<Konto> findExpireKontos(@Param("expireDate") LocalDate expireDate);

    @Query(value = "SELECT k.* FROM konto k " +
           "INNER JOIN v_konto_benutzer vkb ON k.KID = vkb.KID " +
           "WHERE vkb.UID = :userId", nativeQuery = true)
    List<Konto> findKontosForBenutzer(@Param("userId") Long userId);
}
