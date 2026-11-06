package com.apartment.maintenance.service;

import com.apartment.maintenance.dto.CreateRequestDto;
import com.apartment.maintenance.exception.NotFoundException;
import com.apartment.maintenance.model.MaintenanceRequest;
import com.apartment.maintenance.model.RequestStatus;
import com.apartment.maintenance.model.RequestType;
import com.apartment.maintenance.model.Technician;
import com.apartment.maintenance.repository.MaintenanceRequestRepository;
import com.apartment.maintenance.repository.TechnicianRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MaintenanceService {

    private final MaintenanceRequestRepository requestRepo;
    private final TechnicianRepository technicianRepo;
    private final PhotoStorageService photoStorage;

    public MaintenanceService(MaintenanceRequestRepository requestRepo,
                              TechnicianRepository technicianRepo,
                              PhotoStorageService photoStorage) {
        this.requestRepo = requestRepo;
        this.technicianRepo = technicianRepo;
        this.photoStorage = photoStorage;
    }

    // ---------- create ----------

    public MaintenanceRequest create(CreateRequestDto dto) {
        RequestType type = dto.requestType();
        LocalDateTime now = LocalDateTime.now();

        MaintenanceRequest request = new MaintenanceRequest();
        request.setApartmentNumber(dto.apartmentNumber());
        request.setDescription(dto.description());
        request.setRequestType(type);
        request.setPriority(type.getPriority());              // <-- ranking happens here
        request.setStatus(RequestStatus.NEW);
        request.setCreatedAt(now);
        request.setEstimatedCompletionTime(now.plusHours(type.getHoursToFix()));

        return requestRepo.save(request);
    }

    // ---------- read ----------

    /** The prioritized queue: gas leak > no heat > water leak > broken appliance. */
    public List<MaintenanceRequest> findAll() {
        return requestRepo.findAllByOrderByPriorityAscCreatedAtAsc();
    }

    public List<MaintenanceRequest> findByStatus(RequestStatus status) {
        return requestRepo.findByStatusOrderByPriorityAscCreatedAtAsc(status);
    }

    public List<MaintenanceRequest> findByTechnician(Long technicianId) {
        getTechnician(technicianId); // 404 if the technician does not exist
        return requestRepo.findByTechnicianIdOrderByPriorityAscCreatedAtAsc(technicianId);
    }

    public MaintenanceRequest findById(Long id) {
        return requestRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Request " + id + " not found"));
    }

    /** Small dashboard summary for the React home screen. */
    public Map<String, Long> summary() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("total", requestRepo.count());
        for (RequestStatus status : RequestStatus.values()) {
            counts.put(status.name(), requestRepo.countByStatus(status));
        }
        return counts;
    }

    // ---------- assign ----------

    public MaintenanceRequest assign(Long requestId, Long technicianId) {
        MaintenanceRequest request = findById(requestId);
        Technician technician = getTechnician(technicianId);

        request.setTechnician(technician);
        if (request.getStatus() == RequestStatus.NEW) {
            request.setStatus(RequestStatus.IN_PROGRESS);
        }
        return requestRepo.save(request);
    }

    /** Picks whichever technician currently has the fewest IN_PROGRESS jobs. */
    public MaintenanceRequest autoAssign(Long requestId) {
        List<Technician> technicians = technicianRepo.findAll();
        if (technicians.isEmpty()) {
            throw new NotFoundException("No technicians exist yet - add one first");
        }

        Technician bestTechnician = technicians.get(0);
        long bestLoad = Long.MAX_VALUE;

        for (Technician technician : technicians) {
            long load = requestRepo.countByTechnicianIdAndStatus(
                    technician.getId(), RequestStatus.IN_PROGRESS);
            if (load < bestLoad) {
                bestLoad = load;
                bestTechnician = technician;
            }
        }

        return assign(requestId, bestTechnician.getId());
    }

    // ---------- status ----------

    public MaintenanceRequest updateStatus(Long requestId, RequestStatus status) {
        MaintenanceRequest request = findById(requestId);
        request.setStatus(status);

        if (status == RequestStatus.COMPLETED) {
            request.setCompletedAt(LocalDateTime.now());
        } else {
            request.setCompletedAt(null);
        }
        return requestRepo.save(request);
    }

    // ---------- photo ----------

    public MaintenanceRequest addPhoto(Long requestId, MultipartFile file) {
        MaintenanceRequest request = findById(requestId);
        request.setPhotoUrl(photoStorage.save(file));
        return requestRepo.save(request);
    }

    // ---------- delete ----------

    public void delete(Long requestId) {
        MaintenanceRequest request = findById(requestId);
        requestRepo.delete(request);
    }

    // ---------- technicians ----------

    public List<Technician> findAllTechnicians() {
        return technicianRepo.findAll();
    }

    public Technician getTechnician(Long id) {
        return technicianRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Technician " + id + " not found"));
    }

    public Technician createTechnician(String name, String phone) {
        return technicianRepo.save(new Technician(name, phone));
    }

    public void deleteTechnician(Long id) {
        Technician technician = getTechnician(id);

        // Un-assign their open jobs so we never leave a dangling foreign key.
        List<MaintenanceRequest> jobs =
                requestRepo.findByTechnicianIdOrderByPriorityAscCreatedAtAsc(id);
        for (MaintenanceRequest job : jobs) {
            job.setTechnician(null);
            if (job.getStatus() == RequestStatus.IN_PROGRESS) {
                job.setStatus(RequestStatus.NEW);
            }
        }
        requestRepo.saveAll(jobs);

        technicianRepo.delete(technician);
    }
}
