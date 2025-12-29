package com.ebay.behavior.gds.mdm.signal.common.service.pmsvc;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.ActionV1;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.ClickV1;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.FamilyV1;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.ModuleV1;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.PageV1;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.PmsvcModelV1;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.PropertyV1;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request.SearchIn;
import com.ebay.com.google.common.annotations.VisibleForTesting;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.val;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
@Validated
public class PmsvcService {

    public static final String SEARCH_TERM_SIZE_MSG = "searchTerm must be at least 3 characters";

    @Autowired
    private List<TmsExtractor<?>> extractors;

    private Map<Class<?>, TmsExtractor<?>> extractorMap;

    @PostConstruct
    @VisibleForTesting
    public void init() {
        extractorMap = extractors.stream().collect(toMap(TmsExtractor::getType, Function.identity()));
    }

    public ClickV1 getClickById(long id) {
        return getById(id, ClickV1.class);
    }

    public Set<ClickV1> getClickByIds(Set<Long> ids) {
        return ids.stream()
                .map(id -> getById(id, ClickV1.class))
                .collect(toSet());
    }

    public ModuleV1 getModuleById(long id) {
        return getById(id, ModuleV1.class);
    }

    public Set<ModuleV1> getModuleByIds(Set<Long> ids) {
        return ids.stream()
                .map(id -> getById(id, ModuleV1.class))
                .collect(toSet());
    }

    public PageV1 getPageById(long id) {
        return getById(id, PageV1.class);
    }

    public Set<PageV1> getPageByIds(Set<Long> ids) {
        return ids.stream()
                .map(id -> getById(id, PageV1.class))
                .collect(toSet());
    }

    public PropertyV1 getPropertyById(long id) {
        return getById(id, PropertyV1.class);
    }

    public FamilyV1 getFamilyById(long id) {
        return getById(id, FamilyV1.class);
    }

    public ActionV1 getActionById(long id) {
        return getById(id, ActionV1.class);
    }

    public List<ClickV1> searchClicks(@NotNull @Size(min = 1, message = SEARCH_TERM_SIZE_MSG) String searchTerm,
                                      @NotNull SearchIn searchBy, @NotNull SearchCriterion searchCriterion) {
        validateSearchTerm(searchTerm, searchBy);
        return search(searchTerm, searchBy, searchCriterion, ClickV1.class);
    }

    public List<ModuleV1> searchModules(@NotNull @Size(min = 1, message = SEARCH_TERM_SIZE_MSG) String searchTerm,
                                        @NotNull SearchIn searchBy, @NotNull SearchCriterion searchCriterion) {
        validateSearchTerm(searchTerm, searchBy);
        return search(searchTerm, searchBy, searchCriterion, ModuleV1.class);
    }

    public List<PageV1> searchPages(@NotNull @Size(min = 1, message = SEARCH_TERM_SIZE_MSG) String searchTerm,
                                    @NotNull SearchIn searchBy, @NotNull SearchCriterion searchCriterion) {
        validateSearchTerm(searchTerm, searchBy);
        return search(searchTerm, searchBy, searchCriterion, PageV1.class);
    }

    public List<PropertyV1> searchProperties(@NotNull String searchTerm,
                                             @NotNull SearchIn searchBy, @NotNull SearchCriterion searchCriterion) {
        return search(searchTerm, searchBy, searchCriterion, PropertyV1.class);
    }

    public List<FamilyV1> searchFamilies(@NotNull @Size(min = 3, message = SEARCH_TERM_SIZE_MSG) String searchTerm,
                                         @NotNull SearchIn searchBy, @NotNull SearchCriterion searchCriterion) {
        return search(searchTerm, searchBy, searchCriterion, FamilyV1.class);
    }

    public List<ActionV1> searchActions(@NotNull @Size(min = 3, message = SEARCH_TERM_SIZE_MSG) String searchTerm,
                                        @NotNull SearchIn searchBy, @NotNull SearchCriterion searchCriterion) {
        return search(searchTerm, searchBy, searchCriterion, ActionV1.class);
    }

    private <M extends PmsvcModelV1> List<M> search(String searchTerm, SearchIn searchBy, SearchCriterion searchCriterion, Class<M> type) {
        val extractor = extractorMap.get(type);
        Validate.isTrue(Objects.nonNull(extractor), String.format("No extractor found for type: %s", type.getSimpleName()));

        val request = extractor.createRequest(searchTerm, searchBy.getValue(), searchCriterion);
        val listResponse = extractor.post(null, null, request, extractor.getResponseType());
        return (List<M>) listResponse.getList();
    }

    private <M extends PmsvcModelV1> M getById(long id, @NotNull Class<M> type) {
        val models = search(String.valueOf(id), SearchIn.ID, EXACT_MATCH, type);

        if (models.isEmpty()) {
            throw new DataNotFoundException(type, id);
        }

        if (models.size() > 1) {
            throw new IllegalStateException(String.format("Multiple %s found for id: %s", type.getSimpleName(), id));
        }

        return models.get(0);
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private void validateSearchTerm(String searchTerm, SearchIn searchBy) {
        if (searchBy == SearchIn.ID) {
            try {
                Long.parseLong(searchTerm);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("searchTerm must be a number", ex);
            }
        } else if (searchTerm.length() < 3) {
            throw new IllegalArgumentException(SEARCH_TERM_SIZE_MSG);
        }
    }
}
