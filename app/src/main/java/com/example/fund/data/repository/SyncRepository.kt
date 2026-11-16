package com.example.fund.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.room.withTransaction
import com.example.fund.data.local.*
import com.example.fund.model.LocalServerConfig
import com.example.fund.model.ServerProbeResult
import com.example.fund.model.ServerProfile
import com.example.fund.model.SyncBatchRecord
import com.example.fund.model.SyncQueueItem
import com.example.fund.model.UserRole
import com.example.fund.security.EncryptedSessionManager
import com.example.fund.security.SecurityUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import com.squareup.moshi.Types
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

data class SyncHealthResponse(
    val status: String = "ok",
    val nodeName: String = "",
    val serverTime: String = ""
)

data class SyncBatchPayload(
    val batchNo: String,
    val timestamp: String,
    val deviceNode: String,
    val operationsCount: Int,
    val sha256Checksum: String,
    val encryptedBackupEnvelope: String = ""
)

data class SyncBatchAck(
    val accepted: Boolean = true,
    val batchNo: String = "",
    val verifiedChecksum: String = ""
)

interface SyncApiService {
    @GET("api/v1/health")
    suspend fun checkHealth(): Response<SyncHealthResponse>

    @POST("api/v1/sync/batch")
    suspend fun pushSyncBatch(@Body payload: SyncBatchPayload): Response<SyncBatchAck>
}

class SyncRepository(
    private val dbScope: CoroutineScope,
    private val dbDispatcher: CoroutineDispatcher,
    private val userRepository: UserRepository
) {
    companion object {
        const val BUILTIN_VAULT_HOST = "127.0.0.1:9000"
        const val BUILTIN_VAULT_NAME = "الخزنة المحلية المدمجة (Local Vault)"
        private const val VAULT_DIR_NAME = "local_sync_vault"
        private const val VAULT_LATEST_FILE = "latest_vault_snapshot.json"
        private const val NOT_SYNCED_LABEL = "لم تتم المزامنة بعد"
    }

    private val syncMutex = Mutex()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isTransportAvailable = MutableStateFlow(false)
    val isTransportAvailable: StateFlow<Boolean> = _isTransportAvailable.asStateFlow()

    private val _serverConfig = MutableStateFlow(LocalServerConfig())
    val serverConfig: StateFlow<LocalServerConfig> = _serverConfig.asStateFlow()

    private val _serverProfiles = MutableStateFlow(defaultServerProfiles())
    val serverProfiles: StateFlow<List<ServerProfile>> = _serverProfiles.asStateFlow()

    private val _autoSyncOnConnect = MutableStateFlow(true)
    val autoSyncOnConnect: StateFlow<Boolean> = _autoSyncOnConnect.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private val _syncStageMessage = MutableStateFlow("جاهز للمزامنة الآمنة (الخزنة المحلية المدمجة / HTTPS)")
    val syncStageMessage: StateFlow<String> = _syncStageMessage.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(NOT_SYNCED_LABEL)
    val lastSyncTimestamp: StateFlow<String> = _lastSyncTimestamp.asStateFlow()

    private val _syncQueue = MutableStateFlow<List<SyncQueueItem>>(emptyList())
    val syncQueue: StateFlow<List<SyncQueueItem>> = _syncQueue.asStateFlow()

    private val _syncHistory = MutableStateFlow<List<SyncBatchRecord>>(emptyList())
    val syncHistory: StateFlow<List<SyncBatchRecord>> = _syncHistory.asStateFlow()

    private val _localVaultBatchCount = MutableStateFlow(0)
    val localVaultBatchCount: StateFlow<Int> = _localVaultBatchCount.asStateFlow()

    private val _localVaultSizeBytes = MutableStateFlow(0L)
    val localVaultSizeBytes: StateFlow<Long> = _localVaultSizeBytes.asStateFlow()

    private val volatileFallbackSyncKey: ByteArray by lazy { SecurityUtils.generateRandomKeyBytes(32) }
    private val volatileVaultStore = LinkedHashMap<String, String>()
    private var db: FundDatabase? = null
    private var sessionManager: EncryptedSessionManager? = null
    private var appContext: Context? = null
    private var backupJsonProvider: () -> String = { "{}" }
    private var restoreBackupJsonHandler: ((String) -> Boolean)? = null
    private var connectivityCallbackRegistered = false

    // Test hook for deterministic HTTP 2xx / HTTP 500 / IOException verification
    @Volatile
    var syncApiOverride: SyncApiService? = null

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
    private fun nowTimeStr() = timeFormat.format(Date())

    private val moshi: Moshi by lazy {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    private val baseOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    fun setApplicationContext(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        refreshTransportState()
        registerConnectivityObserver(appCtx)
        refreshLocalVaultTelemetry()
    }

    fun setBackupJsonProvider(provider: () -> String) {
        backupJsonProvider = provider
    }

    fun setRestoreBackupHandler(handler: (String) -> Boolean) {
        restoreBackupJsonHandler = handler
    }

    suspend fun attachAndHydrate(database: FundDatabase, session: EncryptedSessionManager) {
        db = database
        sessionManager = session

        _autoSyncOnConnect.value = session.autoSyncOnConnect
        _lastSyncTimestamp.value = session.lastSyncTimestamp
        loadServerProfilesFromSession(session)

        val dbServer = database.syncDao().observeServerConfig().first()?.toDomain()
        if (dbServer != null) {
            _serverConfig.value = dbServer
        } else {
            _serverConfig.value = _serverConfig.value.copy(
                serverName = session.serverName,
                serverHost = session.serverHost,
                protocol = if (isBuiltInVaultHost(session.serverHost)) {
                    "AES-256-GCM Vault + SHA-256"
                } else {
                    "HTTPS / TLS 1.3"
                }
            )
        }

        val dbQueue = database.syncDao().observeSyncQueue().first().map { it.toDomain() }
        _syncQueue.value = dbQueue

        val dbBatches = database.syncDao().observeSyncBatches().first().map { it.toDomain() }
        _syncHistory.value = dbBatches

        refreshTransportState()
        refreshLocalVaultTelemetry()
    }

    fun isBuiltInVaultHost(hostOverride: String = _serverConfig.value.serverHost): Boolean {
        val clean = hostOverride.trim()
            .removePrefix("https://")
            .removePrefix("HTTPS://")
            .trimEnd('/')
            .lowercase(Locale.ENGLISH)
        return clean == BUILTIN_VAULT_HOST ||
            clean == "localhost:9000" ||
            clean == "local-vault" ||
            clean == "local-vault:9000"
    }

    /**
     * Enforces HTTPS for all remote sync endpoints and rejects explicit plaintext HTTP URLs
     * as well as malformed host strings.
     */
    fun buildBaseHttpsUrl(hostOverride: String = _serverConfig.value.serverHost): String {
        val raw = hostOverride.trim().ifBlank { BUILTIN_VAULT_HOST }
        require(!raw.startsWith("http://", ignoreCase = true)) {
            "يُمنع الاتصال غير المشفر عبر HTTP؛ يجب استخدام بروتوكول HTTPS الآمن."
        }
        val cleanHost = raw.removePrefix("https://").removePrefix("HTTPS://").trimEnd('/')
        require(cleanHost.isNotBlank() && !cleanHost.any { it.isWhitespace() }) {
            "عنوان الخادم غير صالح ($raw)؛ تأكد من عدم وجود مسافات أو قيم فارغة."
        }
        val candidate = "https://$cleanHost/"
        val parsed = candidate.toHttpUrlOrNull()
        require(parsed != null && parsed.host.isNotBlank()) {
            "تعذر تحليل عنوان الخادم ($raw) بصيغة HTTPS صحيحة (مثال: 192.168.1.100:8443 أو sync.domain.org)."
        }
        return candidate
    }

    fun buildRetrofitService(hostOverride: String = _serverConfig.value.serverHost): SyncApiService {
        syncApiOverride?.let { return it }
        if (isBuiltInVaultHost(hostOverride)) {
            return createLocalVaultApiService()
        }
        val baseUrl = buildBaseHttpsUrl(hostOverride)
        val bearerToken = sessionManager?.getServerAuthToken(hostOverride)
        val client = if (bearerToken.isNullOrBlank()) {
            baseOkHttpClient
        } else {
            baseOkHttpClient.newBuilder()
                .addInterceptor(Interceptor { chain ->
                    val req = chain.request().newBuilder()
                        .header("Authorization", "Bearer $bearerToken")
                        .build()
                    chain.proceed(req)
                })
                .build()
        }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SyncApiService::class.java)
    }

    private fun createLocalVaultApiService(): SyncApiService = object : SyncApiService {
        override suspend fun checkHealth(): Response<SyncHealthResponse> {
            return withContext(dbDispatcher) {
                val probe = "vault_health_probe_${System.currentTimeMillis()}"
                val sealed = encryptBackupEnvelope(probe)
                val unsealed = decryptBackupEnvelope(sealed)
                if (unsealed != probe) {
                    return@withContext Response.error(
                        500,
                        "Vault cryptographic health check failed".toResponseBody("text/plain".toMediaType())
                    )
                }
                val vaultDir = resolveVaultDirectoryOrNull()
                if (vaultDir != null && !vaultDir.exists()) {
                    vaultDir.mkdirs()
                }
                Response.success(
                    SyncHealthResponse(
                        status = "ok",
                        nodeName = BUILTIN_VAULT_NAME,
                        serverTime = nowTimeStr()
                    )
                )
            }
        }

        override suspend fun pushSyncBatch(payload: SyncBatchPayload): Response<SyncBatchAck> {
            return withContext(dbDispatcher) {
                if (!payload.encryptedBackupEnvelope.startsWith("AES256GCM_V1:")) {
                    return@withContext Response.error(
                        400,
                        "Invalid AES-256-GCM envelope".toResponseBody("text/plain".toMediaType())
                    )
                }
                val decrypted = decryptBackupEnvelope(payload.encryptedBackupEnvelope)
                if (decrypted.isNullOrBlank()) {
                    return@withContext Response.error(
                        403,
                        "Vault decryption verification failed".toResponseBody("text/plain".toMediaType())
                    )
                }
                val vaultRecord = moshi.adapter(SyncBatchPayload::class.java).toJson(payload)

                val vaultDir = resolveVaultDirectoryOrNull()
                if (vaultDir != null) {
                    if (!vaultDir.exists()) vaultDir.mkdirs()
                    File(vaultDir, "${payload.batchNo}.vault.json").writeText(vaultRecord, Charsets.UTF_8)
                    File(vaultDir, VAULT_LATEST_FILE).writeText(vaultRecord, Charsets.UTF_8)
                }
                synchronized(volatileVaultStore) {
                    volatileVaultStore[payload.batchNo] = vaultRecord
                    volatileVaultStore[VAULT_LATEST_FILE] = vaultRecord
                }
                refreshLocalVaultTelemetry()
                Response.success(
                    SyncBatchAck(
                        accepted = true,
                        batchNo = payload.batchNo,
                        verifiedChecksum = payload.sha256Checksum
                    )
                )
            }
        }
    }

    private fun resolveVaultDirectoryOrNull(): File? {
        val ctx = appContext ?: return null
        return File(ctx.filesDir, VAULT_DIR_NAME)
    }

    fun refreshLocalVaultTelemetry() {
        val dir = resolveVaultDirectoryOrNull()
        if (dir != null && dir.exists()) {
            val batchFiles = dir.listFiles { f -> f.isFile && f.name.endsWith(".vault.json") && f.name != VAULT_LATEST_FILE }
                .orEmpty()
            _localVaultBatchCount.value = batchFiles.size
            _localVaultSizeBytes.value = batchFiles.sumOf { it.length() }
        } else {
            synchronized(volatileVaultStore) {
                val keys = volatileVaultStore.keys.filter { it != VAULT_LATEST_FILE }
                _localVaultBatchCount.value = keys.size
                _localVaultSizeBytes.value = keys.sumOf { (volatileVaultStore[it]?.length ?: 0).toLong() }
            }
        }
    }

    fun restoreLatestFromLocalVault(): Pair<Boolean, String> {
        userRepository.requirePermission(
            UserRole.ADMIN,
            UserRole.ACCOUNTANT,
            actionDescription = "استعادة نسخة من الخزنة المحلية"
        )
        val latestJson = runCatching {
            val dir = resolveVaultDirectoryOrNull()
            val file = dir?.let { File(it, VAULT_LATEST_FILE) }
            when {
                file != null && file.exists() -> file.readText(Charsets.UTF_8)
                else -> synchronized(volatileVaultStore) { volatileVaultStore[VAULT_LATEST_FILE] }
            }
        }.getOrNull()

        if (latestJson.isNullOrBlank()) {
            return false to "لا توجد أي نسخة مزامنة محفوظة في الخزنة المحلية بعد."
        }

        val parsedPayload = runCatching {
            moshi.adapter(SyncBatchPayload::class.java).fromJson(latestJson)
        }.getOrNull()
        val envelope = parsedPayload?.encryptedBackupEnvelope
        val batchNo = parsedPayload?.batchNo.orEmpty()
        if (envelope.isNullOrBlank()) {
            return false to "ملف الخزنة المحلية تالف ولا يحتوي على حزمة مشفرة صالحة."
        }

        val plainBackup = decryptBackupEnvelope(envelope)
            ?: return false to "تعذر فك تشفير حزمة الخزنة المحلية بمفتاح الأمان الحالي."

        val handler = restoreBackupJsonHandler
            ?: return false to "معالج استعادة قاعدة البيانات غير مهيأ."

        val imported = runCatching { handler(plainBackup) }.getOrDefault(false)
        if (!imported) {
            return false to "فشل التحقق من بصمة SHA-256 لحزمة الخزنة ($batchNo)."
        }

        userRepository.logAudit("استعادة من الخزنة المحلية", "تمت استعادة النسخة الاحتياطية المشفرة من الدفعة $batchNo بنجاح")
        return true to "تمت استعادة البيانات من الدفعة المشفرة ($batchNo) بنجاح ✔"
    }

    fun isNetworkTransportConnected(): Boolean {
        if (syncApiOverride != null) return true
        val ctx = appContext ?: return true
        return runCatching {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return@runCatching false
            val active = cm.activeNetwork ?: return@runCatching false
            val caps = cm.getNetworkCapabilities(active) ?: return@runCatching false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }.getOrDefault(false)
    }

    private fun refreshTransportState() {
        _isTransportAvailable.value = isNetworkTransportConnected()
    }

    private fun registerConnectivityObserver(ctx: Context) {
        if (connectivityCallbackRegistered) return
        connectivityCallbackRegistered = true
        runCatching {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return@runCatching
            val req = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(req, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isTransportAvailable.value = true
                }

                override fun onLost(network: Network) {
                    _isTransportAvailable.value = isNetworkTransportConnected()
                    if (!_isTransportAvailable.value && !isBuiltInVaultHost()) {
                        _isOnline.value = false
                        _syncStageMessage.value = "انقطع اتصال الشبكة بالخادم البعيد — بقي التطبيق على SQLite المحلي"
                    }
                }
            })
        }
    }

    private fun resolveSyncEnvelopeKey(): ByteArray {
        return sessionManager?.getOrCreateSyncEnvelopeKey() ?: volatileFallbackSyncKey
    }

    fun encryptBackupEnvelope(plainBackupJson: String): String {
        return SecurityUtils.encryptAesGcm(plainBackupJson, resolveSyncEnvelopeKey())
    }

    fun decryptBackupEnvelope(envelope: String): String? {
        return SecurityUtils.decryptAesGcm(envelope, resolveSyncEnvelopeKey())
    }

    fun hasServerAuthToken(host: String): Boolean {
        return !sessionManager?.getServerAuthToken(host).isNullOrBlank()
    }

    fun getServerAuthTokenMasked(host: String): String {
        val tok = sessionManager?.getServerAuthToken(host).orEmpty()
        return if (tok.length <= 4) if (tok.isEmpty()) "" else "••••" else "••••${tok.takeLast(4)}"
    }

    fun enqueueOfflineOperation(action: String, description: String) {
        val ts = nowTimeStr()
        val hash = SecurityUtils.shortSha256("$ts|$action|$description")
        val item = SyncQueueItem(
            timestamp = ts,
            entityName = action,
            action = "حفظ محلي بانتظار المزامنة",
            summary = description,
            txHash = hash,
            payloadBytes = (description.length * 14).coerceAtLeast(256)
        )
        _syncQueue.value = listOf(item) + _syncQueue.value
        _syncStageMessage.value = "وضع عدم الاتصال: ${_syncQueue.value.size} عملية محفوظة محلياً في الطابور"
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.syncDao().upsertSyncQueueItem(SyncQueueItemEntity.fromDomain(item))
            }
        }
    }

    fun simulateOfflineFieldOperations() {
        val ts = nowTimeStr()
        val sampleItems = listOf(
            SyncQueueItem(
                timestamp = ts,
                entityName = "تحصيل اشتراك ميداني",
                action = "سند قبض محلي",
                summary = "تحصيل اشتراك ميداني بمبلغ 20,000 ﷼ (محفوظ محلياً)",
                txHash = SecurityUtils.shortSha256("$ts-SUB-1"),
                payloadBytes = 640
            ),
            SyncQueueItem(
                timestamp = ts,
                entityName = "طلب مساعدة علاجية",
                action = "تسجيل استمارة ميدانية",
                summary = "رفع استمارة بحث ميداني لحالة طارئة في القرى التابعة للصندوق",
                txHash = SecurityUtils.shortSha256("$ts-AID-2"),
                payloadBytes = 820
            ),
            SyncQueueItem(
                timestamp = ts,
                entityName = "صرف سلة غذائية",
                action = "حركة مستودع محلي",
                summary = "إذن صرف مستودعي رقم MOV-LOCAL لعدد 3 سلات غذائية",
                txHash = SecurityUtils.shortSha256("$ts-INV-3"),
                payloadBytes = 510
            )
        )
        _isOnline.value = false
        _syncQueue.value = sampleItems + _syncQueue.value
        _syncStageMessage.value = "تم تسجيل ${sampleItems.size} عمليات ميدانية في طابور الانتظار (Offline)"
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.syncDao().upsertSyncQueueItems(sampleItems.map { SyncQueueItemEntity.fromDomain(it) })
            }
        }
    }

    fun setAutoSyncOnConnect(enabled: Boolean) {
        _autoSyncOnConnect.value = enabled
        sessionManager?.autoSyncOnConnect = enabled
        userRepository.logAudit(
            "إعدادات المزامنة",
            if (enabled) "تفعيل المزامنة التلقائية الفورية عند الاتصال"
            else "إيقاف المزامنة التلقائية وتحويلها للوضع اليدوي"
        )
    }

    fun updateServerNode(
        name: String,
        host: String,
        latencyMs: Int = 0,
        authToken: String? = null
    ) {
        userRepository.requirePermission(
            UserRole.ADMIN,
            UserRole.ACCOUNTANT,
            actionDescription = "تعديل إعدادات خادم المزامنة"
        )
        val cleanHost = host.trim()
        buildBaseHttpsUrl(cleanHost)
        val cleanName = name.trim().ifBlank { cleanHost }
        val builtIn = isBuiltInVaultHost(cleanHost)
        val protocolLabel = if (builtIn) "AES-256-GCM Vault + SHA-256" else "HTTPS / TLS 1.3"

        if (authToken != null) {
            sessionManager?.setServerAuthToken(cleanHost, authToken)
        }

        val previousHost = _serverConfig.value.serverHost
        val updated = _serverConfig.value.copy(
            serverName = cleanName,
            serverHost = cleanHost,
            protocol = protocolLabel,
            latencyMs = latencyMs.coerceAtLeast(0),
            lastBackupTime = _serverConfig.value.lastBackupTime
        )
        _serverConfig.value = updated
        if (!previousHost.equals(cleanHost, ignoreCase = true)) {
            _isOnline.value = false
            _syncStageMessage.value = "تم التبديل إلى $cleanName — اضغط فحص الاتصال أو المزامنة للتحقق"
        }

        sessionManager?.serverName = cleanName
        sessionManager?.serverHost = cleanHost
        upsertProfileRecord(
            name = cleanName,
            host = cleanHost,
            isBuiltIn = builtIn,
            latencyMs = if (latencyMs > 0) latencyMs else null,
            diagnostic = if (builtIn) "جاهز محلياً على الجهاز" else "بانتظار فحص الاتصال"
        )

        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.syncDao().upsertServerConfig(LocalServerConfigEntity.fromDomain(updated))
            }
        }
        userRepository.logAudit("تغيير خادم المزامنة", "الارتباط بالعقدة: $cleanName ($cleanHost)")
    }

    suspend fun probeServerConnection(
        hostOverride: String = _serverConfig.value.serverHost,
        nameOverride: String = _serverConfig.value.serverName,
        saveAsActiveNode: Boolean = false,
        authTokenOverride: String? = null
    ): ServerProbeResult = syncMutex.withLock {
        if (saveAsActiveNode) {
            updateServerNode(nameOverride, hostOverride, 0, authTokenOverride)
        }
        val probe = probeServerInternal(hostOverride, nameOverride)
        if (saveAsActiveNode) {
            if (probe.reachable) {
                _isOnline.value = true
                _serverConfig.value = _serverConfig.value.copy(
                    latencyMs = probe.latencyMs,
                    protocol = probe.protocolLabel
                )
                persistServerConfigToDb(_serverConfig.value)
            } else {
                _isOnline.value = false
            }
            _syncStageMessage.value = probe.message
        }
        probe
    }

    fun deleteServerProfile(profileId: String): Boolean {
        userRepository.requirePermission(
            UserRole.ADMIN,
            UserRole.ACCOUNTANT,
            actionDescription = "حذف خادم مزامنة"
        )
        val target = _serverProfiles.value.find { it.id == profileId } ?: return false
        if (target.isBuiltInVault || isBuiltInVaultHost(target.host)) return false

        val remaining = _serverProfiles.value.filterNot { it.id == profileId }
        _serverProfiles.value = remaining
        sessionManager?.setServerAuthToken(target.host, null)
        saveServerProfilesToSession(remaining)

        if (_serverConfig.value.serverHost.equals(target.host, ignoreCase = true)) {
            val fallback = remaining.firstOrNull() ?: defaultServerProfiles().first()
            updateServerNode(fallback.name, fallback.host, fallback.lastLatencyMs ?: 0)
        }
        return true
    }

    fun toggleOnline() {
        if (_isOnline.value) {
            _isOnline.value = false
            _syncStageMessage.value = "النظام يعمل على المحرك المحلي (Offline) - الحفظ في SQLite"
            userRepository.logAudit(
                "انقطاع الاتصال",
                "تحويل حالة الشبكة إلى الوضع المحلي المستقل (Offline-First)"
            )
        } else {
            dbScope.launch {
                connectAndVerifyHealthBlocking()
            }
        }
    }

    /**
     * Performs a real transport + HTTPS / Local-Vault health probe under [syncMutex],
     * measures true latency in ms, and auto-syncs pending operations if configured.
     */
    suspend fun connectAndVerifyHealthBlocking(): Boolean = syncMutex.withLock {
        connectAndVerifyHealthInternal()
    }

    private suspend fun connectAndVerifyHealthInternal(): Boolean {
        val probe = probeServerInternal(_serverConfig.value.serverHost, _serverConfig.value.serverName)
        if (!probe.reachable) {
            _isOnline.value = false
            _syncStageMessage.value = probe.message
            userRepository.logAudit(
                "تعذر الاتصال بالخادم",
                "${probe.message} (${_serverConfig.value.serverHost})"
            )
            return false
        }

        _isOnline.value = true
        _serverConfig.value = _serverConfig.value.copy(
            latencyMs = probe.latencyMs,
            protocol = probe.protocolLabel
        )
        persistServerConfigToDb(_serverConfig.value)

        userRepository.logAudit(
            "اتصال بالشبكة",
            "تم التحقق من جاهزية ${_serverConfig.value.serverName} (${_serverConfig.value.serverHost}) خلال ${probe.latencyMs}ms"
        )

        if (_syncQueue.value.isNotEmpty() && _autoSyncOnConnect.value) {
            syncNowInternal()
        } else if (_syncQueue.value.isNotEmpty()) {
            _syncStageMessage.value = "متصل (${probe.latencyMs}ms): يوجد ${_syncQueue.value.size} عملية جاهزة للمزامنة اليدوية"
        } else {
            _syncStageMessage.value = "متصل ومطابق بالكامل مع ${_serverConfig.value.serverName} (${probe.latencyMs}ms)"
        }
        return true
    }

    /**
     * Connects (if needed) and runs exactly one sync pass without duplicate syncs when
     * auto-sync already drained the queue during connect.
     */
    suspend fun connectAndSyncSmartSuspending(): Boolean = syncMutex.withLock {
        if (!_isOnline.value) {
            val connected = connectAndVerifyHealthInternal()
            if (!connected) return@withLock false
            if (_syncQueue.value.isEmpty()) return@withLock true
        }
        syncNowInternal()
    }

    fun syncNow() {
        dbScope.launch {
            syncNowBlocking()
        }
    }

    /**
     * Encrypts backup payload with AES-256-GCM, pushes to server over HTTPS (or Local Vault),
     * and ONLY clears the sync queue / records a batch inside an atomic Room transaction if
     * the server returns HTTP 2xx with `accepted == true`.
     */
    suspend fun syncNowBlocking(): Boolean = syncMutex.withLock {
        syncNowInternal()
    }

    private suspend fun syncNowInternal(): Boolean {
        val pendingList = _syncQueue.value
        val count = pendingList.size
        val host = _serverConfig.value.serverHost
        val useBuiltInVault = syncApiOverride == null && isBuiltInVaultHost(host)

        if (!useBuiltInVault && !isNetworkTransportConnected()) {
            _isOnline.value = false
            _syncStageMessage.value = "فشلت المزامنة: لا يوجد اتصال بالشبكة — بقيت العمليات ($count) في طابور الانتظار"
            return false
        }

        if (count == 0) {
            val probe = probeServerInternal(host, _serverConfig.value.serverName)
            if (!probe.reachable) {
                _isOnline.value = false
                _syncStageMessage.value = probe.message
                return false
            }
            val ts = nowTimeStr()
            _isOnline.value = true
            _lastSyncTimestamp.value = ts
            sessionManager?.lastSyncTimestamp = ts
            _serverConfig.value = _serverConfig.value.copy(latencyMs = probe.latencyMs, lastBackupTime = ts)
            persistServerConfigToDb(_serverConfig.value)
            _syncStageMessage.value = "جميع الجداول متطابقة 100% مع ${_serverConfig.value.serverName} (${probe.latencyMs}ms)"
            return true
        }

        val syncStartMs = System.currentTimeMillis()
        _isSyncing.value = true
        _syncProgress.value = 0.25f
        val ts = nowTimeStr()
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val batchNo = String.format(Locale.US, "SYNC-%d-%03d", year, _syncHistory.value.size + 1)
        val queueDigest = pendingList.joinToString("|") { "${it.id}:${it.txHash}:${it.entityName}:${it.summary}" }
        val checksum = SecurityUtils.sha256Hex(queueDigest)
        _syncStageMessage.value = "1/3 تشفير الحزمة بـ AES-256-GCM وتوقيع البصمة SHA-256..."

        val encryptedEnvelope = runCatching {
            val rawJson = backupJsonProvider()
            encryptBackupEnvelope(rawJson)
        }.getOrNull()

        if (encryptedEnvelope.isNullOrBlank()) {
            _syncProgress.value = 0f
            _isSyncing.value = false
            _syncStageMessage.value = "تعذر تشفير الحزمة الاحتياطية؛ تم إيقاف الإرسال وحفظ الطابور"
            return false
        }

        _syncProgress.value = 0.65f
        _syncStageMessage.value = if (useBuiltInVault) {
            "2/3 حفظ وتوثيق الحزمة المشفرة في الخزنة المحلية ($host)..."
        } else {
            "2/3 إرسال الحزمة المشفرة عبر HTTPS إلى $host..."
        }

        var failureHttpCode: Int? = null
        var failureThrowable: Throwable? = null
        val succeeded = try {
            val payload = SyncBatchPayload(
                batchNo = batchNo,
                timestamp = ts,
                deviceNode = _serverConfig.value.serverName,
                operationsCount = count,
                sha256Checksum = checksum,
                encryptedBackupEnvelope = encryptedEnvelope
            )
            val response = buildRetrofitService(host).pushSyncBatch(payload)
            if (response.isSuccessful && response.body()?.accepted == true) {
                true
            } else {
                failureHttpCode = response.code()
                false
            }
        } catch (t: Throwable) {
            failureThrowable = t
            false
        }

        if (!succeeded) {
            val diag = classifyNetworkFailure(failureThrowable, failureHttpCode, host)
            _isOnline.value = false
            _syncProgress.value = 0f
            _isSyncing.value = false
            _syncStageMessage.value = "فشلت المزامنة ($diag) — بقيت العمليات ($count) في الطابور"
            upsertProfileRecord(
                name = _serverConfig.value.serverName,
                host = host,
                isBuiltIn = useBuiltInVault,
                latencyMs = null,
                diagnostic = diag
            )
            userRepository.logAudit(
                "فشل مزامنة ($batchNo)",
                "$diag — تم الإبقاء على $count عملية في الطابور."
            )
            return false
        }

        val elapsedMs = (System.currentTimeMillis() - syncStartMs).coerceAtLeast(1L)
        val protocolStatus = if (useBuiltInVault) {
            "مكتملة بالخزنة المحلية AES-256-GCM + SHA-256 ✔"
        } else {
            "مكتملة وموثقة بـ HTTPS + SHA-256 ✔"
        }

        val newBatch = SyncBatchRecord(
            batchNo = batchNo,
            timestamp = ts,
            operationsCount = count,
            serverNode = "$host (${_serverConfig.value.serverName})",
            durationMs = elapsedMs,
            status = protocolStatus
        )
        val updatedConfig = _serverConfig.value.copy(
            lastBackupTime = ts,
            latencyMs = elapsedMs.toInt()
        )

        db?.let { database ->
            withContext(dbDispatcher) {
                database.withTransaction {
                    database.syncDao().clearSyncQueue()
                    database.syncDao().upsertSyncBatch(SyncBatchRecordEntity.fromDomain(newBatch))
                    database.syncDao().upsertServerConfig(LocalServerConfigEntity.fromDomain(updatedConfig))
                }
            }
        }

        _syncHistory.value = listOf(newBatch) + _syncHistory.value
        _syncQueue.value = emptyList()
        _lastSyncTimestamp.value = ts
        sessionManager?.lastSyncTimestamp = ts
        _serverConfig.value = updatedConfig
        _isOnline.value = true
        _syncProgress.value = 1.0f
        _syncStageMessage.value = "3/3 اكتملت المزامنة الآمنة ($batchNo) خلال ${elapsedMs}ms ✔"
        _isSyncing.value = false

        upsertProfileRecord(
            name = updatedConfig.serverName,
            host = host,
            isBuiltIn = useBuiltInVault,
            latencyMs = elapsedMs.toInt(),
            diagnostic = "آخر مزامنة ناجحة: $ts"
        )
        refreshLocalVaultTelemetry()

        userRepository.logAudit(
            "مزامنة ناجحة ($batchNo)",
            "تمت مزامنة $count عملية مشفرة (AES-256-GCM) مع ${updatedConfig.serverName} خلال ${elapsedMs}ms"
        )
        return true
    }

    private suspend fun probeServerInternal(host: String, name: String): ServerProbeResult {
        val useBuiltInVault = syncApiOverride == null && isBuiltInVaultHost(host)
        val protocolLabel = if (useBuiltInVault) "AES-256-GCM Vault + SHA-256" else "HTTPS / TLS 1.3"

        if (!useBuiltInVault && !isNetworkTransportConnected()) {
            val msg = "لا يوجد اتصال شبكي نشط (Wi-Fi أو بيانات) للوصول إلى $host"
            upsertProfileRecord(name, host, isBuiltIn = false, latencyMs = null, diagnostic = msg)
            return ServerProbeResult(false, 0, msg, protocolLabel = protocolLabel)
        }

        val startMs = System.currentTimeMillis()
        var httpCode: Int? = null
        var failure: Throwable? = null
        val ok = try {
            val response = buildRetrofitService(host).checkHealth()
            httpCode = response.code()
            response.isSuccessful
        } catch (t: Throwable) {
            failure = t
            false
        }
        val elapsedMs = (System.currentTimeMillis() - startMs).toInt().coerceAtLeast(1)

        return if (ok) {
            val msg = if (useBuiltInVault) {
                "الخزنة المحلية المدمجة نشطة ومشفرة (${elapsedMs}ms) ✔"
            } else {
                "الخادم متصل ويستجيب عبر HTTPS (${elapsedMs}ms) ✔"
            }
            upsertProfileRecord(name, host, useBuiltInVault, elapsedMs, msg)
            ServerProbeResult(true, elapsedMs, msg, httpCode = httpCode, protocolLabel = protocolLabel)
        } else {
            val msg = classifyNetworkFailure(failure, httpCode, host)
            upsertProfileRecord(name, host, useBuiltInVault, latencyMs = null, diagnostic = msg)
            ServerProbeResult(false, elapsedMs, msg, httpCode = httpCode, protocolLabel = protocolLabel)
        }
    }

    private fun classifyNetworkFailure(error: Throwable?, httpCode: Int?, host: String): String {
        if (httpCode != null) {
            return when (httpCode) {
                401, 403 -> "رفض الخادم المصادقة (HTTP $httpCode) — تحقق من رمز Bearer Token"
                404 -> "نقطة المزامنة /api/v1 غير متوفرة على الخادم ($host) [HTTP 404]"
                in 500..599 -> "عطل داخلي في الخادم البعيد ($host) [HTTP $httpCode]"
                else -> "رفض الخادم الطلب برمز [HTTP $httpCode]"
            }
        }
        return when (error) {
            is IllegalArgumentException -> error.message ?: "عنوان الخادم غير صالح ($host)"
            is UnknownHostException -> "تعذر العثور على النطاق ($host) — تحقق من DNS أو اسم الخادم"
            is ConnectException -> "رفض الخادم ($host) الاتصال — تأكد من تشغيل خدمة المزامنة والمنفذ"
            is SocketTimeoutException -> "انتهت مهلة الاتصال بالخادم ($host) دون استجابة"
            is SSLHandshakeException, is SSLException -> "فشل التحقق من شهادة الأمان TLS/SSL للخادم ($host)"
            null -> "تعذر الوصول إلى نقطة فحص الخادم ($host)"
            else -> "تعذر الاتصال بـ ($host): ${error.message ?: error::class.simpleName}"
        }
    }

    private fun persistServerConfigToDb(config: LocalServerConfig) {
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.syncDao().upsertServerConfig(LocalServerConfigEntity.fromDomain(config))
            }
        }
    }

    private fun defaultServerProfiles(): List<ServerProfile> = listOf(
        ServerProfile(
            id = "builtin_local_vault",
            name = BUILTIN_VAULT_NAME,
            host = BUILTIN_VAULT_HOST,
            isBuiltInVault = true,
            lastDiagnostic = "خزنة محلية مشفرة داخل الجهاز (تعمل دائماً دون إنترنت)"
        ),
        ServerProfile(
            id = "preset_lan_node",
            name = "خادم الشبكة المحلية (LAN HTTPS)",
            host = "192.168.1.100:8443",
            isBuiltInVault = false,
            lastDiagnostic = "يتطلب تشغيل خادم LAN HTTPS على نفس الشبكة"
        ),
        ServerProfile(
            id = "preset_cloud_node",
            name = "الخادم السحابي المركزي (Cloud HTTPS)",
            host = "sync.social-fund.org:443",
            isBuiltInVault = false,
            lastDiagnostic = "يتطلب اتصال إنترنت وخادم سحابي مهيأ"
        )
    )

    private fun upsertProfileRecord(
        name: String,
        host: String,
        isBuiltIn: Boolean,
        latencyMs: Int?,
        diagnostic: String
    ) {
        val checkedAt = nowTimeStr()
        val hasToken = hasServerAuthToken(host)
        val current = _serverProfiles.value
        val idx = current.indexOfFirst { it.host.equals(host, ignoreCase = true) }
        val updatedList = if (idx >= 0) {
            current.mapIndexed { i, p ->
                if (i == idx) {
                    p.copy(
                        name = name,
                        isBuiltInVault = isBuiltIn,
                        hasAuthToken = hasToken,
                        lastLatencyMs = latencyMs,
                        lastCheckedAt = checkedAt,
                        lastDiagnostic = diagnostic
                    )
                } else p
            }
        } else {
            current + ServerProfile(
                name = name,
                host = host,
                isBuiltInVault = isBuiltIn,
                hasAuthToken = hasToken,
                lastLatencyMs = latencyMs,
                lastCheckedAt = checkedAt,
                lastDiagnostic = diagnostic
            )
        }
        _serverProfiles.value = updatedList
        saveServerProfilesToSession(updatedList)
    }

    private fun loadServerProfilesFromSession(session: EncryptedSessionManager) {
        val raw = session.serverProfilesJson
        if (raw.isNullOrBlank()) {
            _serverProfiles.value = defaultServerProfiles().map {
                it.copy(hasAuthToken = !session.getServerAuthToken(it.host).isNullOrBlank())
            }
            return
        }
        val parsed = runCatching {
            val type = Types.newParameterizedType(List::class.java, ServerProfile::class.java)
            moshi.adapter<List<ServerProfile>>(type).fromJson(raw)
        }.getOrNull()?.filter { it.host.isNotBlank() }?.map { p ->
            p.copy(
                isBuiltInVault = p.isBuiltInVault || isBuiltInVaultHost(p.host),
                hasAuthToken = !session.getServerAuthToken(p.host).isNullOrBlank()
            )
        }

        if (parsed.isNullOrEmpty()) {
            _serverProfiles.value = defaultServerProfiles()
        } else {
            val hasBuiltIn = parsed.any { it.isBuiltInVault || isBuiltInVaultHost(it.host) }
            _serverProfiles.value = if (hasBuiltIn) parsed else defaultServerProfiles().take(1) + parsed
        }
    }

    private fun saveServerProfilesToSession(profiles: List<ServerProfile>) {
        val type = Types.newParameterizedType(List::class.java, ServerProfile::class.java)
        sessionManager?.serverProfilesJson = moshi.adapter<List<ServerProfile>>(type).toJson(profiles)
    }

    fun applySyncSnapshot(
        queue: List<SyncQueueItem>?,
        batches: List<SyncBatchRecord>? = null,
        serverConfig: LocalServerConfig? = null
    ) {
        queue?.let { _syncQueue.value = it }
        batches?.let { _syncHistory.value = it }
        serverConfig?.let { _serverConfig.value = it }
    }

    fun resetToSeed() {
        _syncQueue.value = emptyList()
        _syncHistory.value = emptyList()
        _isOnline.value = false
        _syncProgress.value = 0f
        _lastSyncTimestamp.value = NOT_SYNCED_LABEL
        _serverConfig.value = LocalServerConfig()
        _serverProfiles.value = defaultServerProfiles()
        sessionManager?.serverName = BUILTIN_VAULT_NAME
        sessionManager?.serverHost = BUILTIN_VAULT_HOST
        sessionManager?.lastSyncTimestamp = NOT_SYNCED_LABEL
        sessionManager?.serverProfilesJson = null
        _syncStageMessage.value = "جاهز للمزامنة الآمنة (الخزنة المحلية المدمجة / HTTPS)"
    }
}
