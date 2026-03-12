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
    var saved = localStorage.getItem('sidebar-collapsed');
    if (saved === 'true') {
      var layout = document.querySelector('.app-layout');
      if (layout) layout.classList.add('collapsed');
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
