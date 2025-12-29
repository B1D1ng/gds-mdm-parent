package com.ebay.behavior.gds.mdm.common.exception;

import java.util.Collection;
import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static java.util.stream.Collectors.joining;

public class DataNotFoundException extends MdmException {

    public DataNotFoundException() {
        super();
    }

    public DataNotFoundException(String message) {
        super(message);
    }

    public DataNotFoundException(Class<?> type, Long id) {
        this(type, List.of(id));
    }

    public DataNotFoundException(Class<?> type, Collection<Long> ids) {
        super(String.format("%s id=[%s] doesn't found", type.getSimpleName(), ids.stream().map(String::valueOf).collect(joining(COMMA))));
    }

    public DataNotFoundException(Class<?> type, long id) {
        super(String.format("%s id=%d doesn't found", type.getSimpleName(), id));
    }

    public DataNotFoundException(Class<?> type, String id1, String id2) {
        super(String.format("%s id=[%s, %s] doesn't found", type.getSimpleName(), id1, id2));
    }

    public DataNotFoundException(Class<?> type, long id1, long id2) {
        super(String.format("%s id=[%d, %d] doesn't found", type.getSimpleName(), id1, id2));
    }

    public DataNotFoundException(Class<?> type, String id) {
        super(String.format("%s id=%s doesn't found", type.getSimpleName(), id));
    }

    public DataNotFoundException(String message, Exception cause) {
        super(message, cause);
    }

    public DataNotFoundException(Exception cause) {
        super(cause);
    }
}
