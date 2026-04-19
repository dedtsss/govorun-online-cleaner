package com.govorun.lite.model

import android.content.Context
import java.io.File

/**
 * Hardcoded config for GigaAM v3 E2E RNNT (Sber, MIT license).
 * End-to-end Russian ASR with punctuation and capitalization. WER ~8.4%.
 *
 * Files are converted to sherpa-onnx by Smirnov75 on HuggingFace.
 * Source repo: github.com/salute-developers/GigaAM
 *
 * TODO: before going public, re-host these files on our own GitHub Releases
 *       (license permits redistribution) and swap URLs here.
 */
object GigaAmModel {

    const val ENCODER = "gigaam_v3_e2e_rnnt_encoder_int8.onnx"
    const val DECODER = "gigaam_v3_e2e_rnnt_decoder.onnx"
    const val JOINER  = "gigaam_v3_e2e_rnnt_joint.onnx"
    const val TOKENS  = "gigaam_v3_e2e_rnnt_tokens.txt"

    data class ModelFile(val name: String, val url: String, val sizeBytes: Long, val sha256: String? = null)

    private const val BASE = "https://huggingface.co/Smirnov75/GigaAM-v3-sherpa-onnx/resolve/main"

    val FILES = listOf(
        ModelFile(ENCODER, "$BASE/$ENCODER", 318_995_997L),
        ModelFile(DECODER, "$BASE/$DECODER", 4_600_058L),
        ModelFile(JOINER,  "$BASE/$JOINER",  2_712_896L),
        ModelFile(TOKENS,  "$BASE/$TOKENS",  13_353L)
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
