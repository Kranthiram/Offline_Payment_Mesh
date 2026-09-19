function log(msg) {
    const el = document.getElementById('log');
    el.textContent = '[' + new Date().toLocaleTimeString() + '] ' + msg + '\n' + el.textContent;
}

async function refresh() {
    // Mesh state (devices + idempotency cache size)
    const m = await fetch('/api/mesh/state').then(r => r.json());
    const devicesDiv = document.getElementById('devices');
    devicesDiv.innerHTML = m.devices.map(d => `
        <div class="device ${d.hasInternet ? 'bridge' : 'offline'}">
            <strong>${d.deviceId}</strong>
            <span class="badge ${d.hasInternet ? 'badge-online' : 'badge-offline'}">
                ${d.hasInternet ? 'BRIDGE' : 'OFFLINE'}
            </span>
            <span class="small">holding ${d.packetCount} packet(s)</span>
            <div>${d.packetIds.map(id => `<span class="packet-id">${id}</span>`).join('')}</div>
        </div>
    `).join('');
    document.getElementById('cacheInfo').textContent =
        `Idempotency cache size: ${m.idempotencyCacheSize}`;

    // Accounts
    const accs = await fetch('/api/accounts').then(r => r.json());
    document.querySelector('#accounts-table tbody').innerHTML = accs.map(a => `
        <tr><td>${a.vpa}</td><td>${a.ownerName}</td>
            <td class="balance">₹${parseFloat(a.balance).toFixed(2)}</td></tr>
    `).join('');

    // Transactions
    const txs = await fetch('/api/transactions').then(r => r.json());
    document.querySelector('#tx-table tbody').innerHTML = txs.map(t => `
        <tr>
            <td>${t.id}</td><td>${t.senderVpa}</td><td>${t.receiverVpa}</td>
            <td class="balance">₹${parseFloat(t.amount).toFixed(2)}</td>
            <td>${t.bridgeNodeId || '-'}</td><td>${t.hopCount}</td>
            <td class="small">${t.settledAt ? new Date(t.settledAt).toLocaleTimeString() : '-'}</td>
        </tr>
    `).join('');
}

async function setupMesh() {
    const r = await fetch('/api/mesh/setup', { method: 'POST' }).then(r => r.json());
    log(`Mesh setup: ${r.status}`);
    refresh();
}

async function injectPayment() {
    const body = {
        senderVpa: document.getElementById('sender').value,
        receiverVpa: document.getElementById('receiver').value,
        amount: parseFloat(document.getElementById('amount').value)
    };
    const r = await fetch('/api/mesh/inject', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    }).then(r => r.json());
    log(`Payment injected into phone-A: ${r.status}`);
    refresh();
}

async function propagate() {
    const r = await fetch('/api/mesh/propagate', { method: 'POST' }).then(r => r.json());
    log(`Gossip round complete: ${r.status}`);
    refresh();
}

async function syncBridges() {
    const results = await fetch('/api/mesh/sync', { method: 'POST' }).then(r => r.json());
    if (!results.length) {
        log('Sync: no packets waiting at bridge devices');
    } else {
        results.forEach(res => {
            log(`Packet ${res.status}` + (res.detail ? ` — ${res.detail}` : ''));
        });
    }
    refresh();
}

async function resetMesh() {
    await fetch('/api/mesh/reset', { method: 'POST' });
    log('Mesh + idempotency cache + transactions + balances reset');
    refresh();
}

refresh();
setInterval(refresh, 3000);