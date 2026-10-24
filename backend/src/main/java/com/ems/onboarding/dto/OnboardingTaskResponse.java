package com.ems.onboarding.dto;

import com.ems.onboarding.entity.OnboardingTask;

import java.time.LocalDate;
import java.util.List;

public record OnboardingTaskResponse(Long id, String title, String owner, LocalDate dueOn, boolean completed) {
    public static OnboardingTaskResponse from(OnboardingTask task) {
        return new OnboardingTaskResponse(task.getId(), task.getTitle(), task.getOwner(), task.getDueOn(), task.isCompleted());
    }
    public record Page(List<OnboardingTaskResponse> items, int page, int totalPages, long totalElements, long completedTasks, long totalTasks) {}
}
