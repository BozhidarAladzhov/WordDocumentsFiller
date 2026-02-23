function formatDateEu(isoDate) {
  if (!isoDate || typeof isoDate !== "string" || isoDate.length < 10) return "";
  var parts = isoDate.split("-");
  if (parts.length !== 3) return "";
  return parts[2] + "." + parts[1] + "." + parts[0];
}

function initDateInput(input) {
  if (input.dataset.dateInit === "1") return;
  input.dataset.dateInit = "1";

  var wrapper = document.createElement("div");
  wrapper.className = "date-wrap";
  input.parentNode.insertBefore(wrapper, input);
  wrapper.appendChild(input);

  var display = document.createElement("span");
  display.className = "date-display";
  display.setAttribute("aria-hidden", "true");
  wrapper.appendChild(display);

  function sync() {
    var value = input.value || "";
    var formatted = formatDateEu(value);
    display.textContent = formatted || "dd.mm.yyyy";
    display.classList.toggle("is-empty", !formatted);
  }

  input.addEventListener("change", sync);
  input.addEventListener("input", sync);
  input.addEventListener("blur", sync);
  sync();
}

function initAllDateInputs() {
  var inputs = document.querySelectorAll('input[type="date"]');
  for (var i = 0; i < inputs.length; i += 1) {
    initDateInput(inputs[i]);
  }
}

function normalizeStatusToken(value) {
  return (value || "").toLowerCase().replace(/_/g, "-");
}

function updatePillClass(select) {
  if (!select || !select.classList || !select.classList.contains("pill")) return;
  var name = (select.name || "").toLowerCase();
  var prefix = "";
  if (name === "status") prefix = "pill--status-";
  if (name === "paid") prefix = "pill--paid-";
  if (name === "titles") prefix = "pill--titles-";
  if (!prefix) return;

  var toRemove = [];
  for (var i = 0; i < select.classList.length; i += 1) {
    var cls = select.classList[i];
    if (cls.indexOf(prefix) === 0) toRemove.push(cls);
  }
  for (var j = 0; j < toRemove.length; j += 1) {
    select.classList.remove(toRemove[j]);
  }
  select.classList.add(prefix + normalizeStatusToken(select.value));
}

function initStatusPills() {
  var selects = document.querySelectorAll("select.pill");
  for (var i = 0; i < selects.length; i += 1) {
    updatePillClass(selects[i]);
    selects[i].addEventListener("change", function (event) {
      updatePillClass(event.target);
    });
  }
}

function cssEscapeValue(value) {
  if (window.CSS && typeof window.CSS.escape === "function") {
    return window.CSS.escape(value);
  }
  return value.replace(/"/g, '\\"');
}

function getControlsForForm(form) {
  var controls = [];
  var seen = new Set();

  function addControl(control) {
    if (!control || seen.has(control)) return;
    seen.add(control);
    controls.push(control);
  }

  var inside = form.querySelectorAll("input[name], select[name], textarea[name]");
  for (var i = 0; i < inside.length; i += 1) addControl(inside[i]);

  if (form.id) {
    var outside = document.querySelectorAll('[form="' + cssEscapeValue(form.id) + '"]');
    for (var j = 0; j < outside.length; j += 1) {
      if (outside[j].name) addControl(outside[j]);
    }
  }
  return controls;
}

function buildFormData(form, controls) {
  var formData = new FormData();
  for (var i = 0; i < controls.length; i += 1) {
    var c = controls[i];
    if (c.disabled || !c.name) continue;

    if (c.type === "checkbox") {
      if (c.checked) formData.append(c.name, c.value || "on");
      continue;
    }
    if (c.type === "radio") {
      if (c.checked) formData.set(c.name, c.value || "");
      continue;
    }
    formData.set(c.name, c.value == null ? "" : c.value);
  }
  return formData;
}

var autoSaveMap = new WeakMap();

function triggerAutoSave(form, controls) {
  var state = autoSaveMap.get(form);
  if (!state) {
    state = { timerId: null, inFlight: false, queued: false };
    autoSaveMap.set(form, state);
  }
  if (state.inFlight) {
    state.queued = true;
    return;
  }

  var formData = buildFormData(form, controls);
  state.inFlight = true;

  fetch(form.action, {
    method: "POST",
    body: formData,
    credentials: "same-origin",
    headers: { "X-Requested-With": "XMLHttpRequest" }
  }).catch(function () {
    // Keep editing uninterrupted; user can still click Save manually.
  }).finally(function () {
    state.inFlight = false;
    if (state.queued) {
      state.queued = false;
      triggerAutoSave(form, controls);
    }
  });
}

function scheduleAutoSave(form, controls, delayMs) {
  var state = autoSaveMap.get(form);
  if (!state) {
    state = { timerId: null, inFlight: false, queued: false };
    autoSaveMap.set(form, state);
  }
  if (state.timerId) clearTimeout(state.timerId);
  state.timerId = setTimeout(function () {
    state.timerId = null;
    triggerAutoSave(form, controls);
  }, delayMs);
}

function bindAutoSaveForForm(form) {
  if (!form || form.dataset.autosaveBound === "1") return;
  form.dataset.autosaveBound = "1";

  var controls = getControlsForForm(form);
  for (var i = 0; i < controls.length; i += 1) {
    var c = controls[i];
    var tag = (c.tagName || "").toUpperCase();
    var type = (c.type || "").toLowerCase();

    c.addEventListener("change", function () {
      scheduleAutoSave(form, controls, 120);
    });

    if (tag === "INPUT" && type !== "date" && type !== "checkbox" && type !== "radio") {
      c.addEventListener("input", function () {
        scheduleAutoSave(form, controls, 700);
      });
      c.addEventListener("blur", function () {
        scheduleAutoSave(form, controls, 120);
      });
    }

    if (tag === "TEXTAREA") {
      c.addEventListener("input", function () {
        scheduleAutoSave(form, controls, 700);
      });
      c.addEventListener("blur", function () {
        scheduleAutoSave(form, controls, 120);
      });
    }
  }
}

function initContainerTrackerAutoSave() {
  var containerForm = document.getElementById("containerEditForm");
  if (containerForm) bindAutoSaveForForm(containerForm);

  var vehicleForms = document.querySelectorAll(".vehicle-meta__form");
  for (var i = 0; i < vehicleForms.length; i += 1) {
    bindAutoSaveForForm(vehicleForms[i]);
  }
}

function initOpenDetailsOnDoubleClick() {
  var rows = document.querySelectorAll(".js-open-details-row");
  for (var i = 0; i < rows.length; i += 1) {
    rows[i].addEventListener("dblclick", function (event) {
      if (event.button !== 0) return;
      if (event.target.closest("a,button,input,select,textarea,form,label")) return;

      var url = this.getAttribute("data-details-url");
      if (url) window.location.href = url;
    });
  }
}

document.addEventListener("DOMContentLoaded", function () {
  initAllDateInputs();
  initStatusPills();
  initContainerTrackerAutoSave();
  initOpenDetailsOnDoubleClick();
});

document.addEventListener("pointerdown", function (event) {
  var input = event.target.closest('input[type="date"]');
  if (!input || input.disabled || input.readOnly) return;
  if (typeof input.showPicker === "function") {
    input.showPicker();
  } else {
    input.focus();
  }
});
