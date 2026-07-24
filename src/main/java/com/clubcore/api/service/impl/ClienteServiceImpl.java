package com.clubcore.api.service.impl;

import com.clubcore.api.dto.request.ClienteRequest;
import com.clubcore.api.dto.response.ClienteResponse;
import com.clubcore.api.entity.Cliente;
import com.clubcore.api.entity.Usuario;
import com.clubcore.api.exception.ResourceNotFoundException;
import com.clubcore.api.mapper.ClienteMapper;
import com.clubcore.api.repository.ClienteRepository;
import com.clubcore.api.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ClienteResponse crearCliente(ClienteRequest request) {
        if (clienteRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                    "Ya existe un cliente con el email: " + request.email());
        }

        Usuario usuario = Usuario.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .rol(request.rol())
                .activo(true)
                .build();

        Cliente cliente = Cliente.builder()
                .nombre(request.nombre())
                .apellido(request.apellido())
                .email(request.email())
                .telefono(request.telefono())
                .fechaNacimiento(request.fechaNacimiento())
                .usuario(usuario)
                .build();

        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerCliente(Long id) {
        return clienteMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponse> listarClientes(Pageable pageable) {
        return clienteRepository.findAll(pageable).map(clienteMapper::toResponse);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(Long id, ClienteRequest request) {
        Cliente cliente = findOrThrow(id);
        cliente.setNombre(request.nombre());
        cliente.setApellido(request.apellido());
        cliente.setTelefono(request.telefono());
        cliente.setFechaNacimiento(request.fechaNacimiento());

        if (request.password() != null && !request.password().isBlank()) {
            cliente.getUsuario().setPassword(passwordEncoder.encode(request.password()));
        }

        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public void eliminarCliente(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cliente", "id", id);
        }
        clienteRepository.deleteById(id);
    }

    private Cliente findOrThrow(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", id));
    }
}
