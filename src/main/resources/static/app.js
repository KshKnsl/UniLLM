// API Configuration
const API_BASE_URL = 'http://localhost:8080/api/usage';
const REFRESH_INTERVAL = 10000; // 10 seconds

// DOM Elements
const statElements = {
    requests: document.getElementById('stat-requests'),
    success: document.getElementById('stat-success'),
    latency: document.getElementById('stat-latency'),
    tokens: document.getElementById('stat-tokens')
};
const recentRecordsBody = document.getElementById('recentRecordsBody');
const errorCountBadge = document.getElementById('errorCountBadge');
const refreshBtn = document.getElementById('refreshBtn');

// Chart Instances
let timelineChart = null;
let providerChart = null;

// Chart.js Global Config for Dark Theme
Chart.defaults.color = '#94a3b8';
Chart.defaults.font.family = "'Outfit', sans-serif";
Chart.defaults.plugins.tooltip.backgroundColor = 'rgba(24, 24, 27, 0.9)';
Chart.defaults.plugins.tooltip.titleColor = '#f8fafc';
Chart.defaults.plugins.tooltip.bodyColor = '#f8fafc';
Chart.defaults.plugins.tooltip.borderColor = 'rgba(255,255,255,0.1)';
Chart.defaults.plugins.tooltip.borderWidth = 1;
Chart.defaults.plugins.tooltip.padding = 10;
Chart.defaults.plugins.tooltip.cornerRadius = 8;

/**
 * Initialize Dashboard
 */
async function initDashboard() {
    setupEventListeners();
    await refreshData();
    
    // Auto refresh
    setInterval(refreshData, REFRESH_INTERVAL);
}

/**
 * Setup Event Listeners
 */
function setupEventListeners() {
    refreshBtn.addEventListener('click', () => {
        refreshBtn.classList.add('rotating');
        refreshData().finally(() => {
            setTimeout(() => refreshBtn.classList.remove('rotating'), 500);
        });
    });
}

/**
 * Fetch and Render Data
 */
async function refreshData() {
    try {
        const [statsRes, recentRes] = await Promise.all([
            fetch(`${API_BASE_URL}/stats`),
            fetch(`${API_BASE_URL}/recent?limit=20`)
        ]);

        if (!statsRes.ok || !recentRes.ok) throw new Error('Network response was not ok');

        const stats = await statsRes.json();
        const recent = await recentRes.json();

        updateStats(stats);
        updateCharts(stats);
        updateRecentTable(recent, stats.totalErrors);

    } catch (error) {
        console.error('Failed to fetch dashboard data:', error);
        // Optional: show a toast notification here
    }
}

/**
 * Update Top Stat Cards
 */
function updateStats(stats) {
    animateValue(statElements.requests, stats.totalRequests);
    statElements.success.textContent = `${stats.successRate}%`;
    statElements.success.style.color = stats.successRate < 95 ? 'var(--accent-warning)' : 'var(--text-primary)';
    statElements.latency.textContent = `${stats.avgLatencyMs} ms`;
    animateValue(statElements.tokens, stats.totalTokens);
}

/**
 * Utility: Animate Number Counter
 */
function animateValue(obj, end, duration = 1000) {
    let startTimestamp = null;
    const step = (timestamp) => {
        if (!startTimestamp) startTimestamp = timestamp;
        const progress = Math.min((timestamp - startTimestamp) / duration, 1);
        const current = Math.floor(progress * end);
        obj.textContent = current.toLocaleString();
        if (progress < 1) {
            window.requestAnimationFrame(step);
        }
    };
    window.requestAnimationFrame(step);
}

/**
 * Update Charts Area
 */
function updateCharts(stats) {
    renderTimelineChart(stats.timeline);
    renderProviderChart(stats.byProvider);
}

/**
 * Render Timeline (Line Chart)
 */
function renderTimelineChart(timelineData) {
    const ctx = document.getElementById('timelineChart').getContext('2d');
    
    // Process timeline data
    // Assuming backend returns last 24h ordered by oldest to newest
    const labels = timelineData.map(d => {
        if (d.hour === 0) return 'Now';
        return `${d.hour}h`;
    });
    const data = timelineData.map(d => d.count);

    // Create gradient
    const gradient = ctx.createLinearGradient(0, 0, 0, 400);
    gradient.addColorStop(0, 'rgba(59, 130, 246, 0.5)');
    gradient.addColorStop(1, 'rgba(59, 130, 246, 0.0)');

    if (timelineChart) {
        timelineChart.data.labels = labels;
        timelineChart.data.datasets[0].data = data;
        timelineChart.update();
        return;
    }

    timelineChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Requests',
                data: data,
                borderColor: '#3b82f6',
                borderWidth: 2,
                backgroundColor: gradient,
                fill: true,
                tension: 0.4,
                pointBackgroundColor: '#09090b',
                pointBorderColor: '#3b82f6',
                pointBorderWidth: 2,
                pointRadius: 3,
                pointHoverRadius: 5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: {
                        color: 'rgba(255, 255, 255, 0.05)',
                        borderDash: [5, 5]
                    },
                    border: { display: false }
                },
                x: {
                    grid: { display: false },
                    border: { display: false }
                }
            },
            interaction: {
                intersect: false,
                mode: 'index',
            },
        }
    });
}

/**
 * Render Provider Breakdown (Doughnut Chart)
 */
function renderProviderChart(providerData) {
    const ctx = document.getElementById('providerChart').getContext('2d');
    
    // Sort providers by request count
    const sortedProviders = Object.entries(providerData)
        .sort((a, b) => b[1].requests - a[1].requests);
        
    const labels = sortedProviders.map(p => p[0]);
    const data = sortedProviders.map(p => p[1].requests);
    
    // Theme colors for Chart
    const colors = [
        '#3b82f6', // blue
        '#8b5cf6', // purple
        '#10b981', // emerald
        '#f59e0b', // amber
        '#ec4899', // pink
        '#0ea5e9'  // sky
    ];

    if (providerChart) {
        providerChart.data.labels = labels;
        providerChart.data.datasets[0].data = data;
        providerChart.update();
        return;
    }

    providerChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: colors,
                borderWidth: 0,
                hoverOffset: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '75%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        usePointStyle: true,
                        padding: 20,
                        font: { size: 12 }
                    }
                }
            }
        }
    });
}

/**
 * Populate Recent Requests Table
 */
function updateRecentTable(records, totalErrors) {
    errorCountBadge.textContent = `${totalErrors} Errors`;
    
    if (!records || records.length === 0) {
        recentRecordsBody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center loading-text">No traffic yet.</td>
            </tr>
        `;
        return;
    }

    recentRecordsBody.innerHTML = '';
    
    records.forEach(rc => {
        const tr = document.createElement('tr');
        
        // Status Badge
        const statusHtml = rc.success 
            ? `<span class="status-badge status-success"><i class="fa-solid fa-check"></i> 200 OK</span>`
            : `<span class="status-badge status-error"><i class="fa-solid fa-xmark"></i> Error</span>`;
            
        // Human readable time
        const timeStr = new Date(rc.timestamp).toLocaleTimeString([], { hour12: false });
        
        tr.innerHTML = `
            <td>${statusHtml}</td>
            <td style="color: var(--text-secondary)">${timeStr}</td>
            <td><span class="provider-tag">${rc.provider || 'unknown'}</span></td>
            <td class="model-name">${rc.model || 'model-unknown'}</td>
            <td>${rc.latencyMs}ms</td>
            <td>
                <span>${rc.totalTokens.toLocaleString()}</span>
                <span style="color: var(--text-muted); font-size: 0.8em; margin-left: 0.5rem">
                    (${rc.promptTokens}/${rc.completionTokens})
                </span>
            </td>
        `;
        recentRecordsBody.appendChild(tr);
    });
}

// Small rotating animation class
const style = document.createElement('style');
style.textContent = `
    .rotating i {
        animation: rotate 0.5s linear infinite;
    }
    @keyframes rotate {
        100% { transform: rotate(360deg); }
    }
`;
document.head.appendChild(style);

// Run initialization
document.addEventListener('DOMContentLoaded', initDashboard);
