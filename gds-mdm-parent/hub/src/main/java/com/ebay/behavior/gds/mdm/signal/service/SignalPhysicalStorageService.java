package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalPhysicalStorage;
import com.ebay.behavior.gds.mdm.signal.repository.SignalPhysicalStorageRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class SignalPhysicalStorageService
        extends AbstractCrudService<SignalPhysicalStorage>
        implements CrudService<SignalPhysicalStorage> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<SignalPhysicalStorage> modelType = SignalPhysicalStorage.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private SignalPhysicalStorageRepository repository;

    @Autowired
    private StagedSignalService signalService;

    @Lazy
    @Autowired
    private SignalTypeLookupService signalTypeLookupService;

    @Override
    public Page<SignalPhysicalStorage> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public Set<SignalPhysicalStorage> getAll() {
        return Set.copyOf(repository.findAll());
    }

    @Transactional(readOnly = true)
    public SignalPhysicalStorage getByKafkaTopicAndEnvironment(@NotBlank String kafkaTopic, @NotNull Environment env) {
        return repository.findByKafkaTopicAndEnvironment(kafkaTopic, env)
                .orElseThrow(() -> new DataNotFoundException(getModelType(), kafkaTopic, env.name()));
    }

    @Transactional(readOnly = true)
    public SignalPhysicalStorage getBySignalId(@Valid @NotNull VersionedId signalId) {
        val signal = signalService.getById(signalId);

        val storages = signalTypeLookupService.getByName(signal.getType())
                .getPhysicalStorages().stream()
                .filter(storage -> storage.getEnvironment().equals(signal.getEnvironment()))
                .collect(Collectors.toSet());

        if (storages.isEmpty()) {
            throw new DataNotFoundException(
                    SignalPhysicalStorage.class,
                    String.format("No physical storage found for signal type %s and environment %s",
                            signal.getType(), signal.getEnvironment()));
        }

        if (storages.size() > 1) {
            throw new IllegalStateException(
                    String.format("Multiple physical storages found for signal type %s and environment %s",
                            signal.getType(), signal.getEnvironment()));
        }

        return storages.iterator().next();
    }

    @Override
    public SignalPhysicalStorage getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }
}
