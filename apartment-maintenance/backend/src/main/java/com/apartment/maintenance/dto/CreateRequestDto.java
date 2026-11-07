package com.apartment.maintenance.dto;

import com.apartment.maintenance.model.RequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** What React posts to create a new request. */
public record CreateRequestDto(

        @NotBlank(message = "apartmentNumber is required")
        String apartmentNumber,

        @NotBlank(message = "description is required")
        String description,

        @NotNull(message = "requestType is required (GAS_LEAK, NO_HEAT, WATER_LEAK, BROKEN_APPLIANCE, OTHER)")
        RequestType requestType
) {
}
