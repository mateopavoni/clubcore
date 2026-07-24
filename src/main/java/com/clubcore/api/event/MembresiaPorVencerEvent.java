package com.clubcore.api.event;

import com.clubcore.api.entity.Membresia;
import org.springframework.context.ApplicationEvent;

/**
 * Evento publicado por el job diario de {@code MembresiaStateService}
 * cuando detecta que una membresía vence al día siguiente.
 *
 * <p>El listener async {@code NotificationDispatchService} lo consume
 * y dispara el webhook saliente a n8n (o cualquier webhook configurado
 * en {@code N8N_WEBHOOK_URL}).</p>
 */
public class MembresiaPorVencerEvent extends ApplicationEvent {

    private final Membresia membresia;

    public MembresiaPorVencerEvent(Object source, Membresia membresia) {
        super(source);
        this.membresia = membresia;
    }

    public Membresia getMembresia() {
        return membresia;
    }
}
