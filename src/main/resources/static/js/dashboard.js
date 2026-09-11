function showResult(boxId, badgeClass, badgeText, detail) {
    const box = document.getElementById(boxId);
    box.className = 'result-box visible';
    box.innerHTML = `
        <span class="badge ${badgeClass}">${badgeText}</span>
        <div class="result-detail">${detail}</div>
    `;
}

async function post(url, body) {
    const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: body ? JSON.stringify(body) : undefined
    });
    return res.json();
}

async function setupMesh() {
    const data = await post('/api/mesh/setup');
    showResult('result-setup', 'info', 'Done', data.status);
}

async function injectPayment() {
    const sender = document.getElementById('sender').value;
    const receiver = document.getElementById('receiver').value;
    const amount = parseFloat(document.getElementById('amount').value);
    const data = await post('/api/mesh/inject', { senderVpa: sender, receiverVpa: receiver, amount: amount });
    showResult('result-inject', 'info', 'Injected', data.status || JSON.stringify(data));
}

async function propagate() {
    const data = await post('/api/mesh/propagate');
    showResult('result-propagate', 'info', 'Done', data.status);
}

async function syncBridges() {
    const results = await post('/api/mesh/sync');
    if (!results.length) {
        showResult('result-sync', 'info', 'No Packets', 'No packets waiting at bridge devices');
        return;
    }
    const summary = results.map(r => {
        const cls = r.status === 'SETTLED' ? 'settled' : (r.status === 'DUPLICATE' ? 'duplicate' : 'failed');
        return `<span class="badge ${cls}">${r.status}</span> ${r.detail || (r.transaction ? `${r.transaction.senderVpa} → ${r.transaction.receiverVpa}, ₹${r.transaction.amount}` : '')}`;
    }).join('<br>');
    showResult('result-sync', results[0].status === 'SETTLED' ? 'settled' : 'info', `${results.length} packet(s) processed`, summary);
}

async function refreshDevices() {
    const res = await fetch('/api/mesh/devices');
    const devices = await res.json();
    if (!devices.length) {
        document.getElementById('devicesTable').innerHTML = '<p class="empty-state">No devices yet — click Setup Mesh first</p>';
        return;
    }
    let rows = devices.map(d => `
        <tr>
            <td>${d.deviceId}</td>
            <td>${d.hasInternet ? '🌐 Bridge' : '📵 Offline'}</td>
            <td>${d.inboxSize} packet(s)</td>
        </tr>
    `).join('');
    document.getElementById('devicesTable').innerHTML = `
        <table>
            <tr><th>Device</th><th>Type</th><th>Inbox</th></tr>
            ${rows}
        </table>
    `;
}

async function refreshTransactions() {
    const res = await fetch('/api/transactions');
    const txs = await res.json();
    if (!txs.length) {
        document.getElementById('txTable').innerHTML = '<p class="empty-state">No settled transactions yet</p>';
        return;
    }
    let rows = txs.map(t => `
        <tr>
            <td>${t.senderVpa}</td>
            <td>${t.receiverVpa}</td>
            <td>₹${t.amount}</td>
            <td class="hash-cell">${t.packetHash.substring(0, 16)}...</td>
        </tr>
    `).join('');
    document.getElementById('txTable').innerHTML = `
        <table>
            <tr><th>Sender</th><th>Receiver</th><th>Amount</th><th>Packet Hash</th></tr>
            ${rows}
        </table>
    `;
}