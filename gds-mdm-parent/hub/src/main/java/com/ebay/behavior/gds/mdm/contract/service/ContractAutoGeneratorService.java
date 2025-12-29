package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.model.CompletionStatus;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.contract.model.HiveConfig;
import com.ebay.behavior.gds.mdm.contract.model.HiveSource;
import com.ebay.behavior.gds.mdm.contract.model.HiveStorage;
import com.ebay.behavior.gds.mdm.contract.model.LdmViewSink;
import com.ebay.behavior.gds.mdm.contract.model.Transformation;
import com.ebay.behavior.gds.mdm.contract.model.Transformer;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.contract.model.manyToMany.ContractSignalMapping;
import com.ebay.behavior.gds.mdm.contract.repository.manyToMany.ContractSignalMappingRepository;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldSignalMapping;
import com.ebay.behavior.gds.mdm.dec.model.enums.SignalType;
import com.ebay.behavior.gds.mdm.dec.repository.LdmFieldSignalMappingRepository;
import com.ebay.behavior.gds.mdm.dec.service.LdmEntityService;
import com.ebay.behavior.gds.mdm.dec.service.LdmFieldService;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalPhysicalStorage;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTypeLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.service.DomainLookupService;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.PlatformLookupService;
import com.ebay.behavior.gds.mdm.signal.service.SignalPhysicalStorageService;
import com.ebay.behavior.gds.mdm.signal.service.SignalTypeLookupService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.signal.util.ServiceUtils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.contract.util.Constants.CONTRACT_AUTO_GEN_PREFIX;
import static com.ebay.behavior.gds.mdm.contract.util.Constants.DEFAULT_DB_NAME;
import static com.ebay.behavior.gds.mdm.contract.util.Constants.HIVE_PLATFORM;
import static com.ebay.behavior.gds.mdm.contract.util.Constants.READABLE_NAMES;
import static com.ebay.behavior.gds.mdm.contract.util.Constants.SIGNAL_VIEW_NAME_SUFFIX;
import static com.ebay.behavior.gds.mdm.contract.util.ContractUtils.getContractSink;
import static com.ebay.behavior.gds.mdm.contract.util.ContractUtils.getContractSource;
import static com.ebay.behavior.gds.mdm.contract.util.ContractUtils.getHiveConfigByEnv;
import static com.ebay.behavior.gds.mdm.contract.util.ValidationUtils.validateHiveContract;
import static com.ebay.behavior.gds.mdm.signal.model.SpecialPlanType.CONTRACT_AUTO_GEN;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.HIVE_EVENT;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.createImportPlanIfAbsent;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.createPlatformIfAbsent;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Service responsible for auto-generating signals, LDM fields, and related resources from contracts.
 * This service handles the creation and synchronization of derived data artifacts for batch (Hive) contracts.
 */
@Slf4j
@Service
public class ContractAutoGeneratorService {

    @Autowired
    private UnstagedSignalService signalService;

    @Autowired
    private UnstagedFieldService fieldService;

    @Autowired
    private SignalPhysicalStorageService physicalStorageService;

    @Autowired
    private SignalTypeLookupService lookupService;

    @Autowired
    private ContractSignalMappingRepository mappingRepository;

    @Autowired
    private PlanService planService;

    @Autowired
    private DomainLookupService domainService;

    @Autowired
    private LdmEntityService ldmEntityService;

    @Autowired
    private LdmFieldService ldmFieldService;

    @Autowired
    private LdmFieldSignalMappingRepository signalMappingRepository;

    @Autowired
    private PlatformLookupService platformLookupService;

    /**
     * Creates or updates an UnstagedSignal and all related resources (fields, storages, mappings) for a contract.
     *
     * @param contract    the contract to generate resources from
     * @param environment the target environment (STAGING or PRODUCTION)
     * @return the created or updated UnstagedSignal
     */
    public UnstagedSignal upsertUnstagedSignalsForContract(@NotNull @Valid UnstagedContract contract, Environment environment) {
        // Hive source only has one routing and one source.
        validateHiveContract(contract);
        val mapping = mappingRepository.findByContractIdAndContractVersion(contract.getId(), contract.getVersion());
        UnstagedSignal signal;
        String oldType = null;
        if (mapping.isPresent()) {
            signal = mapping.get().getSignal();
            oldType = signal.getType();
            enrichSignal(contract, signal);
            signalService.update(signal);
        } else {
            val platform = createPlatformIfAbsent(HIVE_PLATFORM, READABLE_NAMES.get(HIVE_PLATFORM), platformLookupService);
            val planId = createImportPlanIfAbsent(CONTRACT_AUTO_GEN, platform, planService, domainService);
            signal = UnstagedSignal.builder()
                    .completionStatus(CompletionStatus.COMPLETED)
                    .platformId(platform.getId())
                    .fields(Sets.newHashSet())
                    .build();

            signal.setPlanId(planId);

            enrichSignal(contract, signal);
            signalService.create(signal);
            mappingRepository.save(new ContractSignalMapping(contract, signal));
        }

        upsertStorageMappings(signal, contract, environment, oldType);
        upsertFields(contract, signal);
        return signal;
    }

    private void enrichSignal(UnstagedContract contract, UnstagedSignal signal) {
        signal.setName(CONTRACT_AUTO_GEN_PREFIX + contract.getName());
        signal.setEnvironment(contract.getEnvironment());
        signal.setDescription(contract.getDescription());
        signal.setDomain(contract.getDomain());
        signal.setType(signal.getName());
        signal.setVersion(contract.getVersion());
        signal.setOwners(contract.getOwners());
        signal.setDataSource(UdcDataSourceType.STAGED);
        signal.setUpdateBy(null); // Leverage onUpdate of AbstractVersionedAuditable to set the updateBy and updateDate
        signal.setUpdateDate(null);
    }

    private void upsertStorageMappings(UnstagedSignal signal, UnstagedContract contract, Environment environment, String oldType) {
        val hiveSource = getContractSource(contract)
                .filter(cp -> cp instanceof HiveSource)
                .map(cp -> (HiveSource) cp)
                .orElseThrow();
        val hiveStorage = ofNullable(getHiveConfigByEnv(hiveSource, environment))
                .map(HiveConfig::getHiveStorage)
                .orElseThrow();

        if (isNotBlank(oldType)) {
            val lookupOpt = lookupService.findByName(oldType);
            if (lookupOpt.isPresent()) {
                updateMappings(lookupOpt.get(), signal, environment, hiveStorage);
                return;
            }
        }
        createMappings(signal, environment, hiveSource, hiveStorage);
    }

    private void updateMappings(SignalTypeLookup lookup, UnstagedSignal signal, Environment environment, HiveStorage storage) {
        if (!StringUtils.equals(lookup.getName(), signal.getType())) {
            lookup.setName(signal.getType());
            lookup.setReadableName(signal.getType());
            lookupService.update(lookup);
        }
        Hibernate.initialize(lookup.getPhysicalStorages());
        val physicalStorage = ofNullable(lookup.getPhysicalStorages())
                .flatMap(pss ->
                        pss.stream().filter(ps -> ps.getEnvironment() == environment).findFirst());
        if (physicalStorage.isPresent()) {
            val ps = physicalStorage.get();
            ps.setDescription(signal.getDescription());
            ps.setHiveTableName(storage.getTableName());
            ps.setDoneFilePath(storage.getDoneFilePath());
            physicalStorageService.update(ps);
        } else {
            createPhysicalStorage(signal, environment, storage, lookup);
        }
    }

    private void createPhysicalStorage(UnstagedSignal signal, Environment environment, HiveStorage storage, SignalTypeLookup lookup) {
        val newPhysicalStorage = SignalPhysicalStorage.builder()
                .description(signal.getDescription())
                .environment(environment)
                .hiveTableName(DEFAULT_DB_NAME + "." + storage.getTableName() + SIGNAL_VIEW_NAME_SUFFIX)
                .doneFilePath(storage.getDoneFilePath())
                .build();
        physicalStorageService.create(newPhysicalStorage);
        lookupService.createPhysicalStorageMapping(lookup.getId(), newPhysicalStorage.getId());
    }

    private void createMappings(UnstagedSignal signal, Environment environment, HiveSource source, HiveStorage storage) {
        val lookup = SignalTypeLookup.builder()
                .name(signal.getType())
                .platformId(signal.getPlatformId())
                .readableName(signal.getType())
                .logicalDataEntity(source.getEntityType())
                .build();
        lookupService.create(lookup);
        createPhysicalStorage(signal, environment, storage, lookup);
    }

    private void upsertFields(UnstagedContract contract, UnstagedSignal signal) {
        val fields = upsertSignalFields(contract, signal);
        signal.setFields(fields);
        val sinkOpt = getContractSink(contract);
        if (!sinkOpt.map(sk -> sk instanceof LdmViewSink).orElse(false)) {
            return;
        }
        val sink = (LdmViewSink) sinkOpt.get();
        val ldm = ldmEntityService.getByIdCurrentVersion(sink.getViewId());
        upsertLdmFields(fields, signal, ldm);
    }

    private void upsertLdmFields(Set<UnstagedField> fields, UnstagedSignal signal, LdmEntity ldm) {
        Hibernate.initialize(ldm.getFields());
        val existing = ldm.getFields();
        Map<String, LdmField> ldmFields = Maps.newHashMap();
        if (isNotEmpty(existing)) {
            existing.forEach(lf -> ldmFields.put(lf.getName(), lf));
        }
        Set<LdmField> toSave = Sets.newHashSet();
        Set<LdmField> toUpdate = Sets.newHashSet();

        fields.stream().map(sf -> toLdmField(sf, signal, ldm))
                .forEach(lf -> {
                    if (ldmFields.containsKey(lf.getName())) {
                        val ext = ldmFields.get(lf.getName());
                        ServiceUtils.copyModelProperties(lf, ext);
                        ext.setUpdateBy(null);
                        ext.setUpdateDate(null);
                        upsertSignalFieldsMapping(lf, ext);
                        toUpdate.add(ext);
                    } else {
                        toSave.add(lf);
                    }
                });
        if (isNotEmpty(toUpdate)) {
            ldmFieldService.updateFields(ldm.getId(), ldm, toUpdate);
        }
        if (isNotEmpty(toSave)) {
            ldmFieldService.saveAll(ldm.getId(), toSave);
        }
    }

    private void upsertSignalFieldsMapping(LdmField field, LdmField ext) {
        val target = field.getSignalMapping().stream().findFirst().orElse(null);
        if (isNull(target)) {
            return;
        }
        val signalMapping = signalMappingRepository.findByLdmFieldId(ext.getId());
        if (isEmpty(signalMapping)) {
            target.setLdmFieldId(ext.getId());
            signalMappingRepository.save(target);
            return;
        }
        val existingMapping = signalMapping.iterator().next();
        ServiceUtils.copyModelProperties(target, existingMapping);
        signalMappingRepository.save(existingMapping);
    }

    private LdmField toLdmField(UnstagedField field, UnstagedSignal signal, LdmEntity ldm) {
        return LdmField.builder()
                .ldmEntityId(ldm.getId())
                .ldmVersion(ldm.getVersion())
                .name(field.getName())
                .hierarchicalName(field.getName())
                .description(field.getDescription())
                .dataType(field.getJavaType().toString())
                .signalMapping(Sets.newHashSet(LdmFieldSignalMapping.builder()
                        .signalDefinitionId(signal.getId())
                        .signalVersion(signal.getVersion())
                        .signalName(signal.getName())
                        .signalType(SignalType.ODS)
                        .signalFieldName(field.getName())
                        .signalFieldExpression(field.getName())
                        .signalFieldExpressionOffline(field.getName())
                        .build()))
                .build();
    }

    private Set<UnstagedField> upsertSignalFields(UnstagedContract contract, UnstagedSignal signal) {
        val transformerOpt = contract.getRoutings().iterator().next().getComponentChain().stream()
                .filter(Transformer.class::isInstance)
                .map(cp -> (Transformer) cp)
                .filter(tf -> isNotEmpty(tf.getTransformations()))
                .findFirst();

        if (transformerOpt.isEmpty()) {
            return new HashSet<>();
        }

        Map<String, UnstagedField> fields = Maps.newHashMap();
        Hibernate.initialize(signal.getFields());
        val existing = signal.getFields();

        if (isNotEmpty(existing)) {
            existing.forEach(uf -> fields.put(uf.getName(), uf));
        }

        transformerOpt.get().getTransformations().stream()
                .map(tfm -> toUnstagedField(tfm, signal))
                .forEach(uf -> {
                    val name = uf.getName();
                    if (fields.containsKey(name)) {
                        val ext = fields.get(name);
                        ServiceUtils.copyModelProperties(uf, ext);
                        ext.setUpdateBy(null);
                        ext.setUpdateDate(null);
                        fieldService.update(ext);
                    } else {
                        fieldService.create(uf, null);
                        fields.put(name, uf);
                    }
                });

        return Sets.newHashSet(fields.values());
    }

    private UnstagedField toUnstagedField(Transformation tf, UnstagedSignal signal) {
        return UnstagedField.builder()
                .name(tf.getField())
                .description(tf.getDescription())
                .tag(tf.getField())
                // TODO we need unify field data type among contract, signal and LDM, ticket: https://jirap.sddz.ebay.com/browse/CJSONB-748
                .javaType(JavaType.STRING)
                .avroSchema(JavaType.STRING.toSchema())
                .expression(tf.getExpression())
                .expressionType(tf.getExpressionType())
                .isMandatory(false)
                .signalId(signal.getId())
                .signalVersion(signal.getVersion())
                .eventTypes(HIVE_EVENT)
                .build();
    }
}
