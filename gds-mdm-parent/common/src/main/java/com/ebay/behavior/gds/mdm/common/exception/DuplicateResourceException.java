package com.ebay.behavior.gds.mdm.common.exception;

import java.util.Set;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(Set<String> resources, String env, String streamName) {
        super(String.format("Duplicate resources violation: One or more resources in %s are already used by another streaming config with env='%s' and "
                + "stream_name='%s'", resources, env, streamName));
    }
}
