package gearth

import javafx.beans.property.*
import javafx.scene.paint.Color
import javafx.scene.paint.Material
import javafx.scene.paint.PhongMaterial
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class PropertiesManager(private val saveFilePath: Path) {

    private val logger = LoggerFactory.getLogger(PropertiesManager::class.java)
    private val properties = Properties()
    private lateinit var saveThread: Thread

    fun loadFromFile(): Boolean {

        if (!saveFilePath.toFile().exists())
            return false

        try {
            properties.load(saveFilePath.toFile().reader())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    fun startSaveThread() {
        if (!this::saveThread.isInitialized) {
            saveThread = Thread {
                saveToFile()
                Thread.sleep(2500L)
            }
            saveThread.start()
        }
    }

    fun saveToFile() {
        logger.info("Saving properties to $saveFilePath")
        try {
            val saveFile = saveFilePath.toFile()
            if (!saveFile.parentFile.exists())
                saveFile.parentFile.mkdir()
            properties.store(saveFile.writer(), "Contains properties for the Qodat application.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun <T> bind(key: String, property: Property<T>, transformer: (String) -> T) {
        val value = properties.getProperty(key)
        if (value != null)
            property.value = transformer.invoke(value)
        property.addListener { _ ->
            properties.setProperty(key, property.value.toString())
        }
    }

    /**
     * TODO: finish
     */
    fun bindMaterial(key: String, property: ObjectProperty<Material>) =
        bind(key, property) { PhongMaterial() }

    fun bindPath(key: String, property: ObjectProperty<Path>) =
        bind(key, property) { Paths.get(it) }

    inline fun <reified T : Enum<T>> bindEnum(key: String, property: ObjectProperty<T>) =
        bind(key, property) { enumValueOf<T>(it) }

    fun bindColor(key: String, property: ObjectProperty<Color>) =
        bind(key, property) { Color.valueOf(it) }

    fun bindBoolean(key: String, property: BooleanProperty) =
        bind(key, property) { java.lang.Boolean.parseBoolean(it) }

    fun bindDouble(key: String, property: DoubleProperty) =
        bind(key, property) { java.lang.Double.parseDouble(it) }

    fun bindInt(key: String, property: IntegerProperty) =
        bind(key, property) { Integer.parseInt(it) }

    fun bindString(key: String, property: StringProperty) =
        bind(key, property) { it }

}

