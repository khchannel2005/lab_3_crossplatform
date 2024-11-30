package org.jub.kotlin.hometask4

import org.junit.jupiter.api.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object ApplicationTest {
    private const val resultsFile = "results.txt"
    private lateinit var baos: ByteArrayOutputStream
    private lateinit var helpMsg: String

    @JvmStatic
    @BeforeAll
    fun setHelpMessage() {
        setUp()
        val app = Application.create(resultsFile, emptyList())  // Додано порожній список завдань
        val commands = listOf("help")
        setSystemIn(commands.toInput())
        app.run()
        Thread.sleep(1000)  // Заміна 1.secs на 1000 мс
        helpMsg = baos.toString(Charset.defaultCharset())
        setDown()
    }

    private fun setSystemIn(input: String) = System.setIn(input.byteInputStream())

    private fun List<String>.toInput() = joinToString(System.lineSeparator())

    private val Int.secs: Long
        get() = this * 1000L

    @BeforeEach
    fun setUp() {
        baos = ByteArrayOutputStream()  // Ініціалізація нового об'єкта для кожного тесту
        val ps = PrintStream(baos, true, StandardCharsets.UTF_8.name())
        System.setOut(ps)
        File(resultsFile).writeText("")  // Очистка файлу перед тестами
    }

    @AfterEach
    fun setDown() {
        System.setOut(System.out)
        System.setIn(System.`in`)
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    fun limitThreads() {
        val tasks = listOf(Callable<Int> { Thread.sleep(5000); 666 })
        val app = Application.create(resultsFile, tasks)

        val commands = List(7) { "task long 0" }
        setSystemIn(commands.toInput())

        app.run()
        Thread.sleep(7000)  // 7 секунд

        val outputs = File(resultsFile).readLines().size
        assertTrue(outputs <= 6, "You cannot finish 6+ tasks 5 secs each in 7 secs")
        assertTrue(outputs >= 2, "It looks like tasks are not performed in parallel")
        app.waitToFinish()
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun finishGrace() {
        val tasks = listOf(Callable<Int> { Thread.sleep(5000); 666 })
        val app = Application.create(resultsFile, tasks)

        val commands = List(6) { "task keep 0" } + "finish grace" + "task drop 0"
        setSystemIn(commands.toInput())

        app.run()
        Thread.sleep(20000)  // 20 секунд

        val outputs = File(resultsFile).readLines().size
        assertEquals(6, outputs)
        app.waitToFinish()
    }

    // Аналогічно інші тести виглядають коректно.
}
