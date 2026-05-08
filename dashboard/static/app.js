const state = {
  summary: null,
  matrix: [],
  matrixFilter: "",
};

const $ = (id) => document.getElementById(id);

function fmtAge(value) {
  if (value === null || value === undefined) return "-";
  return `${Number(value).toFixed(1)}s`;
}

function logAction(text) {
  const node = $("actionLog");
  const stamp = new Date().toLocaleTimeString();
  node.textContent = `[${stamp}] ${text}\n${node.textContent}`;
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  const data = await response.json();
  if (!response.ok || data.ok === false) {
    throw new Error(data.error || `HTTP ${response.status}`);
  }
  return data;
}

async function post(path, payload = {}) {
  return api(path, { method: "POST", body: JSON.stringify(payload) });
}

function renderSummary(summary) {
  state.summary = summary;
  const session = summary.session || {};
  $("framesTotal").textContent = summary.frames_total ?? 0;
  $("fps").textContent = `${Number(summary.fps || 0).toFixed(1)}/s`;
  $("lastFrameAge").textContent = fmtAge(summary.last_frame_age);
  $("deviceState").textContent = session.note || "unknown";

  const badge = $("connectionBadge");
  badge.textContent = `${session.mode || "idle"} ${session.running ? "running" : "stopped"}`;
  badge.className = `badge ${session.running ? "live" : "neutral"}`;

  renderStates(summary.states || []);
  renderTopIds(summary.top || []);
  renderRecent(summary.recent || []);
}

function renderStates(groups) {
  const root = $("stateGroups");
  root.innerHTML = groups.map((group) => `
    <section class="panel">
      <div class="panel-head">
        <h2>${group.group}</h2>
        <span>${group.items.length} signals</span>
      </div>
      ${group.items.map((item) => `
        <div class="state-row">
          <div class="row-title">
            <strong title="${item.label}">${item.label}</strong>
            <span>${item.source}</span>
          </div>
          <div class="mono" title="${item.value}">${item.value}</div>
          <div class="status-text" title="${item.status}">${item.age}</div>
        </div>
      `).join("")}
    </section>
  `).join("");
}

function renderTopIds(rows) {
  const root = $("topIds");
  if (!rows.length) {
    root.innerHTML = `<div class="state-row"><div class="status-text">no frames yet</div></div>`;
    return;
  }
  root.innerHTML = rows.map((row) => `
    <div class="id-row">
      <strong class="mono">${row.id}</strong>
      <span class="status-text">${row.channel} dlc=${row.dlc} changed=[${row.changed.join(",") || "-"}]</span>
      <span>${row.count}</span>
      <span>${Number(row.hz).toFixed(1)} Hz</span>
    </div>
  `).join("");
}

function renderRecent(rows) {
  const root = $("recentFrames");
  if (!rows.length) {
    root.innerHTML = `<div class="frame-row"><span class="status-text">no frames yet</span></div>`;
    return;
  }
  root.innerHTML = rows.slice(0, 180).map((frame) => `
    <div class="frame-row">
      <span class="status-text">ch${frame.ch}</span>
      <strong class="mono">${frame.id_hex}</strong>
      <span class="status-text">dlc=${frame.dlc}</span>
      <span class="mono" title="${frame.data_spaced}">${frame.data_spaced || "-"}</span>
    </div>
  `).join("");
}

function renderMatrix() {
  const filter = state.matrixFilter.trim().toLowerCase();
  const rows = state.matrix.filter((row) => {
    if (!filter) return true;
    return Object.values(row).join(" ").toLowerCase().includes(filter);
  });
  $("matrixBody").innerHTML = rows.slice(0, 400).map((row) => `
    <tr>
      <td>${row.category || ""}</td>
      <td>${row.function || ""}</td>
      <td>${row.dbc_bus || row.observed_bitrate || ""}</td>
      <td class="mono">${row.dbc_id_hex || row.observed_id_hex || ""}</td>
      <td>${row.dbc_name || ""}<br><span class="status-text">${row.dbc_signals || ""}</span></td>
      <td>${row.status || ""}<br><span class="status-text">${row.implementation_type || ""}</span></td>
    </tr>
  `).join("");
}

async function refreshStatus() {
  const data = await api("/api/status");
  if (data.auto_port && !$("portInput").value) $("portInput").value = data.auto_port;
  renderSummary(data.summary);
  const device = data.gsusb_present === true ? "gs_usb present" : data.auto_port || "no USB";
  $("deviceState").textContent = device;
}

async function loadMatrix() {
  state.matrix = await api("/api/matrix");
  renderMatrix();
}

function setupEvents() {
  const source = new EventSource("/api/events");
  source.onmessage = (event) => {
    const payload = JSON.parse(event.data);
    if (payload.summary) renderSummary(payload.summary);
    if (payload.marker) logAction(`marker: ${payload.marker.name}`);
  };
}

function setupTabs() {
  document.querySelectorAll(".rail-item").forEach((button) => {
    button.addEventListener("click", () => {
      document.querySelectorAll(".rail-item").forEach((item) => item.classList.remove("active"));
      document.querySelectorAll(".tab").forEach((item) => item.classList.remove("active"));
      button.classList.add("active");
      $(`tab-${button.dataset.tab}`).classList.add("active");
    });
  });
}

function setupActions() {
  $("refreshStatus").addEventListener("click", () => refreshStatus().catch((error) => logAction(error.message)));
  $("startSample").addEventListener("click", async () => {
    await post("/api/log/start", { mode: "sample", speed: 20 });
    logAction("sample replay started");
  });
  $("startGsusb").addEventListener("click", async () => {
    const result = await post("/api/log/start", { mode: "gsusb", bitrate0: 100000, bitrate1: 500000 });
    logAction(`live gs_usb logger started; TX control ${result.tx_control || "off"}`);
  });
  $("startCdc").addEventListener("click", async () => {
    await post("/api/log/start", { mode: "cdc", port: $("portInput").value });
    logAction("live CDC logger started");
  });
  $("stopLog").addEventListener("click", async () => {
    await post("/api/log/stop");
    logAction("logger stopped");
  });
  document.querySelectorAll("[data-send]").forEach((button) => {
    button.addEventListener("click", async () => {
      const payload = {
        scenario: button.dataset.send,
        port: $("portInput").value,
        fm: $("fmText").value,
        media: $("mediaText").value,
        track: $("trackText").value,
        meters: Number($("navMeters").value),
        eta: Number($("etaDistance").value),
        seconds: Number($("repeatSeconds").value),
      };
      const result = await post("/api/send/display", payload);
      logAction(`${result.scenario}: sent ${result.sent} frames to ${result.port}`);
    });
  });
  document.querySelectorAll("[data-mode]").forEach((button) => {
    button.addEventListener("click", async () => {
      const result = await post("/api/mode", { port: $("portInput").value, mode: button.dataset.mode });
      logAction(`mode ${button.dataset.mode}: ${result.stdout || result.stderr || "sent"}`);
    });
  });
  $("sendCanFrame").addEventListener("click", async () => {
    const payload = {
      channel: Number($("txChannel").value),
      id: $("txCanId").value,
      data: $("txData").value,
      count: Number($("txCount").value),
      interval: Number($("txInterval").value),
      extended: $("txExtended").checked,
      confirm: $("txConfirm").checked,
    };
    const result = await post("/api/can/send", payload);
    logAction(`CAN TX queued: ${result.frames} frames`);
  });
  $("sweepCanByte").addEventListener("click", async () => {
    const payload = {
      channel: Number($("txChannel").value),
      id: $("txCanId").value,
      data: $("txData").value,
      interval: Number($("txInterval").value),
      extended: $("txExtended").checked,
      confirm: $("txConfirm").checked,
      byte_index: Number($("sweepByte").value),
      start: $("sweepFrom").value,
      end: $("sweepTo").value,
      count_each: Number($("sweepCountEach").value),
    };
    const result = await post("/api/can/sweep", payload);
    logAction(`CAN sweep queued: ${result.queued} values / ${result.frames} frames`);
  });
  $("addMarker").addEventListener("click", async () => {
    const name = $("markerText").value || "marker";
    await post("/api/marker", { name });
    $("markerText").value = "";
  });
  $("matrixSearch").addEventListener("input", (event) => {
    state.matrixFilter = event.target.value;
    renderMatrix();
  });
}

async function main() {
  setupTabs();
  setupActions();
  setupEvents();
  await Promise.all([refreshStatus(), loadMatrix()]);
}

main().catch((error) => logAction(error.message));
