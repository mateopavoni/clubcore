package com.clubcore.api.service.impl;

import com.clubcore.api.dto.request.ReservaRequest;
import com.clubcore.api.dto.response.ReservaResponse;
import com.clubcore.api.entity.*;
import com.clubcore.api.exception.MembresiaInvalidaException;
import com.clubcore.api.exception.ResourceNotFoundException;
import com.clubcore.api.mapper.ReservaMapper;
import com.clubcore.api.repository.ClienteRepository;
import com.clubcore.api.repository.MembresiaRepository;
import com.clubcore.api.repository.ReservaRepository;
import com.clubcore.api.security.AuthenticatedClienteResolver;
import com.clubcore.api.service.MembresiaStateService;
import com.clubcore.api.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final MembresiaRepository membresiaRepository;
    private final ReservaMapper reservaMapper;
    private final MembresiaStateService membresiaStateService;
    private final AuthenticatedClienteResolver authenticatedClienteResolver;
    private final TransactionTemplate transactionTemplate;

    // Lock por cliente para el chequeo de cupo mensual — ver Javadoc de crearReserva().
    // En memoria, solo válido para una única instancia (la que corre hoy en la VPS).
    // Si se escala a múltiples réplicas, hace falta un lock distribuido (ej. Redis) o mover el
    // conteo de cupo a un UPDATE atómico condicionado en SQL.
    private final ConcurrentHashMap<Long, Lock> clienteLocks = new ConcurrentHashMap<>();

    public ReservaServiceImpl(ReservaRepository reservaRepository, ClienteRepository clienteRepository,
                               MembresiaRepository membresiaRepository, ReservaMapper reservaMapper,
                               MembresiaStateService membresiaStateService,
                               AuthenticatedClienteResolver authenticatedClienteResolver,
                               PlatformTransactionManager transactionManager) {
        this.reservaRepository = reservaRepository;
        this.clienteRepository = clienteRepository;
        this.membresiaRepository = membresiaRepository;
        this.reservaMapper = reservaMapper;
        this.membresiaStateService = membresiaStateService;
        this.authenticatedClienteResolver = authenticatedClienteResolver;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Crea una reserva previa validación del estado de membresía (incluido el cupo mensual del plan).
     *
     * <p>El motor de estados se invoca aquí (momento de la verdad) para verificar
     * que el cliente puede reservar. Si la membresía no está ACTIVA, se lanza
     * {@link MembresiaInvalidaException} que el GlobalExceptionHandler mapea a 403.</p>
     *
     * <p><b>Por qué un lock en memoria y no {@code @Transactional} + lock pesimista de fila:</b>
     * se probó con {@code SELECT ... FOR UPDATE} sobre la membresía y NO cerró la carrera bajo
     * concurrencia real (verificado con requests paralelas contra un plan con cupo=1 — las 5
     * se persistían igual). Un lock de JVM por cliente, sosteniendo el chequeo de cupo y el
     * insert dentro de la MISMA sección crítica, sí la cierra — verificado de la misma forma.
     * Se usa {@link TransactionTemplate} (no la anotación {@code @Transactional}) a propósito:
     * con la anotación, el proxy de Spring recién hace commit DESPUÉS de que el método retorna,
     * es decir después de liberar el lock si este viviera dentro del cuerpo del método — dejando
     * la misma ventana de carrera abierta. Con TransactionTemplate el commit ocurre de forma
     * síncrona dentro de {@code execute(...)}, todavía sosteniendo el lock.</p>
     */
    @Override
    public ReservaResponse crearReserva(ReservaRequest request) {
        // IDOR guard: un CLIENTE solo puede reservar para si mismo, nunca en nombre de otro
        // clienteId arbitrario recibido en el body. ADMIN conserva acceso completo.
        if (authenticatedClienteResolver.esClienteSinPrivilegios()
                && !authenticatedClienteResolver.clienteIdActual().equals(request.clienteId())) {
            throw new ResourceNotFoundException("Cliente", "id", request.clienteId());
        }

        Lock lock = clienteLocks.computeIfAbsent(request.clienteId(), id -> new ReentrantLock());
        lock.lock();
        try {
            return transactionTemplate.execute(status -> {
                Cliente cliente = clienteRepository.findById(request.clienteId())
                        .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", request.clienteId()));

                Membresia membresia = membresiaRepository
                        .findFirstByClienteIdOrderByFechaVencimientoDesc(request.clienteId())
                        .orElseThrow(() -> new MembresiaInvalidaException(
                                "El cliente no tiene ninguna membresía registrada"));

                // Momento de la verdad: evaluación on-demand del estado real (incluye cupo)
                EstadoMembresia estado = membresiaStateService.evaluarEstado(membresia);
                if (estado != EstadoMembresia.ACTIVA) {
                    throw new MembresiaInvalidaException(estado, "crear reserva");
                }

                Reserva reserva = Reserva.builder()
                        .cliente(cliente)
                        .descripcion(request.descripcion())
                        .fechaReserva(request.fechaReserva())
                        .estado(EstadoReserva.CONFIRMADA)
                        .build();

                return reservaMapper.toResponse(reservaRepository.save(reserva));
            });
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse obtenerReserva(Long id) {
        Reserva reserva = findOrThrow(id);
        verificarPropiedad(reserva);
        return reservaMapper.toResponse(reserva);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservaResponse> listarReservas(Pageable pageable) {
        return reservaRepository.findAll(pageable).map(reservaMapper::toResponse);
    }

    @Override
    @Transactional
    public ReservaResponse cancelarReserva(Long id) {
        Reserva reserva = findOrThrow(id);
        verificarPropiedad(reserva);
        reserva.setEstado(EstadoReserva.CANCELADA);
        return reservaMapper.toResponse(reservaRepository.save(reserva));
    }

    private Reserva findOrThrow(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));
    }

    /**
     * IDOR guard: un CLIENTE solo puede leer/cancelar sus propias reservas. Se lanza
     * {@link ResourceNotFoundException} (404) en vez de un 403 para no filtrar, via el
     * codigo de estado, que la reserva de otro cliente existe.
     */
    private void verificarPropiedad(Reserva reserva) {
        if (authenticatedClienteResolver.esClienteSinPrivilegios()
                && !reserva.getCliente().getId().equals(authenticatedClienteResolver.clienteIdActual())) {
            throw new ResourceNotFoundException("Reserva", "id", reserva.getId());
        }
    }
}
