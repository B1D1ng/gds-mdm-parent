package com.ebay.behavior.gds.mdm.signal.common.service;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcQueryBuilder;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils;

import lombok.val;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.common.util.TimeUtils.toNowSqlTimestamp;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.INTEGRATION_TEST;
import static java.util.Locale.US;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StagedUdcSignalServiceIT {

    @Autowired
    private MetadataWriteService writeService;

    @Autowired
    private StagedUdcSignalService stagedUdcSignalService;

    @Autowired
    private UdcQueryBuilder udcQueryBuilder;

    @Autowired
    private UdcConfiguration config;

    private final VersionedId signalId1 = VersionedId.of(36L, 1);
    private UnstagedSignal signal1;
    private UdcDataSourceType dataSource;
    private final String name = "Test_signal_1";

    @BeforeAll
    void setUpAll() {
        dataSource = config.getDataSource();
        var randomString = TestUtils.getRandomSmallString().toLowerCase(US);
        var description = "test_desc_" + randomString + " POSTFIX";
        var now = toNowSqlTimestamp();
        var user = "Test_user";

        signal1 = unstagedSignal(1L).toBuilder()
                .id(signalId1.getId())
                .version(signalId1.getVersion())
                .name(name)
                .description(description)
                .signalTemplateSourceId(1L)
                .events(Set.of())
                .fields(Set.of())
                .createBy(user)
                .updateBy(user)
                .createDate(now)
                .updateDate(now)
                .build();

        var signal2 = unstagedSignal(1L).toBuilder()
                .id(2L)
                .version(1)
                .name("test_signal_2")
                .description(description)
                .environment(Environment.STAGING)
                .events(Set.of())
                .fields(Set.of())
                .createBy(user)
                .updateBy(user)
                .createDate(now)
                .updateDate(now)
                .build();

        var signal3 = unstagedSignal(1L).toBuilder()
                .id(3L)
                .version(1)
                .name("test_signal_3")
                .description("test_desc_3")
                .environment(PRODUCTION)
                .events(Set.of())
                .fields(Set.of())
                .createBy(user)
                .updateBy(user)
                .createDate(now)
                .updateDate(now)
                .build();

        writeService.upsertSignal(Set.of(), signal1);
        writeService.upsertSignal(Set.of(), signal2);
        writeService.upsertSignal(Set.of(), signal3);
        TestUtils.sleep(5);
    }

    @Test
    void getById() {
        var persisted = stagedUdcSignalService.getById(signalId1.getId());

        assertThat(persisted.getId()).isEqualTo(signalId1.getId());
        assertThat(persisted.getCreateBy()).isNotNull();
        assertThat(persisted.getUpdateBy()).isNotNull();
        assertThat(persisted.getCreateDate()).isNotNull();
        assertThat(persisted.getUpdateDate()).isNotNull();
        assertThat(persisted.getOwners()).isEqualTo(signal1.getOwners());
        assertThat(persisted.getCompletionStatus()).isEqualTo(signal1.getCompletionStatus());
    }

    @Test
    void getAll() {
        var pageable = EsPageable.of(0, 2); // two out of three
        var page = stagedUdcSignalService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page.getContent()).hasSizeLessThanOrEqualTo(2);

        pageable = EsPageable.of(0, 1); // one out of three
        page = stagedUdcSignalService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page.getContent()).hasSizeLessThanOrEqualTo(1);

        pageable = EsPageable.of(3, 1); // last page
        page = stagedUdcSignalService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page.getContent()).hasSizeLessThanOrEqualTo(1);

        pageable = EsPageable.of((int) page.getTotalElements() + 1, 1); // no such offset
        page = stagedUdcSignalService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void search_termByName() {
        val term = name.toLowerCase(US); // we must lower case the term since it is a processed property
        var pageable = EsPageable.of(0, 10);
        var queryBuilder = QueryBuilders.termQuery(NAME, term);
        var searchBuilder = udcQueryBuilder.toSearchSourceBuilder(pageable, queryBuilder);

        var page = stagedUdcSignalService.search(searchBuilder.toString());

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getName().toLowerCase(US)).contains(term);
    }

    @Test
    void search_multiMatchByNameOrDescription() {
        val text = "POSTFIX";
        var pageable = EsPageable.of(0, 10);
        val queryBuilder = QueryBuilders.multiMatchQuery(text, NAME, "description").operator(Operator.OR);
        var searchBuilder = udcQueryBuilder.toSearchSourceBuilder(pageable, queryBuilder);

        var page = stagedUdcSignalService.search(searchBuilder.toString());

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getDescription()).contains(text);
    }
}