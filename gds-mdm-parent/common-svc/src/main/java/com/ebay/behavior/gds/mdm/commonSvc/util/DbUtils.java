package com.ebay.behavior.gds.mdm.commonSvc.util;

import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.Model;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;

@UtilityClass
public class DbUtils {

    @SafeVarargs
    public static <M extends Model> Page<M> getPage(M... model) {
        val size = model.length;
        val content = Arrays.asList(model);
        val pageable = PageRequest.of(0, size);

        return new PageImpl<>(content, pageable, size);
    }

    public Pageable getAuditablePageable(int pageNumber, int pageSize) {
        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, Auditable.UPDATE_DATE));
    }

    public EsPageable getAuditableEsPageable(int from, int pageSize) {
        return EsPageable.of(from, pageSize, Sort.by(Sort.Direction.DESC, Auditable.UPDATE_DATE));
    }
}
