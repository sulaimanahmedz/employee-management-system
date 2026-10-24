package com.ems.leave.dto;

import com.ems.leave.entity.LeaveRequest;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LeaveRequestInput(@NotNull LeaveRequest.Type type, @NotNull LocalDate startOn, @NotNull LocalDate endOn) {}
