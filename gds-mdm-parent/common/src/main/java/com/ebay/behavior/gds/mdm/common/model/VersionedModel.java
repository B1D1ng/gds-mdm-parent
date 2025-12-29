package com.ebay.behavior.gds.mdm.common.model;

public interface VersionedModel extends Model {

    String VERSION = "version";

    Integer getVersion();
}
