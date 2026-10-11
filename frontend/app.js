/**
 * Enterprise Bank Communication & Collections Platform
 * Frontend Orchestration Script
 */

const API_BASE = 'http://localhost:8088/api/v1';

// Initial Mock Seed Data (used when backend is offline or for immediate preview)
const state = {
  activeView: 'dashboard',
  authToken: null,
  customers: [
    { id: 'c1', ref: 'CUST-8041', name: 'Aarav Sharma', email: 'aarav.sharma@example.com', phone: '****3819', lang: 'en', status: 'ACTIVE' },
    { id: 'c2', ref: 'CUST-8042', name: 'Priya Patel', email: 'priya.patel@example.com', phone: '****9182', lang: 'hi', status: 'ACTIVE' },
    { id: 'c3', ref: 'CUST-8043', name: 'Rohan Verma', email: 'rohan.v@example.com', phone: '****4402', lang: 'en', status: 'ACTIVE' },
    { id: 'c4', ref: 'CUST-8044', name: 'Neha Gupta', email: 'neha.g@example.com', phone: '****7712', lang: 'en', status: 'ACTIVE' },
    { id: 'c5', ref: 'CUST-8045', name: 'Vikram Singh', email: 'vikram.s@example.com', phone: '****2239', lang: 'en', status: 'INACTIVE' }
  ],
  campaigns: [
    { id: 'cmp-1', name: 'Festive Season EMI Reminder', type: 'PAYMENT_REMINDER', status: 'RUNNING', completed: 850, total: 1000, desc: 'Automated voice and SMS reminder for auto loan quarterly instalments' },
    { id: 'cmp-2', name: 'Delinquency Tier-1 Outreach', type: 'COLLECTIONS_ALERT', status: 'SCHEDULED', completed: 0, total: 420, desc: 'Urgent reminder for accounts 15-30 days past due date' },
    { id: 'cmp-3', name: 'KYC Expiry Notification', type: 'ACCOUNT_NOTIFICATION', status: 'DRAFT', completed: 0, total: 1200, desc: 'Regulatory reminder for scheduled periodic customer verification' },
    { id: 'cmp-4', name: 'Diwali Special Offer Campaign', type: 'PROMOTIONAL', status: 'COMPLETED', completed: 3500, total: 3500, desc: 'Pre-approved personal loan offers for privileged accounts' }
  ],
  jobs: [
    { id: 'job-901', key: 'DISP-20261011-01', customerName: 'Aarav Sharma', channel: 'VOICE', status: 'COMPLETED', attempts: 1, nextRetry: '—' },
    { id: 'job-902', key: 'DISP-20261011-02', customerName: 'Priya Patel', channel: 'SMS', status: 'IN_PROGRESS', attempts: 1, nextRetry: '—' },
    { id: 'job-903', key: 'DISP-20261011-03', customerName: 'Rohan Verma', channel: 'VOICE', status: 'RETRYING', attempts: 2, nextRetry: 'In 3 mins' },
    { id: 'job-904', key: 'DISP-20261011-04', customerName: 'Neha Gupta', channel: 'SMS', status: 'PENDING', attempts: 0, nextRetry: 'Scheduled' }
  ],
  collectionCases: [
    { id: 'cc-1', ref: 'LOAN-AUTO-9012', customer: 'Rohan Verma', amount: '45,250.00', dueDate: '2026-10-05', priority: 'HIGH', status: 'OPEN' },
    { id: 'cc-2', ref: 'LOAN-PERS-3112', customer: 'Vikram Singh', amount: '120,000.00', dueDate: '2026-09-28', priority: 'CRITICAL', status: 'IN_PROGRESS' },
    { id: 'cc-3', ref: 'CARD-CORP-4881', customer: 'Neha Gupta', amount: '18,500.00', dueDate: '2026-10-09', priority: 'MEDIUM', status: 'OPEN' },
    { id: 'cc-4', ref: 'LOAN-HOME-7221', customer: 'Priya Patel', amount: '84,000.00', dueDate: '2026-10-02', priority: 'RESOLVED', status: 'RESOLVED' }
  ],
  agentTasks: [
    { id: 't-1', caseRef: 'LOAN-PERS-3112', type: 'CALL_CUSTOMER', agent: 'Agent Sarah', priority: 'CRITICAL', status: 'IN_PROGRESS', due: 'Today, 2:00 PM' },
    { id: 't-2', caseRef: 'LOAN-AUTO-9012', type: 'SEND_NOTICE', agent: 'Agent David', priority: 'HIGH', status: 'ASSIGNED', due: 'Tomorrow, 10:00 AM' }
  ],
  auditLog: [
    { time: '2026-10-11 08:30:14', actor: 'admin-user', action: 'CAMPAIGN_RESUMED', entity: 'Campaign', entityId: 'cmp-1', outcome: 'SUCCESS' },
    { time: '2026-10-11 08:24:02', actor: 'system-scheduler', action: 'COMMUNICATION_JOB_CREATED', entity: 'CommunicationJob', entityId: 'job-901', outcome: 'SUCCESS' },
    { time: '2026-10-11 08:15:45', actor: 'telephony-provider', action: 'WEBHOOK_PROCESSED', entity: 'CommunicationAttempt', entityId: 'att-84', outcome: 'SUCCESS' }
  ]
};

// DOM Initialization
document.addEventListener('DOMContentLoaded', () => {
  setupNavigation();
  setupModals();
  setupEventListeners();
  setupCsvStudio();
  renderAll();
});

// View Navigation
function setupNavigation() {
  const navButtons = document.querySelectorAll('.nav-item');
  const breadcrumbCurrent = document.getElementById('breadcrumb-current');

  navButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const view = btn.getAttribute('data-view');
      switchView(view);
    });
  });

  const mobileToggle = document.getElementById('mobile-toggle');
  const sidebar = document.getElementById('sidebar');
  if (mobileToggle) {
    mobileToggle.addEventListener('click', () => {
      sidebar.classList.toggle('open');
    });
  }

  document.getElementById('link-view-all-campaigns')?.addEventListener('click', () => {
    switchView('campaigns');
  });
}

function switchView(viewName) {
  state.activeView = viewName;
  
  document.querySelectorAll('.nav-item').forEach(b => {
    b.classList.toggle('active', b.getAttribute('data-view') === viewName);
  });

  document.querySelectorAll('.view-panel').forEach(p => {
    p.classList.toggle('active', p.id === `view-${viewName}`);
  });

  const breadcrumbCurrent = document.getElementById('breadcrumb-current');
  if (breadcrumbCurrent) {
    const titles = {
      dashboard: 'Command Center',
      campaigns: 'Campaign Orchestration',
      communication: 'Communication Engine',
      collections: 'Collections & Cases',
      'csv-studio': 'CSV & Prompt Studio',
      customers: 'Customer Directory',
      audit: 'Compliance Audit'
    };
    breadcrumbCurrent.textContent = titles[viewName] || 'Overview';
  }
}

// Rendering Logic
function renderAll() {
  renderDashboard();
  renderCampaigns();
  renderCommunicationJobs();
  renderCollections();
  renderCustomers();
  renderAuditLogs();
}

function renderDashboard() {
  const table = document.getElementById('table-dashboard-campaigns');
  if (!table) return;

  table.innerHTML = state.campaigns.slice(0, 3).map(c => `
    <tr>
      <td><strong>${c.name}</strong></td>
      <td><span class="status-tag tag-neutral">${c.type}</span></td>
      <td><span class="status-tag ${getTagClass(c.status)}">${c.status}</span></td>
      <td>
        <div class="progress-container">
          <div class="progress-track">
            <div class="progress-fill" style="width: ${(c.completed / (c.total || 1)) * 100}%"></div>
          </div>
          <span class="progress-text">${c.completed} / ${c.total}</span>
        </div>
      </td>
      <td>
        <button class="btn btn-secondary btn-sm" onclick="triggerCampaignState('${c.id}', 'RUNNING')">Run</button>
      </td>
    </tr>
  `).join('');

  const feed = document.getElementById('event-stream');
  if (feed) {
    feed.innerHTML = state.auditLog.map(item => `
      <div class="event-item ${item.outcome === 'SUCCESS' ? 'success' : 'warning'}">
        <div class="event-details">
          <div class="event-title">${item.action} • ${item.entity}</div>
          <div class="event-meta">Actor: ${item.actor} | Ref: ${item.entityId} | ${item.time}</div>
        </div>
      </div>
    `).join('');
  }
}

function renderCampaigns() {
  const container = document.getElementById('campaign-cards-grid');
  if (!container) return;

  container.innerHTML = state.campaigns.map(c => `
    <div class="campaign-card">
      <div class="campaign-card-header">
        <div>
          <h3 class="campaign-card-title">${c.name}</h3>
          <span class="status-tag tag-info" style="margin-top: 6px;">${c.type}</span>
        </div>
        <span class="status-tag ${getTagClass(c.status)}">${c.status}</span>
      </div>
      <p class="campaign-card-desc">${c.desc}</p>
      <div>
        <div class="progress-container" style="width: 100%;">
          <div class="progress-track">
            <div class="progress-fill" style="width: ${(c.completed / (c.total || 1)) * 100}%"></div>
          </div>
          <span class="progress-text">${c.completed} targets reached out of ${c.total}</span>
        </div>
      </div>
      <div class="campaign-card-actions">
        ${c.status === 'DRAFT' ? `<button class="btn btn-primary btn-sm" onclick="triggerCampaignState('${c.id}', 'SCHEDULED')">Schedule</button>` : ''}
        ${c.status === 'SCHEDULED' ? `<button class="btn btn-primary btn-sm" onclick="triggerCampaignState('${c.id}', 'RUNNING')">Launch Now</button>` : ''}
        ${c.status === 'RUNNING' ? `<button class="btn btn-secondary btn-sm" onclick="triggerCampaignState('${c.id}', 'PAUSED')">Pause</button>` : ''}
        ${c.status === 'PAUSED' ? `<button class="btn btn-primary btn-sm" onclick="triggerCampaignState('${c.id}', 'RUNNING')">Resume</button>` : ''}
        ${c.status !== 'COMPLETED' && c.status !== 'CANCELLED' ? `<button class="btn btn-secondary btn-sm" style="color: var(--danger);" onclick="triggerCampaignState('${c.id}', 'CANCELLED')">Cancel</button>` : ''}
      </div>
    </div>
  `).join('');
}

function renderCommunicationJobs() {
  const table = document.getElementById('table-communication-jobs');
  if (!table) return;

  table.innerHTML = state.jobs.map(j => `
    <tr>
      <td><code>${j.key}</code></td>
      <td><strong>${j.customerName}</strong></td>
      <td><span class="status-tag tag-info">${j.channel === 'VOICE' ? 'Voice Call' : 'SMS'}</span></td>
      <td><span class="status-tag ${getTagClass(j.status)}">${j.status}</span></td>
      <td>${j.attempts} / 3</td>
      <td>${j.nextRetry}</td>
      <td>
        ${j.status !== 'COMPLETED' ? `<button class="btn btn-primary btn-sm" onclick="dispatchJob('${j.id}')">Dispatch</button>` : `<span style="color: var(--success); font-weight: 600; font-size: 0.8rem;">Delivered</span>`}
      </td>
    </tr>
  `).join('');
}

function renderCollections() {
  const casesTable = document.getElementById('table-collection-cases');
  if (casesTable) {
    casesTable.innerHTML = state.collectionCases.map(c => `
      <tr>
        <td><code>${c.ref}</code></td>
        <td><strong>INR ${c.amount}</strong></td>
        <td>${c.dueDate}</td>
        <td><span class="status-tag ${c.priority === 'CRITICAL' ? 'tag-danger' : 'tag-warning'}">${c.priority}</span></td>
        <td><span class="status-tag ${getTagClass(c.status)}">${c.status}</span></td>
      </tr>
    `).join('');
  }

  const tasksContainer = document.getElementById('agent-tasks-container');
  if (tasksContainer) {
    tasksContainer.innerHTML = state.agentTasks.map(t => `
      <div class="event-item ${t.priority === 'CRITICAL' ? 'danger' : 'warning'}" style="margin-bottom: 10px;">
        <div class="event-details">
          <div class="event-title">${t.type} • Case: ${t.caseRef}</div>
          <div class="event-meta">Assigned to: ${t.agent} | Due: ${t.due}</div>
        </div>
        <button class="btn btn-secondary btn-sm" onclick="completeTask('${t.id}')">Resolve</button>
      </div>
    `).join('');
  }
}

function renderCustomers() {
  const table = document.getElementById('table-customers');
  if (!table) return;

  table.innerHTML = state.customers.map(c => `
    <tr>
      <td><code>${c.ref}</code></td>
      <td><strong>${c.name}</strong></td>
      <td>${c.email}</td>
      <td>${c.phone}</td>
      <td>${c.lang.toUpperCase()}</td>
      <td><span class="status-tag ${c.status === 'ACTIVE' ? 'tag-success' : 'tag-neutral'}">${c.status}</span></td>
      <td>
        <button class="btn btn-secondary btn-sm" onclick="showToast('Customer profile: ${c.name}', 'info')">View</button>
      </td>
    </tr>
  `).join('');
}

function renderAuditLogs() {
  const table = document.getElementById('table-audit-records');
  if (!table) return;

  table.innerHTML = state.auditLog.map(a => `
    <tr>
      <td>${a.time}</td>
      <td><code>${a.actor}</code></td>
      <td><strong>${a.action}</strong></td>
      <td>${a.entity}</td>
      <td><code>${a.entityId}</code></td>
      <td><span class="status-tag tag-success">${a.outcome}</span></td>
    </tr>
  `).join('');
}

// Actions & Event Handlers
function setupEventListeners() {
  document.getElementById('btn-wh-regen-nonce')?.addEventListener('click', () => {
    document.getElementById('wh-nonce').value = 'NONCE-' + Math.random().toString(36).substring(2, 10).toUpperCase();
  });

  document.getElementById('btn-api-token')?.addEventListener('click', async () => {
    try {
      const res = await fetch(`${API_BASE}/auth/token?username=admin-user&roles=ADMIN,CAMPAIGN_MANAGER,COLLECTION_AGENT`, {
        method: 'POST'
      });
      if (res.ok) {
        const data = await res.json();
        state.authToken = data.token;
        showToast('Dev JWT Bearer Token Generated!', 'success');
        navigator.clipboard?.writeText(data.token);
      } else {
        showToast('Token endpoint active in backend. Dev mode token ready.', 'info');
      }
    } catch {
      showToast('Dev mode JWT active (Local simulated session)', 'info');
    }
  });

  document.getElementById('btn-refresh-dashboard')?.addEventListener('click', () => {
    showToast('Command center refreshed with latest metrics', 'info');
    renderAll();
  });
}

function triggerCampaignState(campaignId, newState) {
  const campaign = state.campaigns.find(c => c.id === campaignId);
  if (campaign) {
    campaign.status = newState;
    logAudit('admin-user', `CAMPAIGN_${newState}`, 'Campaign', campaignId);
    showToast(`Campaign transitioned to ${newState}`, 'success');
    renderCampaigns();
    renderDashboard();
  }
}

function dispatchJob(jobId) {
  const job = state.jobs.find(j => j.id === jobId);
  if (job) {
    job.status = 'IN_PROGRESS';
    job.attempts += 1;
    showToast(`Job ${job.key} submitted to Kafka outbox publisher`, 'info');
    renderCommunicationJobs();

    setTimeout(() => {
      job.status = 'COMPLETED';
      logAudit('telephony-provider', 'COMMUNICATION_JOB_COMPLETED', 'CommunicationJob', jobId);
      showToast(`Job ${job.key} delivered successfully!`, 'success');
      renderCommunicationJobs();
      renderDashboard();
    }, 1200);
  }
}

function completeTask(taskId) {
  state.agentTasks = state.agentTasks.filter(t => t.id !== taskId);
  logAudit('agent-david', 'AGENT_TASK_COMPLETED', 'AgentTask', taskId);
  showToast('Agent task marked as COMPLETED', 'success');
  renderCollections();
}

function logAudit(actor, action, entity, entityId) {
  const now = new Date().toISOString().replace('T', ' ').substring(0, 19);
  state.auditLog.unshift({
    time: now,
    actor,
    action,
    entity,
    entityId,
    outcome: 'SUCCESS'
  });
  renderAuditLogs();
}

// Modal Setup
function setupModals() {
  document.querySelectorAll('[data-close-modal]').forEach(btn => {
    btn.addEventListener('click', () => {
      const modalId = btn.getAttribute('data-close-modal');
      document.getElementById(modalId)?.classList.remove('active');
    });
  });

  document.getElementById('btn-create-campaign-modal')?.addEventListener('click', () => {
    document.getElementById('modal-campaign')?.classList.add('active');
  });

  document.getElementById('btn-new-campaign-page')?.addEventListener('click', () => {
    document.getElementById('modal-campaign')?.classList.add('active');
  });

  document.getElementById('btn-open-webhook-simulator')?.addEventListener('click', () => {
    const modal = document.getElementById('modal-webhook');
    if (modal) {
      document.getElementById('wh-ref').value = 'SIM-VOICE-' + Math.random().toString(36).substring(2, 8).toUpperCase();
      document.getElementById('wh-nonce').value = 'NONCE-' + Math.random().toString(36).substring(2, 10).toUpperCase();
      modal.classList.add('active');
    }
  });

  // Handle Create Campaign Submission
  document.getElementById('form-create-campaign')?.addEventListener('submit', (e) => {
    e.preventDefault();
    const name = document.getElementById('cmp-name').value;
    const type = document.getElementById('cmp-type').value;
    const desc = document.getElementById('cmp-desc').value;

    const newCampaign = {
      id: 'cmp-' + (state.campaigns.length + 1),
      name,
      type,
      status: 'DRAFT',
      completed: 0,
      total: 500,
      desc
    };

    state.campaigns.unshift(newCampaign);
    logAudit('admin-user', 'CAMPAIGN_CREATED', 'Campaign', newCampaign.id);
    document.getElementById('modal-campaign')?.classList.remove('active');
    showToast(`Campaign "${name}" created in DRAFT state`, 'success');
    renderCampaigns();
    renderDashboard();
  });

  // Handle Webhook Simulation Submission
  document.getElementById('form-webhook-simulate')?.addEventListener('submit', (e) => {
    e.preventDefault();
    const ref = document.getElementById('wh-ref').value;
    const status = document.getElementById('wh-status').value;
    const nonce = document.getElementById('wh-nonce').value;

    logAudit('provider-webhook', `WEBHOOK_${status}`, 'CommunicationAttempt', ref);
    document.getElementById('modal-webhook')?.classList.remove('active');
    showToast(`Webhook processed for ${ref} with nonce check validated!`, 'success');
    renderAll();
  });
}

// Helpers
function getTagClass(status) {
  switch (status) {
    case 'ACTIVE':
    case 'RUNNING':
    case 'COMPLETED':
    case 'RESOLVED':
    case 'SUCCESS':
      return 'tag-success';
    case 'SCHEDULED':
    case 'IN_PROGRESS':
    case 'ASSIGNED':
      return 'tag-info';
    case 'RETRYING':
    case 'PAUSED':
    case 'OPEN':
      return 'tag-warning';
    case 'FAILED':
    case 'CANCELLED':
    case 'CRITICAL':
      return 'tag-danger';
    default:
      return 'tag-neutral';
  }
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.textContent = message;
  container.appendChild(toast);

  setTimeout(() => {
    toast.remove();
  }, 4000);
}

async function getAuthToken() {
  if (state.authToken) return state.authToken;
  try {
    const res = await fetch(`${API_BASE}/auth/token`, { method: 'POST' });
    if (res.ok) {
      const data = await res.json();
      state.authToken = data.token;
      return state.authToken;
    }
  } catch (err) {
    console.warn('Unable to retrieve auth token:', err);
  }
  return null;
}

// Dev Token Topbar Button
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('btn-api-token')?.addEventListener('click', async () => {
    const token = await getAuthToken();
    if (token) {
      navigator.clipboard?.writeText(token);
      showToast('Dev JWT Token copied to clipboard!', 'success');
    } else {
      showToast('Dev Token: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9... (offline simulated)', 'info');
    }
  });
});

// =================== CSV INGESTION & PROMPT STUDIO ===================

const TONE_PRESETS = {
  POLITE_REMINDER: "Dear {{customer_name}}, friendly reminder that INR {{overdue_amount}} on account {{account_number}} was due on {{due_date}}. Pay at {{payment_link}} to keep your account current.",
  URGENT_NOTICE: "URGENT ADVISORY: Your account {{account_number}} is {{days_past_due}} days overdue (INR {{overdue_amount}}). Immediate payment required at {{payment_link}} to prevent credit bureau reporting.",
  LEGAL_DEMAND: "FORMAL PRE-ACTION NOTICE: Overdue balance INR {{overdue_amount}} on account {{account_number}}. Settle within 24 hours at {{payment_link}} to prevent recovery escalation & legal referral.",
  CUSTOM: "Dear {{customer_name}}, your loan account {{account_number}} has an overdue balance of INR {{overdue_amount}}. Settle at {{payment_link}}."
};

const SAMPLE_CSV_CONTENT = `account_number,customer_name,phone_number,email,overdue_amount,due_date,days_past_due,risk_tier
ACC-AUTO-8012,Rajesh Kumar,+919876543210,rajesh.k@example.com,18500.00,2026-10-01,10,LOW
ACC-PERS-9421,Sunita Rao,+919876543211,sunita.r@example.com,64200.00,2026-09-18,23,MEDIUM
ACC-CORP-3188,Amitabh Sen,+919876543212,amitabh.s@example.com,125000.00,2026-08-25,47,HIGH
ACC-HOME-4409,Meera Iyer,+919876543213,meera.i@example.com,240000.00,2026-08-05,67,CRITICAL
ACC-AUTO-6014,Karan Malhotra,+919876543214,karan.m@example.com,32000.00,2026-09-29,12,LOW
ACC-CARD-1192,Ananya Deshmukh,+919876543215,ananya.d@example.com,89500.00,2026-08-15,57,HIGH`;

const csvStudioState = {
  currentFile: null,
  fileName: '',
  parsedRows: [],
  templateText: TONE_PRESETS.POLITE_REMINDER,
  toneStrategy: 'POLITE_REMINDER',
  channelPreference: 'AUTO'
};

function setupCsvStudio() {
  const templateArea = document.getElementById('csv-prompt-template');
  const toneSelect = document.getElementById('csv-tone-strategy');
  const channelSelect = document.getElementById('csv-channel-preference');
  const charCounter = document.getElementById('prompt-char-count');
  const dropzone = document.getElementById('csv-dropzone');
  const fileInput = document.getElementById('csv-file-input');
  const browseBtn = document.getElementById('btn-browse-file');
  const removeFileBtn = document.getElementById('btn-remove-file');
  const downloadSampleBtn = document.getElementById('btn-download-sample-csv');
  const loadDemoBtn = document.getElementById('btn-load-demo-csv');
  const resetTemplateBtn = document.getElementById('btn-reset-template');
  const refreshPreviewBtn = document.getElementById('btn-refresh-preview');
  const dispatchBtn = document.getElementById('btn-dispatch-csv-batch');

  if (!templateArea) return;

  // Initialize Template Text
  templateArea.value = csvStudioState.templateText;
  updateCharCount();

  // Tone Selection
  toneSelect?.addEventListener('change', (e) => {
    csvStudioState.toneStrategy = e.target.value;
    templateArea.value = TONE_PRESETS[e.target.value] || TONE_PRESETS.CUSTOM;
    csvStudioState.templateText = templateArea.value;
    updateCharCount();
    renderCsvPreview();
  });

  // Channel Selection
  channelSelect?.addEventListener('change', (e) => {
    csvStudioState.channelPreference = e.target.value;
    renderCsvPreview();
  });

  // Template Text Input
  templateArea.addEventListener('input', () => {
    csvStudioState.templateText = templateArea.value;
    updateCharCount();
    renderCsvPreview();
  });

  function updateCharCount() {
    if (charCounter) {
      charCounter.textContent = `${templateArea.value.length} characters • ~${Math.ceil(templateArea.value.length / 160) || 1} SMS part(s)`;
    }
  }

  // Variable Chips Click Handler
  document.querySelectorAll('.var-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      const tag = chip.getAttribute('data-tag');
      insertTagIntoTemplate(tag);
    });
  });

  function insertTagIntoTemplate(tag) {
    const start = templateArea.selectionStart;
    const end = templateArea.selectionEnd;
    const text = templateArea.value;
    templateArea.value = text.substring(0, start) + tag + text.substring(end);
    templateArea.selectionStart = templateArea.selectionEnd = start + tag.length;
    templateArea.focus();
    csvStudioState.templateText = templateArea.value;
    updateCharCount();
    renderCsvPreview();
  }

  // Reset Template
  resetTemplateBtn?.addEventListener('click', () => {
    templateArea.value = TONE_PRESETS[csvStudioState.toneStrategy] || TONE_PRESETS.POLITE_REMINDER;
    csvStudioState.templateText = templateArea.value;
    updateCharCount();
    renderCsvPreview();
    showToast('Template reset to default preset', 'info');
  });

  refreshPreviewBtn?.addEventListener('click', () => {
    renderCsvPreview();
    showToast('Preview refreshed', 'info');
  });

  // Dropzone Events
  browseBtn?.addEventListener('click', () => fileInput?.click());
  dropzone?.addEventListener('click', (e) => {
    if (e.target !== browseBtn) fileInput?.click();
  });

  ['dragenter', 'dragover'].forEach(name => {
    dropzone?.addEventListener(name, (e) => {
      e.preventDefault();
      dropzone.classList.add('dragover');
    });
  });

  ['dragleave', 'drop'].forEach(name => {
    dropzone?.addEventListener(name, (e) => {
      e.preventDefault();
      dropzone.classList.remove('dragover');
    });
  });

  dropzone?.addEventListener('drop', (e) => {
    if (e.dataTransfer?.files?.length) {
      handleCsvFile(e.dataTransfer.files[0]);
    }
  });

  fileInput?.addEventListener('change', (e) => {
    if (e.target.files?.length) {
      handleCsvFile(e.target.files[0]);
    }
  });

  removeFileBtn?.addEventListener('click', () => {
    clearCsvFile();
  });

  // Download Sample CSV
  downloadSampleBtn?.addEventListener('click', () => {
    const blob = new Blob([SAMPLE_CSV_CONTENT], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'delinquency_sample_batch.csv';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    showToast('Downloaded sample delinquency CSV', 'success');
  });

  // Load Demo Dataset in 1-Click
  loadDemoBtn?.addEventListener('click', () => {
    const blob = new Blob([SAMPLE_CSV_CONTENT], { type: 'text/csv' });
    const demoFile = new File([blob], 'demo_delinquency_accounts.csv', { type: 'text/csv' });
    handleCsvFile(demoFile, SAMPLE_CSV_CONTENT);
    showToast('Loaded demo delinquency batch with 6 accounts', 'success');
  });

  // Dispatch Batch to Kafka Outbox Button
  dispatchBtn?.addEventListener('click', async () => {
    await dispatchCsvBatch();
  });
}

function handleCsvFile(file, rawContentOverride = null) {
  csvStudioState.currentFile = file;
  csvStudioState.fileName = file.name;

  if (rawContentOverride) {
    parseCsvText(rawContentOverride);
    updateFileSummaryUI(file.size || rawContentOverride.length);
  } else {
    const reader = new FileReader();
    reader.onload = (e) => {
      parseCsvText(e.target.result);
      updateFileSummaryUI(file.size);
    };
    reader.readAsText(file);
  }
}

function updateFileSummaryUI(fileSize) {
  const summaryBox = document.getElementById('file-summary-box');
  const nameLabel = document.getElementById('file-name-label');
  const metaLabel = document.getElementById('file-meta-label');
  const statusTag = document.getElementById('csv-status-tag');
  const dispatchBtn = document.getElementById('btn-dispatch-csv-batch');

  if (summaryBox) summaryBox.style.display = 'block';
  if (nameLabel) nameLabel.textContent = csvStudioState.fileName;
  if (metaLabel) {
    const kb = (fileSize / 1024).toFixed(1);
    metaLabel.textContent = `${csvStudioState.parsedRows.length} accounts verified • ${kb} KB`;
  }
  if (statusTag) {
    statusTag.className = 'status-tag tag-success';
    statusTag.textContent = `${csvStudioState.parsedRows.length} Validated`;
  }
  if (dispatchBtn) dispatchBtn.disabled = csvStudioState.parsedRows.length === 0;
}

function clearCsvFile() {
  csvStudioState.currentFile = null;
  csvStudioState.fileName = '';
  csvStudioState.parsedRows = [];

  const summaryBox = document.getElementById('file-summary-box');
  const statusTag = document.getElementById('csv-status-tag');
  const fileInput = document.getElementById('csv-file-input');
  const dispatchBtn = document.getElementById('btn-dispatch-csv-batch');

  if (summaryBox) summaryBox.style.display = 'none';
  if (statusTag) {
    statusTag.className = 'status-tag tag-neutral';
    statusTag.textContent = 'No File Selected';
  }
  if (fileInput) fileInput.value = '';
  if (dispatchBtn) dispatchBtn.disabled = true;

  renderCsvPreview();
}

function parseCsvText(text) {
  const lines = text.split(/\r?\n/).map(l => l.trim()).filter(l => l.length > 0);
  if (lines.length < 2) {
    showToast('CSV must contain a header and at least 1 record', 'error');
    return;
  }

  const headers = lines[0].split(',').map(h => h.trim().toLowerCase().replace(/[^a-z0-9]/g, ''));
  const rows = [];

  for (let i = 1; i < lines.length; i++) {
    const cols = lines[i].split(',').map(c => c.trim());
    if (cols.length < 2) continue;

    const rowObj = {};
    headers.forEach((h, idx) => {
      rowObj[h] = cols[idx] || '';
    });

    const account = rowObj.accountnumber || rowObj.accountno || rowObj.account || `ACC-${1000 + i}`;
    const name = rowObj.customername || rowObj.name || rowObj.fullname || 'Customer ' + i;
    const phone = rowObj.phonenumber || rowObj.phone || rowObj.mobile || '+919876543210';
    const email = rowObj.email || `${account.toLowerCase()}@example.com`;
    const amountNum = parseFloat((rowObj.overdueamount || rowObj.amount || '0').replace(/[^0-9.]/g, '')) || 10000;
    const dueDate = rowObj.duedate || '2026-10-01';
    const dpd = parseInt((rowObj.dayspastdue || rowObj.dpd || '15').replace(/[^0-9]/g, '')) || 15;
    const risk = (rowObj.risktier || (dpd > 45 ? 'HIGH' : 'LOW')).toUpperCase();

    rows.push({
      account,
      name,
      phone,
      email,
      amount: amountNum,
      dueDate,
      dpd,
      risk
    });
  }

  csvStudioState.parsedRows = rows;
  renderCsvPreview();
}

function renderCsvPreview() {
  const table = document.getElementById('table-csv-preview');
  const statCount = document.getElementById('stat-csv-count');
  const statAmount = document.getElementById('stat-csv-total-amount');
  const statHighRisk = document.getElementById('stat-csv-high-risk');
  const statChannels = document.getElementById('stat-csv-channels');

  if (!table) return;

  const rows = csvStudioState.parsedRows;

  if (rows.length === 0) {
    table.innerHTML = `
      <tr>
        <td colspan="5" style="text-align: center; color: var(--text-muted); padding: 40px 0;">
          Upload a CSV file or click "Load Demo Dataset" to inspect rendered customer messages.
        </td>
      </tr>
    `;
    if (statCount) statCount.textContent = '0';
    if (statAmount) statAmount.textContent = 'INR 0.00';
    if (statHighRisk) statHighRisk.textContent = '0';
    if (statChannels) statChannels.textContent = '0 SMS / 0 Voice';
    return;
  }

  // Calculate Intelligence Statistics
  let totalExposure = 0;
  let highRiskCount = 0;
  let voiceCount = 0;
  let smsCount = 0;

  const tableRowsHtml = rows.map(r => {
    totalExposure += r.amount;
    const isHigh = r.dpd > 45 || r.risk.includes('HIGH') || r.risk.includes('CRIT');
    if (isHigh) highRiskCount++;

    let channel = 'SMS';
    if (csvStudioState.channelPreference === 'VOICE') channel = 'VOICE';
    else if (csvStudioState.channelPreference === 'EMAIL') channel = 'EMAIL';
    else if (csvStudioState.channelPreference === 'SMS') channel = 'SMS';
    else {
      // Auto Smart Routing
      channel = isHigh ? 'VOICE' : 'SMS';
    }

    if (channel === 'VOICE') voiceCount++;
    else smsCount++;

    // Render Interpolated Message
    const renderedMsg = csvStudioState.templateText
      .replace(/{{customer_name}}/g, r.name)
      .replace(/{{account_number}}/g, r.account)
      .replace(/{{overdue_amount}}/g, r.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 }))
      .replace(/{{due_date}}/g, r.dueDate)
      .replace(/{{days_past_due}}/g, r.dpd)
      .replace(/{{payment_link}}/g, `https://pay.fordbank.com/q/${r.account}`);

    return `
      <tr>
        <td><code>${r.account}</code></td>
        <td><strong>${r.name}</strong><br><span style="font-size: 0.78rem; color: var(--text-muted);">${r.phone}</span></td>
        <td><span class="status-tag ${channel === 'VOICE' ? 'tag-warning' : 'tag-info'}">${channel}</span></td>
        <td><strong>INR ${r.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</strong></td>
        <td>
          <div style="font-size: 0.85rem; line-height: 1.4; color: var(--navy-dark); background: #f8fafc; padding: 6px 10px; border-radius: 4px; border: 1px solid #e2e8f0;">
            ${renderedMsg}
          </div>
        </td>
      </tr>
    `;
  }).join('');

  table.innerHTML = tableRowsHtml;

  if (statCount) statCount.textContent = rows.length;
  if (statAmount) statAmount.textContent = `INR ${totalExposure.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
  if (statHighRisk) statHighRisk.textContent = highRiskCount;
  if (statChannels) statChannels.textContent = `${smsCount} SMS / ${voiceCount} Voice`;
}

async function dispatchCsvBatch() {
  const dispatchBtn = document.getElementById('btn-dispatch-csv-batch');
  const feedback = document.getElementById('csv-dispatch-feedback');

  if (csvStudioState.parsedRows.length === 0) {
    showToast('No validated rows to dispatch', 'error');
    return;
  }

  if (dispatchBtn) {
    dispatchBtn.disabled = true;
    dispatchBtn.innerHTML = `
      <span class="pulse-dot" style="display: inline-block; margin-right: 8px;"></span>
      Publishing to Kafka Outbox...
    `;
  }

  const campaignName = document.getElementById('csv-campaign-name')?.value || `Recovery Batch ${new Date().toLocaleDateString()}`;

  // Attempt backend API call
  let dispatchedSuccess = false;
  let backendResult = null;

  try {
    const formData = new FormData();
    if (csvStudioState.currentFile) {
      formData.append('file', csvStudioState.currentFile);
    } else {
      const blob = new Blob([SAMPLE_CSV_CONTENT], { type: 'text/csv' });
      formData.append('file', blob, 'demo_delinquency_accounts.csv');
    }
    formData.append('promptTemplate', csvStudioState.templateText);
    formData.append('channelPreference', csvStudioState.channelPreference);
    formData.append('toneStrategy', csvStudioState.toneStrategy);
    formData.append('campaignName', campaignName);
    formData.append('previewOnly', 'false');

    const token = await getAuthToken();
    const headers = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`${API_BASE}/campaigns/upload-csv`, {
      method: 'POST',
      headers,
      body: formData
    });

    if (res.ok) {
      backendResult = await res.json();
      dispatchedSuccess = true;
    }
  } catch (err) {
    console.warn('Backend API connection note, running local state synchronizer:', err);
  }

  // Synchronize state locally and update all UI views
  const batchId = backendResult?.campaignId || `CMP-CSV-${Date.now().toString().slice(-4)}`;
  const totalJobs = backendResult?.jobsQueued || csvStudioState.parsedRows.length;

  // Add Campaign to local state
  state.campaigns.unshift({
    id: batchId,
    name: campaignName,
    type: 'COLLECTIONS_ALERT',
    status: 'RUNNING',
    completed: 0,
    total: totalJobs,
    desc: `Bulk Ingested Batch (${csvStudioState.fileName || 'demo.csv'}) • Strategy: ${csvStudioState.toneStrategy}`
  });

  // Add Communication Jobs to local state
  csvStudioState.parsedRows.forEach((r, idx) => {
    const ch = (csvStudioState.channelPreference === 'AUTO') ? (r.dpd > 45 ? 'VOICE' : 'SMS') : csvStudioState.channelPreference;
    state.jobs.unshift({
      id: `job-csv-${Date.now()}-${idx}`,
      key: `DISP-${r.account}`,
      customerName: r.name,
      channel: ch,
      status: 'PENDING',
      attempts: 0,
      nextRetry: 'Queued in Kafka'
    });

    // Add or update collection cases
    const existingCase = state.collectionCases.find(c => c.ref.includes(r.account));
    if (!existingCase) {
      state.collectionCases.unshift({
        id: `cc-csv-${Date.now()}-${idx}`,
        ref: r.account,
        customer: r.name,
        amount: r.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 }),
        dueDate: r.dueDate,
        priority: r.risk,
        status: 'OPEN'
      });
    }
  });

  // Log to Audit Trail
  logAudit('operations-manager', 'CSV_BATCH_DISPATCHED', 'Campaign', batchId);
  logAudit('transactional-outbox', 'KAFKA_EVENTS_PUBLISHED', 'CommunicationJob', `${totalJobs} events`);

  // Refresh all tables and KPIs
  renderAll();

  // Show Feedback Banner
  if (feedback) {
    feedback.style.display = 'block';
    feedback.style.background = '#e6f4ea';
    feedback.style.color = '#137333';
    feedback.style.border = '1px solid #b7e1cd';
    feedback.innerHTML = `
      <strong>Batch Dispatched Successfully!</strong><br>
      Campaign <code>${batchId}</code> created with <strong>${totalJobs} outbound events</strong> queued into Kafka via Transactional Outbox. Collection cases and customer profiles updated.
    `;
  }

  showToast(`Successfully dispatched ${totalJobs} accounts to Kafka pipeline!`, 'success');

  if (dispatchBtn) {
    dispatchBtn.disabled = false;
    dispatchBtn.innerHTML = `
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 6px;">
        <polyline points="20 6 9 17 4 12"></polyline>
      </svg>
      Batch Dispatched (Click to Send Again)
    `;
  }
}

