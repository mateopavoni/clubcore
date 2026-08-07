package com.clubcore.api.security;

import com.clubcore.api.entity.Cliente;
import com.clubcore.api.exception.ResourceNotFoundException;
import com.clubcore.api.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resuelve el {@link Cliente} asociado al usuario autenticado actual, a partir del
 * {@code SecurityContext} que {@link JwtFilter} deja poblado con el email (username)
 * del {@code Usuario} que hizo login.
 *
 * <p>Usado por los servicios que exponen recursos propiedad de un Cliente (Reservas,
 * Rutinas) para aplicar ownership checks: un caller con rol CLIENTE solo debe poder
 * operar sobre sus propios recursos. Un caller con rol ADMIN o ENTRENADOR conserva
 * acceso completo — ver {@link #esClienteSinPrivilegios()}.</p>
 *
 * <p>{@code Cliente.email} y {@code Usuario.email} son siempre el mismo valor
 * ({@link com.clubcore.api.service.impl.ClienteServiceImpl#crearCliente} los crea juntos
 * con el mismo email), asi que {@code auth.getName()} (el username del JWT) sirve
 * directamente como clave de busqueda en {@link ClienteRepository#findByEmail}.</p>
 */
@Component
@RequiredArgsConstructor
public class AuthenticatedClienteResolver {

    private final ClienteRepository clienteRepository;

    /**
     * True si el caller autenticado tiene rol CLIENTE y ningun rol de staff (ADMIN/ENTRENADOR).
     *
     * <p>Sin autenticacion en el SecurityContext (por ejemplo, un test o un caller interno que
     * invoca el servicio directamente sin pasar por el filtro JWT) se considera "sin
     * restriccion" — el @PreAuthorize a nivel de controller ya garantiza que, en un request
     * HTTP real, siempre hay una Authentication presente en este punto.</p>
     */
    public boolean esClienteSinPrivilegios() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        boolean esStaff = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(rol -> rol.equals("ROLE_ADMIN") || rol.equals("ROLE_ENTRENADOR"));
        return !esStaff;
    }

    /**
     * Resuelve el ID del Cliente asociado al usuario autenticado.
     *
     * @throws ResourceNotFoundException si no hay usuario autenticado o el usuario
     *         autenticado no tiene un Cliente asociado
     */
    public Long clienteIdActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("Cliente", "usuario autenticado", "ninguno");
        }
        return clienteRepository.findByEmail(auth.getName())
                .map(Cliente::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "email", auth.getName()));
    }
}
