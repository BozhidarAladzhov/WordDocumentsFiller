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
  if (name === "talonstatus") prefix = "pill--talon-";
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

function autoResizeTextarea(textarea) {
  if (!textarea) return;
  textarea.style.height = "auto";
  textarea.style.height = textarea.scrollHeight + "px";
}

function initAutoResizeTextareas() {
  var textareas = document.querySelectorAll("textarea.notes-field");
  for (var i = 0; i < textareas.length; i += 1) {
    var textarea = textareas[i];
    if (textarea.dataset.autoresizeInit === "1") {
      autoResizeTextarea(textarea);
      continue;
    }

    textarea.dataset.autoresizeInit = "1";
    autoResizeTextarea(textarea);
    textarea.addEventListener("input", function (event) {
      autoResizeTextarea(event.target);
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

var autoSaveMap = new WeakMap();
var autoSaveForms = [];

function ensureAutoSaveState(form) {
  var state = autoSaveMap.get(form);
  if (!state) {
    state = { timerId: null, inFlight: false, queued: false, dirty: false };
    autoSaveMap.set(form, state);
  }
  return state;
}

function buildUrlEncodedData(controls) {
  var params = new URLSearchParams();
  for (var i = 0; i < controls.length; i += 1) {
    var c = controls[i];
    if (c.disabled || !c.name) continue;

    if (c.type === "checkbox") {
      if (c.checked) params.append(c.name, c.value || "on");
      continue;
    }
    if (c.type === "radio") {
      if (c.checked) params.set(c.name, c.value || "");
      continue;
    }
    params.set(c.name, c.value == null ? "" : c.value);
  }
  return params;
}

function triggerAutoSave(form, controls) {
  var state = ensureAutoSaveState(form);
  if (state.inFlight) {
    state.queued = true;
    return;
  }

  var formData = buildUrlEncodedData(controls);
  state.inFlight = true;

  fetch(form.action, {
    method: "POST",
    body: formData,
    keepalive: true,
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
      "X-Requested-With": "XMLHttpRequest"
    }
  }).then(function (response) {
    if (!response || !response.ok) {
      state.dirty = true;
      throw new Error("Auto-save failed");
    }
    state.dirty = false;
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
  var state = ensureAutoSaveState(form);
  if (state.timerId) clearTimeout(state.timerId);
  state.timerId = setTimeout(function () {
    state.timerId = null;
    triggerAutoSave(form, controls);
  }, delayMs);
}

function flushPendingAutoSaves() {
  for (var i = 0; i < autoSaveForms.length; i += 1) {
    var form = autoSaveForms[i];
    if (!form) continue;

    var state = autoSaveMap.get(form);
    if (!state) continue;

    if (state.timerId) {
      clearTimeout(state.timerId);
      state.timerId = null;
    }

    if (!state.dirty) continue;

    var controls = getControlsForForm(form);
    var params = buildUrlEncodedData(controls);

    try {
      if (navigator.sendBeacon) {
        var blob = new Blob([params.toString()], {
          type: "application/x-www-form-urlencoded;charset=UTF-8"
        });
        navigator.sendBeacon(form.action, blob);
        state.dirty = false;
        continue;
      }
    } catch (e) {
      // Fall through to keepalive fetch.
    }

    try {
      fetch(form.action, {
        method: "POST",
        body: params,
        keepalive: true,
        credentials: "same-origin",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
          "X-Requested-With": "XMLHttpRequest"
        }
      });
      state.dirty = false;
    } catch (e2) {
      // Ignore unload-time failures.
    }
  }
}

function bindAutoSaveForForm(form) {
  if (!form || form.dataset.autosaveBound === "1") return;
  form.dataset.autosaveBound = "1";
  autoSaveForms.push(form);

  var controls = getControlsForForm(form);
  for (var i = 0; i < controls.length; i += 1) {
    var c = controls[i];
    var tag = (c.tagName || "").toUpperCase();
    var type = (c.type || "").toLowerCase();

    c.addEventListener("change", function () {
      ensureAutoSaveState(form).dirty = true;
      if (type === "date" || type === "checkbox" || tag === "SELECT") {
        triggerAutoSave(form, controls);
        return;
      }
      scheduleAutoSave(form, controls, 120);
    });

    if (tag === "INPUT" && type === "date") {
      c.addEventListener("input", function () {
        ensureAutoSaveState(form).dirty = true;
        scheduleAutoSave(form, controls, 120);
      });
    }

    if (tag === "INPUT" && type !== "date" && type !== "checkbox" && type !== "radio") {
      c.addEventListener("input", function () {
        ensureAutoSaveState(form).dirty = true;
        scheduleAutoSave(form, controls, 700);
      });
      c.addEventListener("blur", function () {
        ensureAutoSaveState(form).dirty = true;
        scheduleAutoSave(form, controls, 120);
      });
    }

    if (tag === "TEXTAREA") {
      c.addEventListener("input", function () {
        ensureAutoSaveState(form).dirty = true;
        scheduleAutoSave(form, controls, 700);
      });
      c.addEventListener("blur", function () {
        ensureAutoSaveState(form).dirty = true;
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

function initFleetVehicleAutoSave() {
  var fleetVehicleForm = document.getElementById("fleetVehicleDetailsForm");
  if (fleetVehicleForm) bindAutoSaveForForm(fleetVehicleForm);
}

function initCanadaVehicleAutoSave() {
  var canadaVehicleForm = document.getElementById("canadaVehicleDetailsForm");
  if (canadaVehicleForm) bindAutoSaveForForm(canadaVehicleForm);
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

function initCarTransportDragAndDrop() {
  var cards = document.querySelectorAll(".car-transport-vehicle-card[draggable='true']");
  var dropzones = document.querySelectorAll(".car-transport-dropzone");
  var form = document.getElementById("carTransportAssignForm");

  if (!cards.length || !dropzones.length || !form) return;

  for (var i = 0; i < cards.length; i += 1) {
    cards[i].addEventListener("dragstart", function (event) {
      event.dataTransfer.setData("text/plain", this.getAttribute("data-vehicle-id") || "");
      event.dataTransfer.effectAllowed = "move";
      this.classList.add("is-dragging");
    });

    cards[i].addEventListener("dragend", function () {
      this.classList.remove("is-dragging");
    });
  }

  function submitMove(vehicleId, haulerId) {
    if (!vehicleId) return;
    form.action = haulerId
      ? "/car-transport/vehicles/" + vehicleId + "/assign/" + haulerId
      : "/car-transport/vehicles/" + vehicleId + "/unassign";
    form.submit();
  }

  for (var j = 0; j < dropzones.length; j += 1) {
    dropzones[j].addEventListener("dragover", function (event) {
      event.preventDefault();
      this.classList.add("is-over");
    });

    dropzones[j].addEventListener("dragleave", function () {
      this.classList.remove("is-over");
    });

    dropzones[j].addEventListener("drop", function (event) {
      event.preventDefault();
      this.classList.remove("is-over");
      submitMove(
        event.dataTransfer.getData("text/plain"),
        this.getAttribute("data-hauler-id") || ""
      );
    });
  }
}

function normalizeClipboardText(text) {
  return String(text == null ? "" : text).replace(/\r\n/g, "\n");
}

function textFromHtml(html) {
  var box = document.createElement("div");
  box.innerHTML = String(html == null ? "" : html);
  return normalizeClipboardText(box.innerText || box.textContent || "");
}

async function copyToClipboard(options) {
  var opts = options || {};
  var html = opts.html == null ? "" : String(opts.html);
  var text = normalizeClipboardText(opts.text);
  var fallbackTextarea = opts.fallbackTextarea || null;
  var richHtmlElement = opts.richHtmlElement || null;

  if (!text && html) {
    text = textFromHtml(html);
  }

  if (!text && !html) return false;

  try {
    if (html && navigator.clipboard && window.ClipboardItem && typeof navigator.clipboard.write === "function") {
      await navigator.clipboard.write([
        new ClipboardItem({
          "text/html": new Blob([html], { type: "text/html" }),
          "text/plain": new Blob([text], { type: "text/plain" })
        })
      ]);
      return true;
    }
  } catch (e) {
    // Continue with plain-text fallback.
  }

  try {
    if (navigator.clipboard && typeof navigator.clipboard.writeText === "function") {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch (e2) {
    // Continue with legacy fallback.
  }

  if (html) {
    var richSource = richHtmlElement;
    var cleanupRichSource = null;

    if (!richSource) {
      richSource = document.createElement("div");
      richSource.setAttribute("contenteditable", "true");
      richSource.style.position = "fixed";
      richSource.style.left = "-9999px";
      richSource.style.top = "-9999px";
      richSource.style.whiteSpace = "pre-wrap";
      richSource.innerHTML = html;
      document.body.appendChild(richSource);
      cleanupRichSource = function () {
        if (richSource && richSource.parentNode) richSource.parentNode.removeChild(richSource);
      };
    }

    try {
      var selection = window.getSelection();
      var range = document.createRange();
      range.selectNodeContents(richSource);
      selection.removeAllRanges();
      selection.addRange(range);
      var richCopied = document.execCommand("copy");
      selection.removeAllRanges();
      if (cleanupRichSource) cleanupRichSource();
      if (richCopied) return true;
    } catch (e3) {
      if (cleanupRichSource) cleanupRichSource();
    }
  }

  if (fallbackTextarea) {
    fallbackTextarea.value = text;
    var originalPosition = fallbackTextarea.style.position;
    var originalLeft = fallbackTextarea.style.left;
    var originalTop = fallbackTextarea.style.top;
    var originalWidth = fallbackTextarea.style.width;

    fallbackTextarea.style.position = "fixed";
    fallbackTextarea.style.left = "-9999px";
    fallbackTextarea.style.top = "-9999px";
    fallbackTextarea.style.width = "1px";
    fallbackTextarea.focus();
    fallbackTextarea.select();

    var copied = false;
    try {
      copied = document.execCommand("copy");
    } catch (e4) {
      copied = false;
    }

    fallbackTextarea.style.position = originalPosition;
    fallbackTextarea.style.left = originalLeft;
    fallbackTextarea.style.top = originalTop;
    fallbackTextarea.style.width = originalWidth;
    return copied;
  }

  return false;
}

window.copyToClipboard = copyToClipboard;

document.addEventListener("DOMContentLoaded", function () {
  initAllDateInputs();
  initStatusPills();
  initAutoResizeTextareas();
  initContainerTrackerAutoSave();
  initFleetVehicleAutoSave();
  initCanadaVehicleAutoSave();
  initOpenDetailsOnDoubleClick();
  initCarTransportDragAndDrop();
});

window.addEventListener("beforeunload", flushPendingAutoSaves);
window.addEventListener("pagehide", flushPendingAutoSaves);

document.addEventListener("pointerdown", function (event) {
  var input = event.target.closest('input[type="date"]');
  if (!input || input.disabled || input.readOnly) return;
  if (typeof input.showPicker === "function") {
    input.showPicker();
  } else {
    input.focus();
  }
});
