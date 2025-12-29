package com.ebay.behavior.gds.mdm.signal.repository.manyToMany;

import com.ebay.behavior.gds.mdm.signal.model.manyToMany.StagedSignalEventMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface StagedSignalEventMappingRepository
        extends JpaRepository<StagedSignalEventMapping, Long>, StagedSignalEventMappingRepositoryCustom {

    // a list is a workaround for a known issue with duplicated mappings (should be Optional)
    List<StagedSignalEventMapping> findBySignalIdAndSignalVersionAndEventId(long signalId, int signalVersion, long eventId);

    List<StagedSignalEventMapping> findBySignalIdAndSignalVersion(long signalId, int signalVersion);

    @Modifying
    @Query("DELETE FROM StagedSignalEventMapping mp WHERE mp.signal.id = :signalId "
            + "AND mp.signal.version = :signalVersion AND mp.event.id IN :eventIds")
    void deleteAllBySignalIdAndSignalVersionAndEventIds(@Param("signalId") long signalId,
                                                        @Param("signalVersion") int signalVersion,
                                                        @Param("eventIds") Set<Long> eventIds);
}