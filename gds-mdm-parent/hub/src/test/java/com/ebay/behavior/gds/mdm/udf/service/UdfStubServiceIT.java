package com.ebay.behavior.gds.mdm.udf.service;

import com.ebay.behavior.gds.mdm.udf.common.model.Udf;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStub;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStubVersions;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfStubLanguage;
import com.ebay.behavior.gds.mdm.udf.testUtil.TestModelUtils;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.csvStringToSet;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UdfStubServiceIT {
    private final Pageable pageable = PageRequest.of(0, 10);

    @Autowired
    private UdfService udfService;

    @Autowired
    private UdfStubService udfStubService;

    @Autowired
    private UdfStubVersionService udfStubVersionService;

    private UdfStub udfStub;

    long udfId;

    @BeforeEach
    void setUp() {
        Udf udf = TestModelUtils.udf();
        udf = udfService.create(udf);
        udf = udfService.getById(udf.getId());
        udfId = udf.getId();

        udfStub = TestModelUtils.udfStub(udfId);
        udfStub = udfStubService.create(udfStub);
    }

    @Test
    void create() {
        assertThat(udfStub.getId()).isNotNull();
        assertThat(udfStub.getStubName()).isNotNull();
    }

    @Test
    void createWithExistingName() {
        UdfStubVersions udfStubVersion = TestModelUtils.udfStubVersion(udfStub.getId());
        udfStubVersionService.create(udfStubVersion);
        val nameSet = csvStringToSet(udfStub.getStubName());
        UdfStub stub = TestModelUtils.udfStub(udfId);
        stub.setLanguage(UdfStubLanguage.FLINK_SQL);
        UdfStub newUdfStub = udfStubService.create(stub, nameSet);
        assertThat(newUdfStub.getId()).isNotEqualTo(udfStub.getId());
    }

    @Test
    void createWithError() {
        UdfStubVersions udfStubVersion = TestModelUtils.udfStubVersion(udfStub.getId());
        udfStubVersionService.create(udfStubVersion);
        val nameSet = csvStringToSet(udfStub.getStubName());
        UdfStub stub = TestModelUtils.udfStub(udfId);
        stub.setLanguage(UdfStubLanguage.FLINK_SQL);
        stub.setStubParameters("abc");
        assertThatThrownBy(() -> udfStubService.create(stub, nameSet)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getByNames() {
        Set<String> names = new HashSet<>(List.of("initial_test_udf_stub"));
        List<UdfStub> udfStubList = udfStubService.getByNames(names, true);
        assertThat(udfStubList).isNotEmpty();
        assertThat(udfStubList.get(0).getUdfStubVersions().stream().toList().get(0).getStubVersion()).isNotNull();
    }

    @Test
    void getByIds() {
        Set<Long> ids = new HashSet<>(List.of(1L));
        List<UdfStub> udfStubList = udfStubService.getByIds(ids, true);
        assertThat(udfStubList).isNotEmpty();
        assertThat(udfStubList.get(0).getUdfStubVersions().stream().toList().get(0).getStubVersion()).isNotNull();
    }

    @Test
    void update() {
        udfStubService.update(udfStub, 1L);
    }

    @Test
    void updateWithError() {
        udfStub.setStubParameters("abc");
        assertThatThrownBy(() -> udfStubService.update(udfStub, 1L)).isInstanceOf(IllegalArgumentException.class);
    }
}
