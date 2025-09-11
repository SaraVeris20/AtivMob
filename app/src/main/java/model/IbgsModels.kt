package com.example.ativmob.model // Or your chosen package

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Estado(
    @SerialName("id") val id: Int,
    @SerialName("sigla") val sigla: String,
    @SerialName("nome") val nome: String,
    @SerialName("regiao") val regiao: Regiao
)

@Serializable
data class Regiao(
    @SerialName("id") val id: Int,
    @SerialName("sigla") val sigla: String,
    @SerialName("nome") val nome: String
)