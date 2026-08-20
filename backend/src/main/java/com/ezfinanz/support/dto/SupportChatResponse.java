package com.ezfinanz.support.dto;

import java.util.List;

public record SupportChatResponse(
        String reply,
        List<String> sources
) {
}
