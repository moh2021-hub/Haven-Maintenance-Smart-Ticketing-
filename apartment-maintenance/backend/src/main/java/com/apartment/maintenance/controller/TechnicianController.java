package com.apartment.maintenance.controller;

import com.apartment.maintenance.dto.TechnicianDto;
import com.apartment.maintenance.model.MaintenanceRequest;
import com.apartment.maintenance.model.Technician;
import com.apartment.maintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/technicians")
public class TechnicianController {

    private final MaintenanceService service;

    public TechnicianController(MaintenanceService service) {
        this.service = service;
    }

    /** GET /api/technicians */
    @GetMapping
    public List<Technician> list() {
        return service.findAllTechnicians();
    }

    /** GET /api/technicians/2 */
    @GetMapping("/{id}")
    public Technician getOne(@PathVariable Long id) {
        return service.getTechnician(id);
    }

    /** GET /api/technicians/2/requests -> that technician's job list, urgent first */
    @GetMapping("/{id}/requests")
    public List<MaintenanceRequest> jobs(@PathVariable Long id) {
        return service.findByTechnician(id);
    }

    /** POST /api/technicians  {"name":"John","phone":"313-555-0101"} */
    @PostMapping
    public ResponseEntity<Technician> create(@Valid @RequestBody TechnicianDto dto) {
        Technician saved = service.createTechnician(dto.name(), dto.phone());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /** DELETE /api/technicians/2 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteTechnician(id);
        return ResponseEntity.noContent().build();
    }
}
