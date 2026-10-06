package com.kaoyan.platform.common.result;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageVO<T> {

    private List<T> list;
    private long total;
    private int page;
    private int size;
}
