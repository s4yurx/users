package com.example.crypto

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Robustly reads, stages, and compresses photos picked from Android Photo Picker / Document Picker
 * (including HEIC, HEIF, WebP, PNG, JPEG, Ultra HDR, and cloud-backed Photos URIs) into crisp HD
 * JPEG Base64 payloads guaranteed to fit safely within end-to-end encrypted Firestore limits,
 * and saves HD decoded photos to the device Gallery using zero-permission MediaStore APIs.
 */
object PhotoCompressor {
    private const val TAG = "PhotoCompressor"
    // True HD photo target (up to 1920px longest edge, ~440KB JPEG -> ~785KB AES-GCM Base64 ciphertext < 880KB limit)
    private const val MAX_DIMENSION_PX = 1920
    private const val MAX_JPEG_BYTES = 440_000
    // HD Doodle canvas target (up to 1080px, ~240KB JPEG -> ~320KB Base64 < 360KB limit)
    private const val MAX_DOODLE_DIMENSION_PX = 1080
    private const val MAX_DOODLE_BYTES = 240_000
    private const val STAGED_DIR_NAME = "duet_staged_photos"

    /**
     * Immediately copies a picked content URI into an app-private cache file while the picker's
     * temporary read grant is guaranteed active (also prevents double-open failures on cloud URIs).
     */
    suspend fun stageUriToCacheFile(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, STAGED_DIR_NAME).apply { mkdirs() }
            cleanupOldStagedFiles(dir)
            val target = File(dir, "photo_${System.currentTimeMillis()}.img")
            val copied = copyUriToFile(context, uri, target)
            if (copied && target.exists() && target.length() > 0L) {
                target
            } else {
                target.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed staging URI $uri to cache file", e)
            null
        }
    }

    /**
     * Decodes a fast preview thumbnail from a staged file or content URI for composer previews.
     */
    suspend fun decodePreviewThumbnail(context: Context, uri: Uri?, stagedFile: File?): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val bmp = when {
                    stagedFile != null && stagedFile.exists() -> decodeFileBitmap(stagedFile, 540)
                    uri != null -> decodeUriBitmap(context, uri, 540)
                    else -> null
                }
                bmp?.let { ensureSoftwareBitmap(it) }
            } catch (e: Exception) {
                Log.w(TAG, "Preview thumbnail decode failed", e)
                null
            }
        }

    /**
     * Compresses either a pre-staged local file (preferred) or a raw content URI to a crisp HD
     * JPEG Base64 string guaranteed to fit inside encrypted Firestore documents.
     */
    suspend fun compressToBase64(context: Context, uri: Uri?, stagedFile: File? = null): String? =
        withContext(Dispatchers.IO) {
            try {
                val decoded = when {
                    stagedFile != null && stagedFile.exists() && stagedFile.length() > 0L ->
                        decodeFileBitmap(stagedFile, MAX_DIMENSION_PX)
                            ?: uri?.let { decodeUriBitmap(context, it, MAX_DIMENSION_PX) }
                    uri != null -> decodeUriBitmap(context, uri, MAX_DIMENSION_PX)
                    else -> null
                } ?: return@withContext null

                compressBitmapToBase64(
                    inputBitmap = decoded,
                    maxDim = MAX_DIMENSION_PX,
                    maxBytes = MAX_JPEG_BYTES,
                    initialQuality = 92
                )
            } catch (e: Exception) {
                Log.e(TAG, "Photo compression failed for uri=$uri file=${stagedFile?.path}", e)
                null
            }
        }

    suspend fun compressUriToBase64(context: Context, uri: Uri): String? =
        compressToBase64(context, uri, null)

    /**
     * Compresses a rendered Compose Doodle bitmap to HD JPEG Base64 for the Love Doodle Gallery.
     */
    suspend fun compressDoodleBitmapToBase64(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return@withContext null
            compressBitmapToBase64(
                inputBitmap = copy,
                maxDim = MAX_DOODLE_DIMENSION_PX,
                maxBytes = MAX_DOODLE_BYTES,
                initialQuality = 90
            )
        } catch (e: Exception) {
            Log.e(TAG, "Doodle compression failed", e)
            null
        }
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fast bounds-only inspection returning Pair(width, height) without allocating pixel buffers.
     */
    fun inspectImageDimensions(base64: String): Pair<Int, Int>? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            if (opts.outWidth > 0 && opts.outHeight > 0) opts.outWidth to opts.outHeight else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Play-policy compliant HD photo save to the device Gallery (Pictures/Duet) using MediaStore
     * on Android 10+ (API 29+, zero storage permissions required) and app-scoped Pictures fallback
     * on older API levels.
     */
    suspend fun saveBase64ImageToGallery(
        context: Context,
        imageBase64: String,
        filePrefix: String = "Duet_HD"
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val rawBytes = Base64.decode(imageBase64, Base64.NO_WRAP)
            require(rawBytes.isNotEmpty()) { "Empty image data" }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val safePrefix = filePrefix.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(24)
            val fileName = "${safePrefix}_${stamp}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Duet")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("Could not create MediaStore entry")
                try {
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(rawBytes)
                        out.flush()
                    } ?: error("Could not open MediaStore output stream")
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                } catch (e: Exception) {
                    runCatching { resolver.delete(uri, null, null) }
                    throw e
                }
                "Saved to Gallery • Pictures/Duet"
            } else {
                val dir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir,
                    "Duet"
                ).apply { mkdirs() }
                val dest = File(dir, fileName)
                FileOutputStream(dest).use { out ->
                    out.write(rawBytes)
                    out.flush()
                }
                "Saved HD photo ($fileName)"
            }
        }
    }

    private fun decodeUriBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width
                    val h = info.size.height
                    val longest = max(w, h)
                    if (longest > maxDim && longest > 0) {
                        val scale = maxDim.toFloat() / longest.toFloat()
                        decoder.setTargetSize(
                            (w * scale).roundToInt().coerceAtLeast(1),
                            (h * scale).roundToInt().coerceAtLeast(1)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "ImageDecoder URI decode failed, falling back to temp file copy", e)
            }
        }

        val tempFile = File(context.cacheDir, "duet_decode_${System.nanoTime()}.img")
        try {
            if (copyUriToFile(context, uri, tempFile) && tempFile.length() > 0L) {
                decodeFileBitmap(tempFile, maxDim)?.let { return it }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Temp file fallback decode failed", e)
        } finally {
            tempFile.delete()
        }

        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val fd = pfd.fileDescriptor
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFileDescriptor(fd, null, bounds)
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
                }
                BitmapFactory.decodeFileDescriptor(fd, null, opts)
            }
        } catch (e: Exception) {
            Log.e(TAG, "ParcelFileDescriptor decode fallback failed", e)
            null
        }
    }

    private fun decodeFileBitmap(file: File, maxDim: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(file)
                return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width
                    val h = info.size.height
                    val longest = max(w, h)
                    if (longest > maxDim && longest > 0) {
                        val scale = maxDim.toFloat() / longest.toFloat()
                        decoder.setTargetSize(
                            (w * scale).roundToInt().coerceAtLeast(1),
                            (h * scale).roundToInt().coerceAtLeast(1)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "ImageDecoder file decode failed, falling back to BitmapFactory", e)
            }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
    }

    private fun copyUriToFile(context: Context, uri: Uri, dest: File): Boolean {
        val resolver = context.contentResolver
        val input = try {
            resolver.openInputStream(uri)
        } catch (_: Exception) {
            null
        } ?: try {
            resolver.openAssetFileDescriptor(uri, "r")?.createInputStream()
        } catch (_: Exception) {
            null
        } ?: return false

        input.use { src ->
            FileOutputStream(dest).use { out ->
                src.copyTo(out, bufferSize = 16_384)
                out.flush()
            }
        }
        return true
    }

    private fun compressBitmapToBase64(
        inputBitmap: Bitmap,
        maxDim: Int,
        maxBytes: Int,
        initialQuality: Int
    ): String? {
        val softBitmap = ensureSoftwareBitmap(inputBitmap)
        if (softBitmap != inputBitmap) {
            inputBitmap.recycle()
        }

        var working = scaleDownIfNeeded(softBitmap, maxDim)
        if (working != softBitmap) {
            softBitmap.recycle()
        }

        val output = ByteArrayOutputStream()
        var bytes = ByteArray(0)

        // Adaptive HD quality + gentle dimension safeguard guarantees crisp HD inside Firestore 1MiB cap
        repeat(5) { pass ->
            var quality = when (pass) {
                0 -> initialQuality
                1 -> 86
                2 -> 78
                else -> 68
            }
            val minQuality = if (pass == 0) 76 else 52
            do {
                output.reset()
                working.compress(Bitmap.CompressFormat.JPEG, quality, output)
                bytes = output.toByteArray()
                quality -= 6
            } while (bytes.size > maxBytes && quality >= minQuality)

            if (bytes.size <= maxBytes) {
                working.recycle()
                return Base64.encodeToString(bytes, Base64.NO_WRAP)
            }

            val nextMax = (max(working.width, working.height) * 0.84f).roundToInt().coerceAtLeast(480)
            val smaller = scaleDownIfNeeded(working, nextMax)
            if (smaller != working) {
                working.recycle()
                working = smaller
            }
        }

        working.recycle()
        return if (bytes.isNotEmpty()) Base64.encodeToString(bytes, Base64.NO_WRAP) else null
    }

    private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
        } else {
            bitmap
        }
    }

    private fun computeSampleSize(width: Int, height: Int, maxDim: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        val longest = max(width, height)
        while (longest / sample > maxDim * 2) {
            sample *= 2
        }
        return sample
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longest = max(width, height)
        if (longest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / longest.toFloat()
        val targetW = (width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun cleanupOldStagedFiles(dir: File) {
        val cutoff = System.currentTimeMillis() - 20 * 60 * 1000L
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }
}
