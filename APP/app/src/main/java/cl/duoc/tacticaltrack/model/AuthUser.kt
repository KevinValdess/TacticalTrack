package cl.duoc.tacticaltrack.model

data class AuthUser(
    val id: Long,
    val email: String,
    val name: String,
    val role: String,
    val enabled: Boolean,
    val supervisorId: String?
) {
    val displayRole: String
        get() = when (role) {
            "SUPERVISOR" -> "Supervisor"
            "OPERARIO" -> "Operario"
            else -> error("Rol de usuario no reconocido.")
        }

    fun validated(): AuthUser {
        require(enabled && id > 0 && name.isNotBlank() && email.isNotBlank())
        require(role == "OPERARIO" || role == "SUPERVISOR")
        require(role != "SUPERVISOR" || !supervisorId.isNullOrBlank())
        return this
    }
}
