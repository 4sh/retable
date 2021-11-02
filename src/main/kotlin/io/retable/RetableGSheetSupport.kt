package io.retable


import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import java.io.*
import java.util.*


class GSheetReadOptions(
        val credentialFilePath: String = "credentials.json",
        var spreadsheetId: String? = null,
        val sheetName: String? = null, // sheet name (used in priority over sheet index, if provided)
        val sheetIndex: Int? = null, // sheet index (one based)
        val tokensDirectoryPath: String = "tokens",
    trimValues: Boolean = true,
    ignoreEmptyLines: Boolean = true,
    firstRecordAsHeader: Boolean = true
) : ReadOptions(trimValues, ignoreEmptyLines, firstRecordAsHeader)


class RetableGSheetSupport<T : RetableColumns>(
    columns: T,
    options: GSheetReadOptions
) : BaseSupport<T, GSheetReadOptions>(columns, options) {

    private val service : Sheets = Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
            .setApplicationName("Retable")
            .build()

    override fun iterator(input: InputStream): Iterator<List<String>> {
        val sheetName = options.sheetName
                ?: service.spreadsheets()
                        .get(options.spreadsheetId)
                        .execute()
                        .sheets[options.sheetIndex?.minus(1) ?: 0].properties.title

        val response: ValueRange = service.spreadsheets().values().get(options.spreadsheetId, sheetName)
                .execute()
        return response.getValues().map { it.map { value -> value.toString() } }.iterator()
    }

    override fun write(columns: T, records: Sequence<RetableRecord>, outputStream: OutputStream) {
        if (options.spreadsheetId == null) {
            var spreadsheet: Spreadsheet = Spreadsheet()
                    .setProperties(SpreadsheetProperties()
                            .setTitle(options.sheetName))

            spreadsheet = service.spreadsheets().create(spreadsheet)
                    .setFields("spreadsheetId")
                    .execute()

            options.spreadsheetId = spreadsheet.spreadsheetId
        }

        val values = listOf(
                columns.list()
                        .sortedBy { it.index }
                        .map { it.name }
        ) + records.sortedBy { it.lineNumber }
                .map { it.rawData }

        val body = ValueRange()
                .setValues(values)

        service.spreadsheets().values().update(options.spreadsheetId, "A1", body)
                .setValueInputOption("USER_ENTERED")
                .execute()

    }

    private fun getCredentials(httpTransport: NetHttpTransport): Credential? {
        val inputStream: InputStream = this::class.java.classLoader.getResourceAsStream(options.credentialFilePath)
                ?: throw FileNotFoundException("Resource not found " + options.credentialFilePath)
        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(inputStream))
        val flow = GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPE)
                .setDataStoreFactory(FileDataStoreFactory(File(options.tokensDirectoryPath)))
                .build()
        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("jean-pierre")
    }

    companion object {
        private val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()
        private val SCOPE = Collections.singletonList(SheetsScopes.SPREADSHEETS)
        private val HTTP_TRANSPORT: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()
    }
}
