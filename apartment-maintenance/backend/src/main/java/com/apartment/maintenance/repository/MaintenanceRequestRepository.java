package com.apartment.maintenance.repository;

import com.apartment.maintenance.model.MaintenanceRequest;
import com.apartment.maintenance.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    // THE QUEUE: most urgent first, oldest first inside the same priority.
    List<MaintenanceRequest> findAllByOrderByPriorityAscCreatedAtAsc();

    List<MaintenanceRequest> findByStatusOrderByPriorityAscCreatedAtAsc(RequestStatus status);

    List<MaintenanceRequest> findByTechnicianIdOrderByPriorityAscCreatedAtAsc(Long technicianId);

    long countByTechnicianIdAndStatus(Long technicianId, RequestStatus status);

    long countByStatus(RequestStatus status);
}
