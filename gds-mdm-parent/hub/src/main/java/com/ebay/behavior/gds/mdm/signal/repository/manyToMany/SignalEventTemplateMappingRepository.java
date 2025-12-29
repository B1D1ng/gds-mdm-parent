package com.ebay.behavior.gds.mdm.signal.repository.manyToMany;

import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SignalEventTemplateMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SignalEventTemplateMappingRepository extends JpaRepository<SignalEventTemplateMapping, Long> {

    Optional<SignalEventTemplateMapping> findBySignalIdAndEventId(long signalId, long eventId);

    List<SignalEventTemplateMapping> findBySignalId(long signalId);

    @Modifying
    @Query("DELETE FROM SignalEventTemplateMapping mp WHERE mp.signal.id = :signalId AND mp.event.id IN :eventIds")
    void deleteAllBySignalIdAndEventIds(@Param("signalId") long signalId, @Param("eventIds") Set<Long> eventIds);
}