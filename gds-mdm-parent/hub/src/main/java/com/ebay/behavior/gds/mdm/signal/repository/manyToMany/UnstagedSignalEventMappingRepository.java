package com.ebay.behavior.gds.mdm.signal.repository.manyToMany;

import com.ebay.behavior.gds.mdm.signal.model.manyToMany.UnstagedSignalEventMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UnstagedSignalEventMappingRepository extends JpaRepository<UnstagedSignalEventMapping, Long> {

    Optional<UnstagedSignalEventMapping> findBySignalIdAndSignalVersionAndEventId(long signalId, int signalVersion, long eventId);

    List<UnstagedSignalEventMapping> findBySignalIdAndSignalVersion(long signalId, int signalVersion);

    Optional<UnstagedSignalEventMapping> findByEventId(long eventId);

    @Modifying
    @Query("DELETE FROM UnstagedSignalEventMapping mp WHERE mp.signal.id = :signalId "
            + "AND mp.signal.version = :signalVersion AND mp.event.id IN :eventIds")
    void deleteAllBySignalIdAndSignalVersionAndEventIds(@Param("signalId") long signalId,
                                                        @Param("signalVersion") int signalVersion,
                                                        @Param("eventIds") Set<Long> eventIds);
}