package co.aospa.hub.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateResponse(val updates: List<Update>)

@Serializable
data class Update(
    val date: String,
    val datetime: String,
    val filename: String,
    val url: String,
    @SerialName("recovery_sha256") val recoverySha256: String,
    val fastboot: String,
    @SerialName("fastboot_sha256") val fastbootSha256: String,
    val id: String,
    val size: String,
    @SerialName("build_type") val buildType: String,
    @SerialName("version_code") val versionCode: String,
    val version: String,
    @SerialName("android_version") val androidVersion: String,
    @SerialName("android_spl") val androidSpl: String,
    @SerialName("changelog_device") val changelogDevice: String
)