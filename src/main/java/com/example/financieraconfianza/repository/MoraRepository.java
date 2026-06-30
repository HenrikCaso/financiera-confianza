package com.example.financieraconfianza.repository;

import com.example.financieraconfianza.model.entity.Mora;
import com.example.financieraconfianza.model.entity.CronogramaPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MoraRepository extends JpaRepository<Mora, Long> {

    Optional<Mora> findByCronogramaPago(CronogramaPago cronogramaPago);

    List<Mora> findByCronogramaPagoIn(
            List<CronogramaPago> cuotas
    );

}