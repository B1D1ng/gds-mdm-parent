package com.ebay.behavior.gds.mdm.udf.repository;

import com.ebay.behavior.gds.mdm.udf.common.model.UdfStub;
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
class UdfStubRepositoryIT {

    @Autowired
    private UdfStubRepository repository;

    private UdfStub udfStub;

    @BeforeEach
    void setUp() {
        udfStub = TestModelUtils.udfStub(1L);
        repository.save(udfStub);
    }

    @Test
    void findByUdfStubNameIn() {
        Set<String> names = new HashSet<>(List.of("initial_test_udf_stub"));
        UdfStub newUdfStub = repository.findByStubNameIn(names).get(0);
        assertThat(newUdfStub.getStubName()).isEqualTo(udfStub.getStubName());
    }
}
