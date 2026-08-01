package com.huamanga.tourism.insignia.repository;

import com.huamanga.tourism.insignia.domain.InsigniaTraduccion;
import com.huamanga.tourism.insignia.domain.InsigniaTraduccionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsigniaTraduccionRepository
        extends JpaRepository<InsigniaTraduccion, InsigniaTraduccionId> {
}
