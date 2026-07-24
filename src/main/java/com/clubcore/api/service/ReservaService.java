package com.clubcore.api.service;

import com.clubcore.api.dto.request.ReservaRequest;
import com.clubcore.api.dto.response.ReservaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReservaService {

    ReservaResponse crearReserva(ReservaRequest request);

    ReservaResponse obtenerReserva(Long id);

    Page<ReservaResponse> listarReservas(Pageable pageable);

    ReservaResponse cancelarReserva(Long id);
}
