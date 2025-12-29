package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;

public interface UnstagedSignalRepository extends JpaRepository<UnstagedSignal, VersionedId> {

    @Query("SELECT MAX(s.version) FROM UnstagedSignal s WHERE s.id = :id")
    Optional<Integer> findLatestVersion(@Param(ID) Long id);

    @Query("SELECT s FROM UnstagedSignal s WHERE s.id = :id AND s.version = "
            + "COALESCE((SELECT MAX(s1.version) FROM UnstagedSignal s1 WHERE s1.id = :id), 0)")
    Optional<UnstagedSignal> findByIdAndLatestVersion(@Param(ID) Long id);

    @Query("SELECT s FROM UnstagedSignal s WHERE s.id IN :ids AND s.version = "
            + "(SELECT MAX(s1.version) FROM UnstagedSignal s1 WHERE s1.id = s.id)")
    List<UnstagedSignal> findAllByIdInAndLatestVersion(@Param("ids") Set<Long> ids);

    Optional<UnstagedSignal> findByIdAndVersion(long id, int version);

    List<UnstagedSignal> findByLegacyId(String legacyId);

    List<UnstagedSignal> findByPlanId(long planId); // get associated signals

    @Query("SELECT a.eventId FROM UnstagedAttribute a "
            + "JOIN UnstagedFieldAttributeMapping mp ON a.id = mp.attribute.id "
            + "JOIN UnstagedField f ON f.id = mp.field.id "
            + "WHERE f.signalId = :signalId AND f.signalVersion = :signalVersion")
    Set<Long> getConnectedEventIds(@Param("signalId") long signalId, @Param("signalVersion") long signalVersion);
}
