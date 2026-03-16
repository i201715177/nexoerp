/* =========================================================
   NexoERP – Shared JavaScript
   ========================================================= */

function toggleSidebar() {
  var isMobile = window.innerWidth < 992;
  if (isMobile) {
    var sidebar = document.getElementById('sidebar');
    var backdrop = document.getElementById('sidebarBackdrop');
    sidebar.classList.toggle('open');
    if (backdrop) backdrop.classList.toggle('show');
  } else {
    var layout = document.querySelector('.app-layout');
    layout.classList.toggle('collapsed');
    localStorage.setItem('sidebar-collapsed', layout.classList.contains('collapsed'));
  }
}

function closeSidebar() {
  var sidebar = document.getElementById('sidebar');
  var backdrop = document.getElementById('sidebarBackdrop');
  if (sidebar) sidebar.classList.remove('open');
  if (backdrop) backdrop.classList.remove('show');
}

/* ======================== NEXO NOTIFICATIONS & CONFIRM ======================== */
var nexoS = (function () {
  var toastContainer = null;
  var modalBackdrop = null;
  var modalResolve = null;

  function getToastContainer() {
    if (!toastContainer) {
      toastContainer = document.createElement('div');
      toastContainer.id = 'nexo-toast-container';
      document.body.appendChild(toastContainer);
    }
    return toastContainer;
  }

  function toast(opts) {
    var type = (opts && opts.type) ? opts.type : 'info';
    var message = (opts && opts.message) ? opts.message : '';
    var duration = (opts && opts.duration) != null ? opts.duration : 4500;

    var icons = {
      success: 'bi-check-circle-fill',
      error: 'bi-exclamation-circle-fill',
      warning: 'bi-exclamation-triangle-fill',
      info: 'bi-info-circle-fill'
    };
    var icon = icons[type] || icons.info;

    var el = document.createElement('div');
    el.className = 'nexo-toast ' + type;
    el.innerHTML =
      '<span class="nexo-toast-icon"><i class="bi ' + icon + '"></i></span>' +
      '<span class="nexo-toast-msg">' + escapeHtml(message) + '</span>' +
      '<button type="button" class="nexo-toast-close" aria-label="Cerrar"><i class="bi bi-x-lg"></i></button>';
    getToastContainer().appendChild(el);

    function close() {
      el.classList.add('closing');
      setTimeout(function () {
        if (el.parentNode) el.parentNode.removeChild(el);
      }, 250);
    }

    el.querySelector('.nexo-toast-close').addEventListener('click', close);
    if (duration > 0) setTimeout(close, duration);
  }

  function escapeHtml(text) {
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  function getModal() {
    if (!modalBackdrop) {
      modalBackdrop = document.createElement('div');
      modalBackdrop.id = 'nexo-modal-backdrop';
      modalBackdrop.className = 'hidden';
      modalBackdrop.innerHTML =
        '<div id="nexo-modal-box">' +
        '  <div class="nexo-modal-icon-wrap"><div class="nexo-modal-icon"><i class="bi bi-question-lg"></i></div></div>' +
        '  <div class="nexo-modal-title" id="nexo-modal-title">Confirmar</div>' +
        '  <div class="nexo-modal-message" id="nexo-modal-message"></div>' +
        '  <div class="nexo-modal-buttons">' +
        '    <button type="button" class="nexo-modal-btn-cancel" id="nexo-modal-cancel">Cancelar</button>' +
        '    <button type="button" class="nexo-modal-btn-confirm" id="nexo-modal-confirm">Aceptar</button>' +
        '  </div>' +
        '</div>';
      document.body.appendChild(modalBackdrop);

      modalBackdrop.addEventListener('click', function (e) {
        if (e.target === modalBackdrop) {
          if (modalResolve) modalResolve(false);
          modalBackdrop._pendingForm = null;
          modalBackdrop.classList.add('hidden');
        }
      });
      modalBackdrop.querySelector('#nexo-modal-cancel').addEventListener('click', function () {
        if (modalResolve) modalResolve(false);
        modalBackdrop._pendingForm = null;
        modalBackdrop.classList.add('hidden');
      });
      modalBackdrop.querySelector('#nexo-modal-confirm').addEventListener('click', function () {
        if (modalBackdrop._pendingForm) {
          modalBackdrop._pendingForm.submit();
          modalBackdrop._pendingForm = null;
        } else if (modalResolve) {
          modalResolve(true);
        }
        modalBackdrop.classList.add('hidden');
      });
    }
    return modalBackdrop;
  }

  function confirm(message) {
    return new Promise(function (resolve) {
      modalResolve = resolve;
      modalBackdrop = getModal();
      modalBackdrop._pendingForm = null;
      modalBackdrop.querySelector('#nexo-modal-message').textContent = message || '¿Continuar?';
      modalBackdrop.querySelector('#nexo-modal-title').textContent = 'Confirmar';
      modalBackdrop.classList.remove('hidden');
    });
  }

  function confirmSubmit(event, message) {
    if (event && event.preventDefault) event.preventDefault();
    var form = event && event.target;
    if (!form || form.tagName !== 'FORM') return false;
    modalResolve = null;
    var m = getModal();
    m._pendingForm = form;
    m.querySelector('#nexo-modal-message').textContent = message || '¿Continuar?';
    m.querySelector('#nexo-modal-title').textContent = 'Confirmar';
    m.classList.remove('hidden');
    return false;
  }

  return {
    toast: toast,
    confirm: confirm,
    confirmSubmit: confirmSubmit
  };
})();

function confirmarAccion(msg) {
  return window.confirm(msg || '¿Está seguro?');
}

document.addEventListener('DOMContentLoaded', function () {
  // Restore collapsed state from localStorage (desktop only)
  if (window.innerWidth >= 992) {
    var layout = document.querySelector('.app-layout');
    if (layout) {
      var saved = localStorage.getItem('sidebar-collapsed');
      // Por defecto: colapsado en desktop si no hay preferencia guardada
      if (saved === null || saved === 'true') {
        layout.classList.add('collapsed');
        localStorage.setItem('sidebar-collapsed', 'true');
      }

      var sidebar = document.getElementById('sidebar');
      if (sidebar) {
        // Expandir al pasar el mouse sobre todo el sidebar
        sidebar.addEventListener('mouseenter', function () {
          layout.classList.remove('collapsed');
          localStorage.setItem('sidebar-collapsed', 'false');
        });
        // Volver a colapsar cuando el mouse sale del sidebar
        sidebar.addEventListener('mouseleave', function () {
          layout.classList.add('collapsed');
          localStorage.setItem('sidebar-collapsed', 'true');
        });
      }
    }
  }

  // Mobile backdrop click → close sidebar
  var backdrop = document.getElementById('sidebarBackdrop');
  if (backdrop) {
    backdrop.addEventListener('click', closeSidebar);
  }

  // Auto-collapse sidebar when a menu link is clicked (desktop)
  var sidebarLinks = document.querySelectorAll('.sidebar-link');
  sidebarLinks.forEach(function (link) {
    link.addEventListener('click', function () {
      if (window.innerWidth >= 992) {
        localStorage.setItem('sidebar-collapsed', 'true');
      } else {
        closeSidebar();
      }
    });
  });

  initTablePagination();
  initNotificacionesBell();
});

/* ======================== TABLE PAGINATION ======================== */
var TABLE_PAGE_SIZE = 5;

function initTablePagination() {
  document.querySelectorAll('table.data-table').forEach(function (table) {
    if (table.dataset.paginationInit) return;
    table.dataset.paginationInit = 'true';

    var tbody = table.querySelector('tbody');
    if (!tbody) return;

    var allRows = Array.from(tbody.querySelectorAll('tr'));
    if (allRows.length <= TABLE_PAGE_SIZE) return;

    var currentPage = 1;

    var pagDiv = document.createElement('div');
    pagDiv.className = 'table-pagination';
    table.parentNode.insertBefore(pagDiv, table.nextSibling);

    function render() {
      var visibleRows = allRows.filter(function (r) {
        return !r.classList.contains('filter-hidden');
      });
      var totalPages = Math.ceil(visibleRows.length / TABLE_PAGE_SIZE);
      if (totalPages < 1) totalPages = 1;
      if (currentPage > totalPages) currentPage = totalPages;

      var start = (currentPage - 1) * TABLE_PAGE_SIZE;
      var end = start + TABLE_PAGE_SIZE;

      allRows.forEach(function (r) {
        if (r.classList.contains('filter-hidden')) return;
        r.style.display = 'none';
      });
      visibleRows.forEach(function (r, i) {
        r.style.display = (i >= start && i < end) ? '' : 'none';
      });

      var showing = visibleRows.length > 0 ? (start + 1) : 0;
      var showEnd = Math.min(end, visibleRows.length);
      var html = '<span class="pg-info">' + showing + '–' + showEnd + ' de ' + visibleRows.length + '</span>';

      if (totalPages > 1) {
        html += '<div class="pg-buttons">';
        html += '<button class="pg-btn" ' + (currentPage <= 1 ? 'disabled' : '') +
                ' data-page="' + (currentPage - 1) + '">&laquo;</button>';

        var startPage = Math.max(1, currentPage - 2);
        var endPage = Math.min(totalPages, startPage + 4);
        if (endPage - startPage < 4) startPage = Math.max(1, endPage - 4);

        for (var p = startPage; p <= endPage; p++) {
          html += '<button class="pg-btn' + (p === currentPage ? ' active' : '') +
                  '" data-page="' + p + '">' + p + '</button>';
        }

        html += '<button class="pg-btn" ' + (currentPage >= totalPages ? 'disabled' : '') +
                ' data-page="' + (currentPage + 1) + '">&raquo;</button>';
        html += '</div>';
      }

      pagDiv.innerHTML = html;

      pagDiv.querySelectorAll('.pg-btn:not([disabled])').forEach(function (btn) {
        btn.addEventListener('click', function () {
          currentPage = parseInt(this.dataset.page);
          render();
        });
      });
    }

    table._paginateRender = render;
    table._paginateReset = function () { currentPage = 1; render(); };
    render();
  });
}

window.refreshPagination = function (tableEl) {
  if (tableEl && tableEl._paginateReset) {
    tableEl._paginateReset();
  } else {
    document.querySelectorAll('table.data-table').forEach(function (t) {
      if (t._paginateReset) t._paginateReset();
    });
  }
};

/* ======================== NOTIFICATION BELL (SAAS_ADMIN) ======================== */
function initNotificacionesBell() {
  var topbarUser = document.querySelector('.topbar-user');
  if (!topbarUser) return;

  var style = document.createElement('style');
  style.textContent =
    '.nexo-bell-wrap{position:relative;margin-right:12px;cursor:pointer;}' +
    '.nexo-bell-icon{font-size:22px;color:#64748b;transition:color .2s;}' +
    '.nexo-bell-wrap:hover .nexo-bell-icon{color:#f59e0b;}' +
    '.nexo-bell-badge{position:absolute;top:-4px;right:-6px;background:#ef4444;color:#fff;font-size:11px;font-weight:700;' +
      'min-width:18px;height:18px;border-radius:50%;display:flex;align-items:center;justify-content:center;' +
      'line-height:1;padding:0 4px;box-shadow:0 1px 4px rgba(0,0,0,.2);animation:nexoBellPulse 2s infinite;}' +
    '@keyframes nexoBellPulse{0%,100%{transform:scale(1);}50%{transform:scale(1.15);}}' +
    '.nexo-bell-dropdown{display:none;position:absolute;top:36px;right:0;width:340px;background:#fff;' +
      'border-radius:10px;box-shadow:0 8px 32px rgba(0,0,0,.18);z-index:9999;overflow:hidden;}' +
    '.nexo-bell-dropdown.show{display:block;}' +
    '.nexo-bell-dd-header{background:linear-gradient(135deg,#1e3a5f,#2563eb);color:#fff;padding:10px 14px;font-weight:600;font-size:14px;display:flex;justify-content:space-between;align-items:center;}' +
    '.nexo-bell-dd-body{max-height:280px;overflow-y:auto;}' +
    '.nexo-bell-item{padding:10px 14px;border-bottom:1px solid #f1f5f9;transition:background .15s;cursor:pointer;}' +
    '.nexo-bell-item:hover{background:#f8fafc;}' +
    '.nexo-bell-item:last-child{border-bottom:none;}' +
    '.nexo-bell-item-title{font-weight:600;font-size:13px;color:#1e293b;}' +
    '.nexo-bell-item-sub{font-size:12px;color:#64748b;margin-top:2px;}' +
    '.nexo-bell-dd-footer{padding:8px 14px;text-align:center;border-top:1px solid #e2e8f0;}' +
    '.nexo-bell-dd-footer a{font-size:13px;color:#2563eb;text-decoration:none;font-weight:600;}' +
    '.nexo-bell-dd-footer a:hover{text-decoration:underline;}' +
    '.nexo-bell-empty{padding:24px 14px;text-align:center;color:#94a3b8;font-size:13px;}';
  document.head.appendChild(style);

  var wrap = document.createElement('div');
  wrap.className = 'nexo-bell-wrap';
  wrap.id = 'nexoBellWrap';
  wrap.style.display = 'none';
  wrap.innerHTML =
    '<div class="nexo-bell-icon" title="Notificaciones"><i class="bi bi-bell-fill"></i></div>' +
    '<span class="nexo-bell-badge" id="nexoBellBadge" style="display:none;">0</span>' +
    '<div class="nexo-bell-dropdown" id="nexoBellDropdown">' +
      '<div class="nexo-bell-dd-header">' +
        '<span><i class="bi bi-bell me-1"></i>Solicitudes pendientes</span>' +
        '<span id="nexoBellCount" class="badge bg-light text-dark">0</span>' +
      '</div>' +
      '<div class="nexo-bell-dd-body" id="nexoBellBody"></div>' +
      '<div class="nexo-bell-dd-footer"><a href="/web/admin/solicitudes">Ver todas las solicitudes</a></div>' +
    '</div>';
  topbarUser.parentNode.insertBefore(wrap, topbarUser);

  wrap.querySelector('.nexo-bell-icon').addEventListener('click', function (e) {
    e.stopPropagation();
    var dd = document.getElementById('nexoBellDropdown');
    dd.classList.toggle('show');
  });

  document.addEventListener('click', function (e) {
    var dd = document.getElementById('nexoBellDropdown');
    if (dd && !wrap.contains(e.target)) dd.classList.remove('show');
  });

  function fetchNotificaciones() {
    fetch('/api/notificaciones/pendientes')
      .then(function (r) {
        if (r.status === 403 || r.status === 401) { wrap.style.display = 'none'; return null; }
        if (!r.ok) return null;
        return r.json();
      })
      .then(function (data) {
        if (!data) return;
        wrap.style.display = '';
        var badge = document.getElementById('nexoBellBadge');
        var countEl = document.getElementById('nexoBellCount');
        var body = document.getElementById('nexoBellBody');

        var count = data.count || 0;
        if (badge) { badge.textContent = count; badge.style.display = count > 0 ? 'flex' : 'none'; }
        if (countEl) countEl.textContent = count;

        if (!data.items || data.items.length === 0) {
          body.innerHTML = '<div class="nexo-bell-empty"><i class="bi bi-check-circle me-1"></i>Sin solicitudes pendientes</div>';
          return;
        }

        var html = '';
        data.items.forEach(function (item) {
          html += '<div class="nexo-bell-item" onclick="window.location.href=\'/web/admin/solicitudes\'">';
          html += '<div class="nexo-bell-item-title"><i class="bi bi-building me-1"></i>' + escHtml(item.empresa) + '</div>';
          html += '<div class="nexo-bell-item-sub"><i class="bi bi-person me-1"></i>' + escHtml(item.contacto);
          if (item.plan) html += ' &middot; Plan: ' + escHtml(item.plan);
          html += '</div>';
          html += '<div class="nexo-bell-item-sub"><i class="bi bi-clock me-1"></i>' + escHtml(item.fecha) + '</div>';
          html += '</div>';
        });
        body.innerHTML = html;
      })
      .catch(function () {});
  }

  function escHtml(t) {
    var d = document.createElement('div');
    d.textContent = t || '';
    return d.innerHTML;
  }

  fetchNotificaciones();
  setInterval(fetchNotificaciones, 30000);
}
