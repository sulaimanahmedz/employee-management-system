package com.ems.onboarding.dto;

import jakarta.validation.constraints.NotNull;

public record OnboardingTaskCompletionRequest(@NotNull Boolean completed) {}
