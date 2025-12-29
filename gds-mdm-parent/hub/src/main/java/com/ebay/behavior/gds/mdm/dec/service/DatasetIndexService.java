package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.dec.model.DatasetIndex;
import com.ebay.behavior.gds.mdm.dec.repository.DatasetIndexRepository;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.MIN_VERSION;

@Validated
@Service
public class DatasetIndexService extends AbstractIndexService<DatasetIndex> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<DatasetIndex> modelType = DatasetIndex.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private DatasetIndexRepository repository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public DatasetIndex initialize(@NotBlank String name) {
        val index =
                DatasetIndex.builder()
                        .name(name)
                        .currentVersion(MIN_VERSION)
                        .build();
        return repository.save(index);
    }
}
