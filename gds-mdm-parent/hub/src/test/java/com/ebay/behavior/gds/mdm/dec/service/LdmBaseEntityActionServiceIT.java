package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmBootstrapRequest;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmBootstrapResponse;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmBaseEntity;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmEntity;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.namespace;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LdmBaseEntityActionServiceIT {

    @MockitoBean
    private DecCompilerClient decCompilerClient;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private LdmBaseEntityActionService ldmBaseEntityActionService;

    @Autowired
    private LdmBaseEntityService ldmBaseEntityService;

    @Autowired
    private LdmEntityService ldmEntityService;

    private LdmBaseEntity baseEntity;
    private LdmEntity rawView;
    private LdmEntity rawView2;
    private LdmEntity snapshotView;

    @BeforeEach
    void setUp() {
        var namespace = namespace();
        namespace = namespaceService.create(namespace);

        val baseLdm = ldmBaseEntity(namespace.getId());

        this.baseEntity = ldmBaseEntityService.create(baseLdm);

        this.rawView = ldmEntity(null, null, "Item - RAW", ViewType.RAW, this.baseEntity.getNamespaceId(), this.baseEntity.getId());
        this.rawView2 = ldmEntity(null, null, "Item - RAW - Another", ViewType.RAW, this.baseEntity.getNamespaceId(), this.baseEntity.getId());
        this.snapshotView = ldmEntity(null, null, "Item - SNAPSHOT", ViewType.SNAPSHOT, this.baseEntity.getNamespaceId(), this.baseEntity.getId());
        this.rawView = ldmEntityService.create(this.rawView);
        this.rawView2 = ldmEntityService.create(this.rawView2);
        this.snapshotView = ldmEntityService.create(this.snapshotView);
    }

    @Test
    void bootstrap_whenBothViewsAreValid_shouldSucceed() {
        when(decCompilerClient.post(eq("/bootstrap"), eq(null), any(), eq(LdmBootstrapResponse.class))).thenReturn(new LdmBootstrapResponse(true));

        val request = new LdmBootstrapRequest(rawView.getId(), snapshotView.getId());
        val response = ldmBaseEntityActionService.bootstrap(baseEntity.getId(), request);

        assertThat(response.success()).isEqualTo(Boolean.TRUE);
    }

    @Test
    void bootstrap_whenSnapshotViewIsInvalid_shouldThrowIllegalArgumentException() {
        when(decCompilerClient.post(eq("/bootstrap"), eq(null), any(), eq(LdmBootstrapResponse.class))).thenReturn(new LdmBootstrapResponse(true));

        val request = new LdmBootstrapRequest(rawView.getId(), rawView2.getId());

        assertThatThrownBy(() -> ldmBaseEntityActionService.bootstrap(baseEntity.getId(), request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bootstrap_whenExternalServiceError_shouldThrowExternalCallException() {
        when(decCompilerClient.post(eq("/bootstrap"), eq(null), any(), eq(LdmBootstrapResponse.class))).thenThrow(ExternalCallException.class);

        val request = new LdmBootstrapRequest(rawView.getId(), snapshotView.getId());

        assertThatThrownBy(() -> ldmBaseEntityActionService.bootstrap(baseEntity.getId(), request)).isInstanceOf(ExternalCallException.class);
    }
}