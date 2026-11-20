package cl.duoc.tacticaltrack.domain

object VisitRules {
    val equipmentStatuses = listOf("Operativo", "Operativo con observaciones", "Con falla")
    fun editError(status: String, observations: String): String? = when {
        status !in equipmentStatuses -> "Selecciona un estado válido."
        observations.length > 2000 -> "Máximo 2000 caracteres."
        status != "Operativo" && observations.trim().length < 5 -> "Describe la falla con al menos 5 caracteres."
        else -> null
    }
}
