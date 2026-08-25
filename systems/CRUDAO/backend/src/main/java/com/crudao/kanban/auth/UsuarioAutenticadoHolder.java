package com.crudao.kanban.auth;

import com.crudao.kanban.domain.usuario.Usuario;

/**
 * Expõe o {@link Usuario} local resolvido pelo {@code AtivoUsuarioFilter} para controllers, dentro
 * do mesmo request thread (Tomcat não reusa threads entre requests concorrentes).
 */
public final class UsuarioAutenticadoHolder {

    private static final ThreadLocal<Usuario> CONTEXTO = new ThreadLocal<>();

    private UsuarioAutenticadoHolder() {}

    public static void set(Usuario usuario) {
        CONTEXTO.set(usuario);
    }

    public static Usuario get() {
        return CONTEXTO.get();
    }

    public static void clear() {
        CONTEXTO.remove();
    }
}
