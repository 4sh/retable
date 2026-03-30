package io.retable

import org.apache.poi.ss.usermodel.DataValidation
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.usermodel.XSSFSheet

/**
 * Sets up Excel data validation (dropdown lists) for all [StringRetableColumn]s that define allowed values.
 *
 * Tries the standard inline approach first ([buildStdValueValidations]). If it fails due to the
 * Excel 255-character formula limit, falls back to a hidden sheet-based approach
 * ([buildHiddenSheetBasedValueValidations]).
 */
fun XSSFSheet.withValueConstraints(options: ExcelReadOptions, columns: RetableColumns): XSSFSheet {
    val constrainedColumns = columns.list()
        .filterIsInstance<StringRetableColumn>()
        .filter { it.allowedValues.orEmpty().isNotEmpty() }

    if (constrainedColumns.isNotEmpty()) {
        val validations = try {
            buildStdValueValidations(options, constrainedColumns)
        } catch (_: IllegalArgumentException) {
            // excel formula 255-character limit potentially exceeded,
            // fallback to hidden sheet-based value constraints
            buildHiddenSheetBasedValueValidations(options, constrainedColumns)
        }

        validations.forEach { addValidationData(it) }
    }

    return this
}

private fun XSSFSheet.buildStdValueValidations(
    options: ExcelReadOptions,
    columns: List<StringRetableColumn>
): List<DataValidation> =
    columns.map { column ->
        val columnIndex = column.index - 1

        val allowedValues = column.allowedValues.orEmpty()
        val constraint = dataValidationHelper.createExplicitListConstraint(allowedValues.toTypedArray())
        val cellRange = CellRangeAddressList(
            if (options.firstRecordAsHeader) 1 else 0,
            workbook.spreadsheetVersion.lastRowIndex,
            columnIndex,
            columnIndex
        )
        dataValidationHelper
            .createValidation(constraint, cellRange)
            .also {
                it.showPromptBox = true
                it.suppressDropDownArrow = true
            }
    }

private fun XSSFSheet.buildHiddenSheetBasedValueValidations(
    options: ExcelReadOptions,
    columns: List<StringRetableColumn>
): List<DataValidation> {
    val constraintSheetName = "_constraints_${this.sheetName}"
    val constraintSheet = workbook.createSheet(constraintSheetName)
    val constraintSheetIndex = workbook.getSheetIndex(constraintSheet)
    workbook.setSheetHidden(constraintSheetIndex, true)

    return columns.map { column ->
        val columnIndex = column.index - 1

        val allowedValues = column.allowedValues.orEmpty()
        allowedValues.forEachIndexed { rowIndex, value ->
            val row = constraintSheet.getRow(rowIndex)
                ?: constraintSheet.createRow(rowIndex)
            row.createCell(columnIndex).setCellValue(value)
        }

        val rangeName = "${constraintSheetName}_${columnIndex}"
        val lastRow = allowedValues.size
        val colLetter = columnIndexToLetter(columnIndex)
        val formulaRef = "'$constraintSheetName'!\$${colLetter}\$1:\$${colLetter}\$$lastRow"

        val name = workbook.createName()
        name.nameName = rangeName
        name.refersToFormula = formulaRef

        val constraint = dataValidationHelper.createFormulaListConstraint(rangeName)
        val cellRange = CellRangeAddressList(
            if (options.firstRecordAsHeader) 1 else 0,
            workbook.spreadsheetVersion.lastRowIndex,
            columnIndex,
            columnIndex
        )

        dataValidationHelper
            .createValidation(constraint, cellRange)
            .also {
                it.showPromptBox = true
                it.suppressDropDownArrow = true
            }
    }
}

private fun columnIndexToLetter(index: Int): String {
    var result = ""
    var i = index
    while (i >= 0) {
        result = ('A' + i % 26) + result
        i = i / 26 - 1
    }
    return result
}
