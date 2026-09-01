package com.crudao.kanban.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class WsTicketServiceTest {

    private final WsTicketService service = new WsTicketService("segredo-de-teste-1234567890");

    @Test
    void construtor_falha_quando_segredo_ausente_ou_curto() {
        assertThatThrownBy(() -> new WsTicketService(null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new WsTicketService("   "))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new WsTicketService("curto"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void emitir_e_validar_roundtrip_devolve_o_email() {
        String ticket = service.emitir("ana@crudao.local");

        assertThat(service.validar(ticket)).contains("ana@crudao.local");
    }

    @Test
    void validar_rejeita_assinatura_adulterada() {
        String ticket = service.emitir("ana@crudao.local");
        String adulterado = ticket.substring(0, ticket.indexOf('.') + 1) + "assinaturaErrada";

        assertThat(service.validar(adulterado)).isEmpty();
    }

    @Test
    void validar_rejeita_payload_adulterado() {
        String ticket = service.emitir("ana@crudao.local");
        // troca o payload por outro e-mail mantendo a assinatura original
        String outroPayload =
                java.util.Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString("mallory@crudao.local|99999999999".getBytes());
        String forjado = outroPayload + ticket.substring(ticket.indexOf('.'));

        assertThat(service.validar(forjado)).isEmpty();
    }

    @Test
    void validar_rejeita_ticket_de_outro_segredo() {
        String ticketOutroSegredo =
                new WsTicketService("outro-segredo-completamente-diferente")
                        .emitir("ana@crudao.local");

        assertThat(service.validar(ticketOutroSegredo)).isEmpty();
    }

    @Test
    void validar_rejeita_entradas_malformadas() {
        assertThat(service.validar(null)).isEmpty();
        assertThat(service.validar("")).isEmpty();
        assertThat(service.validar("sem-ponto")).isEmpty();
        assertThat(service.validar(".semPayload")).isEmpty();
        assertThat(service.validar("semAssinatura.")).isEmpty();
        assertThat(service.validar("@@@.@@@")).isEqualTo(Optional.empty());
    }
}
