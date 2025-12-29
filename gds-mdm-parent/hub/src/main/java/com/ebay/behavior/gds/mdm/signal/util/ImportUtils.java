package com.ebay.behavior.gds.mdm.signal.util;

import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.PlatformLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimValueLookup;
import com.ebay.behavior.gds.mdm.signal.model.SpecialPlanType;
import com.ebay.behavior.gds.mdm.signal.service.DomainLookupService;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.PlatformLookupService;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Pageable;

import java.util.Objects;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.UNKNOWN;

@UtilityClass
public class ImportUtils {

    public static final String DEFAULT_DOMAIN = "Cart";
    public static final String DEFAULT_JIRA = "CJSONB";
    public static final String DEFAULT_TEAM_DL = "DL-eBay-CJS-ScrumTeam@ebay.com";
    public static final String CJS = "CJS";
    public static final String EJS = "EJS";
    public static final String ITEM = "ITEM";

    private static SignalDimValueLookup getDefaultDomain(long dimTypeId) {
        return SignalDimValueLookup.builder()
                .name(DEFAULT_DOMAIN)
                .readableName(DEFAULT_DOMAIN)
                .dimensionTypeId(dimTypeId)
                .build();
    }

    private static String getDefaultPlanName(SpecialPlanType type, String platformName) {
        return type.name() + "_PLAN_" + platformName;
    }

    private static Plan getDefaultPlan(SpecialPlanType type, PlatformLookup platform) {
        return Plan.builder()
                .name(getDefaultPlanName(type, platform.getName()))
                .description(getDefaultPlanName(type, platform.getName()))
                .teamDls(DEFAULT_TEAM_DL)
                .owners(UNKNOWN)
                .domain(DEFAULT_DOMAIN)
                .platformId(platform.getId())
                .jiraProject(DEFAULT_JIRA)
                .build();
    }

    public static PlatformLookup createPlatformIfAbsent(String name, String readableName, PlatformLookupService service) {
        Validate.isTrue(Objects.nonNull(name), "name cannot be null");
        Validate.isTrue(Objects.nonNull(readableName), "readableName cannot be null");
        Validate.isTrue(Objects.nonNull(service), "PlatformLookupService cannot be null");

        val opt = service.findByName(name);
        return opt.orElseGet(() -> service.create(PlatformLookup.builder().name(name).readableName(readableName).build()));
    }

    public static Long createImportPlanIfAbsent(SpecialPlanType type, PlatformLookup platform,
                                                PlanService planService,
                                                DomainLookupService domainService
    ) {
        Validate.isTrue(Objects.nonNull(type), "SpecialPlanType cannot be null");
        Validate.isTrue(Objects.nonNull(platform.getName()), "PlatformType cannot be null");
        Validate.isTrue(Objects.nonNull(planService), "PlanService cannot be null");
        Validate.isTrue(Objects.nonNull(domainService), "DomainLookupService cannot be null");

        val name = getDefaultPlanName(type, platform.getName());
        val plans = planService.getAllByName(name, Pageable.ofSize(1));

        if (!plans.isEmpty()) {
            return plans.getContent().get(0).getId();
        }

        val domain = getDefaultDomain(domainService.getDimensionTypeId());
        domainService.createIfAbsent(domain);

        val plan = getDefaultPlan(type, platform);
        plan.setStatus(PlanStatus.CREATED);
        val created = planService.create(plan);
        created.setStatus(PlanStatus.HIDDEN);
        planService.update(created);

        return created.getId();
    }
}
