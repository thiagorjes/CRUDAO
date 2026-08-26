package com.crudao.kanban.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.websocket.WsTicket;
import com.crudao.kanban.domain.websocket.WsTicketRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Ticket de curta duração para autenticar o handshake STOMP/SockJS via browser — TASK-07.2. */
@ExtendWith(MockitoExtension.class)
class WsTicketServiceTest {

    @Mock private WsTicketRepository wsTicketRepository;

    private WsTicketService service;

    private final Usuario usuario = new Usuario();

    @BeforeEach
    void setUp() {
        service = new WsTicketService(wsTicketRepository);
        usuario.setId(UUID.randomUUID());
    }

    @Test
    void emitir_criaTicketNaoUsadoComExpiracaoFutura() {
        when(wsTicketRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        WsTicket ticket = service.emitir(usuario);

        assertThat(ticket.isUsado()).isFalse();
        assertThat(ticket.getUsuario()).isEqualTo(usuario);
        assertThat(ticket.getExpiraEm()).isAfter(OffsetDateTime.now());
    }

    @Test
    void validarEUsar_ticketValido_retornaUsuarioEMarcaComoUsado() {
        WsTicket ticket = ticketValido();
        UUID id = ticket.getId();
        when(wsTicketRepository.findById(id)).thenReturn(Optional.of(ticket));

        Optional<Usuario> resultado = service.validarEUsar(id);

        assertThat(resultado).contains(usuario);
        assertThat(ticket.isUsado()).isTrue();
        verify(wsTicketRepository).save(ticket);
    }

    @Test
    void validarEUsar_ticketJaUsado_retornaVazio() {
        WsTicket ticket = ticketValido();
        ticket.setUsado(true);
        when(wsTicketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThat(service.validarEUsar(ticket.getId())).isEmpty();
    }

    @Test
    void validarEUsar_ticketExpirado_retornaVazio() {
        WsTicket ticket = ticketValido();
        ticket.setExpiraEm(OffsetDateTime.now().minusSeconds(1));
        when(wsTicketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThat(service.validarEUsar(ticket.getId())).isEmpty();
    }

    @Test
    void validarEUsar_ticketExpirado_mesmoAssimEMarcadoComoUsado_naoPodeSerReaproveitado() {
        WsTicket ticket = ticketValido();
        ticket.setExpiraEm(OffsetDateTime.now().minusSeconds(1));
        when(wsTicketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        service.validarEUsar(ticket.getId());

        assertThat(ticket.isUsado()).isTrue();
    }

    @Test
    void validarEUsar_ticketInexistente_retornaVazioSemSalvar() {
        UUID id = UUID.randomUUID();
        when(wsTicketRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(service.validarEUsar(id)).isEmpty();
        verify(wsTicketRepository, never()).save(any());
    }

    @Test
    void validarEUsar_chamadoDuasVezesComMesmoTicket_segundaChamadaRetornaVazio() {
        WsTicket ticket = ticketValido();
        when(wsTicketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThat(service.validarEUsar(ticket.getId())).contains(usuario);
        assertThat(service.validarEUsar(ticket.getId())).isEmpty();
        verify(wsTicketRepository, times(2)).save(ticket);
    }

    @Test
    void limparExpirados_delegaAoRepositorioComInstanteAtual() {
        when(wsTicketRepository.deleteByExpiraEmBefore(any())).thenReturn(3L);

        service.limparExpirados();

        verify(wsTicketRepository).deleteByExpiraEmBefore(any(OffsetDateTime.class));
    }

    private WsTicket ticketValido() {
        WsTicket ticket = new WsTicket();
        ticket.setId(UUID.randomUUID());
        ticket.setUsuario(usuario);
        ticket.setCriadoEm(OffsetDateTime.now());
        ticket.setExpiraEm(OffsetDateTime.now().plusSeconds(20));
        ticket.setUsado(false);
        return ticket;
    }
}
