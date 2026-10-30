package com.taller.repositories;

import com.taller.models.ConfiguracionTaller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionTallerRepository extends JpaRepository<ConfiguracionTaller, Long> {
}
