package com.taller.repositories;

import com.taller.models.GastoTesoreria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GastoTesoreriaRepository extends JpaRepository<GastoTesoreria, Long> {
    List<GastoTesoreria> findAllByOrderByFechaDescIdDesc();
}
