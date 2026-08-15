package com.arc_e_tect.gradle.trackerlens.dashboard;

/**
 * The dashboard's fixed bootstrap {@code <script>} tag: builds the Chart.js charts from the
 * {@code #dashboard-data} JSON blob and drives the lens switcher.
 *
 * <p>This is this module's own asset, not lensable and not part of the {@code ContractRule}
 * contract in the CSS sense - a lens can style whatever this script renders into, but the script
 * itself is fixed, identical in every generated dashboard.</p>
 */
final class DashboardBootstrapScript {

    private DashboardBootstrapScript() {}

    static final String SOURCE = """
            <script>
            (function () {
              var dataElement = document.getElementById('dashboard-data');
              var data = dataElement ? JSON.parse(dataElement.textContent) : { trackers: [], lensFiles: {} };
              var rootStyle = getComputedStyle(document.documentElement);
              var stageColors = [
                rootStyle.getPropertyValue('--dashboard-stage-1').trim() || '#4c6ef5',
                rootStyle.getPropertyValue('--dashboard-stage-2').trim() || '#12b886',
                rootStyle.getPropertyValue('--dashboard-stage-3').trim() || '#f59f00',
                rootStyle.getPropertyValue('--dashboard-stage-4').trim() || '#e64980'
              ];

              (data.trackers || []).forEach(function (tracker) {
                var section = document.querySelector('.tracker[data-tracker="' + cssEscape(tracker.id) + '"]');
                var canvas = section ? section.querySelector('.chart canvas') : null;
                if (!canvas || typeof Chart === 'undefined') {
                  return;
                }
                var datasets = tracker.stages.map(function (stage, index) {
                  var color = stageColors[index % stageColors.length];
                  return {
                    label: stage,
                    data: tracker.series[stage],
                    borderColor: color,
                    backgroundColor: color,
                    fill: false,
                    tension: 0.2
                  };
                });
                new Chart(canvas.getContext('2d'), {
                  type: 'line',
                  data: { labels: tracker.dates, datasets: datasets },
                  options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: true } } }
                });
              });

              function cssEscape(value) {
                return String(value).replace(/["\\\\]/g, '\\\\$&');
              }

              var select = document.getElementById('lens-select');
              var stylesheet = document.getElementById('lens-stylesheet');
              if (!select || !stylesheet) {
                return;
              }
              var storageKey = 'trackerLens.selectedLens';
              var stored = null;
              try {
                stored = localStorage.getItem(storageKey);
              } catch (ignored) {
                stored = null;
              }
              var hasStored = stored && Array.prototype.some.call(select.options, function (option) {
                return option.value === stored;
              });
              if (hasStored) {
                select.value = stored;
                applyLens(stored);
              }
              select.addEventListener('change', function () {
                applyLens(select.value);
                try {
                  localStorage.setItem(storageKey, select.value);
                } catch (ignored) {
                  // localStorage unavailable (e.g. file:// under strict privacy settings) - selection just won't persist.
                }
              });

              function applyLens(lensId) {
                var file = (data.lensFiles || {})[lensId];
                if (file) {
                  stylesheet.setAttribute('href', file);
                }
              }
            })();
            </script>
            """;
}
