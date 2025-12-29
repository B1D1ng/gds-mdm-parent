package com.ebay.behavior.gds.mdm.common.model.search;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Pageable;

@Getter
@Setter
public class Search {

    public static final String SEARCH_BY = "searchBy";
    public static final String SEARCH_TERM = "searchTerm";
    public static final String SEARCH_CRITERION = "searchCriterion";

    public static final String OWNED_BY_ME = "ownedByMe";
    public static final String DOMAIN = "domain";
    public static final String DESCRIPTION = "description";
    public static final String NAME = "name";
    public static final String TAG = "tag";
    public static final String TYPE = "type";
    public static final String PLATFORM = "platform";
    public static final String DATA_SOURCE = "dataSource";
    public static final String ENTITY_TYPE = "entityType";
    public static final String ENVIRONMENT = "environment";
    public static final String IS_MANDATORY = "isMandatory";

    // MySql pagination parameters
    public static final String PAGE_NUMBER = "pageNumber";
    public static final String PAGE_SIZE = "pageSize";

    // Elasticsearch pagination parameters
    public static final String FROM = "from";
    public static final String SIZE = "size";

    @NotBlank
    private String searchBy;

    @NotBlank
    private String searchTerm;

    @NotNull
    private SearchCriterion searchCriterion;

    @Valid
    @NotNull
    private Pageable pageable;

    private String user;

    public Search(String searchBy, String searchTerm, SearchCriterion searchCriterion) {
        this.searchBy = searchBy;
        this.searchTerm = searchTerm;
        this.searchCriterion = searchCriterion;
    }

    public Search(String searchBy, String searchTerm, SearchCriterion searchCriterion, Pageable pageable) {
        this(searchBy, searchTerm, searchCriterion);
        this.pageable = pageable;
    }

    public Search(String searchBy, String searchTerm, SearchCriterion searchCriterion, Pageable pageable, String user) {
        this(searchBy, searchTerm, searchCriterion);
        this.pageable = pageable;
        this.user = user;
    }
}