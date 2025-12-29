package com.ebay.behavior.gds.mdm.common.util;

import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.Model;
import com.ebay.behavior.gds.mdm.common.model.VersionedAuditable;
import com.ebay.behavior.gds.mdm.common.model.VersionedModel;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Response;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.http.HttpStatus;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notBlank;

@UtilityClass
public class CommonValidationUtils {

    private static final long ILLEGAL_ID = 0;

    /**
     * Conditional validation is very problematic, since various fields get values during various stages in the code.
     * Revision is null, but hibernate adds the zero revision before the validation.
     * CreateDate and updateDate set during @PrePersist and @PreUpdate, but not before the validation.
     * CreateBy and updateBy set after resource validation but before service validation.
     */
    public static <A extends Auditable> void validateForCreate(A auditable) {
        isTrue(isNull(auditable.getId()), "id must be null");
        isTrue(isNull(auditable.getRevision()), "revision must be null");
    }

    public static <A extends VersionedAuditable> void validateForCreate(A auditable) {
        validateForCreate((Auditable) auditable);
        isTrue(isNull(auditable.getVersion()), "version must be null");
    }

    public static <M extends Model> void validateForUpdate(M model) {
        isTrue(nonNull(model.getId()), "id must not be null");
        isTrue(nonNull(model.getRevision()), "revision must not be null");
    }

    public static <M extends VersionedModel> void validateForUpdate(M model) {
        validateForUpdate((Model) model);
        isTrue(nonNull(model.getVersion()), "version must not be null");
    }

    public static void validateStatus(Response response, String message) {
        isTrue(nonNull(response), "response must not be null");
        notBlank(message, "message must not be blank");

        if (response.getStatus() != HttpStatus.OK.value()) {
            throw new InternalServerErrorException(
                    String.format("Error %s. Response status = %d", message, response.getStatus()));
        }
    }

    public static void validateId(String str) {
        validateNumeric(str);
        long id = NumberUtils.toLong(str, ILLEGAL_ID);
        isTrue(id > ILLEGAL_ID, "ID must be greater than " + ILLEGAL_ID);
    }

    public static void validateNumeric(String str) {
        isTrue(NumberUtils.isParsable(str), "ID must be a valid long number");
    }
}
