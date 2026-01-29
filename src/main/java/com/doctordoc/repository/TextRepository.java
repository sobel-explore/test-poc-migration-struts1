package com.doctordoc.repository;

import com.doctordoc.entity.Text;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Text entity operations.
 */
@Repository
public interface TextRepository extends JpaRepository<Text, Long> {

    List<Text> findByTexttyp(String texttyp);

    List<Text> findByKontoIdAndTexttyp(Long kontoId, String texttyp);

    Optional<Text> findByKontoIdAndTexttypAndInhalt(Long kontoId, String texttyp, String inhalt);

    @Query("SELECT t FROM Text t WHERE t.texttyp = :texttyp AND (t.kontoId IS NULL OR t.kontoId = 0)")
    List<Text> findGlobalByTexttyp(@Param("texttyp") String texttyp);

    @Query("SELECT t FROM Text t WHERE t.texttyp = 'gtc_version' ORDER BY t.id DESC")
    Optional<Text> findCurrentGtcVersion();
}
