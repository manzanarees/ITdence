package com.itdence.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    public static String hashear(String contrasenaPlano) {
        return BCrypt.hashpw(contrasenaPlano, BCrypt.gensalt());
    }

    public static boolean verificar(String contrasenaPlano, String contrasenaHash) {
        return BCrypt.checkpw(contrasenaPlano, contrasenaHash);
    }
}
