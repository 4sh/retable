package io.retable

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.isTrue
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.*

class RetablePerformanceTest {

    fun List<Char>.random() = this[Random().nextInt(this.size)]

    fun buildRandomString(minLength: Int, variation: Int): String {
        val chars = ('A'..'Z') + ('a'..'z')
        val length = Random().nextInt(variation) + minLength
        return (1..length).map { chars.random() }
            .joinToString("")
    }

    @ParameterizedTest
//    @ValueSource(ints = [1_000, 10_000, 20_000, 30_000, 40_000])
    @ValueSource(ints = [40_000])
    fun `should list columns`(recordNumber: Int) {
        val columns = object : RetableColumns() {
            val FIRST_NAME = string("first_name", index = 2)
            val LAST_NAME2 = string("last_name1", index = 4)
            val LAST_NAME3 = string("last_name2", index = 5)
            val LAST_NAME4 = string("last_name3", index = 6)
            val LAST_NAME5 = string("last_name4", index = 7)
            val AGE = int("age", index = 3)
            val DATE_2 = localDate("localDate", index = 9)
        }
        val values = (0..recordNumber).map {
            Person2(
                buildRandomString(3, 6),
                buildRandomString(3, 30),
                buildRandomString(3, 30),
                buildRandomString(3, 30),
                buildRandomString(3, 30),
                buildRandomString(3, 30),
                Random().nextInt(30)
            )
        }

        var resultFilePath = pathTo("export_${recordNumber}_EXPERIMENTAL_records_indexed_cols.xlsx")

        val retable = Retable(columns)
            .data(
                values
            ) {
                mapOf(
                    FIRST_NAME to it.firstName,
                    LAST_NAME2 to it.lastName2,
                    LAST_NAME3 to it.lastName3,
                    LAST_NAME4 to it.lastName4,
                    LAST_NAME5 to it.lastName5,
                    AGE to it.age,
                    DATE_2 to it.date2
                )
            }

        var start = Instant.now().toEpochMilli()
        retable.write(
            Retable.excel(
                columns,
                ExcelReadOptions(useExperimentalFasterAutoSizeColumn = true)
            ) to File(resultFilePath).outputStream()
        )
        var end = Instant.now().toEpochMilli()
        val customDuration = end - start
        println("Write Duration (Experimental) : $customDuration")
        expectThat(File(resultFilePath)) {
            get { exists() }.isTrue()
        }

        resultFilePath = pathTo("export_${recordNumber}_POI_records_indexed_cols.xlsx")
        start = Instant.now().toEpochMilli()
        retable
            .write(Retable.excel(columns) to File(resultFilePath).outputStream())
        end = Instant.now().toEpochMilli()
        val classicDuration = end - start
        println("Write Duration (ApachePoi) : $classicDuration")
        expectThat(File(resultFilePath)) {
            get { exists() }.isTrue()
        }

        println("Write Duration (Experimental / ApachePoi) : $customDuration / $classicDuration")
    }

    fun pathTo(s: String) = "src/test/resources/examples/$s"

    data class Person2(
        val firstName: String,
        val lastName: String,
        val lastName2: String,
        val lastName3: String,
        val lastName4: String,
        val lastName5: String,
        val age: Int,
        val date1: Instant = Instant.now(),
        val date2: LocalDate = LocalDate.now()
    )
}
