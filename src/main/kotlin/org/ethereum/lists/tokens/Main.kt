package org.ethereum.lists.tokens

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import java.io.File
import kotlin.system.exitProcess
import org.kethereum.model.ChainId

val networkMapping = mapOf(
    "eth" to 1L,
    "esn" to 2,
    "rop" to 3,
    "rin" to 4,
    "gor" to 5,
    "ubq" to 8,
    "rsk" to 30,
    "kov" to 42,
    "bsc" to 56,
    "etc" to 61,
    "ella" to 64,
    "sonic" to 146,
    "arb" to 42161,
    "avax" to 43114,
    "zks" to 324,
    "vc" to 207
)

suspend fun main() {
    checkForTokenDefinitionsInWrongPath()

    allNetworksTokenDir.listFiles()?.forEach { singleNetworkTokenDirectory ->
        val jsonArray = JsonArray<JsonObject>()

        if (!networkMapping.containsKey(singleNetworkTokenDirectory.name)) {
            println("Found directory for unknown chain " + singleNetworkTokenDirectory.name)

            exitProcess(1)
        }

        singleNetworkTokenDirectory.listFiles()?.forEach {
            try {
                it.reader().use { reader ->
                    jsonArray.add(Klaxon().parseJsonObject(reader))
                }

                checkTokenFile(it, false, getChainId(singleNetworkTokenDirectory.name))
            } catch (e: Exception) {
                println("Problem with $it: $e")

                exitProcess(1)
            }
        }

        jsonArray.writeJSON("full", singleNetworkTokenDirectory.name)
        val minified = jsonArray.copyFields(mandatoryFields)
        minified.writeJSON("minified", singleNetworkTokenDirectory.name)
        networkMapping[singleNetworkTokenDirectory.name]?.let {
            minified.writeJSON("minifiedByNetworkId", it.toString())
        }
    }
}

fun getChainId(networkName: String) = networkMapping[networkName]?.let {
    ChainId(it.toBigInteger())
}

private fun checkForTokenDefinitionsInWrongPath() {
    val tokensRoot = allNetworksTokenDir.toPath().toAbsolutePath().normalize()
    val testTokensRoot = File("src/test/resources/test_tokens").toPath().toAbsolutePath().normalize()

    File(".").walk().forEach { candidate ->
        if (!candidate.isDirectory) {
            return@forEach
        }

        val normalizedPath = candidate.toPath().toAbsolutePath().normalize()
        val isTokensDir = normalizedPath.startsWith(tokensRoot)
        val isTestTokensDir = normalizedPath.startsWith(testTokensRoot)

        if (!isTokensDir && !isTestTokensDir) {
            candidate.list()?.firstOrNull { it.startsWith("0x") }?.let {
                throw IllegalArgumentException(
                    "There is a token definition file ($it) placed in a directory where it does not belong ($normalizedPath)"
                )
            }
        }
    }
}

fun JsonArray<*>.writeJSON(pathName: String, filename: String) {
    val fullOutDir = File(outDir, pathName)
    fullOutDir.mkdirs()
    val fullOutFile = File(fullOutDir, "$filename.json")

    fullOutFile.writeText(toJsonString(false))
}

fun List<JsonObject>.copyFields(fields: List<String>): JsonArray<JsonObject> {
    val minimalJSONArray = JsonArray<JsonObject>()
    forEach { jsonObject ->
        val minimalJsonObject = JsonObject()
        fields.forEach {
            minimalJsonObject[it] = jsonObject[it]
        }
        minimalJSONArray.add(minimalJsonObject)
    }
    return minimalJSONArray
}