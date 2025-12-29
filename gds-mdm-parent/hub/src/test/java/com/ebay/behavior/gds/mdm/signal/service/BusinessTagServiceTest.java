package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.PropertyV1;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request.SearchIn;
import com.ebay.behavior.gds.mdm.signal.common.service.pmsvc.PmsvcService;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static com.ebay.behavior.gds.mdm.common.model.Model.INVALID_ID;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessTagServiceTest {

    @Mock
    private ResultSet resultSet;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private BusinessTagWriteService writeService;

    @Mock
    private PmsvcService pmsvcService;

    @Spy
    @InjectMocks
    private BusinessTagService service;

    @Test
    void loadBusinessTags() {
        var maybeJdbcTemplate = Optional.of(jdbcTemplate);
        ReflectionTestUtils.setField(service, "maybeJdbcTemplate", maybeJdbcTemplate);
        var pmsvcTags = List.of(new PropertyV1());

        doReturn(pmsvcTags).when(pmsvcService).searchProperties("3", SearchIn.LIFECYCLE_STATE, EXACT_MATCH);

        service.loadBusinessTags("2021-01-01");
    }

    @Test
    void init() {
        service.init();

        assertThat(service.maybeJdbcTemplate).isPresent();
    }

    @Test
    void toSojEvent() throws SQLException {
        when(resultSet.getString("eactn")).thenReturn("someEactn");
        when(resultSet.getString("page_id")).thenReturn("123");
        when(resultSet.getString("module_id")).thenReturn("456");
        when(resultSet.getString("click_id")).thenReturn("789");
        when(resultSet.getString("tags")).thenReturn("tag1,tag2");

        var result = service.toSojEvent(resultSet);

        assertThat(result).isNotNull();
        assertThat(result.getEactn()).isEqualTo("someEactn");
        assertThat(result.getPageId()).isEqualTo(123L);
        assertThat(result.getModuleId()).isEqualTo(456L);
        assertThat(result.getClickId()).isEqualTo(789L);
        assertThat(result.getTags()).isEqualTo("tag1,tag2");
    }

    @Test
    void toSojEvent_error() throws SQLException {
        when(resultSet.getString("eactn")).thenReturn("someEactn");
        when(resultSet.getString("page_id")).thenReturn(null);
        when(resultSet.getString("module_id")).thenReturn("not a number");
        when(resultSet.getString("click_id")).thenReturn("789");
        when(resultSet.getString("tags")).thenThrow(SQLException.class);

        var result = service.toSojEvent(resultSet);

        assertThat(result).isNotNull();
        assertThat(result.getEactn()).isEqualTo("someEactn");
        assertThat(result.getPageId()).isNull();
        assertThat(result.getModuleId()).isEqualTo(INVALID_ID);
        assertThat(result.getClickId()).isEqualTo(789L);
    }
}