package com.ebay.behavior.gds.mdm.udf.service;

import com.ebay.behavior.gds.mdm.common.util.TimeUtils;
import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.udf.common.model.Udf;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStub;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfUsage;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfVersions;
import com.ebay.behavior.gds.mdm.udf.testUtil.TestModelUtils;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
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
class UdfServiceIT {

    @Autowired
    private UdfService udfService;

    @Autowired
    private UdfStubService udfStubService;

    @Autowired
    private UdfUsageService udfUsageService;

    @Autowired
    private UdfVersionService udfVersionService;

    private Udf udf;

    @BeforeEach
    void setUp() {
        udf = TestModelUtils.udf();
        udf = udfService.create(udf);
        udf = udfService.getById(udf.getId());
        long udfId = udf.getId();

        UdfStub udfStub = TestModelUtils.udfStub(udfId);
        UdfVersions udfVersion = TestModelUtils.udfVersion(udfId);
        UdfUsage udfUsage = TestModelUtils.udfUsage(udfId);

        udfStub = udfStubService.create(udfStub);
        udfUsage = udfUsageService.create(udfUsage);
        udfVersion = udfVersionService.create(udfVersion);

        assertThat(udfStub.getId()).isNotNull();
        assertThat(udfUsage.getId()).isNotNull();
        assertThat(udfVersion.getId()).isNotNull();
    }

    @Test
    void create() {
        assertThat(udf.getId()).isNotNull();
        assertThat(udf.getName()).isNotNull();
    }

    @Test
    void createWithError() {
        val nameSet = csvStringToSet(udf.getName());
        val badUdf = TestModelUtils.udf();
        badUdf.setParameters("abc");
        assertThatThrownBy(() -> udfService.create(badUdf, nameSet)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createWithExistingName() {
        val nameSet = csvStringToSet(udf.getName());
        udfService.create(TestModelUtils.udf(), nameSet);
        //assertThat(udf.getId()).isNotEqualTo(newUdf.getId());
    }

    @Test
    void getByIdWithAssociations() {
        Udf testUdf = udfService.getByIdWithAssociations(udf.getId());
        assertThat(testUdf.getName()).isNotNull();
        assertThat(testUdf.getUdfStubs()).isNotNull();
        assertThat(testUdf.getUdfStubs().stream().toList().get(0).getId()).isNotNull();
    }

    @Test
    void getAllUdf() {
        Page<Udf> udfList = udfService.getAll(DbUtils.getAuditablePageable(0, 1));
        assertThat(udfList).isNotEmpty();
        assertThat(udfList.toList().get(0).getId()).isNotNull();
        assertThat(udfList.toList().get(0).getUdfUsages().stream().toList().get(0).getId()).isNotNull();
        assertThat(udfList.toList().get(0).getUdfStubs().stream().toList().get(0).getId()).isNotNull();
        assertThat(udfList.toList().get(0).getUdfVersions().stream().toList().get(0).getId()).isNotNull();
    }

    @Test
    void getByIds() {
        Set<Long> ids = new HashSet<>(List.of(1L));
        List<Udf> udfList = udfService.getByIds(ids, true);
        assertThat(udfList).isNotEmpty();
        assertThat(udfList.get(0).getId()).isNotNull();
        assertThat(udfList.get(0).getUdfUsages().stream().toList().get(0).getUdfId()).isNotNull();
        assertThat(udfList.get(0).getUdfStubs().stream().toList().get(0).getUdfId()).isNotNull();
        assertThat(udfList.get(0).getUdfVersions().stream().toList().get(0).getUdfId()).isNotNull();
    }

    @Test
    void getByNames() {
        Set<String> names = new HashSet<>(List.of("testUdf"));
        List<Udf> udfList = udfService.getByNames(names, true);
        assertThat(udfList).isNotEmpty();
        assertThat(udfList.get(0).getId()).isNotNull();
        assertThat(udfList.get(0).getUdfUsages().stream().toList().get(0).getUdfId()).isNotNull();
        assertThat(udfList.get(0).getUdfStubs().stream().toList().get(0).getUdfId()).isNotNull();
        assertThat(udfList.get(0).getUdfVersions().stream().toList().get(0).getUdfId()).isNotNull();
    }

    @Test
    void getById() {
        Udf udf = udfService.getById(1L, true);
        assertThat(udf).isNotNull();
        assertThat(udf.getId()).isNotNull();
        assertThat(udf.getUdfUsages().stream().toList().get(0).getUdfId()).isNotNull();
        assertThat(udf.getUdfStubs().stream().toList().get(0).getUdfId()).isNotNull();
        assertThat(udf.getUdfVersions().stream().toList().get(0).getUdfId()).isNotNull();
    }

    @Test
    void update() {
        Udf udf = TestModelUtils.udf();
        var now = TimeUtils.toNowSqlTimestamp();
        udf.setId(1L);
        udf.setRevision(0);
        udf.setCreateDate(now);
        udf.setUpdateDate(now);
        udfService.update(udf, 1L);
    }

    @Test
    void updateWithError() {
        Udf udf = TestModelUtils.udf();
        var now = TimeUtils.toNowSqlTimestamp();
        udf.setId(1L);
        udf.setRevision(0);
        udf.setCreateDate(now);
        udf.setUpdateDate(now);
        udf.setParameters("abc");
        assertThatThrownBy(() -> udfService.update(udf, 1L)).isInstanceOf(IllegalArgumentException.class);
    }
}
