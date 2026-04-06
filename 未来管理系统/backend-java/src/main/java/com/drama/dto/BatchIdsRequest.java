package com.drama.dto;

import java.util.List;
import lombok.Data;

@Data
public class BatchIdsRequest {
    private List<Integer> ids;
}
