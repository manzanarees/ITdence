package com.itdence.servicio;

import com.itdence.dao.UsuarioDAO;
import com.itdence.modelo.Usuario;
import com.itdence.util.PasswordUtil;

public class AuthServicio {
    private final  UsuarioDAO usuarioDAO = new UsuarioDAO();
    public Usuario login(String email, String contrasenaPlano){
        if (email == null || contrasenaPlano == null) {
            return null;
        }
        Usuario usuario = usuarioDAO.buscarPorEmail(email.trim().toLowerCase());
        if(usuario == null) {
            return null;
        }
        if (!PasswordUtil.verificar(contrasenaPlano, usuario.getContrasenaHash())) {
            return null;
        }
        return usuario;
    }
}
