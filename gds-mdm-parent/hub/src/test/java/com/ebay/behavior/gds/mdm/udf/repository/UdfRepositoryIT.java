package com.ebay.behavior.gds.mdm.udf.repository;

import com.ebay.behavior.gds.mdm.udf.common.model.Udf;
import com.ebay.behavior.gds.mdm.udf.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UdfRepositoryIT {

    @Autowired
    private UdfRepository repository;

    private Udf udf;

    @BeforeEach
    void setUp() {
        udf = TestModelUtils.udf();
        repository.save(udf);
    }

    @Test
    void findByUdfNameIn() {
        Set<String> names = new HashSet<>(List.of("testUdf"));
        Udf newUdf = repository.findByNameIn(names).get(0);
        assertThat(newUdf.getName()).isEqualTo(udf.getName());
    }
}
