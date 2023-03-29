package gearth

import javafx.beans.property.SimpleBooleanProperty
import java.nio.file.Paths

private val propertiesManager by lazy { PropertiesManager(Paths.get("session.properties")) }


val autoConnectSelected by lazy {
    booleanProperty("autoConnectSelected", true)
}

private fun booleanProperty(name: String, default: Boolean) =
    SimpleBooleanProperty(default).apply { propertiesManager.bindBoolean(name, this) }