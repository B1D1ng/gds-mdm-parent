package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.signal.common.model.HadoopSojEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.PropertyV1;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request.SearchIn;
import com.ebay.behavior.gds.mdm.signal.common.service.pmsvc.PmsvcService;
import com.ebay.kernel.util.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.ebay.behavior.gds.mdm.common.model.Model.INVALID_ID;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;

@Slf4j
@Service
public class BusinessTagService {

    @Autowired
    private BusinessTagWriteService writeService;

    @Autowired
    private PmsvcService pmsvcService;

    @Autowired(required = false)
    @Qualifier("hiveDataSource")
    private DataSource dataSource;

    protected Optional<JdbcTemplate> maybeJdbcTemplate = Optional.empty();

    @PostConstruct
    public void init() {
        if (Objects.nonNull(dataSource)) {
            this.maybeJdbcTemplate = Optional.of(new JdbcTemplate(dataSource));
        }
    }

    @Async
    public void loadBusinessTags(String date) {
        val sojResults = getSojQueryResults(date);
        val pmsvcTags = getPmsvcTags();
        writeService.save(sojResults, pmsvcTags);
    }

    /**
     * Fetch UBI tags from Hermes view cjs_v.business_tags
     *
     * @return List of BusinessTag
     */
    @VisibleForTesting
    protected List<HadoopSojEvent> getSojQueryResults(String date) {
        val jdbcTemplate = maybeJdbcTemplate.orElseThrow(() -> new IllegalStateException("JdbcTemplate is not available"));
        log.info("Fetching data from UBI...");
        val query = "SELECT eactn, page_id, module_id, click_id, tags FROM cjs_v.business_tags WHERE dt = ?";
        return jdbcTemplate.query(query, ps -> ps.setString(1, date), (rs, rowNum) -> toSojEvent(rs));
    }

    /**
     * Fetch Braavos tags from PMSVC
     *
     * @return List of PropertyV1
     */
    private List<PropertyV1> getPmsvcTags() {
        log.info("Fetching tags from PMSVC...");
        return pmsvcService.searchProperties("3", SearchIn.LIFECYCLE_STATE, EXACT_MATCH);
    }

    @VisibleForTesting
    protected HadoopSojEvent toSojEvent(ResultSet rs) {
        var res = new HadoopSojEvent();
        try {
            res.setEactn(rs.getString("eactn"));
            res.setPageId(getLongColumn(rs.getString("page_id")));
            res.setModuleId(getLongColumn(rs.getString("module_id")));
            res.setClickId(getLongColumn(rs.getString("click_id")));
            res.setTags(rs.getString("tags"));
        } catch (Exception ex) {
            log.error("Error while mapping result set to BusinessTag", ex);
        }
        return res;
    }

    protected Long getLongColumn(String num) {
        if (num == null) {
            return null;
        }
        if (!StringUtils.isNumeric(num)) {
            return INVALID_ID;
        }
        return NumberUtils.toLong(num);
    }
}
