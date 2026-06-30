package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.QCEmailLog
import com.example.ui.theme.*
import com.example.ui.viewmodel.QCViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: QCViewModel = viewModel()
) {
    val logs by viewModel.allLogs.collectAsState()
    val selectedLog by viewModel.selectedLog.collectAsState()
    
    // Configuration to support adaptive master-detail layout
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 720

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterRoute by remember { mutableStateOf<String?>(null) } // "CRITICAL_DEFECT", "CUSTOMER_FEEDBACK", "GENERAL_QUERY", "ERROR"

    val filteredLogs = remember(logs, searchQuery, selectedFilterRoute) {
        logs.filter { log ->
            val matchesSearch = log.sender.contains(searchQuery, ignoreCase = true) ||
                    log.subject.contains(searchQuery, ignoreCase = true) ||
                    log.body.contains(searchQuery, ignoreCase = true)
            val matchesFilter = selectedFilterRoute == null || log.route == selectedFilterRoute
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "QC System Engine",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "QC Assistant",
                                fontWeight = FontWeight.Medium,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.resetToDefaults() },
                            modifier = Modifier.testTag("reset_defaults_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Database",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearAllLogs() },
                            modifier = Modifier.testTag("clear_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Logs",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HorizontalDivider(color = Color(0xFFEADDFF), thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        
        // Split layout if tablet/wide width, otherwise stack/conditional layout
        if (isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Left Column: Navigation, Inputs, Lists
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(0.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    DashboardHeaderSection(
                        logs = logs,
                        currentFilter = selectedFilterRoute,
                        onFilterSelect = { selectedFilterRoute = it }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SimulatedTriggerPanel(viewModel = viewModel)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ManualPayloadForm(viewModel = viewModel)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LogsListView(
                        logs = filteredLogs,
                        selectedLog = selectedLog,
                        onLogSelect = { viewModel.selectLog(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Right Column: Details Pane
                Box(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedLog != null) {
                        LogDetailPane(
                            log = selectedLog!!,
                            onClose = { viewModel.deselectLog() },
                            onDispatch = { viewModel.dispatchDraftReply(it) },
                            onDelete = { viewModel.deleteLog(it) },
                            isTablet = true
                        )
                    } else {
                        EmptyDetailState()
                    }
                }
            }
        } else {
            // Mobile Stack Layout: If selectedLog is not null, view detail, otherwise list
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AnimatedContent(
                    targetState = selectedLog,
                    transitionSpec = {
                        if (targetState != null) {
                            slideInHorizontally { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width } + fadeOut()
                        }
                    },
                    label = "MobileNavigationTransition"
                ) { activeLog ->
                    if (activeLog != null) {
                        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            LogDetailPane(
                                log = activeLog,
                                onClose = { viewModel.deselectLog() },
                                onDispatch = { viewModel.dispatchDraftReply(it) },
                                onDelete = { viewModel.deleteLog(it) },
                                isTablet = false
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            DashboardHeaderSection(
                                logs = logs,
                                currentFilter = selectedFilterRoute,
                                onFilterSelect = { selectedFilterRoute = it }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            SimulatedTriggerPanel(viewModel = viewModel)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            ManualPayloadForm(viewModel = viewModel)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Box(modifier = Modifier.heightIn(min = 300.dp, max = 800.dp)) {
                                LogsListView(
                                    logs = filteredLogs,
                                    selectedLog = null,
                                    onLogSelect = { viewModel.selectLog(it) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHeaderSection(
    logs: List<QCEmailLog>,
    currentFilter: String?,
    onFilterSelect: (String?) -> Unit
) {
    val totalCount = logs.size
    val criticalCount = logs.count { log -> log.route == "CRITICAL_DEFECT" }
    val feedbackCount = logs.count { log -> log.route == "CUSTOMER_FEEDBACK" }
    val queryCount = logs.count { log -> log.route == "GENERAL_QUERY" }
    val failedCount = logs.count { log -> log.status == "FAILED" }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Stream Engine Label & Live badge from High Density theme
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inbound Stream Engine",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF49454F)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFFEADDFF))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LIVE",
                    color = Color(0xFF6750A4),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Grid grid-cols-3 from HTML
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Critical
            MetricCard(
                title = "CRITICAL",
                count = criticalCount,
                bgColor = Color(0xFFF2B8B5),
                labelColor = Color(0xFF601410),
                countColor = Color(0xFF410E0B),
                isSelected = currentFilter == "CRITICAL_DEFECT",
                onClick = {
                    if (currentFilter == "CRITICAL_DEFECT") onFilterSelect(null)
                    else onFilterSelect("CRITICAL_DEFECT")
                },
                modifier = Modifier.weight(1f).testTag("filter_crit_pill")
            )

            // Card 2: Feedback
            MetricCard(
                title = "FEEDBACK",
                count = feedbackCount,
                bgColor = Color(0xFFD1E1FF),
                labelColor = Color(0xFF001D35),
                countColor = Color(0xFF001D35),
                isSelected = currentFilter == "CUSTOMER_FEEDBACK",
                onClick = {
                    if (currentFilter == "CUSTOMER_FEEDBACK") onFilterSelect(null)
                    else onFilterSelect("CUSTOMER_FEEDBACK")
                },
                modifier = Modifier.weight(1f).testTag("filter_fdbk_pill")
            )

            // Card 3: Queries
            MetricCard(
                title = "QUERIES",
                count = queryCount,
                bgColor = Color(0xFFE7E0EC),
                labelColor = Color(0xFF1D1B20),
                countColor = Color(0xFF1D1B20),
                isSelected = currentFilter == "GENERAL_QUERY",
                onClick = {
                    if (currentFilter == "GENERAL_QUERY") onFilterSelect(null)
                    else onFilterSelect("GENERAL_QUERY")
                },
                modifier = Modifier.weight(1f).testTag("filter_gen_pill")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Helper filter pills row for "ALL" and "FAILED/ERROR"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // All Pill
            FilterUtilityPill(
                label = "SHOW ALL LOGS",
                count = totalCount,
                isSelected = currentFilter == null,
                onClick = { onFilterSelect(null) },
                modifier = Modifier.weight(1f).testTag("filter_all_pill")
            )

            // Failed System Alerts Pill
            FilterUtilityPill(
                label = "SYSTEM CRASHES",
                count = failedCount,
                isSelected = currentFilter == "ERROR",
                onClick = { onFilterSelect("ERROR") },
                modifier = Modifier.weight(1f).testTag("filter_fail_pill")
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    count: Int,
    bgColor: Color,
    labelColor: Color,
    countColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) countColor else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = bgColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                letterSpacing = 0.5.sp
            )
            Text(
                text = count.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = countColor
            )
        }
    }
}

@Composable
fun FilterUtilityPill(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFEADDFF),
                shape = RoundedCornerShape(8.dp)
            ),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF49454F)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE8DEF8))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color(0xFF6750A4)
                )
            }
        }
    }
}

@Composable
fun SimulatedTriggerPanel(viewModel: QCViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "SIMULATE INBOUND CONGESTION STREAM",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TriggerButton(
                    label = "Inbound Defect",
                    icon = Icons.Default.Warning,
                    tint = Color(0xFFEF4444),
                    onClick = {
                        viewModel.simulateEmailArrival(
                            sender = "assembly-line-bot@chassis-corp.net",
                            ccList = "engineering-leads@chassis-corp.net, safety@chassis-corp.net",
                            subject = "Alert: Chassis Weld Defect Identified",
                            body = "A weld deviation and defect was registered at robot arm station 3. Ultrasonic test failure on unit C-9031. Immediate corrective calibration required."
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("trigger_defect_button")
                )
                
                TriggerButton(
                    label = "Inbound Feedback",
                    icon = Icons.Default.ThumbUp,
                    tint = Color(0xFF3B82F6),
                    onClick = {
                        viewModel.simulateEmailArrival(
                            sender = "v.hughes@partner-logistics.com",
                            ccList = "support@partner-logistics.com",
                            subject = "Outstanding Experience with Quality Assurance Review",
                            body = "Very satisfied with the quick defect resolution loop. Performance of the team is good. High quality standard overall."
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("trigger_feedback_button")
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TriggerButton(
                    label = "General Query",
                    icon = Icons.Default.Info,
                    tint = Color(0xFF6B7280),
                    onClick = {
                        viewModel.simulateEmailArrival(
                            sender = "procure@global-supplies.org",
                            ccList = "archive@global-supplies.org",
                            subject = "Request for ISO 9001 audit schedule sheets",
                            body = "Please forward the planned certification schedule and compliance tracking sheets for Q4 audits."
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("trigger_query_button")
                )
                
                TriggerButton(
                    label = "System Crash/Error",
                    icon = Icons.Default.Build,
                    tint = Color(0xFFF97316),
                    onClick = {
                        // Empty sender triggers simulated validation failure catch-blocks in engine!
                        viewModel.simulateEmailArrival(
                            sender = "   ",
                            ccList = "telemetry@qc-hub.io",
                            subject = "System Telemetry Fault",
                            body = "Empty sender validation crash simulation triggered."
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("trigger_error_button")
                )
            }
        }
    }
}

@Composable
fun TriggerButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.4f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualPayloadForm(viewModel: QCViewModel) {
    var expanded by remember { mutableStateOf(false) }
    
    var sender by remember { mutableStateOf("") }
    var ccList by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Manual Ingestor",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MANUAL PAYLOAD INGESTOR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand Manual Form"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sender,
                        onValueChange = { sender = it },
                        label = { Text("Sender Email Address") },
                        modifier = Modifier.fillMaxWidth().testTag("input_sender"),
                        textStyle = TextStyle(fontSize = 12.sp),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = ccList,
                        onValueChange = { ccList = it },
                        label = { Text("CC Distribution List (comma-separated)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_cc"),
                        textStyle = TextStyle(fontSize = 12.sp),
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject Line Header") },
                        modifier = Modifier.fillMaxWidth().testTag("input_subject"),
                        textStyle = TextStyle(fontSize = 12.sp),
                        leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Email Payload Body Content") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).testTag("input_body"),
                        textStyle = TextStyle(fontSize = 12.sp),
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )

                    Button(
                        onClick = {
                            viewModel.addManualPayload(
                                sender = sender,
                                ccList = ccList,
                                subject = subject,
                                body = body
                            )
                            // Clear fields after success
                            sender = ""
                            ccList = ""
                            subject = ""
                            body = ""
                            expanded = false
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("ingest_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Ingest",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EVALUATE & ROUTE PAYLOAD", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search logs by sender, subject, body...", fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth().testTag("search_bar_input"),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear Search", modifier = Modifier.size(16.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun LogsListView(
    logs: List<QCEmailLog>,
    selectedLog: QCEmailLog?,
    onLogSelect: (QCEmailLog) -> Unit,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No logs match selection",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Use the triggers or forms to inject traffic data.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log ->
                LogItemCard(
                    log = log,
                    isSelected = selectedLog?.id == log.id,
                    onClick = { onLogSelect(log) }
                )
            }
        }
    }
}

@Composable
fun LogItemCard(
    log: QCEmailLog,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Custom label border, badge colors matching High Density spec
    val (borderColor, badgeBgColor, routeLabel) = when (log.route) {
        "CRITICAL_DEFECT" -> Triple(Color(0xFFF2B8B5), Color(0xFFB3261E), log.tag)
        "CUSTOMER_FEEDBACK" -> Triple(Color(0xFFD1E1FF), Color(0xFF0061A4), log.tag)
        "GENERAL_QUERY" -> Triple(Color(0xFFCAC4D0), Color(0xFF49454F), log.tag)
        else -> Triple(Color(0xFFF2B8B5), Color(0xFFB3261E), "SYSTEM-ERROR")
    }

    val cardBorderColor = if (isSelected) {
        BrandPrimary
    } else {
        borderColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("log_item_${log.id}")
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(16.dp) // rounded-2xl
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF7F2FA) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Route Badge (with Route background and white text) + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBgColor)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = routeLabel,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(status = log.status)
                    Text(
                        text = formatTimestampTimeOnly(log.receivedTimestamp),
                        fontSize = 10.sp,
                        color = Color(0xFF49454F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sender address
            Text(
                text = "Sender: ${log.sender}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF1D1B20),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subject Header
            Text(
                text = "Sub: ${log.subject}",
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = Color(0xFF49454F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Body Snippet in quotes & italicized
            Text(
                text = "\"${log.body}\"",
                fontSize = 11.sp,
                color = Color(0xFF49454F),
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "PROCESSED" -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), "READY")
        "DISPATCHED" -> Triple(Color(0xFFDBEAFE), Color(0xFF1E40AF), "SENT")
        "FAILED" -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), "ERROR")
        else -> Triple(Color(0xFFF3F4F6), Color(0xFF374151), status)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun LogDetailPane(
    log: QCEmailLog,
    onClose: () -> Unit,
    onDispatch: (QCEmailLog) -> Unit,
    onDelete: (QCEmailLog) -> Unit,
    isTablet: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Detailed Header bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to List"
                )
            }
            
            Text(
                text = "LOG DETAIL #${log.id}",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )

            IconButton(
                onClick = { onDelete(log) },
                modifier = Modifier.testTag("detail_delete_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Log",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Document Metadata Block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Row with Badge + Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "METADATA INBOUND SOURCE",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                    StatusBadge(status = log.status)
                }

                Spacer(modifier = Modifier.height(8.dp))

                MetaRow(label = "Sender Address:", value = log.sender, tag = "detail_sender")
                MetaRow(label = "CC Distribution List:", value = log.ccList.ifBlank { "None (Standard Local)" }, tag = "detail_cc")
                MetaRow(label = "Subject Header:", value = log.subject, tag = "detail_subject")
                MetaRow(label = "Received Time:", value = formatTimestamp(log.receivedTimestamp), tag = "detail_time")
                
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Email Body Content:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = log.body,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 18.sp,
                        modifier = Modifier.testTag("detail_body")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Route evaluation reasoning
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "ROUTER FILTERS & EXCEPTION LOGS",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                val reason = when (log.route) {
                    "CRITICAL_DEFECT" -> "Route Trigger: Critical defect keywords matched in Subject/Body. Logged to high-priority QC response pipeline."
                    "CUSTOMER_FEEDBACK" -> "Route Trigger: Client feedback reviews matched. Routed directly to QA specialists."
                    "GENERAL_QUERY" -> "Route Trigger: Default route. Categorized as general supply query."
                    else -> "Processing System Error: Exception raised during stream scan."
                }

                Text(
                    text = reason,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Try Catch Error display
                if (log.status == "FAILED" && log.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFEBEB))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "TRY-CATCH DIAGNOSTIC ERROR TRACE:",
                                color = Color(0xFFB91C1C),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = log.errorMessage,
                                color = Color(0xFF7F1D1D),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("detail_error_message")
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Automated Response Card (Email compose format)
        if (log.status != "FAILED") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dispatch_compose_card")
                    .border(BorderStroke(1.dp, Color(0xFFEADDFF)), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row: Icon + Texts + Dispatch/Review Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Icon Box
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8DEF8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Dispatch",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Text details column
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automated Action Dispatcher",
                                color = Color(0xFF6750A4),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp
                            )
                            Text(
                                text = "RCA Template Drafted for #${log.id}",
                                color = Color(0xFF1D1B20),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Dispatch button matching HTML
                        if (log.status == "PROCESSED") {
                            Button(
                                onClick = { onDispatch(log) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6750A4),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("dispatch_action_button"),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Review",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Meta Row details to keep test tags active
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "To: ${log.sender}",
                            fontSize = 10.sp,
                            color = Color(0xFF49454F),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("dispatch_to")
                        )
                        Text(text = "|", fontSize = 10.sp, color = Color(0xFFCAC4D0))
                        Text(
                            text = "Cc: ${log.ccList.ifBlank { "None" }}",
                            fontSize = 10.sp,
                            color = Color(0xFF49454F),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("dispatch_cc")
                        )
                    }
                    Text(
                        text = "Subject: Re: ${log.subject}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF49454F),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("dispatch_subject")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Draft Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF7F2FA))
                            .border(BorderStroke(1.dp, Color(0xFFEADDFF)), RoundedCornerShape(12.dp)) // Simulates the dashed/outline style beautifully
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "DRAFT_PREVIEW_MODE",
                                color = Color(0xFF6750A4),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = log.draftedReply,
                                color = Color(0xFF49454F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp,
                                modifier = Modifier.testTag("dispatch_body")
                            )
                        }
                    }

                    // Dispatched State Banner
                    if (log.status == "DISPATCHED") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFD1E1FF),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF0061A4),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Automated Dispatch Successful",
                                    color = Color(0xFF001D35),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetaRow(label: String, value: String, tag: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(tag),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EmptyDetailState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Select an email log",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Evaluate filters, tags, and verify/dispatch the automated response workflow.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatTimestampTimeOnly(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

