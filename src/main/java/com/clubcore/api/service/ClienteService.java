package com.clubcore.api.service;

import com.clubcore.api.dto.request.ClienteRequest;
import com.clubcore.api.dto.response.ClienteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClienteService {

    ClienteResponse crearCliente(ClienteRequest request);

    ClienteResponse obtenerCliente(Long id);

    Page<ClienteResponse> listarClientes(Pageable pageable);

    ClienteResponse actualizarCliente(Long id, ClienteRequest request);

    void eliminarCliente(Long id);
}
