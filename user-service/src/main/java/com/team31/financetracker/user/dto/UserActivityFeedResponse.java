package com.team31.financetracker.user.dto;

import java.io.Serializable;
import java.util.List;

public record UserActivityFeedResponse(
        List<ActivityEventDTO> content,
        int page,
        int size,
        long totalElements
) implements Serializable {
}