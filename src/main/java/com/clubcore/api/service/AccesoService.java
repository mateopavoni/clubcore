package com.clubcore.api.service;

import com.clubcore.api.dto.request.AccesoRequest;
import com.clubcore.api.dto.response.AccesoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccesoService {

    AccesoResponse registrarAcceso(AccesoRequest request);

    Page<AccesoResponse> listarAccesos(Pageable pageable);
}
