package com.ems.identity.dto;

import java.util.List;
public record SessionIdentity(String login, List<String> authorities) {}
