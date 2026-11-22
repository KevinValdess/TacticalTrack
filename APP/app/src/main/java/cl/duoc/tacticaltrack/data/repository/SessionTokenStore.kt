package cl.duoc.tacticaltrack.data.repository

interface SessionTokenStore {
    fun read(): String?
    fun write(token: String)
    fun clear()
}
