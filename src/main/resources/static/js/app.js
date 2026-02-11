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

document.addEventListener("DOMContentLoaded", initAllDateInputs);

document.addEventListener("pointerdown", function (event) {
  var input = event.target.closest('input[type="date"]');
  if (!input || input.disabled || input.readOnly) return;
  if (typeof input.showPicker === "function") {
    input.showPicker();
  } else {
    input.focus();
  }
});
