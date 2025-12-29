package com.ebay.behavior.gds.mdm.dec.util;

import com.ebay.behavior.gds.mdm.dec.model.dto.LdmChangeRequestLogRecord;
import com.ebay.behavior.gds.mdm.dec.model.dto.StorageDetail;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextUtilsTest {

    LdmChangeRequestLogRecord logEntry = TestModelUtils.ldmChangeRequestLogEntry();
    String logEntryJson = "{\"userName\":\"IT_test_user\",\"createdTime\":1743044691000,\"status\":\"APPROVED\",\"comment\":null}";
    String logEntryListJson = "[{\"userName\":\"IT_test_user\",\"createdTime\":1743044691000,\"status\":\"APPROVED\",\"comment\":null},{\"userName\":\"IT_test_user\",\"createdTime\":1743044691000,\"status\":\"REJECTED\",\"comment\":null}]";

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void readJson() {
        LdmChangeRequestLogRecord entry = TextUtils.readJson(mapper, logEntryJson, LdmChangeRequestLogRecord.class);
        assertThat(entry.getUserName()).isEqualTo(logEntry.getUserName());
        assertThat(entry.getStatus()).isEqualTo(logEntry.getStatus());
    }

    @Test
    void readJson_Null() {
        assertThat(TextUtils.readJson(mapper, null, LdmChangeRequestLogRecord.class)).isNull();
    }

    @Test
    void readJson_Invalid() {
        assertThatThrownBy(() -> TextUtils.readJson(mapper, "a=1", LdmChangeRequestLogRecord.class)).isInstanceOf(Exception.class);
    }

    @Test
    void readJsonFromList() {
        List<LdmChangeRequestLogRecord> entryList = TextUtils.readJson(mapper, logEntryListJson, new TypeReference<List<LdmChangeRequestLogRecord>>() {
        });
        assertThat(entryList.size()).isEqualTo(2);
    }

    @Test
    void readJsonFromList_Null() {
        assertThat(TextUtils.readJson(mapper, null, new TypeReference<List<LdmChangeRequestLogRecord>>() {
        })).isNull();
    }

    @Test
    void readJsonFromList_Invalid() {
        assertThatThrownBy(() -> TextUtils.readJson(mapper, "a=1", new TypeReference<List<LdmChangeRequestLogRecord>>() {
        })).isInstanceOf(Exception.class);
    }

    @Test
    void writeJson() {
        String res = TextUtils.writeJson(mapper, logEntry);
        assertThat(res).isNotEmpty();
    }

    @Test
    void writeJson_Null() {
        assertThat(TextUtils.writeJson(mapper, null)).isNull();
    }

    @Test
    void writeJson_Invalid() {
        assertThatThrownBy(() -> TextUtils.writeJson(mapper, new Object())).isInstanceOf(Exception.class);
    }

    @Test
    void readStorageDetailsFromFile() {
        List<StorageDetail> storageDetails = TextUtils.readStorageDetailsFromFile(mapper, "/dec/signal_storage_item.json");
        assertThat(storageDetails.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void readStorageDetailsFromFile_Null() {
        assertThat(TextUtils.readStorageDetailsFromFile(mapper, null).size()).isEqualTo(0);
    }

    @Test
    void readStorageDetailsFromFile_Invalid() {
        assertThatThrownBy(() -> TextUtils.readStorageDetailsFromFile(mapper, "a=1")).isInstanceOf(Exception.class);
    }
}
