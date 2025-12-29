package com.ebay.behavior.gds.mdm.signal.testUtil;

import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.signal.repository.PlanRepository;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.util.ImportUtils;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;

@Disabled
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class TestPlanUtils {

    @Autowired
    private PlanService service;

    @Autowired
    private PlanRepository repository;

    @Test
    @Disabled
    void createAll() {
        createPlan("Search page impression signals", "Create search page impression signals", "SEARCH");
        createPlan("Search page impression signals update", "Add new page ID to Search page impression signals", "SEARCH");
        createPlan("Add to cart signals", "Create signals for clicking add to cart button", "CART");
        createPlan("VI Watch button click signals", "Create signals for watch button on VI page", "VI");
        createPlan("Payment click signals", "Create signals for click actions on Checkout page", "XO");
    }

    private void createPlan(String name, String description, String domain) {
        var existingPlans = repository.findAllByName(name, PageRequest.of(0, 10));
        if (existingPlans.isEmpty()) {
            var planBuilder = Plan.builder();
            var plan = newPlan(name, description, domain, planBuilder);
            service.create(plan);
        } else {
            var planBuilder = existingPlans.toList().get(0).toBuilder();
            var plan = newPlan(name, description, domain, planBuilder);
            service.update(plan);
        }
    }

    private Plan newPlan(String name, String description, String domain, Plan.PlanBuilder<?, ? extends Plan.PlanBuilder<?, ?>> planBuilder) {
        return planBuilder
                .name(name)
                .description(description)
                .teamDls("DL-eBay-CORE-CJS@ebay.com")
                .owners("knadimpalli,ydrozd,yimhuang,mingshi,feshao,akaskurthy,vsergeev,jialili1")
                .jiraProject(ImportUtils.DEFAULT_JIRA)
                .domain(domain)
                .status(PlanStatus.CREATED)
                .createBy("jialili1")
                .updateBy("jialili1")
                .build();
    }
}
