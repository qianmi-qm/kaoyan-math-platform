package com.kaoyan.platform.question.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ImportResultVO {

    private int totalRows;
    private int imported;
    private int duplicated;
    private int failed;

    /** 每个失败行：行号 + 原因 */
    private List<FailedRow> failures = new ArrayList<>();

    @Data
    public static class FailedRow {
        private int rowIndex;
        private String reason;

        public FailedRow(int rowIndex, String reason) {
            this.rowIndex = rowIndex;
            this.reason = reason;
        }
    }
}
