package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.exception.DuplicateResourceException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.contract.model.StreamingConfig;
import com.ebay.behavior.gds.mdm.contract.repository.StreamingConfigRepository;

import com.google.common.annotations.VisibleForTesting;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Service
@Validated
public class StreamingConfigService
        extends AbstractCrudService<StreamingConfig> {

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private StreamingConfigRepository repository;

    @Override
    protected Class<StreamingConfig> getModelType() {
        return StreamingConfig.class;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StreamingConfig> getAll(@Valid @NotNull Search search) {
        throw new UnsupportedOperationException("Not need to StreamingConfig ");
    }

    @Override
    public StreamingConfig getByIdWithAssociations(long id) {
        throw new UnsupportedOperationException("Not need to StreamingConfig ");
    }

    /**
     * Validates that topics are unique across streaming configs with the same env and stream_name.
     * This method should be called before creating or updating a StreamingConfig.
     *
     * @param config the streaming config to validate
     * @throws DuplicateResourceException if any topic is already used by another config with same env/stream_name
     */
    @VisibleForTesting
    public void validateUniqueTopics(StreamingConfig config) {
        if (isEmpty(config.getTopics())) {
            return;
        }

        boolean exists = repository.existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                config.getTopics(),
                config.getEnv(),
                config.getStreamName(),
                config.getComponentId(),
                config.getId() // null for new configs, actual ID for updates
        );

        if (exists) {
            throw new DuplicateResourceException(config.getTopics(), config.getEnv(), config.getStreamName());
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public StreamingConfig create(@Valid @NotNull StreamingConfig config) {
        validateUniqueTopics(config);
        return super.create(config);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public StreamingConfig update(@Valid @NotNull StreamingConfig config) {
        // TODO we need wait CJSONB-826 finishing merging existing duplicate data then enable this validation during update.
        //        validateUniqueTopics(config);
        return super.update(config);
    }
}