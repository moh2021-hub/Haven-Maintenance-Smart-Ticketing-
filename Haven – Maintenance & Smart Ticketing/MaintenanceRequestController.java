package com.apartment.maintenance.controller;

import com.apartment.maintenance.dto.CreateRequestDto;
import com.apartment.maintenance.model.MaintenanceRequest;
import com.apartment.maintenance.model.RequestStatus;
import com.apartment.maintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
public class MaintenanceRequestController {

    private final MaintenanceService service;

    public MaintenanceRequestController(MaintenanceService service) {
        this.service = service;
    }

    /** POST /api/requests  -> create a request, priority + ETA set automatically */
    @PostMapping
    public ResponseEntity<MaintenanceRequest> create(@Valid @RequestBody CreateRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    /**
     * GET /api/requests               -> whole queue, most urgent first
     * GET /api/requests?status=NEW    -> only that status, still sorted
     */
    @GetMapping
    public List<MaintenanceRequest> list(@RequestParam(required = false) RequestStatus status) {
        return status == null ? service.findAll() : service.findByStatus(status);
    }

    /** GET /api/requests/summary -> counts for the dashboard */
    @GetMapping("/summary")
    public Map<String, Long> summary() {
        return service.summary();
    }

    /** GET /api/requests/5 */
    @GetMapping("/{id}")
    public MaintenanceRequest getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    /** PUT /api/requests/5/assign?technicianId=2 */
    @PutMapping("/{id}/assign")
    public MaintenanceRequest assign(@PathVariable Long id, @RequestParam Long technicianId) {
        return service.assign(id, technicianId);
    }

    /** PUT /api/requests/5/auto-assign -> gives it to the least busy technician */
    @PutMapping("/{id}/auto-assign")
    public MaintenanceRequest autoAssign(@PathVariable Long id) {
        return service.autoAssign(id);
    }

    /** PUT /api/requests/5/status?status=COMPLETED */
    @PutMapping("/{id}/status")
    public MaintenanceRequest updateStatus(@PathVariable Long id, @RequestParam RequestStatus status) {
        return service.updateStatus(id, status);
    }

    /** POST /api/requests/5/photo  (multipart form field name: "file") */
    @PostMapping("/{id}/photo")
    public MaintenanceRequest uploadPhoto(@PathVariable Long id,
                                          @RequestPart("file") MultipartFile file) {
        return service.addPhoto(id, file);
    }

    /** DELETE /api/requests/5 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
