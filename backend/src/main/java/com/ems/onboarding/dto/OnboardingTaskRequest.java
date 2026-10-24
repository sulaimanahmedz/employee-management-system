package com.ems.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record OnboardingTaskRequest(@NotBlank @Size(max = 150) String title,
    @NotBlank @Size(max = 100) String owner, LocalDate dueOn) {}
