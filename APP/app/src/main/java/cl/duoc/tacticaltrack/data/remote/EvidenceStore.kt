package cl.duoc.tacticaltrack.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

interface EvidenceStore {
    suspend fun encode(uri: String): String
    suspend fun persist(id: Long, base64: String): String
    suspend fun remove(id: Long)
}

/** Comprime las fotos para enviarlas a la API y guarda la evidencia recibida. */
class AndroidEvidenceStore(private val context: Context) : EvidenceStore {
    override suspend fun encode(uri: String): String = withContext(Dispatchers.IO) {
        val parsed = Uri.parse(uri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(parsed).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Fotografía inválida" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1280) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(parsed).use {
            requireNotNull(BitmapFactory.decodeStream(it, null, options)) { "No se pudo abrir la fotografía" }
        }
        try {
            val output = ByteArrayOutputStream()
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 75, output))
            val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            require(encoded.length <= 3_000_000) { "La evidencia es demasiado grande" }
            encoded
        } finally { bitmap.recycle() }
    }
    override suspend fun persist(id: Long, base64: String): String = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "remote_evidence_$id.jpg")
        file.writeBytes(Base64.decode(base64, Base64.DEFAULT))
        Uri.fromFile(file).toString()
    }
    override suspend fun remove(id: Long) { withContext(Dispatchers.IO) { File(context.filesDir, "remote_evidence_$id.jpg").delete() } }
}
