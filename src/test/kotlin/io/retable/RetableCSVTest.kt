package io.retable

import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isTrue
import java.io.ByteArrayInputStream
import java.io.File
import java.time.LocalDate

class RetableCSVTest {
    fun pathTo(s:String) = "src/test/resources/examples/$s"

    @Test
    fun `should parse csv with custom charset from string`() {
        val csv = """
            Prénom,Nom
            Ñino,Dalton
        """.trimIndent()

        val retable = Retable.csv(options = CSVReadOptions(
                charset = Charsets.ISO_8859_1
        )).read(ByteArrayInputStream(csv.toByteArray(Charsets.ISO_8859_1)))

        val columns = RetableColumns.ofNames(listOf("Prénom", "Nom"))
        expectThat(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        expectThat(retable.records).containsExactly(
                RetableRecord(columns,1, 2, listOf("Ñino", "Dalton"))
        )
    }

    @Test
    fun `should parse csv with custom settings`() {
        val csv = """
            `Arthur `;` Rimbaud `;`Le dormeur du val ; 1870`

            `Victor `;` Hugo `;`Demain, dès l'aube… ; 1856`
            """.trimIndent()

        val retable = Retable.csv(
                columns = RetableColumns.ofNames(listOf("Prénom", "Nom", "Oeuvre")),
                options = CSVReadOptions(
                            delimiter = ';',
                            quote = '`',
                            trimValues = true,
                            ignoreEmptyLines = true,
                            firstRecordAsHeader = false
        )).read(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        expectThat(retable.records).containsExactly(
                RetableRecord(retable.columns,1, 1,
                        listOf("Arthur", "Rimbaud", "Le dormeur du val ; 1870")),
                RetableRecord(retable.columns,2, 3,
                        listOf("Victor", "Hugo", "Demain, dès l'aube… ; 1856"))
        )
    }

    @Test
    fun `should simple export work`() {
        val resultFilePath = pathTo("simple_export_test_result.csv")

        Retable(
                RetableColumns.ofNames(listOf("first_name", "last_name", "age"))
        )
                .data(
                        listOf(
                                listOf("John",  "Doe", 23),
                                listOf("Jenny", "Boe", 25)
                        )
                )
                .write(Retable.csv() to File(resultFilePath).outputStream())


        expectThat(File(resultFilePath)) {
            get {exists()}.isTrue()
        }
    }

    @Test
    fun `should export indexed columns`() {
        val resultFilePath = pathTo("export_data_indexed_cols.csv")
        val columns = object:RetableColumns() {
            val FIRST_NAME = string("first_name", index = 2)
            val LAST_NAME  = string("last_name", index = 1)
            val AGE        = int("age", index = 3)
        }
        Retable(columns)
                .data(
                        listOf(
                                Person("@+==0x09==+-+John", "@+==0x09==+-+Doe", 23),
                                Person("@+==0x09==+-+Jenny", "@+==0x09==+-+Boe", 25)
                        )
                ) {
                    mapOf(
                            FIRST_NAME to it.firstName,
                            LAST_NAME to it.lastName,
                            AGE to it.age
                    )
                }
                .write(Retable.csv(columns) to File(resultFilePath).outputStream())

        expectThat(File(resultFilePath)) {
            get {exists()}.isTrue()
        }
    }

    @Test
    fun `should export columns with local date`() {
        val resultFilePath = pathTo("export_data_cols_date.csv")
        val columns = object:RetableColumns() {
            val NAME = string("name")
            val START_DATE   = localDate("start_date")
            val END_DATE   = localDate("end_date")
        }
        Retable(columns)
                .data(
                        listOf(
                                Event("So Good Fest",
                                        LocalDate.parse("2020-06-05"),
                                        LocalDate.parse("2020-06-06")
                                ),
                                Event("Les Fous-Cavés",
                                        LocalDate.parse("2019-07-19"),
                                        LocalDate.parse("2020-07-21")
                                )
                        )
                ) {
                    mapOf(
                            NAME to it.name,
                            START_DATE to it.startDate,
                            END_DATE to it.endDate
                    )
                }
                .write(Retable.csv(columns) to File(resultFilePath).outputStream())

        expectThat(File(resultFilePath)) {
            get {exists()}.isTrue()
        }
    }


    @Test
    fun `should export cleaning formula chars starting cells`() {
        val resultFilePath = pathTo("export_data_cleaned_from_formula_chars.csv")
        val columns = object:RetableColumns() {
            val FIRST_NAME = string("first_name", index = 2)
            val LAST_NAME  = string("last_name", index = 1)
            val AGE        = int("age", index = 3)
        }
        Retable(columns)
                .data(
                        listOf(
                                Person("@+==0x09==+-+John", "@+==0x09==+-+Doe", 23),
                                Person("@+==0x09==+-+Jenny", "@+==0x09==+-+Boe", 25)
                        )
                ) {
                    mapOf(
                            FIRST_NAME to it.firstName,
                            LAST_NAME to it.lastName,
                            AGE to it.age
                    )
                }
                .write(Retable.csv(columns, CSVReadOptions(removeFirstFormulaChars = true)) to File(resultFilePath).outputStream())

        expectThat(File(resultFilePath)) {
            get {exists()}.isTrue()
        }
    }

    // helper extensions
    fun <T : Sequence<E>, E> Assertion.Builder<T>.containsExactly(vararg elements: E): Assertion.Builder<List<E>>
     = this.get { toList() }.containsExactly(*elements)
}
