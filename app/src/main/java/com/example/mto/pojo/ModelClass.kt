package com.example.mto.pojo

import com.google.gson.annotations.SerializedName

data class ModelClass(
    @SerializedName("weather") val weather:List<Weather>
)