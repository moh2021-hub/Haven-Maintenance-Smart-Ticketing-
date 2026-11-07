package com.apartment.maintenance.repository;

import com.apartment.maintenance.model.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {
}
