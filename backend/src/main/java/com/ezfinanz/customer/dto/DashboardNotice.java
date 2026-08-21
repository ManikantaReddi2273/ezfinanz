package com.ezfinanz.customer.dto;

/** Short actionable notice shown on the customer dashboard (id, title, navigation target). */
public record DashboardNotice(
        String id,
        String title,
        String target
) {
}
