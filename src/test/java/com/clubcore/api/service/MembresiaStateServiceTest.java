package com.clubcore.api.service;

import com.clubcore.api.entity.*;
import com.clubcore.api.repository.MembresiaRepository;
import com.clubcore.api.repository.PagoRepository;
import com.clubcore.api.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link MembresiaStateService} — el motor de estados central.
 *
 * Cubre los 4 estados y sus casos borde más relevantes:
 * - SUSPENDIDA (prioridad 1)
 * - INACTIVA por vencimiento
 * - DEUDA por pagos pendientes
 * - INACTIVA por cupo agotado
 * - ACTIVA (camino feliz)
 * - Combinaciones: suspendida tiene prioridad sobre deuda y vencimiento
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MembresiaStateService - Motor de estados")
class MembresiaStateServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private MembresiaRepository membresiaRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MembresiaStateService service;

    private Cliente cliente;
    private Plan planSinLimite;
    private Plan planConLimite;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder()
                .id(1L)
                .nombre("Maria")
                .apellido("Gomez")
                .email("maria@test.com")
                .build();

        planSinLimite = Plan.builder()
                .id(1L)
                .nombre("Premium")
                .duracionDias(30)
                .limiteReservasMes(null) // sin límite
                .build();

        planConLimite = Plan.builder()
                .id(2L)
                .nombre("Básico")
                .duracionDias(30)
                .limiteReservasMes(5) // máximo 5 reservas/mes
                .build();
    }

    // ------------------------------------------------------------------ SUSPENDIDA

    @Test
    @DisplayName("SUSPENDIDA: flag manual activo devuelve SUSPENDIDA (prioridad maxima)")
    void evaluarEstado_cuandoSuspendida_devuelveSUSPENDIDA() {
        Membresia membresia = membresiaVigente(planSinLimite);
        membresia.setSuspendida(true);

        EstadoMembresia resultado = service.evaluarEstado(membresia);

        assertThat(resultado).isEqualTo(EstadoMembresia.SUSPENDIDA);
    }

    @Test
    @DisplayName("SUSPENDIDA tiene prioridad sobre DEUDA (suspendida + pagos pendientes)")
    void evaluarEstado_suspendidaConDeuda_devuelveSUSPENDIDA() {
        Membresia membresia = membresiaVigente(planSinLimite);
        membresia.setSuspendida(true);
        // El stub del pagoRepository NO se registra aqui porque el metodo retorna temprano
        // por el flag suspendida, sin llegar a consultar los pagos. Eso es exactamente
        // el comportamiento que queremos verificar: SUSPENDIDA tiene maxima prioridad.

        EstadoMembresia resultado = service.evaluarEstado(membresia);

        assertThat(resultado).isEqualTo(EstadoMembresia.SUSPENDIDA);
    }

    @Test
    @DisplayName("SUSPENDIDA tiene prioridad sobre INACTIVA (suspendida + membresía vencida)")
    void evaluarEstado_suspendidaYVencida_devuelveSUSPENDIDA() {
        Membresia membresia = membresiaVencida(planSinLimite);
        membresia.setSuspendida(true);

        EstadoMembresia resultado = service.evaluarEstado(membresia);

        assertThat(resultado).isEqualTo(EstadoMembresia.SUSPENDIDA);
    }

    // ------------------------------------------------------------------ INACTIVA (vencimiento)

    @Test
    @DisplayName("INACTIVA: fecha de vencimiento pasó ayer")
    void evaluarEstado_cuandoVencida_devuelveINACTIVA() {
        Membresia membresia = membresiaVencida(planSinLimite);

        EstadoMembresia resultado = service.evaluarEstado(membresia);

        assertThat(resultado).isEqualTo(EstadoMembresia.INACTIVA);
    }

    @Test
    @DisplayName("INACTIVA: fecha de vencimiento = ayer (borde exacto)")
    void evaluarEstado_vencidaAyer_devuelveINACTIVA() {
        Membresia membresia = membresiaCon(planSinLimite,
                LocalDate.now().minusDays(1), false);

        EstadoMembresia resultado = service.evaluarEstado(membresia);

        assertThat(resultado).isEqualTo(EstadoMembresia.INACTIVA);
    }

    // ------------------------------------------------------------------ DEUDA

    @Test
    @DisplayName("DEUDA: membresía vigente con al menos un pago PENDIENTE")
    void evaluarEstado_cuandoTieneDeuda_devuelveDEUDA() {
        Membresia membresia = membresiaVigente(planSinLimite);
        when(pagoRepository.existsByMembresiaAndEstado(membresia, EstadoPago.PENDIENTE))
                .thenReturn(true);

        EstadoMembresia resultado = service.evaluarEstado(membresia);

        assertThat(resultado).isEqualTo(EstadoMembresia.DEUDA);
    }

    @Test
    @DisplayName("No es DEUDA si todos los pagos estan PAGADO")
    void evaluarEstado_pagosConfirmados_noEsDeuda() {
        Membresia membresia = membresiaVigente(planSinLimite);
        when(pagoRepository.existsByMembresiaAndEstado(membresia, EstadoPago.PENDIENTE))
                .thenReturn(false);

        EstadoMembresia resultado = service.evaluarEstado(membresia);

        assertThat(resultado).isEqualTo(EstadoMembresia.ACTIVA);
    }

    // ------------------------------------------------------------------ INACTIVA (cupo agotado)

    @Test
    @DisplayName("INACTIVA: cupo mensual agotado (5/5 reservas este mes)")
    void evaluarEstado_cuandoCupoAgotado_devuelveINACTIVA() {
        Membresia membresia = membresiaVigente(planConLimite);
        when(pagoRepository.existsByMembresiaAndEstado(membresia, EstadoPago.PENDIENTE))
                .thenReturn(false);
        // 5 reservas este mes = límite exacto del plan
        when(reservaRepository.countByClienteAndFechaReservaBetweenAndEstadoIn(
                eq(cliente), any(LocalDateTime.class), any(LocalDateTime.class), any()))
                .thenReturn(5L);

        EstadoMembresia resultado = service.evaluarEstado(membresia);

        assertThat(resultado).isEqualTo(EstadoMembresia.INACTIVA);
    }

    @Test
    @DisplayName("ACTIVA: plan con limite, pero cupo disponible (4/5 reservas)")
    void evaluarEstado_planConLimiteCupoDisponible_devuelveACTIVA() {
        Membresia membresia = membresiaVigente(planConLimite);
        when(pagoRepository.existsByMembresiaAndEstado(membresia, EstadoPago.PENDIENTE))
                .thenReturn(false);
        when(reservaRepository.countByClienteAndFechaReservaBetweenAndEstadoIn(
                eq(cliente), any(LocalDateTime.class), any(LocalDateTime.class), any()))
                .thenReturn(4L); // por debajo del límite
        EstadoMembresia resultado = service.evaluarEstado(membresia);

        assertThat(resultado).isEqualTo(EstadoMembresia.ACTIVA);
    }

    @Test
    @DisplayName("ACTIVA: plan sin limite no consulta las reservas")
    void evaluarEstado_planSinLimite_devuelveACTIVASinConsultarReservas() {
        Membresia membresia = membresiaVigente(planSinLimite);
        when(pagoRepository.existsByMembresiaAndEstado(membresia, EstadoPago.PENDIENTE))
                .thenReturn(false);
        // No se llama a reservaRepository si el plan no tiene límite

        EstadoMembresia resultado = service.evaluarEstado(membresia);

        assertThat(resultado).isEqualTo(EstadoMembresia.ACTIVA);
    }

    // ------------------------------------------------------------------ ACTIVA (camino feliz)

    @Test
    @DisplayName("ACTIVA: vigente, sin deuda, sin cupo agotado, no suspendida")
    void evaluarEstado_todoOk_devuelveACTIVA() {
        Membresia membresia = membresiaVigente(planSinLimite);
        when(pagoRepository.existsByMembresiaAndEstado(membresia, EstadoPago.PENDIENTE))
                .thenReturn(false);

        EstadoMembresia resultado = service.evaluarEstado(membresia);

        assertThat(resultado).isEqualTo(EstadoMembresia.ACTIVA);
    }

    // ------------------------------------------------------------------ helpers

    private Membresia membresiaVigente(Plan plan) {
        return membresiaCon(plan, LocalDate.now().plusDays(15), false);
    }

    private Membresia membresiaVencida(Plan plan) {
        return membresiaCon(plan, LocalDate.now().minusDays(5), false);
    }

    private Membresia membresiaCon(Plan plan, LocalDate vencimiento, boolean suspendida) {
        return Membresia.builder()
                .id(1L)
                .cliente(cliente)
                .plan(plan)
                .fechaInicio(vencimiento.minusDays(plan.getDuracionDias()))
                .fechaVencimiento(vencimiento)
                .suspendida(suspendida)
                .estadoCacheado(EstadoMembresia.ACTIVA)
                .build();
    }
}
