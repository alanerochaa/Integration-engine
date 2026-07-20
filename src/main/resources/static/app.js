(function () {
  'use strict';
  var HEALTH_URL   = '/health';
  var POLL_MS      = 30_000;
  var FRONTEND_URL = 'http://localhost:5176';

  var statusDot  = document.getElementById('status-dot');
  var statusText = document.getElementById('status-text');
  var envBadge   = document.getElementById('env-badge');
  var heroCta = document.getElementById('hero-cta');

  if (heroCta) heroCta.href = FRONTEND_URL;
  async function checkHealth() {
    try {
      var controller = new AbortController();
      var timeoutId  = setTimeout(function () { controller.abort(); }, 8000);
      var res = await fetch(HEALTH_URL, {
        cache:  'no-store',
        signal: controller.signal,
      });
      clearTimeout(timeoutId);

      var data     = await res.json();
      var isUp     = data.status === 'UP';
      var ambiente = String(data.ambiente || 'HML').toUpperCase();

      setStatus(isUp ? 'up' : 'degraded', isUp ? 'Online' : 'Degradado', ambiente);
    } catch (_) {
      setStatus('offline', 'Offline', '—');
    }
  }
  function setStatus(state, label, ambiente) {
    var dotClass = 'status-dot';
    if (state !== 'up') dotClass += ' offline';

    if (statusDot)  statusDot.className    = dotClass;
    if (statusText) statusText.textContent = label;
    if (envBadge)   envBadge.textContent   = ambiente;
  }
  checkHealth();
  setInterval(checkHealth, POLL_MS);

})();
