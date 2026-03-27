package com.privacycamera.app.data.ml

import android.graphics.RectF
import androidx.exifinterface.media.ExifInterface
import com.privacycamera.app.domain.model.BoundingBox
import com.privacycamera.app.domain.model.SensitiveInfo
import com.privacycamera.app.domain.model.SensitiveType
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExifMetadataReader @Inject constructor() {

    fun readGpsLocation(file: File): SensitiveInfo? {
        return try {
            val exif = ExifInterface(file)
            val latLong = FloatArray(2)
            if (exif.getLatLong(latLong)) {
                val location = "${latLong[0]}, ${latLong[1]}"
                SensitiveInfo(
                    type = SensitiveType.GPS_LOCATION,
                    boundingBox = BoundingBox(0f, 0f, 0f, 0f),
                    content = location,
                    confidence = 1.0f
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun removeGpsData(file: File): Boolean {
        return try {
            val exif = ExifInterface(file)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null)
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, null)
            exif.saveAttributes()
            true
        } catch (e: Exception) {
            false
        }
    }
}
