package com.apartment.maintenance.config;

import com.apartment.maintenance.dto.CreateRequestDto;
import com.apartment.maintenance.model.RequestType;
import com.apartment.maintenance.repository.TechnicianRepository;
import com.apartment.maintenance.service.MaintenanceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Puts a few technicians and requests in the DB on startup so the app is
 * never empty when you open React. Only runs on the default (local) profile.
 */
@Component
@Profile("!prod")
public class DataSeeder implements CommandLineRunner {

    private final TechnicianRepository technicianRepo;
    private final MaintenanceService service;

    public DataSeeder(TechnicianRepository technicianRepo, MaintenanceService service) {
        this.technicianRepo = technicianRepo;
        this.service = service;
    }

    @Override
    public void run(String... args) {
        if (technicianRepo.count() > 0) {
            return;
        }

        service.createTechnician("John Carter", "313-555-0101");
        service.createTechnician("Maria Lopez", "313-555-0102");
        service.createTechnician("Sam Patel", "313-555-0103");

        service.create(new CreateRequestDto("204", "Dishwasher will not drain", RequestType.BROKEN_APPLIANCE));
        service.create(new CreateRequestDto("112", "Water dripping from bathroom ceiling", RequestType.WATER_LEAK));
        service.create(new CreateRequestDto("302", "Gas smell in the kitchen", RequestType.GAS_LEAK));
        service.create(new CreateRequestDto("509", "No heat, radiator is cold", RequestType.NO_HEAT));

        System.out.println("Seeded 3 technicians and 4 maintenance requests.");
    }
}
