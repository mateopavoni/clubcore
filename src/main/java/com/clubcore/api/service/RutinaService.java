package com.clubcore.api.service;

import com.clubcore.api.dto.request.RutinaRequest;
import com.clubcore.api.dto.response.RutinaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RutinaService {

    RutinaResponse crearRutina(RutinaRequest request);

    RutinaResponse obtenerRutina(Long id);

    Page<RutinaResponse> listarRutinas(Pageable pageable);

    void eliminarRutina(Long id);
}
