package com.ebay.behavior.gds.mdm.contract.repository;

import com.ebay.behavior.gds.mdm.contract.model.StreamingConfig;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface StreamingConfigRepository extends JpaRepository<StreamingConfig, Long> {
    @Modifying
    void deleteAllByComponentId(Long componentId);

    /**
     * Check if any topics already exist for any streaming config with the given env, stream_name, and component type,
     * excluding the specified config ID.
     * Used to validate unique topics across configs with same env/stream_name for components of the same type.
     *
     * @param topics      the topics to check
     * @param env         the environment
     * @param streamName  the stream name
     * @param componentId the component ID to get the type from
     * @param configId    the config ID to exclude from the check (null for new configs)
     * @return true if any topic exists for another config with the same component type, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(sc) > 0 THEN true ELSE false END FROM StreamingConfig sc JOIN sc.topics t JOIN sc.component c "
            + "WHERE t IN :topics AND sc.env = :env "
            + "AND (:streamName IS NULL OR sc.streamName = :streamName) "
            + "AND c.type = (SELECT comp.type FROM Component comp WHERE comp.id = :componentId) "
            + "AND (:configId IS NULL OR sc.id != :configId)")
    boolean existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(@Param("topics") Collection<String> topics,
                                                                @Param("env") String env,
                                                                @Param("streamName") String streamName,
                                                                @Param("componentId") Long componentId,
                                                                @Param("configId") Long configId);
}