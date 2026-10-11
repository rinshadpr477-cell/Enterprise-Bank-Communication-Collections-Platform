/**
 * Enterprise Bank Communication & Collections Platform
 * Frontend Orchestration Script
 */

const API_BASE = 'http://localhost:8080/api/v1';

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
      <td><span class="status-tag tag-info">${j.channel === 'VOICE' ? '📞 Voice Call' : '💬 SMS'}</span></td>
      <td><span class="status-tag ${getTagClass(j.status)}">${j.status}</span></td>
      <td>${j.attempts} / 3</td>
      <td>${j.nextRetry}</td>
      <td>
        ${j.status !== 'COMPLETED' ? `<button class="btn btn-primary btn-sm" onclick="dispatchJob('${j.id}')">⚡ Dispatch</button>` : `<span style="color: var(--success); font-size: 0.8rem;">✓ Delivered</span>`}
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
        <td><strong>₹ ${c.amount}</strong></td>
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
