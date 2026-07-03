package com.sonocare.mindrayreceiver.bridge

import com.topjohnwu.superuser.Shell

/**
 * RootUtils
 * ---------
 * El tablet Android, incluso rooteado, no puede hacer que un proceso de
 * aplicacion normal escuche directamente en el puerto 445 (los puertos
 * <1024 requieren capacidades de root a nivel de kernel Linux). La forma
 * estandar de resolver esto (la misma que usan apps como SimbaDroid o
 * LAN Drive en su modalidad rooteada) es:
 *
 *   1. El servidor SMB real (JFileServer) escucha en un puerto alto y sin
 *      privilegios, ej. 8445.
 *   2. Con una unica regla de iptables (ejecutada con permisos root via
 *      libsu), se redirige TODO el trafico entrante al puerto 445 hacia
 *      el puerto 8445 donde realmente escucha la app.
 *
 * Esta regla es liviana, no requiere que la app en si corra como root,
 * y sobrevive mientras el dispositivo este encendido (se vuelve a aplicar
 * en cada arranque del servicio, ver SmbBridgeService).
 */
object RootUtils {

    /** Verifica si el dispositivo tiene acceso root disponible (Magisk u otro). */
    fun isRootAvailable(): Boolean {
        return Shell.getShell().isRoot
    }

    /**
     * Aplica (de forma idempotente) la redireccion de puerto 445 -> internalPort.
     * Devuelve true si la regla quedo aplicada correctamente.
     */
    fun ensurePortRedirect(internalPort: Int): Boolean {
        if (!isRootAvailable()) return false

        // Primero verificamos si la regla ya existe, para no duplicarla en
        // cada reinicio del servicio (iptables no es idempotente por si solo).
        val checkCmd =
            "iptables -t nat -C PREROUTING -p tcp --dport 445 -j REDIRECT --to-port $internalPort 2>/dev/null"
        val alreadyExists = Shell.cmd(checkCmd).exec().isSuccess

        if (alreadyExists) return true

        val addCmd =
            "iptables -t nat -A PREROUTING -p tcp --dport 445 -j REDIRECT --to-port $internalPort"
        val result = Shell.cmd(addCmd).exec()
        return result.isSuccess
    }

    /** Elimina la regla de redireccion (util al detener el servicio manualmente). */
    fun removePortRedirect(internalPort: Int) {
        if (!isRootAvailable()) return
        Shell.cmd(
            "iptables -t nat -D PREROUTING -p tcp --dport 445 -j REDIRECT --to-port $internalPort"
        ).exec()
    }
}
