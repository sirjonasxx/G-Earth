package gearth

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File

@kotlinx.serialization.Serializable
data class Configuration(val port: Int, val email: String, val password: String)

private val json = Json {
    prettyPrint = true
}

@ExperimentalSerializationApi
val configurations by lazy {
    val file = File("configurations.json")
    if (file.exists()) {
        json.decodeFromStream<Array<Configuration>>(file.inputStream())
    } else
        emptyArray()
}

fun configuration(port: Int) = configurations.find { it.port == port }
