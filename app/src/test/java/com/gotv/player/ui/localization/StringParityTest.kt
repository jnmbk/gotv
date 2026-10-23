package com.gotv.player.ui.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringParityTest {

    private data class ParsedResources(
        val strings: Map<String, String>,
        val plurals: Map<String, Map<String, String>>
    )

    private fun parseResourceFile(file: File): ParsedResources {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(file)
        doc.documentElement.normalize()

        val strings = mutableMapOf<String, String>()
        val plurals = mutableMapOf<String, MutableMap<String, String>>()

        val rootChildren = doc.documentElement.childNodes
        for (i in 0 until rootChildren.length) {
            val node = rootChildren.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                when (element.nodeName) {
                    "string" -> {
                        val name = element.getAttribute("name")
                        val value = element.textContent
                        strings[name] = value
                    }
                    "plurals" -> {
                        val name = element.getAttribute("name")
                        val quantityMap = mutableMapOf<String, String>()
                        val itemChildren = element.getElementsByTagName("item")
                        for (j in 0 until itemChildren.length) {
                            val itemElement = itemChildren.item(j) as Element
                            val quantity = itemElement.getAttribute("quantity")
                            quantityMap[quantity] = itemElement.textContent
                        }
                        plurals[name] = quantityMap
                    }
                }
            }
        }

        return ParsedResources(strings, plurals)
    }

    private fun locateFile(relativePath: String): File {
        val rootPath = File(".").canonicalFile
        val fileAtRoot = File(rootPath, relativePath)
        if (fileAtRoot.exists()) return fileAtRoot

        val fileInApp = File(rootPath, "app/$relativePath")
        if (fileInApp.exists()) return fileInApp

        // Try climbing parent if run from deeper directory
        val parent = rootPath.parentFile
        if (parent != null) {
            val fileFromParent = File(parent, relativePath)
            if (fileFromParent.exists()) return fileFromParent
            val fileFromParentApp = File(parent, "app/$relativePath")
            if (fileFromParentApp.exists()) return fileFromParentApp
        }

        throw AssertionError("Cannot find resource file: $relativePath from ${rootPath.path}")
    }

    @Test
    fun englishAndTurkishResources_haveExactKeyParity() {
        val defaultStringsFile = locateFile("src/main/res/values/strings.xml")
        val turkishStringsFile = locateFile("src/main/res/values-tr/strings.xml")

        assertTrue("Default strings.xml should exist", defaultStringsFile.exists())
        assertTrue("Turkish strings.xml should exist", turkishStringsFile.exists())

        val defaultRes = parseResourceFile(defaultStringsFile)
        val turkishRes = parseResourceFile(turkishStringsFile)

        // String keys parity
        val defaultStringKeys = defaultRes.strings.keys
        val turkishStringKeys = turkishRes.strings.keys

        val missingInTurkish = defaultStringKeys - turkishStringKeys
        val extraInTurkish = turkishStringKeys - defaultStringKeys

        assertTrue("Missing string keys in Turkish: $missingInTurkish", missingInTurkish.isEmpty())
        assertTrue("Extra string keys in Turkish: $extraInTurkish", extraInTurkish.isEmpty())
        assertEquals("Total string count should match", defaultStringKeys.size, turkishStringKeys.size)

        // Plural keys parity
        val defaultPluralKeys = defaultRes.plurals.keys
        val turkishPluralKeys = turkishRes.plurals.keys

        val missingPluralsInTurkish = defaultPluralKeys - turkishPluralKeys
        val extraPluralsInTurkish = turkishPluralKeys - defaultPluralKeys

        assertTrue("Missing plurals in Turkish: $missingPluralsInTurkish", missingPluralsInTurkish.isEmpty())
        assertTrue("Extra plurals in Turkish: $extraPluralsInTurkish", extraPluralsInTurkish.isEmpty())
        assertEquals("Total plurals count should match", defaultPluralKeys.size, turkishPluralKeys.size)

        // Verify all values are non-empty
        defaultRes.strings.forEach { (key, value) ->
            assertTrue("Default string for '$key' should not be empty", value.isNotBlank())
        }
        turkishRes.strings.forEach { (key, value) ->
            assertTrue("Turkish string for '$key' should not be empty", value.isNotBlank())
        }

        // Verify plural item quantities (one, other)
        defaultRes.plurals.forEach { (pluralKey, _) ->
            val turkishItems = turkishRes.plurals[pluralKey]
                ?: throw AssertionError("Turkish plural not found: $pluralKey")
            assertTrue("Turkish plural '$pluralKey' missing 'other' quantity", turkishItems.containsKey("other"))
            assertTrue("Turkish plural '$pluralKey' missing 'one' quantity", turkishItems.containsKey("one"))
        }
    }
}
