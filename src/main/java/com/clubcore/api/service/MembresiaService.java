package com.clubcore.api.service;

import com.clubcore.api.dto.request.MembresiaRequest;
import com.clubcore.api.dto.response.MembresiaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MembresiaService {

    MembresiaResponse crearMembresia(MembresiaRequest request);

    MembresiaResponse obtenerMembresia(Long id);

    Page<MembresiaResponse> listarMembresias(Pageable pageable);

    MembresiaResponse suspenderMembresia(Long id, boolean suspendida);
}
