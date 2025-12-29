package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmBootstrapRequest;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmBootstrapResponse;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ebay.behavior.gds.mdm.dec.service.LdmBaseEntityActionService.getLdmBootstrapMap;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmBaseEntity;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmEntity;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LdmBaseEntityActionServiceTest {

    @Mock
    private LdmBaseEntityService baseEntityService;

    @Mock
    private LdmEntityService ldmEntityService;

    @Mock
    private DecCompilerClient decCompilerClient;

    @InjectMocks
    private LdmBaseEntityActionService actionService;

    private LdmBaseEntity baseEntity;
    private LdmEntity rawView;
    private LdmEntity snapshotView;

    private LdmEntity rawViewInvalidBaseEntityId;
    private LdmEntity snapshotViewInvalidBaseEntityId;
    private LdmEntity rawViewInvalidType;
    private LdmEntity snapshotViewInvalidType;

    private LdmBootstrapRequest request;

    @BeforeEach
    void setUp() {
        val namespaceId = 0L;
        val baseEntityId = 1L;
        this.baseEntity = ldmBaseEntity(namespaceId);
        this.baseEntity.setId(baseEntityId);

        this.rawView = ldmEntity(11L, 0, "Item - RAW", ViewType.RAW, namespaceId, baseEntityId);
        this.snapshotView = ldmEntity(12L, 0, "Item - SNAPSHOT", ViewType.SNAPSHOT, namespaceId, baseEntityId);
        this.request = new LdmBootstrapRequest(rawView.getId(), snapshotView.getId());

        this.rawViewInvalidBaseEntityId = ldmEntity(11L, 0, "Item - RAW", ViewType.RAW, namespaceId, baseEntityId + 1);
        this.snapshotViewInvalidBaseEntityId = ldmEntity(12L, 0, "Item - SNAPSHOT", ViewType.SNAPSHOT, namespaceId, baseEntityId + 1);

        this.rawViewInvalidType = ldmEntity(11L, 0, "Item - RAW", ViewType.TIME_SERIES, namespaceId, baseEntityId);
        this.snapshotViewInvalidType = ldmEntity(12L, 0, "Item - SNAPSHOT", ViewType.TIME_SERIES, namespaceId, baseEntityId);
    }

    @Test
    void bootstrap_whenRequestIsValid_shouldSucceed() {
        // Arrange
        val requestMap = getLdmBootstrapMap(rawView.getBaseEntityId(), rawView.getName(), snapshotView.getName());

        when(baseEntityService.getById(baseEntity.getId())).thenReturn(baseEntity);
        when(ldmEntityService.getByIdCurrentVersion(rawView.getId())).thenReturn(rawView);
        when(ldmEntityService.getByIdCurrentVersion(snapshotView.getId())).thenReturn(snapshotView);

        when(decCompilerClient.post("/bootstrap", null, requestMap, LdmBootstrapResponse.class)).thenReturn(new LdmBootstrapResponse(true));

        // Act
        val response = actionService.bootstrap(baseEntity.getId(), request);

        // Assert
        assertTrue(response.success());
        verify(baseEntityService).getById(baseEntity.getId());
        verify(ldmEntityService).getByIdCurrentVersion(rawView.getId());
        verify(ldmEntityService).getByIdCurrentVersion(snapshotView.getId());
        verify(decCompilerClient).post("/bootstrap", null, requestMap, LdmBootstrapResponse.class);
    }

    @Test
    void bootstrap_whenBaseLdmIsInvalid_shouldThrowDataNotFoundException() {
        // Arrange
        when(baseEntityService.getById(baseEntity.getId())).thenThrow(new DataNotFoundException(LdmBaseEntity.class, baseEntity.getId()));

        // Act
        assertThrows(DataNotFoundException.class, () -> actionService.bootstrap(baseEntity.getId(), request));

        // Assert
        verify(baseEntityService).getById(baseEntity.getId());
        verify(ldmEntityService, never()).getByIdCurrentVersion(rawView.getId());
        verify(ldmEntityService, never()).getByIdCurrentVersion(snapshotView.getId());

        verify(decCompilerClient, never()).post(any(), any(), any(), eq(LdmBootstrapResponse.class));
    }

    @Test
    void bootstrap_whenRawViewBaseLdmIdIsInvalid_shouldThrowIllegalArgumentException() {
        // Arrange
        when(baseEntityService.getById(baseEntity.getId())).thenReturn(baseEntity);
        when(ldmEntityService.getByIdCurrentVersion(rawViewInvalidBaseEntityId.getId())).thenReturn(rawViewInvalidBaseEntityId);

        // Act
        assertThatThrownBy(() -> actionService.bootstrap(baseEntity.getId(), request)).isInstanceOf(IllegalArgumentException.class);

        // Assert
        verify(baseEntityService).getById(baseEntity.getId());
        verify(ldmEntityService).getByIdCurrentVersion(rawViewInvalidBaseEntityId.getId());
        verify(ldmEntityService, never()).getByIdCurrentVersion(snapshotView.getId());
        verify(decCompilerClient, never()).post(any(), any(), any(), eq(LdmBootstrapResponse.class));
    }

    @Test
    void bootstrap_whenRawViewTypeIsInvalid_shouldThrowIllegalArgumentException() {
        // Arrange
        when(baseEntityService.getById(baseEntity.getId())).thenReturn(baseEntity);
        when(ldmEntityService.getByIdCurrentVersion(rawViewInvalidType.getId())).thenReturn(rawViewInvalidType);

        // Act
        assertThatThrownBy(() -> actionService.bootstrap(baseEntity.getId(), request)).isInstanceOf(IllegalArgumentException.class);

        // Assert
        verify(baseEntityService).getById(baseEntity.getId());
        verify(ldmEntityService).getByIdCurrentVersion(rawViewInvalidType.getId());
        verify(ldmEntityService, never()).getByIdCurrentVersion(snapshotView.getId());
        verify(decCompilerClient, never()).post(any(), any(), any(), eq(LdmBootstrapResponse.class));
    }

    @Test
    void bootstrap_whenSnapshotViewBaseLdmIdIsInvalid_shouldThrowIllegalArgumentException() {
        // Arrange

        when(baseEntityService.getById(baseEntity.getId())).thenReturn(baseEntity);
        when(ldmEntityService.getByIdCurrentVersion(rawView.getId())).thenReturn(rawView);
        when(ldmEntityService.getByIdCurrentVersion(snapshotViewInvalidBaseEntityId.getId())).thenReturn(snapshotViewInvalidBaseEntityId);

        // Act
        assertThatThrownBy(() -> actionService.bootstrap(baseEntity.getId(), request)).isInstanceOf(IllegalArgumentException.class);

        // Assert
        verify(baseEntityService).getById(baseEntity.getId());
        verify(ldmEntityService).getByIdCurrentVersion(rawView.getId());
        verify(ldmEntityService).getByIdCurrentVersion(snapshotViewInvalidBaseEntityId.getId());
        verify(decCompilerClient, never()).post(any(), any(), any(), eq(LdmBootstrapResponse.class));
    }

    @Test
    void bootstrap_whenSnapshotViewTypeIsInvalid_shouldThrowIllegalArgumentException() {
        // Arrange
        when(baseEntityService.getById(baseEntity.getId())).thenReturn(baseEntity);
        when(ldmEntityService.getByIdCurrentVersion(rawView.getId())).thenReturn(rawView);
        when(ldmEntityService.getByIdCurrentVersion(snapshotViewInvalidType.getId())).thenReturn(snapshotViewInvalidType);

        // Act
        assertThatThrownBy(() -> actionService.bootstrap(baseEntity.getId(), request)).isInstanceOf(IllegalArgumentException.class);

        // Assert
        verify(baseEntityService).getById(baseEntity.getId());
        verify(ldmEntityService).getByIdCurrentVersion(rawView.getId());
        verify(ldmEntityService).getByIdCurrentVersion(snapshotViewInvalidType.getId());
        verify(decCompilerClient, never()).post(any(), any(), any(), eq(LdmBootstrapResponse.class));
    }
}