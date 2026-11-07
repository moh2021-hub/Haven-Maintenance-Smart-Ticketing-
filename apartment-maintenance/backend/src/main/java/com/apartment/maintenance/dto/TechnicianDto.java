package com.apartment.maintenance.dto;

import jakarta.validation.constraints.NotBlank;

public record TechnicianDto(

        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "phone is required")
        String phone
) {
}
