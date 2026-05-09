const state = {
  summary: null,
  actions: [],
  commands: null,
  selectedAction: null,
  actionFilter: "",
  commandFilter: "",
  car: makeDefaultCar("live"),
  learnedAssignments: [],
  learnedStates: {},
  lastLearnResult: null,
  activeTab: "live",
  demo: {
    running: false,
    scenario: "walkaround",
    index: 0,
    timer: null,
    events: [],
  },
};

const $ = (id) => document.getElementById(id);
const ACTIVE_TAB_KEY = "2can35.activeTab.v2";

function makeDefaultCar(source = "live") {
  return {
    source,
    ignition: false,
    doors: { lf: false, rf: false, lr: false, rr: false },
    trunk: false,
    hood: false,
    sunroof: false,
    locked: false,
    reverse: false,
    reverseOutput12v: "-",
    speed: "0",
    gear: "-",
    brake: "-",
    parkBrake: "-",
    steering: "центр",
    turn: "off",
    hazard: false,
    lowBeam: false,
    highBeam: false,
    autoLight: "-",
    fog: false,
    frontFog: false,
    rearFog: false,
    wiper: "-",
    rearWiper: "-",
    seatbeltDriver: "-",
    seatbeltPassenger: "-",
    climate: "выкл",
    driverTemp: "-",
    passengerTemp: "-",
    fan: "-",
    climateMode: "-",
    autoClimate: "-",
    dual: "-",
    intake: "-",
    ac: false,
    frontDefog: false,
    rearDefog: false,
    heatedWheel: false,
    outsideTemp: "-",
    engineTemp: "-",
    batteryVoltage: "-",
    rpm: "-",
    driverSeatHeat: "off",
    passengerSeatHeat: "off",
    driverSeatVent: "off",
    passengerSeatVent: "off",
    parking: "clear",
    spas: "-",
    dynamicLines: "-",
    rcta: "нет",
    blindSpot: "-",
    tpms: "-",
    media: "нет данных",
    nav: "нет данных",
    mediaSource: "-",
    mediaRaw: "-",
    navRaw: "-",
    clusterRaw: "-",
    raw: {},
    lastLabel: "ожидание",
  };
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;",
  })[char]);
}

function fmtAge(value) {
  if (value === null || value === undefined) return "-";
  return `${Number(value).toFixed(1)}s`;
}

function fmtTime(ms) {
  if (!ms) return "--:--:--";
  return new Date(ms).toLocaleTimeString();
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

function deepMerge(target, patch) {
  for (const [key, value] of Object.entries(patch || {})) {
    if (value && typeof value === "object" && !Array.isArray(value)) {
      target[key] = deepMerge({ ...(target[key] || {}) }, value);
    } else {
      target[key] = value;
    }
  }
  return target;
}

function statusText(value, on = "вкл", off = "выкл") {
  return value ? on : off;
}

function openText(value) {
  return value ? "открыто" : "закрыто";
}

function isRealValue(value) {
  return value !== undefined && value !== null && value !== "" && value !== "-" && value !== "no data";
}

function rawFrame(summary, idHex, channel = null) {
  const latest = summary?.latest || {};
  const normalized = String(idHex).toUpperCase();
  if (channel !== null) {
    return latest[`ch${channel}:${normalized}`] || latest[normalized] || null;
  }
  return latest[normalized] || null;
}

function rawValue(summary, idHex, channel = null) {
  const frame = rawFrame(summary, idHex, channel);
  return frame?.data_spaced || "-";
}

function rawValueWithAge(summary, idHex, channel = null) {
  const frame = rawFrame(summary, idHex, channel);
  if (!frame?.data_spaced) return "-";
  const age = Number(frame.age ?? 0);
  return `${frame.data_spaced} · ${age.toFixed(1)}s`;
}

function semanticByKey(summary, key) {
  return (summary.semantic || []).find((item) => item.key === key);
}

function switchTab(tab) {
  const next = ["live", "learn", "can", "uart", "cluster", "export"].includes(tab) ? tab : "live";
  state.activeTab = next;
  document.querySelectorAll("[data-view-tab]").forEach((button) => {
    const active = button.dataset.viewTab === next;
    button.classList.toggle("active", active);
    button.setAttribute("aria-selected", active ? "true" : "false");
  });
  document.querySelectorAll("[data-tab-panel]").forEach((panel) => {
    panel.classList.toggle("active", panel.dataset.tabPanel === next);
  });
  try {
    localStorage.setItem(ACTIVE_TAB_KEY, next);
  } catch (_error) {
    // localStorage can be disabled; tabs still work in memory.
  }
}

function deviceLabel(session) {
  const mode = session?.mode || "idle";
  if (!session?.running) {
    if (mode === "sample") return "demo log";
    return "остановлено";
  }
  if (mode === "gsusb_uart") return "gs_usb + UART";
  if (mode === "gsusb") return "gs_usb CAN";
  if (mode === "cdc") return "CDC CAN";
  if (mode === "lab") return "CDC lab";
  if (mode === "sample") return "demo log";
  return mode;
}

const DEMO_STEPS = {
  walkaround: [
    { label: "Зажигание включено", patch: { ignition: true, lastLabel: "IGN ON" } },
    { label: "Дверь водителя открыта", patch: { doors: { lf: true }, lastLabel: "дверь LF" } },
    { label: "Дверь водителя закрыта", patch: { doors: { lf: false }, lastLabel: "дверь LF закрыта" } },
    { label: "Дверь пассажира открыта", patch: { doors: { rf: true }, lastLabel: "дверь RF" } },
    { label: "Задние двери открыты", patch: { doors: { lr: true, rr: true }, lastLabel: "задние двери" } },
    { label: "Все двери закрыты", patch: { doors: { rf: false, lr: false, rr: false }, lastLabel: "двери закрыты" } },
    { label: "Багажник открыт", patch: { trunk: true, lastLabel: "багажник" } },
    { label: "Капот открыт", patch: { hood: true, lastLabel: "капот" } },
    { label: "Люк открыт", patch: { sunroof: true, lastLabel: "люк открыт" } },
    { label: "Кузов закрыт", patch: { trunk: false, hood: false, sunroof: false, lastLabel: "кузов закрыт" } },
    { label: "Поворотники", patch: { hazard: true, lastLabel: "аварийка" } },
    { label: "Свет выключен", patch: { hazard: false, lowBeam: false, highBeam: false, lastLabel: "свет выкл" } },
  ],
  parking: [
    { label: "Зажигание и тормоз", patch: { ignition: true, reverse: false, speed: "0", lastLabel: "подготовка R" } },
    { label: "Задний ход включен", patch: { reverse: true, parking: "clear", lastLabel: "R" } },
    { label: "Задние датчики близко", patch: { parking: "rear near", lastLabel: "парктроники зад" } },
    { label: "Предупреждение слева", patch: { rcta: "left", lastLabel: "RCTA left" } },
    { label: "Предупреждение справа", patch: { rcta: "right", lastLabel: "RCTA right" } },
    { label: "Руль влево", patch: { steering: "лево", lastLabel: "руль лево" } },
    { label: "Руль вправо", patch: { steering: "право", lastLabel: "руль право" } },
    { label: "Парковка завершена", patch: { reverse: false, parking: "clear", rcta: "нет", steering: "центр", lastLabel: "P" } },
  ],
  climate: [
    { label: "Климат AUTO", patch: { climate: "AUTO", driverTemp: "22", passengerTemp: "22", fan: "3", lastLabel: "AUTO" } },
    { label: "A/C включен", patch: { ac: true, lastLabel: "A/C" } },
    { label: "Обдув лобового", patch: { frontDefog: true, lastLabel: "front defog" } },
    { label: "Обогрев заднего стекла", patch: { rearDefog: true, lastLabel: "rear defog" } },
    { label: "Подогрев руля", patch: { heatedWheel: true, lastLabel: "руль heat" } },
    { label: "Сиденья heat 3", patch: { driverSeatHeat: "3", passengerSeatHeat: "3", lastLabel: "seat heat" } },
    { label: "Сиденья vent 2", patch: { driverSeatVent: "2", passengerSeatVent: "2", lastLabel: "seat vent" } },
    { label: "Климат выключен", patch: { climate: "выкл", ac: false, frontDefog: false, rearDefog: false, heatedWheel: false, driverSeatHeat: "off", passengerSeatHeat: "off", driverSeatVent: "off", passengerSeatVent: "off", fan: "0", lastLabel: "climate off" } },
  ],
  media: [
    { label: "USB музыка", patch: { media: "USB: Track test", nav: "компас активен", lastLabel: "USB music" } },
    { label: "Bluetooth музыка", patch: { media: "BT: Artist - Track", lastLabel: "BT music" } },
    { label: "FM радио", patch: { media: "FM 101.70", lastLabel: "FM" } },
    { label: "Навигация маневр", patch: { nav: "через 120 м направо", lastLabel: "TBT" } },
    { label: "ETA / distance", patch: { nav: "12.4 км, 18 мин", lastLabel: "ETA" } },
    { label: "Медиа очищено", patch: { media: "нет данных", nav: "нет данных", lastLabel: "media off" } },
  ],
  stress: [
    { label: "Старт stress", patch: { ignition: true, climate: "AUTO", media: "USB", lastLabel: "stress start" } },
    { label: "Дверь + свет", patch: { doors: { lf: true }, lowBeam: true, turn: "left", lastLabel: "LF + left" } },
    { label: "R + датчики", patch: { doors: { lf: false }, reverse: true, parking: "rear near", turn: "off", lastLabel: "R sensors" } },
    { label: "Климат/сиденья", patch: { reverse: false, heatedWheel: true, driverSeatHeat: "2", fan: "5", lastLabel: "comfort" } },
    { label: "Люк/багажник", patch: { sunroof: true, trunk: true, lastLabel: "sunroof trunk" } },
    { label: "Возврат в норму", patch: makeDefaultCar("demo") },
  ],
};

function selectAction(action) {
  state.selectedAction = action;
  $("currentActionTitle").textContent = action.name;
  $("currentActionHint").textContent = action.hint || "Сделай действие один раз или 3 цикла, потом останови запись.";
  renderActions();
}

function renderActions() {
  const filter = state.actionFilter.trim().toLowerCase();
  const groups = new Map();
  for (const action of state.actions) {
    const haystack = `${action.group} ${action.name} ${action.hint}`.toLowerCase();
    if (filter && !haystack.includes(filter)) continue;
    if (!groups.has(action.group)) groups.set(action.group, []);
    groups.get(action.group).push(action);
  }
  const root = $("actionList");
  root.innerHTML = [...groups.entries()].map(([group, actions]) => `
    <section class="action-group">
      <h3>${escapeHtml(group)}</h3>
      ${actions.map((action) => `
        <button class="action-item ${state.selectedAction?.id === action.id ? "selected" : ""}" data-action-id="${escapeHtml(action.id)}">
          <strong>${escapeHtml(action.name)}</strong>
          <span>${escapeHtml(action.hint || "")}</span>
        </button>
      `).join("")}
    </section>
  `).join("");
  root.querySelectorAll("[data-action-id]").forEach((button) => {
    button.addEventListener("click", () => {
      const action = state.actions.find((item) => item.id === button.dataset.actionId);
      if (action) selectAction(action);
    });
  });
}

function semanticToCar(summary) {
  if (state.demo.running) return;
  const next = makeDefaultCar("live");
  const semantic = summary.semantic || [];
  const uart = summary.uart_events || [];
  for (const item of semantic) {
    const label = item.label || "";
    const value = item.value || "";
    const isOpen = value === "открыто";
    const isOn = value === "включено";
    switch (item.key) {
      case "door_lf": next.doors.lf = isOpen; break;
      case "door_rf": next.doors.rf = isOpen; break;
      case "door_lr": next.doors.lr = isOpen; break;
      case "door_rr": next.doors.rr = isOpen; break;
      case "trunk": next.trunk = isOpen; break;
      case "hood": next.hood = isOpen; break;
      case "sunroof": next.sunroof = isOpen; break;
      case "ignition": next.ignition = isOn; break;
      case "reverse_169":
        next.reverse = isOn;
        next.reverseOutput12v = isOn ? "+12V ожидается" : "выкл";
        break;
      case "speed_candidate": next.speed = value.replace(" candidate", "").replace(" км/ч", ""); break;
      case "rpm_316": next.rpm = value; break;
      case "engine_temp": next.engineTemp = value; break;
      case "outside_temp": next.outsideTemp = value; break;
      case "battery_voltage": next.batteryVoltage = value; break;
      case "heated_wheel": next.heatedWheel = isOn; break;
      case "front_defog_up_043":
      case "front_defog_up_132":
        next.frontDefog = isOn;
        break;
      case "parking_sensors":
        next.parking = value === "нет препятствий" ? "clear" : value;
        break;
      case "seat_heat_vent_134":
        next.driverSeatHeat = value;
        next.passengerSeatHeat = value;
        next.driverSeatVent = value;
        next.passengerSeatVent = value;
        break;
      case "driver_seat_heat_4e5":
        next.driverSeatHeat = value;
        break;
      case "parking_spas_390":
        next.spas = value;
        next.dynamicLines = value;
        break;
      case "parking_safety_58b":
        next.rcta = value;
        next.blindSpot = value;
        break;
      case "media_hu_114":
        next.mediaSource = value;
        break;
      case "media_usb_490":
      case "media_fm_4e8":
        next.mediaRaw = value;
        break;
      case "media_nav_197":
      case "media_nav_text_49b":
      case "media_nav_tbt_4bb":
        next.navRaw = value;
        break;
      default:
        if (label.includes("Левая передняя дверь")) next.doors.lf = isOpen;
        if (label.includes("Правая передняя дверь")) next.doors.rf = isOpen;
        if (label.includes("Левая задняя дверь")) next.doors.lr = isOpen;
        if (label.includes("Правая задняя дверь")) next.doors.rr = isOpen;
        if (label.includes("Багажник")) next.trunk = isOpen;
        if (label.includes("Капот")) next.hood = isOpen;
        if (label.includes("Люк")) next.sunroof = isOpen;
        if (label.includes("Зажигание")) next.ignition = isOn;
        if (label.includes("Задний ход")) next.reverse = isOn;
        if (label.includes("Скорость")) next.speed = value.replace(" candidate", "").replace(" км/ч", "");
        if (label.includes("Обороты двигателя")) next.rpm = value;
        if (label.includes("Температура двигателя")) next.engineTemp = value;
        if (label.includes("Наружная температура")) next.outsideTemp = value;
        if (label.includes("Напряжение АКБ")) next.batteryVoltage = value;
        if (label.includes("Подогрев руля")) next.heatedWheel = isOn;
        if (label.includes("Обдув вверх") || label.includes("лобовое")) next.frontDefog = isOn;
        if (label.includes("Парктроники")) next.parking = value === "нет препятствий" ? "clear" : value;
    }
  }

  next.raw = {
    body541: rawValueWithAge(summary, "0x541"),
    body553: rawValueWithAge(summary, "0x553"),
    body559: rawValueWithAge(summary, "0x559"),
    buttons523: rawValueWithAge(summary, "0x523"),
    speed316: rawValueWithAge(summary, "0x316"),
    wheel386: rawValueWithAge(summary, "0x386"),
    gear111: rawValueWithAge(summary, "0x111"),
    gear354: rawValueWithAge(summary, "0x354"),
    gear169: rawValueWithAge(summary, "0x169"),
    steering2b0: rawValueWithAge(summary, "0x2B0"),
    steering381: rawValueWithAge(summary, "0x381"),
    climate034: rawValueWithAge(summary, "0x034", 0),
    climate044: rawValueWithAge(summary, "0x044", 1),
    climate042: rawValueWithAge(summary, "0x042", 1),
    climate043: rawValueWithAge(summary, "0x043", 1),
    climate131: rawValueWithAge(summary, "0x131", 0),
    climate132: rawValueWithAge(summary, "0x132", 0),
    climate134: rawValueWithAge(summary, "0x134", 0),
    climate383: rawValueWithAge(summary, "0x383", 1),
    parking390: rawValueWithAge(summary, "0x390", 1),
    parking436: rawValueWithAge(summary, "0x436", 1),
    safety58b: rawValueWithAge(summary, "0x58B", 1),
    media114: rawValueWithAge(summary, "0x114", 0),
    media197: rawValueWithAge(summary, "0x197", 0),
    media490: rawValueWithAge(summary, "0x490", 0),
    media4e8: rawValueWithAge(summary, "0x4E8", 0),
    media49b: rawValueWithAge(summary, "0x49B", 0),
    media4bb: rawValueWithAge(summary, "0x4BB", 0),
  };

  const climateRaw = [
    next.raw.climate034,
    next.raw.climate044,
    next.raw.climate042,
    next.raw.climate043,
    next.raw.climate131,
    next.raw.climate132,
    next.raw.climate134,
    next.raw.climate383,
  ].filter(isRealValue);
  if (climateRaw.length) next.climate = "данные есть";
  if (isRealValue(next.mediaRaw)) next.media = next.mediaRaw;
  if (isRealValue(next.navRaw)) next.nav = next.navRaw;

  for (const item of uart.slice(0, 10)) {
    const text = item.text || "";
    if (text.includes("USB music")) next.media = text;
    if (text.includes("Bluetooth")) next.media = text;
    if (text.includes("радио")) next.media = text;
    if (text.includes("навигация")) next.nav = text;
    if (text.includes("задний ход on")) next.reverse = true;
    if (text.includes("задний ход off")) next.reverse = false;
  }
  const uartState = summary.uart_state || {};
  if (uartState.source && uartState.source !== "нет данных") {
    const details = [];
    if (uartState.radio && uartState.radio !== "-") details.push(uartState.radio);
    if (uartState.track && uartState.track !== "-") details.push(`трек ${uartState.track}`);
    if (uartState.play_time && uartState.play_time !== "-") details.push(uartState.play_time);
    next.media = [uartState.source, ...details].join(" · ");
  }
  const last = semantic[semantic.length - 1] || uart[0];
  if (last) next.lastLabel = last.label || last.text || "live";
  state.car = next;
}

function renderSummary(summary) {
  state.summary = summary;
  const session = summary.session || {};
  const modules = summary.module_stats || {};
  $("metricMcan").textContent = `${modules.mcan_frames ?? 0} · ${Number(modules.mcan_fps || 0).toFixed(1)}/s`;
  $("metricCcan").textContent = `${modules.ccan_frames ?? 0} · ${Number(modules.ccan_fps || 0).toFixed(1)}/s`;
  $("metricUartRx").textContent = modules.uart_rx ?? 0;
  $("metricUartTx").textContent = modules.uart_tx ?? 0;
  $("metricActiveMode").textContent = modules.active_mode || deviceLabel(session);

  const badge = $("connectionBadge");
  badge.textContent = `${session.mode || "idle"} ${session.running ? "running" : "stopped"}`;
  badge.className = `status-pill ${session.running ? "live" : ""}`;
  renderModeButtons(session);
  renderBridge(summary.bridge || {});

  semanticToCar(summary);
  renderLearnStatus(summary.learn || {});
  renderCarStatus();
  renderSemantic(summary.semantic || [], summary.semantic_events || []);
  renderUartDecoded(summary.uart_state || {}, summary.uart_command_counts || []);
  renderUart(summary.uart_events || []);
  renderRecent(summary.recent || []);
  const result = summary.learn?.result;
  if (result) renderCandidates(result);
}

function renderModeButtons(session) {
  const mode = String(session?.mode || "");
  const running = session?.running === true;
  document.querySelectorAll("[data-mode]").forEach((button) => {
    const target = button.dataset.mode;
    const active =
      (target === "canlog" && running && ["gsusb", "gsusb_uart", "lab"].includes(mode)) ||
      (target === "normal" && mode === "normal") ||
      (target === "update" && mode === "update");
    button.classList.toggle("active-mode", active);
  });
}

function renderLearnStatus(learn) {
  const active = learn.active;
  if (learn.result) state.lastLearnResult = learn.result;
  const changes = Number(learn.detected_changes || 0);
  const target = Number(learn.target_repeats || 5);
  const text = active
    ? `Запись: ${learn.action_name || learn.action_id}. Чистых изменений: ${Math.min(changes, target)}/${target}. CAN ${learn.frames_count}, UART ${learn.uart_count}${learn.capture_closed ? " · 5/5 набрано, можно жать анализ" : ""}`
    : learn.result
      ? `Последний анализ: ${learn.result.action_name}. Чистых изменений ${learn.result.detected_changes || 0}/${learn.result.target_repeats || 5}, скрыто шума ${learn.result.noise_hidden || 0}`
      : "Нет активной записи";
  $("learnStatus").textContent = text;
  $("learnStatus").className = `learn-status ${active ? "recording" : ""} ${learn.capture_closed ? "ready" : ""}`;
}

function renderBridge(bridge) {
  const root = $("bridgeState");
  if (!root) return;
  const enabled = bridge.enabled === true;
  const sent = Number(bridge.sent || 0);
  root.textContent = enabled ? `CAN→UART вкл · TX ${sent}` : "CAN→UART выкл";
  root.className = `bridge-state ${enabled ? "on" : ""}`;
  const button = $("toggleBridge");
  if (button) {
    button.textContent = enabled ? "CAN→UART вкл" : "CAN→UART выкл";
    button.classList.toggle("active", enabled);
  }
}

function stateCards() {
  const car = state.car;
  return [
    ["LF дверь", openText(car.doors.lf), car.doors.lf],
    ["RF дверь", openText(car.doors.rf), car.doors.rf],
    ["LR дверь", openText(car.doors.lr), car.doors.lr],
    ["RR дверь", openText(car.doors.rr), car.doors.rr],
    ["Багажник", openText(car.trunk), car.trunk],
    ["Капот", openText(car.hood), car.hood],
    ["Люк", openText(car.sunroof), car.sunroof],
    ["Задний ход", statusText(car.reverse), car.reverse],
    ["Зажигание", statusText(car.ignition), car.ignition],
    ["Скорость", `${car.speed} км/ч`, Number(car.speed) > 0],
    ["Обороты", car.rpm, car.rpm !== "-"],
    ["Темп. двигателя", car.engineTemp, car.engineTemp !== "-"],
    ["Темп. улицы", car.outsideTemp, car.outsideTemp !== "-"],
    ["АКБ", car.batteryVoltage, car.batteryVoltage !== "-"],
    ["Свет", car.lowBeam ? "ближний" : car.highBeam ? "дальний" : "выкл", car.lowBeam || car.highBeam],
    ["Руль", car.steering, car.steering !== "центр"],
    ["Климат", car.climate, car.climate !== "выкл"],
    ["Обдув вверх", statusText(car.frontDefog), car.frontDefog],
    ["Подогрев руля", statusText(car.heatedWheel), car.heatedWheel],
    ["Парктроники", car.parking, car.parking !== "clear"],
    ["Медиа", car.media, car.media !== "нет данных"],
    ["Навигация", car.nav, car.nav !== "нет данных"],
    ["Источник", car.source, car.source === "demo"],
  ];
}

function renderCarStatus() {
  $("carSubtitle").textContent = `${state.car.source === "demo" ? "Демо" : "Live"} · ${state.car.lastLabel}`;
  renderLiveCategories();
  renderLearnedLive();
}

function liveGroups() {
  const car = state.car;
  return [
    {
      title: "Движение",
      hint: "скорость, тахометр, зажигание, коробка",
      className: "wide",
      items: [
        ["Скорость", `${car.speed} км/ч`, Number(car.speed) > 0],
        ["Обороты / тахометр", car.rpm, car.rpm !== "-"],
        ["Зажигание", statusText(car.ignition, "вкл", "выкл"), car.ignition],
        ["Задний ход", statusText(car.reverse), car.reverse],
        ["Выход R +12V", car.reverseOutput12v, car.reverseOutput12v !== "-"],
        ["Руль", car.steering, car.steering !== "центр"],
        ["Скорость raw 0x316", car.raw.speed316, isRealValue(car.raw.speed316)],
        ["Колеса raw 0x386", car.raw.wheel386, isRealValue(car.raw.wheel386)],
        ["Передача raw 0x111", car.raw.gear111, isRealValue(car.raw.gear111)],
        ["Передача raw 0x354/169", `${car.raw.gear354} | ${car.raw.gear169}`, isRealValue(car.raw.gear354) || isRealValue(car.raw.gear169)],
        ["Угол руля raw", `${car.raw.steering2b0} | ${car.raw.steering381}`, isRealValue(car.raw.steering2b0) || isRealValue(car.raw.steering381)],
      ],
    },
    {
      title: "Кузов",
      hint: "двери, капот, багажник, люк, замки",
      items: [
        ["LF дверь", openText(car.doors.lf), car.doors.lf],
        ["RF дверь", openText(car.doors.rf), car.doors.rf],
        ["LR дверь", openText(car.doors.lr), car.doors.lr],
        ["RR дверь", openText(car.doors.rr), car.doors.rr],
        ["Багажник", openText(car.trunk), car.trunk],
        ["Капот", openText(car.hood), car.hood],
        ["Люк", openText(car.sunroof), car.sunroof],
        ["Замки", car.locked ? "закрыто" : "-", car.locked],
        ["Ремень водителя", car.seatbeltDriver, isRealValue(car.seatbeltDriver)],
        ["Ремень пассажира", car.seatbeltPassenger, isRealValue(car.seatbeltPassenger)],
        ["Кузов raw 0x541", car.raw.body541, isRealValue(car.raw.body541)],
        ["Задние двери raw 0x553", car.raw.body553, isRealValue(car.raw.body553)],
        ["Окна/руль raw 0x559", car.raw.body559, isRealValue(car.raw.body559)],
      ],
    },
    {
      title: "Климат подробно",
      hint: "температуры, режимы, обдув, подогревы, raw ID",
      className: "wide climate-category",
      items: [
        ["Климат", car.climate, car.climate !== "выкл"],
        ["Темп. водитель", car.driverTemp, car.driverTemp !== "-"],
        ["Темп. пассажир", car.passengerTemp, car.passengerTemp !== "-"],
        ["Темп. двигателя", car.engineTemp, car.engineTemp !== "-"],
        ["Темп. улицы", car.outsideTemp, car.outsideTemp !== "-"],
        ["АКБ", car.batteryVoltage, car.batteryVoltage !== "-"],
        ["Вентилятор", car.fan, car.fan !== "-"],
        ["Режим обдува", car.climateMode, car.climateMode !== "-"],
        ["AUTO", car.autoClimate, car.autoClimate !== "-"],
        ["DUAL/SYNC", car.dual, car.dual !== "-"],
        ["Рециркуляция", car.intake, car.intake !== "-"],
        ["A/C", statusText(car.ac), car.ac],
        ["Обдув вверх", statusText(car.frontDefog), car.frontDefog],
        ["Задний обогрев", statusText(car.rearDefog), car.rearDefog],
        ["Подогрев руля", statusText(car.heatedWheel), car.heatedWheel],
        ["Водитель сиденье heat", car.driverSeatHeat, car.driverSeatHeat !== "off"],
        ["Пассажир сиденье heat", car.passengerSeatHeat, car.passengerSeatHeat !== "off"],
        ["Водитель сиденье vent", car.driverSeatVent, car.driverSeatVent !== "off"],
        ["Пассажир сиденье vent", car.passengerSeatVent, car.passengerSeatVent !== "off"],
        ["HU климат 0x034", car.raw.climate034, isRealValue(car.raw.climate034)],
        ["Улица DATC11 0x044", car.raw.climate044, isRealValue(car.raw.climate044)],
        ["Темп. C-CAN 0x042", car.raw.climate042, isRealValue(car.raw.climate042)],
        ["Климат C-CAN 0x043", car.raw.climate043, isRealValue(car.raw.climate043)],
        ["Темп. M-CAN 0x131", car.raw.climate131, isRealValue(car.raw.climate131)],
        ["Климат M-CAN 0x132", car.raw.climate132, isRealValue(car.raw.climate132)],
        ["Сиденья 0x134", car.raw.climate134, isRealValue(car.raw.climate134)],
        ["FATC 0x383", car.raw.climate383, isRealValue(car.raw.climate383)],
      ],
    },
    {
      title: "Свет и безопасность",
      hint: "свет, повороты, парковка, ассистенты",
      items: [
        ["AUTO свет", car.autoLight, isRealValue(car.autoLight)],
        ["Свет", car.lowBeam ? "ближний" : car.highBeam ? "дальний" : "выкл", car.lowBeam || car.highBeam],
        ["Противотуманки", car.fog || car.frontFog || car.rearFog ? "вкл" : "выкл", car.fog || car.frontFog || car.rearFog],
        ["Дворники", car.wiper, isRealValue(car.wiper)],
        ["Задний дворник", car.rearWiper, isRealValue(car.rearWiper)],
        ["Поворот", car.turn, car.turn !== "off"],
        ["Аварийка", statusText(car.hazard), car.hazard],
        ["Парктроники", car.parking, car.parking !== "clear"],
        ["SPAS/линии", car.spas, isRealValue(car.spas)],
        ["Динамические линии", car.dynamicLines, isRealValue(car.dynamicLines)],
        ["RCTA", car.rcta, car.rcta !== "нет"],
        ["Слепые зоны", car.blindSpot, isRealValue(car.blindSpot)],
        ["TPMS", car.tpms, isRealValue(car.tpms)],
        ["SPAS raw 0x390", car.raw.parking390, isRealValue(car.raw.parking390)],
        ["Парктроники raw 0x436", car.raw.parking436, isRealValue(car.raw.parking436)],
        ["LCA/RCTA raw 0x58B", car.raw.safety58b, isRealValue(car.raw.safety58b)],
      ],
    },
    {
      title: "Медиа и навигация",
      hint: "HU, источники, приборка, TBT",
      items: [
        ["Медиа", car.media, car.media !== "нет данных"],
        ["Навигация", car.nav, car.nav !== "нет данных"],
        ["Источник HU", car.mediaSource, car.mediaSource !== "-"],
        ["HU source 0x114", car.raw.media114, isRealValue(car.raw.media114)],
        ["HU nav 0x197", car.raw.media197, isRealValue(car.raw.media197)],
        ["USB текст 0x490", car.raw.media490, isRealValue(car.raw.media490)],
        ["FM/AM текст 0x4E8", car.raw.media4e8, isRealValue(car.raw.media4e8)],
        ["Нави текст 0x49B", car.raw.media49b, isRealValue(car.raw.media49b)],
        ["Нави TBT 0x4BB", car.raw.media4bb, isRealValue(car.raw.media4bb)],
        ["Кнопки руля raw 0x523", car.raw.buttons523, isRealValue(car.raw.buttons523)],
        ["Источник данных", car.source, car.source === "demo"],
      ],
    },
  ];
}

function renderLiveCategories() {
  const root = $("liveCategoryGrid");
  if (!root) return;
  root.innerHTML = liveGroups().map((group) => `
    <section class="live-category ${escapeHtml(group.className || "")}">
      <header>
        <strong>${escapeHtml(group.title)}</strong>
        <span>${escapeHtml(group.hint)}</span>
      </header>
      <div class="live-category-cards">
        ${group.items.map(([label, value, on]) => `
          <div class="status-card ${on ? "on" : ""}">
            <span>${escapeHtml(label)}</span>
            <strong>${escapeHtml(value)}</strong>
          </div>
        `).join("")}
      </div>
    </section>
  `).join("");
}

function candidateText(candidate) {
  if (!candidate) return "кандидат не выбран";
  const bytes = (candidate.changed_bytes || []).length ? `байты ${candidate.changed_bytes.join(",")}` : "байты без явного изменения";
  return `${candidate.bus || "-"} ${candidate.id_hex || "-"} DLC ${candidate.dlc ?? "-"} · ${bytes}`;
}

function learnedKey(item, index) {
  const candidate = item.candidate || {};
  return `${item.saved_ms || index}:${item.action_id || "manual"}:${candidate.key || candidate.id_hex || index}`;
}

function candidateChannel(candidate) {
  const keyParts = String(candidate?.key || "").split(":");
  if (keyParts.length >= 2 && /^\d+$/.test(keyParts[1])) return Number(keyParts[1]);
  return String(candidate?.bus || "").includes("M-CAN") ? 0 : 1;
}

function candidatePayload(candidate, desiredState) {
  if (!candidate) return "";
  const value = desiredState === "on" ? candidate.last || candidate.first : candidate.first || candidate.last;
  return String(value || "").replace(/[^0-9A-Fa-f]/g, "").toUpperCase();
}

function canSendable(candidate) {
  return Boolean(candidate?.id_hex && candidatePayload(candidate, "on"));
}

function semanticSourceFor(labelNeedle) {
  const needle = String(labelNeedle || "").toLowerCase();
  const semantic = state.summary?.semantic || [];
  const matches = semantic.filter((item) => String(item.label || "").toLowerCase().includes(needle));
  if (!matches.length) return "подтвержденный ID пока не пришел в live";
  return [...new Set(matches.map((item) => item.source).filter(Boolean))].join(" + ");
}

function renderLearnedLive() {
  const root = $("learnedLive");
  if (!root) return;
  const rows = [{
    kind: "state",
    title: "Обдув вверх/лобовое",
    text: `${statusText(state.car.frontDefog, "включено", "выключено")} · ${semanticSourceFor("Обдув вверх")}`,
    isOn: state.car.frontDefog,
  }];
  const verifiedAssignments = state.learnedAssignments
    .map((item, index) => ({ item, index }))
    .filter(({ item }) => item.verified === true || item.manual === true || item.locked === true)
    .slice(0, 4);
  for (const { item, index } of verifiedAssignments) {
    const key = learnedKey(item, index);
    const isOn = state.learnedStates[key] === "on";
    rows.push({
      kind: "control",
      index,
      key,
      isOn,
      sendable: canSendable(item.candidate),
      title: item.action_name || item.action_id || "обученное действие",
      text: candidateText(item.candidate),
    });
  }
  root.innerHTML = rows.slice(0, 6).map((item) => `
    <div class="status-card learned-cell ${item.isOn ? "on" : ""}">
      <span>${escapeHtml(item.title)}</span>
      <strong>${escapeHtml(item.text)}</strong>
      ${item.kind === "control" ? `
        <div class="learned-actions">
          <button data-learned-index="${item.index}" data-learned-state="on" ${item.sendable ? "" : "disabled"}>ON</button>
          <button data-learned-index="${item.index}" data-learned-state="off" ${item.sendable ? "" : "disabled"}>OFF</button>
          <small>${item.isOn ? "состояние: включено" : "состояние: выключено"}</small>
        </div>
      ` : ""}
    </div>
  `).join("");
  root.querySelectorAll("[data-learned-index]").forEach((button) => {
    button.addEventListener("click", () => sendLearnedCommand(Number(button.dataset.learnedIndex), button.dataset.learnedState));
  });
}

async function sendLearnedCommand(index, desiredState) {
  const item = state.learnedAssignments[index];
  const candidate = item?.candidate;
  if (!candidate || !canSendable(candidate)) {
    logAction("обученная команда: нечего отправлять");
    return;
  }
  const data = candidatePayload(candidate, desiredState);
  const payload = {
    channel: candidateChannel(candidate),
    id: candidate.id_hex,
    data,
    count: 8,
    interval: 0.04,
    extended: false,
    confirm: true,
  };
  const result = await post("/api/can/send", payload);
  const key = learnedKey(item, index);
  state.learnedStates[key] = desiredState;
  state.car.lastLabel = `${item.action_name || item.action_id}: ${desiredState}`;
  state.demo.events.unshift({ ms: Date.now(), label: `learned TX ${item.action_name || item.action_id}`, value: desiredState, scenario: "learned" });
  state.demo.events = state.demo.events.slice(0, 80);
  renderLearnedLive();
  renderCarStatus();
  renderSemantic(state.summary?.semantic || [], state.summary?.semantic_events || []);
  logAction(`обученная TX: ${item.action_name || item.action_id} ${desiredState}, ${result.frames} кадров`);
}

function renderSemantic(items, events) {
  const cardRoot = $("semanticCards");
  const eventRoot = $("semanticEvents");
  if (!items.length && !state.demo.events.length) {
    cardRoot.innerHTML = `<div class="empty">Жду live-данные или демо-сценарий.</div>`;
  } else {
    cardRoot.innerHTML = stateCards().slice(0, 12).map(([label, value, on]) => `
      <div class="state-row">
        <span>${escapeHtml(label)}</span>
        <strong>${escapeHtml(value)}${on ? "" : ""}</strong>
      </div>
    `).join("");
  }

  const demoEvents = state.demo.events.map((event) => ({
    ms: event.ms,
    label: event.label,
    value: "demo",
    source: event.scenario,
  }));
  const rows = [...demoEvents, ...(events || [])].sort((a, b) => (b.ms || 0) - (a.ms || 0));
  $("eventCount").textContent = `${rows.length} событий`;
  if (!rows.length) {
    eventRoot.innerHTML = `<div class="event-row muted">Пока нет распознанных изменений.</div>`;
    return;
  }
  eventRoot.innerHTML = rows.slice(0, 80).map((event) => `
    <div class="event-row">
      <span>${fmtTime(event.ms)}</span>
      <strong>${escapeHtml(event.label)}</strong>
      <span>${escapeHtml(event.value || event.source || "")}</span>
    </div>
  `).join("");
}

function renderUart(events) {
  const root = $("uartEvents");
  if (!events.length) {
    root.innerHTML = `<div class="event-row muted">UART Raise пока молчит.</div>`;
    return;
  }
  root.innerHTML = events.slice(0, 30).map((event) => `
    <div class="uart-row ${event.valid ? "" : "invalid"}">
      <span>${fmtTime(event.ms)}</span>
      <strong>${escapeHtml(event.cmd || "-")}</strong>
      <span>${escapeHtml(event.text || "")}</span>
      <code>${escapeHtml(event.raw || "")}</code>
    </div>
  `).join("");
}

function renderUartDecoded(uartState, commandCounts) {
  const root = $("uartDecoded");
  if (!root) return;
  const rows = [
    ["Источник", uartState.source || "нет данных"],
    ["Код источника", uartState.source_code || "-"],
    ["Трек", uartState.track || "-"],
    ["Время трека", uartState.play_time || "-"],
    ["Радио", uartState.radio || "-"],
    ["Навигация", uartState.nav || "-"],
    ["Bluetooth", uartState.bt || "-"],
    ["HU время", uartState.hu_time || "-"],
    ["Питание/session", uartState.power || "-"],
    ["Последний валидный", uartState.last_valid || "-"],
  ];
  const counts = (commandCounts || [])
    .map((item) => `<span><b>${escapeHtml(item.cmd)}</b> ${Number(item.count || 0)}</span>`)
    .join("");
  root.innerHTML = `
    <div class="uart-decoded-cards">
      ${rows.map(([label, value]) => `
        <div class="uart-state-cell">
          <span>${escapeHtml(label)}</span>
          <strong>${escapeHtml(String(value || "-"))}</strong>
        </div>
      `).join("")}
    </div>
    <div class="uart-command-counts">
      <span>Команды RX:</span>
      ${counts || "<em>нет данных</em>"}
      <span>valid ${Number(uartState.valid_count || 0)} / bad ${Number(uartState.invalid_count || 0)}</span>
    </div>
  `;
}

function resultLooksLikeFrontDefog(result) {
  const text = `${result.action_id || ""} ${result.action_name || ""}`.toLowerCase();
  return text.includes("front_defog") || text.includes("обдув") || text.includes("лобов");
}

function profileIds(result) {
  const ids = result?.profile?.ids || [];
  return new Set(ids.map((item) => String(item).toLowerCase()));
}

function candidateMatchesAction(result, candidate) {
  if (!candidate) return false;
  const ids = profileIds(result);
  if (ids.size) return ids.has(String(candidate.id_hex || "").toLowerCase()) || candidate.profile_match === true;
  if (!resultLooksLikeFrontDefog(result)) return true;
  const id = String(candidate.id_hex || "").toLowerCase();
  const semanticText = (candidate.semantic || [])
    .map((item) => `${item.label || ""} ${item.value || ""} ${item.source || ""}`)
    .join(" ")
    .toLowerCase();
  return id === "0x043" || id === "0x132" || semanticText.includes("обдув") || semanticText.includes("лобов");
}

function visibleCandidates(result) {
  return (result.candidates || []).filter((candidate) => candidateMatchesAction(result, candidate));
}

function renderCandidates(result) {
  const root = $("candidateList");
  const candidates = visibleCandidates(result);
  const hidden = Math.max(Number(result.noise_hidden || 0), (result.candidates || []).length - candidates.length);
  const transitions = result.transitions || [];
  if (!candidates.length) {
    root.innerHTML = `
      <div class="empty">Явных изменений по выбранному профилю нет. Повтори действие отдельно, ровно 5 раз.</div>
      ${hidden > 0 ? `<div class="candidate-noise-note">Фоновый шум скрыт: ${hidden} кандидатов.</div>` : ""}
    `;
    return;
  }
  root.innerHTML = `
    <div class="learn-result-clean">
      <strong>Чистых изменений: ${Number(result.detected_changes || 0)}/${Number(result.target_repeats || 5)}</strong>
      <span>${(result.profile?.ids || []).join(", ") || "профиль общий"}</span>
      ${transitions.length ? `<div class="transition-list">${transitions.map((event) => `
        <code>${escapeHtml(event.label)}: ${escapeHtml(event.value)} · ${escapeHtml(event.source)}</code>
      `).join("")}</div>` : ""}
    </div>
    ${hidden > 0 ? `<div class="candidate-noise-note">Фоновый шум скрыт: ${hidden} кандидатов не относятся к выбранному действию.</div>` : ""}
    ${candidates.map((item, index) => `
    <article class="candidate">
      <header>
        <strong>${index + 1}. ${escapeHtml(item.bus)} ${escapeHtml(item.id_hex)} DLC ${item.dlc}</strong>
        <span>score ${item.score} · ${item.count} кадров</span>
      </header>
      <div class="candidate-main">
        <div><b>Байты:</b> ${item.changed_bytes.length ? item.changed_bytes.join(", ") : "нет явного изменения"}</div>
        <div><b>Первый:</b> <code>${escapeHtml(item.first || "-")}</code></div>
        <div><b>Последний:</b> <code>${escapeHtml(item.last || "-")}</code></div>
      </div>
      <div class="samples">
        ${(item.samples || []).map((sample) => `<code>${escapeHtml(sample)}</code>`).join("")}
      </div>
      ${(item.semantic || []).length ? `
        <div class="semantic-mini">
          ${item.semantic.map((s) => `<span>${escapeHtml(s.label)}: ${escapeHtml(s.value)}</span>`).join("")}
        </div>
      ` : ""}
      <button class="save-candidate" data-index="${index}">Закрепить как ${escapeHtml(result.action_name)}</button>
    </article>
  `).join("")}`;
  root.querySelectorAll(".save-candidate").forEach((button) => {
    button.addEventListener("click", async () => {
      const candidate = candidates[Number(button.dataset.index)];
      await post("/api/learn/save", {
        action_id: result.action_id,
        action_name: result.action_name,
        candidate,
      });
      if (state.commands?.test_plan) {
        const item = state.commands.test_plan.find((planItem) => planItem.id === result.action_id);
        if (item) {
          item.status = "captured";
          item.candidate = candidate;
          renderPlan();
        }
      }
      state.learnedAssignments.unshift({
        saved_ms: Date.now(),
        action_id: result.action_id,
        action_name: result.action_name,
        candidate,
        verified: true,
      });
      state.learnedAssignments = state.learnedAssignments.slice(0, 80);
      renderLearnedLive();
      logAction(`закреплено: ${result.action_name} -> ${candidate.bus} ${candidate.id_hex}`);
    });
  });
}

function commandRows() {
  if (!state.commands) return [];
  const rows = [];
  for (const item of state.commands.safe_uart_tests || []) {
    rows.push({ group: "TX UART", name: item.name, payload: item.frame, meaning: item.warning || "canbox -> TEYES", status: "sendable" });
  }
  for (const item of state.commands.hu_to_canbox_examples || []) {
    rows.push({ group: "RX UART", name: item.name, payload: item.frame, meaning: "TEYES -> canbox пример", status: "decode" });
  }
  for (const row of state.commands.raise_matrix || []) {
    rows.push({ group: row.direction, name: `${row.cmd_hex} ${row.name}`, payload: row.payload, meaning: row.meaning, status: row.status });
  }
  for (const row of state.commands.can_matrix || []) {
    rows.push({
      group: row.category,
      name: `${row.function} ${row.dbc_id_hex || row.observed_id_hex || ""}`,
      payload: row.dbc_signals || row.observed_signal_or_byte || "-",
      meaning: `${row.direction || ""} ${row.dbc_bus || ""}`.trim(),
      status: row.status || row.implementation_type,
    });
  }
  return rows;
}

function renderCommands() {
  if (!state.commands) return;
  const safeRoot = $("safeCommands");
  safeRoot.innerHTML = (state.commands.safe_uart_tests || []).map((item) => `
    <button class="command-button" data-lab="${escapeHtml(item.lab)}" data-command-id="${escapeHtml(item.id)}" title="${escapeHtml(item.frame)}">
      <strong>${escapeHtml(item.name)}</strong>
      <code>${escapeHtml(item.frame)}</code>
    </button>
  `).join("");
  safeRoot.querySelectorAll("[data-lab]").forEach((button) => {
    button.addEventListener("click", async () => {
      try {
        const result = await post("/api/lab/send", { command: button.dataset.lab });
        previewQuickCommand(button.dataset.commandId);
        logAction(`UART TX ${result.uart_hex || button.dataset.lab}`);
      } catch (error) {
        logAction(`UART TX ошибка: ${error.message}`);
      }
    });
  });

  const filter = state.commandFilter.trim().toLowerCase();
  const rows = commandRows().filter((row) => !filter || Object.values(row).join(" ").toLowerCase().includes(filter));
  $("commandTable").innerHTML = rows.slice(0, 260).map((row) => `
    <div class="command-row">
      <span>${escapeHtml(row.group || "-")}</span>
      <strong>${escapeHtml(row.name || "-")}</strong>
      <code>${escapeHtml(row.payload || "-")}</code>
      <small>${escapeHtml(row.meaning || "")}</small>
      <em>${escapeHtml(row.status || "")}</em>
    </div>
  `).join("");
  renderPlan();
}

function renderPlan() {
  const root = $("planList");
  if (!root || !state.commands) return;
  const plan = state.commands.test_plan || [];
  const done = plan.filter((item) => item.status === "captured").length;
  $("planProgress").textContent = `${done} / ${plan.length}`;
  if (!plan.length) {
    root.innerHTML = `<div class="empty">План пока не загружен.</div>`;
    return;
  }
  root.innerHTML = plan.slice(0, 90).map((item) => `
    <div class="plan-row ${item.status === "captured" ? "done" : ""}">
      <i class="plan-dot"></i>
      <div>
        <strong>${escapeHtml(item.name)}</strong>
        <span>${escapeHtml(item.status === "captured" ? "закреплено" : item.hint || "ожидает записи")}</span>
      </div>
    </div>
  `).join("");
}

function downloadJson(path, filename) {
  const link = document.createElement("a");
  link.href = path;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
}

function previewQuickCommand(id) {
  const patchMap = {
    body_closed: { doors: { lf: false, rf: false, lr: false, rr: false }, trunk: false, hood: false, sunroof: false, lastLabel: "UART body closed" },
    door_driver: { doors: { lf: true }, lastLabel: "UART LF open" },
    door_passenger: { doors: { rf: true }, lastLabel: "UART RF open" },
    door_rear_left: { doors: { lr: true }, lastLabel: "UART LR open" },
    door_rear_right: { doors: { rr: true }, lastLabel: "UART RR open" },
    trunk: { trunk: true, lastLabel: "UART trunk" },
    hood: { hood: true, lastLabel: "UART hood" },
    sunroof: { sunroof: true, lastLabel: "UART sunroof candidate" },
    all_doors: { doors: { lf: true, rf: true, lr: true, rr: true }, lastLabel: "UART all doors" },
    reverse_on: { reverse: true, lastLabel: "UART reverse on" },
    reverse_off: { reverse: false, lastLabel: "UART reverse off" },
    radar_clear: { parking: "clear", lastLabel: "UART radar clear" },
    radar_demo: { parking: "demo near", lastLabel: "UART radar demo" },
    climate_popup: { climate: "popup", driverTemp: "22", passengerTemp: "22", fan: "3", lastLabel: "UART climate" },
    climate_off: { climate: "выкл", fan: "0", lastLabel: "UART climate off" },
  };
  if (!patchMap[id]) return;
  state.car = deepMerge({ ...state.car, doors: { ...state.car.doors }, source: "manual TX" }, patchMap[id]);
  state.demo.events.unshift({ ms: Date.now(), label: state.car.lastLabel, scenario: "manual TX" });
  state.demo.events = state.demo.events.slice(0, 80);
  renderCarStatus();
}

function renderRecent(rows) {
  const root = $("recentFrames");
  if (!rows.length) {
    root.innerHTML = `<div class="empty">сырых кадров пока нет</div>`;
    return;
  }
  root.innerHTML = rows.slice(0, 100).map((frame) => `
    <div class="frame-row">
      <span>ch${frame.ch}</span>
      <strong>${escapeHtml(frame.id_hex)}</strong>
      <span>DLC ${frame.dlc}</span>
      <code>${escapeHtml(frame.data_spaced || "-")}</code>
    </div>
  `).join("");
}

function renderScenarioButtons() {
  const scenarios = state.commands?.demo_scenarios || [
    { id: "walkaround", name: "Обход кузова" },
    { id: "parking", name: "Парковка" },
    { id: "climate", name: "Климат" },
    { id: "media", name: "Медиа" },
    { id: "stress", name: "Стресс" },
  ];
  $("scenarioButtons").innerHTML = scenarios.map((item) => `
    <button data-scenario="${escapeHtml(item.id)}" title="${escapeHtml(item.description || "")}">${escapeHtml(item.name)}</button>
  `).join("");
  $("scenarioButtons").querySelectorAll("[data-scenario]").forEach((button) => {
    button.addEventListener("click", () => startDemo(button.dataset.scenario));
  });
}

function startDemo(scenario = "walkaround") {
  stopDemo();
  state.demo.running = true;
  state.demo.scenario = scenario;
  state.demo.index = 0;
  state.demo.events = [];
  state.car = makeDefaultCar("demo");
  const steps = DEMO_STEPS[scenario] || DEMO_STEPS.walkaround;
  const applyStep = () => {
    const step = steps[state.demo.index % steps.length];
    state.car = deepMerge({ ...state.car, doors: { ...state.car.doors }, source: "demo" }, step.patch);
    state.car.source = "demo";
    state.demo.events.unshift({ ms: Date.now(), label: step.label, scenario });
    state.demo.events = state.demo.events.slice(0, 80);
    state.demo.index += 1;
    renderCarStatus();
    renderSemantic(state.summary?.semantic || [], state.summary?.semantic_events || []);
    logAction(`demo ${scenario}: ${step.label}`);
  };
  applyStep();
  state.demo.timer = window.setInterval(applyStep, 950);
}

function stopDemo() {
  if (state.demo.timer) window.clearInterval(state.demo.timer);
  state.demo.timer = null;
  state.demo.running = false;
}

function drawRoundRect(ctx, x, y, w, h, r) {
  const radius = Math.min(r, Math.abs(w) / 2, Math.abs(h) / 2);
  ctx.beginPath();
  ctx.moveTo(x + radius, y);
  ctx.arcTo(x + w, y, x + w, y + h, radius);
  ctx.arcTo(x + w, y + h, x, y + h, radius);
  ctx.arcTo(x, y + h, x, y, radius);
  ctx.arcTo(x, y, x + w, y, radius);
  ctx.closePath();
}

function drawCar() {
  const canvas = document.getElementById("carCanvas");
  if (!canvas) return;
  const rect = canvas.getBoundingClientRect();
  const dpr = window.devicePixelRatio || 1;
  const width = Math.max(320, Math.floor(rect.width));
  const height = Math.max(260, Math.floor(rect.height));
  if (canvas.width !== Math.floor(width * dpr) || canvas.height !== Math.floor(height * dpr)) {
    canvas.width = Math.floor(width * dpr);
    canvas.height = Math.floor(height * dpr);
  }
  const ctx = canvas.getContext("2d");
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, width, height);
  const car = state.car;
  const t = Date.now() / 1000;
  const blink = Math.sin(t * 8) > 0;

  const cx = width / 2;
  const cy = height / 2 + 8;
  const bw = Math.min(Math.max(width * 0.28, 178), 250);
  const bh = Math.min(Math.max(height * 0.72, 300), 390);
  const x = cx - bw / 2;
  const y = cy - bh / 2;
  const front = y;
  const rear = y + bh;
  const left = x;
  const right = x + bw;

  const accent = "#8be7bb";
  const warn = "#f2bd5a";
  const danger = "#ff716d";
  const glass = "#173044";
  const body = "#22383d";
  const body2 = "#17272b";

  const drawTextBadge = (text, bx, by, tone = "green") => {
    ctx.save();
    ctx.font = "600 12px Inter, system-ui, sans-serif";
    const paddingX = 8;
    const textWidth = ctx.measureText(text).width;
    const w = textWidth + paddingX * 2;
    const h = 24;
    ctx.fillStyle = tone === "red" ? "rgba(255, 113, 109, 0.16)" : tone === "amber" ? "rgba(242, 189, 90, 0.16)" : "rgba(84, 209, 143, 0.15)";
    ctx.strokeStyle = tone === "red" ? "rgba(255, 113, 109, 0.62)" : tone === "amber" ? "rgba(242, 189, 90, 0.62)" : "rgba(135, 230, 183, 0.62)";
    drawRoundRect(ctx, bx, by, w, h, 7);
    ctx.fill();
    ctx.stroke();
    ctx.fillStyle = tone === "red" ? "#ffd8d7" : tone === "amber" ? "#ffe2a7" : "#dfffee";
    ctx.fillText(text, bx + paddingX, by + 16);
    ctx.restore();
  };

  ctx.save();

  ctx.strokeStyle = "rgba(145, 162, 159, 0.07)";
  ctx.lineWidth = 1;
  for (let gx = (width % 56) / 2; gx < width; gx += 56) {
    ctx.beginPath();
    ctx.moveTo(gx, 0);
    ctx.lineTo(gx, height);
    ctx.stroke();
  }
  for (let gy = (height % 56) / 2; gy < height; gy += 56) {
    ctx.beginPath();
    ctx.moveTo(0, gy);
    ctx.lineTo(width, gy);
    ctx.stroke();
  }

  ctx.fillStyle = "rgba(255, 255, 255, 0.025)";
  drawRoundRect(ctx, cx - bw * 1.05, y - 14, bw * 2.1, bh + 28, 34);
  ctx.fill();

  if (car.reverse) {
    ctx.fillStyle = "rgba(255, 113, 109, 0.12)";
    ctx.beginPath();
    ctx.moveTo(left + 22, rear - 6);
    ctx.lineTo(right - 22, rear - 6);
    ctx.lineTo(right + bw * 0.9, Math.min(height + 50, rear + bh * 0.52));
    ctx.lineTo(left - bw * 0.9, Math.min(height + 50, rear + bh * 0.52));
    ctx.closePath();
    ctx.fill();
    ctx.strokeStyle = "rgba(255, 113, 109, 0.38)";
    ctx.setLineDash([10, 10]);
    for (let i = 1; i <= 3; i += 1) {
      ctx.beginPath();
      ctx.moveTo(left + 22 - i * 26, rear + i * 34);
      ctx.lineTo(right - 22 + i * 26, rear + i * 34);
      ctx.stroke();
    }
    ctx.setLineDash([]);
    drawTextBadge("R", right + 12, rear + 4, "red");
  }

  if (car.parking !== "clear") {
    ctx.strokeStyle = "rgba(242, 189, 90, 0.72)";
    ctx.lineWidth = 2.5;
    for (let i = 0; i < 4; i += 1) {
      ctx.beginPath();
      ctx.arc(cx, rear + 8, 42 + i * 26, Math.PI * 0.18, Math.PI * 0.82);
      ctx.stroke();
    }
    drawTextBadge("PARK", left - 60, rear + 12, "amber");
  }

  if (car.rcta === "left" || car.rcta === "right") {
    const isLeft = car.rcta === "left";
    ctx.fillStyle = "rgba(255, 113, 109, 0.17)";
    ctx.beginPath();
    ctx.moveTo(isLeft ? left - 8 : right + 8, rear - 56);
    ctx.lineTo(isLeft ? left - bw * 1.25 : right + bw * 1.25, rear + 34);
    ctx.lineTo(isLeft ? left - bw * 1.25 : right + bw * 1.25, rear + 108);
    ctx.lineTo(isLeft ? left - 8 : right + 8, rear + 12);
    ctx.closePath();
    ctx.fill();
    drawTextBadge(isLeft ? "RCTA L" : "RCTA R", isLeft ? left - 102 : right + 24, rear + 34, "red");
  }

  ctx.shadowColor = "rgba(0, 0, 0, 0.42)";
  ctx.shadowBlur = 28;
  ctx.fillStyle = "rgba(0, 0, 0, 0.32)";
  drawRoundRect(ctx, left - 24, front + 18, bw + 48, bh - 2, 30);
  ctx.fill();
  ctx.shadowBlur = 0;

  const bodyGrad = ctx.createLinearGradient(0, front, 0, rear);
  bodyGrad.addColorStop(0, "#2d4a50");
  bodyGrad.addColorStop(0.5, body);
  bodyGrad.addColorStop(1, body2);
  ctx.fillStyle = bodyGrad;
  drawRoundRect(ctx, left, front, bw, bh, 34);
  ctx.fill();
  ctx.strokeStyle = "rgba(139, 231, 187, 0.26)";
  ctx.lineWidth = 2;
  ctx.stroke();

  ctx.fillStyle = "rgba(255, 255, 255, 0.04)";
  drawRoundRect(ctx, left + 18, front + 20, bw - 36, bh - 40, 26);
  ctx.fill();

  ctx.strokeStyle = "rgba(145, 162, 159, 0.25)";
  ctx.lineWidth = 1;
  for (const py of [front + bh * 0.28, front + bh * 0.51, front + bh * 0.73]) {
    ctx.beginPath();
    ctx.moveTo(left + 18, py);
    ctx.lineTo(right - 18, py);
    ctx.stroke();
  }

  ctx.fillStyle = "#081014";
  drawRoundRect(ctx, left + 44, front + 70, bw - 88, 78, 17);
  ctx.fill();
  drawRoundRect(ctx, left + 44, rear - 152, bw - 88, 82, 17);
  ctx.fill();

  ctx.fillStyle = "rgba(84, 145, 184, 0.28)";
  drawRoundRect(ctx, left + 54, front + 82, bw - 108, 54, 12);
  ctx.fill();
  drawRoundRect(ctx, left + 54, rear - 140, bw - 108, 56, 12);
  ctx.fill();

  ctx.fillStyle = "rgba(84, 145, 184, 0.18)";
  drawRoundRect(ctx, left + 30, front + bh * 0.34, 20, bh * 0.25, 9);
  ctx.fill();
  drawRoundRect(ctx, right - 50, front + bh * 0.34, 20, bh * 0.25, 9);
  ctx.fill();

  if (car.sunroof) {
    ctx.fillStyle = "rgba(84, 209, 143, 0.55)";
    drawRoundRect(ctx, left + bw * 0.34, front + bh * 0.43, bw * 0.32, bh * 0.13, 10);
    ctx.fill();
    ctx.strokeStyle = accent;
    ctx.lineWidth = 2;
    ctx.stroke();
    drawTextBadge("люк", right + 14, front + bh * 0.46, "green");
  } else {
    ctx.strokeStyle = "rgba(145, 162, 159, 0.35)";
    ctx.lineWidth = 1.5;
    drawRoundRect(ctx, left + bw * 0.34, front + bh * 0.43, bw * 0.32, bh * 0.13, 10);
    ctx.stroke();
  }

  const drawPanel = (side, isFront, open, label) => {
    const panelY = isFront ? front + bh * 0.28 : front + bh * 0.54;
    const panelH = bh * 0.19;
    const closedX = side === "left" ? left + 6 : right - 18;
    ctx.strokeStyle = open ? accent : "rgba(145, 162, 159, 0.3)";
    ctx.lineWidth = open ? 2.5 : 1.25;
    ctx.fillStyle = open ? "rgba(84, 209, 143, 0.18)" : "rgba(255, 255, 255, 0.018)";
    drawRoundRect(ctx, closedX, panelY, 12, panelH, 5);
    ctx.fill();
    ctx.stroke();

    if (!open) return;

    ctx.save();
    const hingeX = side === "left" ? left + 8 : right - 8;
    const hingeY = panelY + panelH / 2;
    ctx.translate(hingeX, hingeY);
    ctx.rotate(side === "left" ? -0.55 : 0.55);
    ctx.fillStyle = "rgba(84, 209, 143, 0.22)";
    ctx.strokeStyle = accent;
    ctx.lineWidth = 3;
    const doorW = 74;
    const doorH = panelH;
    drawRoundRect(ctx, side === "left" ? -doorW : 0, -doorH / 2, doorW, doorH, 8);
    ctx.fill();
    ctx.stroke();
    ctx.restore();
    drawTextBadge(label, side === "left" ? left - 96 : right + 18, panelY + panelH * 0.28, "green");
  };
  drawPanel("left", true, car.doors.lf, "LF");
  drawPanel("right", true, car.doors.rf, "RF");
  drawPanel("left", false, car.doors.lr, "LR");
  drawPanel("right", false, car.doors.rr, "RR");

  if (car.hood) {
    ctx.fillStyle = "rgba(242, 189, 90, 0.2)";
    ctx.strokeStyle = warn;
    ctx.lineWidth = 3;
    drawRoundRect(ctx, left + 26, front - 20, bw - 52, 68, 14);
    ctx.fill();
    ctx.stroke();
    drawTextBadge("капот", right + 12, front + 4, "amber");
  }
  if (car.trunk) {
    ctx.fillStyle = "rgba(242, 189, 90, 0.2)";
    ctx.strokeStyle = warn;
    ctx.lineWidth = 3;
    drawRoundRect(ctx, left + 26, rear - 48, bw - 52, 70, 14);
    ctx.fill();
    ctx.stroke();
    drawTextBadge("багажник", right + 12, rear - 40, "amber");
  }

  if (car.lowBeam || car.highBeam) {
    ctx.fillStyle = car.highBeam ? "rgba(114, 182, 255, 0.18)" : "rgba(242, 189, 90, 0.13)";
    ctx.beginPath();
    ctx.moveTo(left + 28, front + 12);
    ctx.lineTo(left - 96, front - 64);
    ctx.lineTo(cx, front - 86);
    ctx.lineTo(right + 96, front - 64);
    ctx.lineTo(right - 28, front + 12);
    ctx.closePath();
    ctx.fill();
  }

  ctx.fillStyle = car.lowBeam || car.highBeam ? "rgba(242, 189, 90, 0.95)" : "rgba(242, 189, 90, 0.35)";
  drawRoundRect(ctx, left + 34, front + 8, 44, 10, 4);
  ctx.fill();
  drawRoundRect(ctx, right - 78, front + 8, 44, 10, 4);
  ctx.fill();

  ctx.fillStyle = "rgba(255, 113, 109, 0.65)";
  drawRoundRect(ctx, left + 36, rear - 18, 40, 9, 4);
  ctx.fill();
  drawRoundRect(ctx, right - 76, rear - 18, 40, 9, 4);
  ctx.fill();

  const leftBlink = car.hazard || car.turn === "left";
  const rightBlink = car.hazard || car.turn === "right";
  ctx.fillStyle = leftBlink && blink ? warn : "rgba(242, 189, 90, 0.2)";
  drawRoundRect(ctx, left + 9, front + 28, 15, 17, 4);
  ctx.fill();
  drawRoundRect(ctx, left + 9, rear - 48, 15, 17, 4);
  ctx.fill();
  ctx.fillStyle = rightBlink && blink ? warn : "rgba(242, 189, 90, 0.2)";
  drawRoundRect(ctx, right - 24, front + 28, 15, 17, 4);
  ctx.fill();
  drawRoundRect(ctx, right - 24, rear - 48, 15, 17, 4);
  ctx.fill();

  const steeringTilt = car.steering === "лево" ? -0.23 : car.steering === "право" ? 0.23 : 0;
  const drawWheel = (wx, wy, frontWheel) => {
    ctx.save();
    ctx.translate(wx, wy);
    if (frontWheel) ctx.rotate(steeringTilt);
    ctx.fillStyle = "#030607";
    drawRoundRect(ctx, -13, -39, 26, 78, 9);
    ctx.fill();
    ctx.strokeStyle = "rgba(255, 255, 255, 0.1)";
    ctx.lineWidth = 1;
    ctx.stroke();
    ctx.restore();
  };
  drawWheel(left - 10, front + 84, true);
  drawWheel(right + 10, front + 84, true);
  drawWheel(left - 10, rear - 102, false);
  drawWheel(right + 10, rear - 102, false);

  ctx.fillStyle = "rgba(145, 162, 159, 0.62)";
  drawRoundRect(ctx, left + 48, front + 28, 44, 8, 3);
  ctx.fill();
  drawRoundRect(ctx, right - 92, front + 28, 44, 8, 3);
  ctx.fill();
  drawRoundRect(ctx, left + 48, rear - 36, 36, 8, 3);
  ctx.fill();
  drawRoundRect(ctx, right - 84, rear - 36, 36, 8, 3);
  ctx.fill();

  if (car.heatedWheel) drawTextBadge("руль heat", left - 102, front + bh * 0.46, "green");

  ctx.restore();
}

function drawLoop() {
  drawCar();
  window.requestAnimationFrame(drawLoop);
}

async function refreshStatus() {
  const data = await api("/api/status");
  if (data.auto_port && !$("portInput").value) $("portInput").value = data.auto_port;
  renderSummary(data.summary);
  if (!data.summary?.session?.running) {
    $("metricActiveMode").textContent = data.gsusb_present === true ? "gs_usb ready" : data.auto_port || "no USB";
  }
}

async function loadCommands() {
  state.commands = await api("/api/commands");
  state.actions = state.commands.actions || [];
  if (!state.selectedAction && state.actions.length) selectAction(state.actions[0]);
  renderActions();
  renderCommands();
}

async function loadLearned() {
  try {
    state.learnedAssignments = (await api("/api/learned")).slice().reverse();
    renderLearnedLive();
  } catch (error) {
    logAction(`learned load: ${error.message}`);
  }
}

function setupEvents() {
  const source = new EventSource("/api/events");
  source.onmessage = (event) => {
    const payload = JSON.parse(event.data);
    if (payload.summary) renderSummary(payload.summary);
    if (payload.marker) logAction(payload.marker.name);
  };
  source.onerror = () => {
    logAction("SSE reconnect...");
  };
}

function setupActions() {
  document.querySelectorAll("[data-view-tab]").forEach((button) => {
    button.addEventListener("click", () => switchTab(button.dataset.viewTab));
  });
  try {
    switchTab(localStorage.getItem(ACTIVE_TAB_KEY) || state.activeTab);
  } catch (_error) {
    switchTab(state.activeTab);
  }

  $("refreshStatus").addEventListener("click", () => refreshStatus().catch((error) => logAction(error.message)));
  $("startGsusb").addEventListener("click", async () => {
    stopDemo();
    const result = await post("/api/log/start", { mode: "gsusb", bitrate0: 100000, bitrate1: 500000 });
    logAction(`Live CAN запущен; TX ${result.tx_control || "off"}`);
  });
  $("startLab").addEventListener("click", async () => {
    stopDemo();
    const result = await post("/api/log/start", { mode: "lab", port: $("portInput").value });
    logAction(`Live CAN+UART запущен; TX ${result.tx_control || "off"}`);
  });
  $("stopLog").addEventListener("click", async () => {
    stopDemo();
    await post("/api/log/stop");
    logAction("лог остановлен");
  });
  $("resetLive").addEventListener("click", async () => {
    const data = await post("/api/reset");
    state.lastLearnResult = null;
    renderSummary(data.summary);
    $("candidateList").innerHTML = `<div class="empty">Экран очищен. Live-сессия не остановлена.</div>`;
    logAction("сброс live-экрана");
  });
  $("toggleBridge").addEventListener("click", async () => {
    const data = await post("/api/bridge", {});
    renderBridge(data.bridge || {});
    logAction(`CAN→UART bridge ${data.bridge?.enabled ? "включен" : "выключен"}`);
  });
  $("startLearn").addEventListener("click", async () => {
    const action = state.selectedAction || { id: "manual", name: "Ручной тест" };
    await post("/api/learn/start", { action_id: action.id, name: action.name });
    logAction(`старт обучения: ${action.name}`);
  });
  $("stopLearn").addEventListener("click", async () => {
    const data = await post("/api/learn/stop");
    state.lastLearnResult = data.result;
    renderCandidates(data.result);
    renderCarStatus();
    logAction(`анализ: ${data.result.action_name}, кандидатов ${data.result.candidates.length}`);
  });
  $("actionSearch").addEventListener("input", (event) => {
    state.actionFilter = event.target.value;
    renderActions();
  });
  $("commandSearch").addEventListener("input", (event) => {
    state.commandFilter = event.target.value;
    renderCommands();
  });
  $("exportBundle").addEventListener("click", () => {
    downloadJson("/api/export", `2can35_lab_export_${Date.now()}.json`);
    logAction("экспорт всего набора");
  });
  $("exportLearned").addEventListener("click", () => {
    downloadJson("/api/learned", `2can35_learned_${Date.now()}.json`);
    logAction("экспорт обученных назначений");
  });
  document.querySelectorAll("[data-export-path]").forEach((button) => {
    button.addEventListener("click", () => {
      const name = button.dataset.exportName || "export";
      downloadJson(button.dataset.exportPath, `2can35_${name}_${Date.now()}.json`);
      logAction(`экспорт: ${name}`);
    });
  });
  $("sendLabCommand").addEventListener("click", async () => {
    const command = $("labCommand").value.trim();
    const result = await post("/api/lab/send", { command });
    logAction(`mode3 UART TX: ${result.uart_hex || command}`);
  });
  document.querySelectorAll("[data-mode]").forEach((button) => {
    button.addEventListener("click", async () => {
      const mode = button.dataset.mode;
      const result = await post("/api/mode", { mode, port: $("portInput").value });
      logAction(`mode ${mode}: ${result.ok ? "ok" : "fail"} ${String(result.stdout || result.stderr || "").trim()}`);
      if (mode === "canlog") state.car.lastLabel = "Mode 3 lab";
      if (mode === "normal") state.car.lastLabel = "Mode 1 canbox";
      renderCarStatus();
    });
  });
  $("sendDisplay").addEventListener("click", async () => {
    const payload = {
      transport: "can",
      bus: $("displayBus").value,
      scenario: $("displayScenario").value,
      seconds: Number($("displaySeconds").value),
      fm: $("displayFm").value,
      media: $("displayMedia").value,
      track: $("displayTrack").value,
      meters: Number($("displayMeters").value),
      eta: Number($("displayEta").value),
    };
    const result = await post("/api/send/display", payload);
    state.car = deepMerge({ ...state.car, doors: { ...state.car.doors }, source: "manual TX" }, {
      media: payload.media,
      nav: payload.scenario.includes("nav") || payload.scenario === "full" ? `${payload.meters} м, ETA ${payload.eta / 10} км` : state.car.nav,
      lastLabel: `display ${payload.scenario}`,
    });
    state.demo.events.unshift({ ms: Date.now(), label: `display TX ${payload.scenario}: ${result.sent} frames`, scenario: "manual TX" });
    state.demo.events = state.demo.events.slice(0, 80);
    renderCarStatus();
    renderSemantic(state.summary?.semantic || [], state.summary?.semantic_events || []);
    logAction(`display CAN TX: ${payload.scenario}, ${payload.bus}, ${result.sent} кадров`);
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
    logAction(`CAN TX: ${result.frames} кадров`);
  });
  $("sendCanSweep").addEventListener("click", async () => {
    const payload = {
      channel: Number($("txChannel").value),
      id: $("txCanId").value,
      data: $("txData").value,
      count: Number($("txCount").value),
      interval: Number($("txInterval").value),
      extended: $("txExtended").checked,
      confirm: $("txConfirm").checked,
      byte_index: Number($("sweepByte").value),
      start: $("sweepStart").value,
      end: $("sweepEnd").value,
      count_each: Number($("sweepCountEach").value),
    };
    const result = await post("/api/can/sweep", payload);
    logAction(`CAN sweep: ${result.frames} кадров, queued ${result.queued}`);
  });
}

async function main() {
  setupActions();
  setupEvents();
  await Promise.all([loadCommands(), loadLearned(), refreshStatus()]);
  renderCarStatus();
}

main().catch((error) => logAction(error.message));
