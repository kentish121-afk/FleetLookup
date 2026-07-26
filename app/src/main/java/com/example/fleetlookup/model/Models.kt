package com.example.fleetlookup.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VehicleListResponse(val results: List<Vehicle> = emptyList())

@Serializable
data class Vehicle(
    val id: Long? = null,
    val slug: String? = null,
    @SerialName("fleet_code") val fleetCode: String? = null,
    @SerialName("fleet_number") val fleetNumber: Int? = null,
    val reg: String? = null,
    @SerialName("vehicle_type") val vehicleType: VehicleType? = null,
    val livery: Livery? = null,
    val operator: Operator? = null,
    val garage: Garage? = null,
    val branding: String? = null,
    @SerialName("special_features") val specialFeatures: List<String>? = null
) {
    val title: String get() = listOfNotNull(fleetCode ?: fleetNumber?.toString(), reg).joinToString(" • ")
}

@Serializable data class VehicleType(val name: String? = null, val fuel: String? = null, @SerialName("double_decker") val doubleDecker: Boolean? = null, val electric: Boolean? = null)
@Serializable data class Livery(val name: String? = null, val colour: String? = null)
@Serializable data class Operator(val id: String? = null, val name: String? = null)
@Serializable data class Garage(val name: String? = null)
