package com.doctordoc.repository;

import com.doctordoc.entity.Benutzer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Benutzer entity operations.
 * Replaces JDBC operations from the original AbstractBenutzer.java
 */
@Repository
public interface BenutzerRepository extends JpaRepository<Benutzer, Long> {

    Optional<Benutzer> findByEmail(String email);

    List<Benutzer> findAllByEmail(String email);

    Optional<Benutzer> findByEmailAndLoginoptTrue(String email);

    @Query("SELECT b FROM Benutzer b WHERE b.email = :email AND b.password = :password")
    Optional<Benutzer> findByEmailAndPassword(@Param("email") String email, @Param("password") String password);

    @Query("SELECT b FROM Benutzer b JOIN b.kontos k WHERE k.id = :kontoId ORDER BY b.name, b.vorname")
    List<Benutzer> findByKontoId(@Param("kontoId") Long kontoId);

    @Query("SELECT b FROM Benutzer b JOIN b.kontos k WHERE k.id = :kontoId AND b.email = :email")
    List<Benutzer> findByKontoIdAndEmail(@Param("kontoId") Long kontoId, @Param("email") String email);

    @Query("SELECT b FROM Benutzer b " +
           "JOIN b.kontos k " +
           "WHERE b.email = :email AND b.password = :password " +
           "AND b.kontostatus = true AND k.kontostatus = true " +
           "AND ((b.loginopt = true AND k.userlogin = true) OR b.rechte >= 2)")
    List<Benutzer> findLoginAllowed(@Param("email") String email, @Param("password") String password);

    @Query("SELECT COUNT(b) FROM Benutzer b WHERE b.categoryId = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);
}
