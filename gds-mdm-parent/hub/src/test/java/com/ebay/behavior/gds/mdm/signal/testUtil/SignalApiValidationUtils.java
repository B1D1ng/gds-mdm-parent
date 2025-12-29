package com.ebay.behavior.gds.mdm.signal.testUtil;

import com.ebay.behavior.gds.mdm.signal.common.model.JaversIdentity;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.EventClassifier;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.Field;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.LogicalDefinition;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.SignalDefinition;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.InitialValueChange;
import org.javers.core.diff.changetype.ReferenceChange;
import org.javers.core.diff.changetype.TerminalValueChange;
import org.javers.core.diff.changetype.container.ListChange;
import org.javers.core.diff.changetype.map.MapChange;
import org.javers.core.metamodel.clazz.EntityDefinition;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

/**
 * Please manually bring legacy and new API data from both production systems
 * Legacy API: GET https://cjs-mdm.epic.vip.ebay.com/data-service/api/signal_meta/v1?latest=true
 * New API: GET https://cjsmdm1.vip.ebay.com/cjs/mdm/v1/metadata/signal?platform=CJS&useCache=false&withLegacyFormat=true&withLatestVersions=true
 * Replace existing resources in src/test/resources/signal-migration/
 * newApiFilePath = "signal-migration/newMdmSignalResponse.json"
 * oldApiFilePath = "signal-migration/legacyMdmSignalResponse.json"
 */
@Disabled
@ExtendWith(MockitoExtension.class)
class SignalApiValidationUtils {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String newApiFilePath = "signal-migration/newMdmSignalResponse.json";
    private final String oldApiFilePath = "signal-migration/legacyMdmSignalResponse.json";

    @Mock
    private ListChange listChange;

    public static final Javers JAVERS;

    private static final Predicate<Change> CHANGE_PREDICATE = change ->
            !(change instanceof InitialValueChange)
                    && !(change instanceof ReferenceChange)
                    && !(change instanceof TerminalValueChange)
                    && !(change instanceof MapChange)
                    && !(change instanceof ListChange);

    static {
        JAVERS = getJaversBuilder().build();
    }

    private static JaversBuilder getJaversBuilder() {
        val builder = JaversBuilder.javers();

        builder.registerEntity(new EntityDefinition(Field.class, "compareKey"));
        builder.registerEntity(new EntityDefinition(EventClassifier.class, "compareKey"));
        builder.registerValueObjects(SignalDefinition.class, LogicalDefinition.class);

        return builder;
    }

    public List<Change> getChanges(SignalDefinition prev, SignalDefinition curr) {
        setCompareKey(prev, LogicalDefinition::getFields);
        setCompareKey(prev, LogicalDefinition::getEventClassifiers);
        setCompareKey(curr, LogicalDefinition::getFields);
        setCompareKey(curr, LogicalDefinition::getEventClassifiers);

        val diff = JAVERS.compare(prev, curr);
        val changes = diff.getChanges(CHANGE_PREDICATE);

        if (emptyIfNull(prev.getLogicalDefinition()).size() != emptyIfNull(curr.getLogicalDefinition()).size()) {
            // validate logicalDefinition sizes
            changes.add(listChange);
        } else if (Objects.nonNull(prev.getLogicalDefinition()) & Objects.nonNull(curr.getLogicalDefinition())) {
            for (int i = 0; i < prev.getLogicalDefinition().size(); i++) {
                // validate filed sizes
                int oldSize = emptyIfNull(prev.getLogicalDefinition().get(i).getFields()).size();
                int newSize = emptyIfNull(curr.getLogicalDefinition().get(i).getFields()).size();

                if (oldSize != newSize) {
                    changes.add(listChange);
                }

                // validate event sizes
                oldSize = emptyIfNull(prev.getLogicalDefinition().get(i).getEventClassifiers()).size();
                newSize = emptyIfNull(curr.getLogicalDefinition().get(i).getEventClassifiers()).size();

                if (oldSize != newSize) {
                    changes.add(listChange);
                }
            }
        }

        return changes;
    }

    private static <M extends JaversIdentity> void setCompareKey(SignalDefinition signal, Function<LogicalDefinition, List<M>> collectionExtractor) {
        emptyIfNull(signal.getLogicalDefinition()).stream()
                .flatMap(ld -> emptyIfNull(collectionExtractor.apply(ld)).stream())
                .forEach(JaversIdentity::setCompareKey);
    }

    @Test
    @Disabled
    @SneakyThrows
    void validate() {
        val newInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(newApiFilePath);
        val oldAInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(oldApiFilePath);

        val newSignalData = objectMapper.readValue(newInputStream, SignalListWrapperData.class);
        val oldSignalData = objectMapper.readValue(oldAInputStream, SignalResponse.class);

        val newSignals = newSignalData.getRecords().stream()
                .collect(toMap(SignalDefinition::getId, Function.identity()));
        val oldSignals = oldSignalData.getData().getRecords().stream()
                .collect(toMap(SignalDefinition::getId, Function.identity()));

        int missingSignalCount = 0;
        int mismatchSignalCount = 0;
        System.out.println();

        for (val oldEntry : oldSignals.entrySet()) {
            val legacyId = oldEntry.getKey();
            val oldSignal = oldEntry.getValue();
            val newSignal = newSignals.get(legacyId);

            if (newSignal == null) {
                missingSignalCount++;
                val message = String.format("New signal definition not found for [legacyId: %s, ver: %d]", legacyId, oldSignal.getVersion());
                System.out.println(message);
                System.out.println();
                continue;
            }

            val changes = getChanges(oldSignal, newSignal);

            if (!changes.isEmpty()) {
                mismatchSignalCount++;
                val message = String.format("Signal change detected for [legacyId: %s, ver: %d]", legacyId, newSignal.getVersion());
                System.out.println(message);
                changes.forEach(System.out::println);
                System.out.println();
            }
        }

        System.out.println("missingSignalCount " + missingSignalCount + ", mismatchSignalCount " + mismatchSignalCount);
    }
}
