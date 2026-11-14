package com.ems.identity.dto;

import java.util.List;

public record AccountPage(List<AccountIdentity> items, int page, int totalPages, long totalElements) {}
