package cl.duoc.tacticaltrack.data.repository

import cl.duoc.tacticaltrack.model.DemoAccount

/** Validación local de prueba; no establece una sesión en el servidor. */
class DemoAccountsRepository {
    private val accounts = listOf(
        DemoAccount("kevin@tacticaltrack.example", "Tactical123", "Kevin", "Supervisor"),
        DemoAccount("jona@tacticaltrack.example", "Tactical123", "Jona", "Operario"),
        DemoAccount("victor@tacticaltrack.example", "Tactical123", "Víctor", "Operario")
    )

    fun authenticate(email: String, password: String): DemoAccount? =
        accounts.firstOrNull {
            it.email.equals(email.trim(), ignoreCase = true) && it.password == password
        }
}
