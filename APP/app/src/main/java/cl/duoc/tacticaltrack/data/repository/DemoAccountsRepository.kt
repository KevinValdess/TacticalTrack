package cl.duoc.tacticaltrack.data.repository

import cl.duoc.tacticaltrack.model.DemoAccount

/** Validación local de prueba; no establece una sesión en el servidor. */
class DemoAccountsRepository {
    private val accounts = listOf(
        DemoAccount("op@duoc.cl", "Prueba", "Victor", "Operario"),
        DemoAccount("ad@duoc.cl", "Prueba", "Kevin", "Supervisor")
    )

    fun authenticate(email: String, password: String): DemoAccount? =
        accounts.firstOrNull {
            it.email.equals(email.trim(), ignoreCase = true) && it.password == password
        }
}
