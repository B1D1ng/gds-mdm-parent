package com.ebay.behavior.gds.mdm.signal.repository.manyToMany;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.model.SignalChildId;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.val;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class StagedSignalEventMappingRepositoryCustomImpl implements StagedSignalEventMappingRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Set<SignalChildId> findEventIdsBySignalIds(Set<VersionedId> signalIds) {
        if (signalIds.isEmpty()) {
            return Set.of();
        }

        // Construct the query dynamically since JQL does not support IN clause with composite keys
        var queryStr = "SELECT new com.ebay.behavior.gds.mdm.signal.model.SignalChildId(s.signal.id, s.signal.version, s.event.id) "
                + "FROM StagedSignalEventMapping s WHERE ";
        queryStr += signalIds.stream()
                .map(id -> "(s.signal.id = " + id.getId() + " AND s.signal.version = " + id.getVersion() + ")")
                .collect(Collectors.joining(" OR "));

        val query = entityManager.createQuery(queryStr, SignalChildId.class);
        return new HashSet<>(query.getResultList());
    }
}
