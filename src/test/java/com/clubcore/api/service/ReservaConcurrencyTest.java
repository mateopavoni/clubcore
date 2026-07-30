package com.clubcore.api.service;

import com.clubcore.api.dto.request.ReservaRequest;
import com.clubcore.api.entity.Cliente;
import com.clubcore.api.entity.EstadoMembresia;
import com.clubcore.api.entity.Membresia;
import com.clubcore.api.entity.Plan;
import com.clubcore.api.exception.MembresiaInvalidaException;
import com.clubcore.api.repository.ClienteRepository;
import com.clubcore.api.repository.MembresiaRepository;
import com.clubcore.api.repository.PlanRepository;
import com.clubcore.api.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduce en H2 el bug real de v0.3.2: N requests concurrentes contra un plan con
 * limiteReservasMes=1 no deben persistir mas de 1 reserva. Ver Javadoc de
 * {@link com.clubcore.api.service.impl.ReservaServiceImpl#crearReserva}.
 *
 * <p>Sin @Transactional en la clase: los threads worker necesitan ver el fixture
 * commiteado de verdad, no atado a la transaccion (con rollback) del hilo de test.</p>
 */
@SpringBootTest
class ReservaConcurrencyTest {

    @Autowired
    private ReservaService reservaService;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private MembresiaRepository membresiaRepository;
    @Autowired
    private PlanRepository planRepository;

    private Long clienteId;

    @BeforeEach
    void setUp() {
        Plan plan = planRepository.save(Plan.builder()
                .nombre("Plan cupo 1")
                .precio(BigDecimal.valueOf(1000))
                .duracionDias(30)
                .limiteReservasMes(1)
                .activo(true)
                .build());

        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Concurrencia")
                .apellido("Test")
                .email("concurrencia+" + System.nanoTime() + "@test.com")
                .build());
        clienteId = cliente.getId();

        membresiaRepository.save(Membresia.builder()
                .cliente(cliente)
                .plan(plan)
                .fechaInicio(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .suspendida(false)
                .estadoCacheado(EstadoMembresia.ACTIVA)
                .build());
    }

    @Test
    void diezRequestsParalelasContraCupoUno_persistenExactamenteUna() throws InterruptedException {
        int hilos = 10;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(hilos);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger rechazosPorCupo = new AtomicInteger();

        for (int i = 0; i < hilos; i++) {
            pool.submit(() -> {
                try {
                    reservaService.crearReserva(new ReservaRequest(
                            clienteId, "reserva concurrente", LocalDateTime.now().plusDays(1)));
                    exitos.incrementAndGet();
                } catch (MembresiaInvalidaException e) {
                    rechazosPorCupo.incrementAndGet();
                } finally {
                    salida.countDown();
                }
            });
        }

        salida.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(exitos.get()).isEqualTo(1);
        assertThat(rechazosPorCupo.get()).isEqualTo(hilos - 1);
        assertThat(reservaRepository.count()).isEqualTo(1);
    }
}
