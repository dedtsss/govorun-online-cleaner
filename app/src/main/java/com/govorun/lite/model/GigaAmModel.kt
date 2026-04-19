package com.govorun.lite.model

import android.content.Context
import java.io.File

/**
 * Hardcoded config for GigaAM v3 E2E RNNT (Sber, MIT license).
 * End-to-end Russian ASR with punctuation and capitalization. WER ~8.4%.
 *
 * Upstream source: github.com/salute-developers/GigaAM
 * sherpa-onnx conversion originally by Smirnov75 on HuggingFace, mirrored
 * unchanged to our own GitHub Release so app installs don't depend on a
 * third-party host.
 *
 * SHA-256 values pinned below — ModelDownloader verifies every downloaded
 * file against them before the model is considered installed.
 */
object GigaAmModel {

    const val ENCODER = "gigaam_v3_e2e_rnnt_encoder_int8.onnx"
    const val DECODER = "gigaam_v3_e2e_rnnt_decoder.onnx"
    const val JOINER  = "gigaam_v3_e2e_rnnt_joint.onnx"
    const val TOKENS  = "gigaam_v3_e2e_rnnt_tokens.txt"

    data class ModelFile(val name: String, val url: String, val sizeBytes: Long, val sha256: String? = null)

    private const val BASE = "https://github.com/amidexe/govorun-lite/releases/download/model-gigaam-v3"

    val FILES = listOf(
        ModelFile(
            ENCODER, "$BASE/$ENCODER", 318_995_997L,
            sha256 = "2cac62d0c270bd128f898f2be1a2d34780d524a6e9483888ebac7b00f97410f1"
        ),
        ModelFile(
            DECODER, "$BASE/$DECODER", 4_600_058L,
            sha256 = "781971998e6a355d6a714f6932a30eab295e7ba0d14fd7e0f78c83b87e811860"
        ),
        ModelFile(
            JOINER,  "$BASE/$JOINER",  2_712_896L,
            sha256 = "602ff7017a93311aad34df1437c8d7f49911353c13d6eae7a6ee7b041339465c"
        ),
        ModelFile(
            TOKENS,  "$BASE/$TOKENS",  13_353L,
            sha256 = "7ddf22514c42c531358182c81446a8159771e9921019f09ae743ea622d40221d"
        ),
    )

    val TOTAL_BYTES: Long = FILES.sumOf { it.sizeBytes }

    fun modelDir(context: Context): File =
        File(context.filesDir, "models/gigaam-v3-e2e-rnnt").also { it.mkdirs() }

    /** True when all expected files exist on disk with the expected sizes. */
    fun isInstalled(context: Context): Boolean {
        val dir = modelDir(context)
        return FILES.all {
            val f = File(dir, it.name)
            f.exists() && f.length() == it.sizeBytes
        }
    }
}
