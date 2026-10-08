/**
 * Comportamiento responsive global (sidebar móvil, tablas, modales, layout).
 */
$(document).ready(function () {
    var MOBILE_BREAKPOINT = 992;
    var TABLET_BREAKPOINT = 768;

    function isMobileViewport() {
        return window.innerWidth < MOBILE_BREAKPOINT;
    }

    function cerrarSidebarMovil() {
        if ($('body').hasClass('sidebar-open')) {
            $('body').removeClass('sidebar-open');
        }
    }

    function aplicarClasesViewport() {
        var $body = $('body');
        $body.toggleClass('viewport-mobile', isMobileViewport());
        $body.toggleClass('viewport-tablet', window.innerWidth >= TABLET_BREAKPOINT && window.innerWidth < MOBILE_BREAKPOINT);
    }

    aplicarClasesViewport();

    // Sidebar colapsado al cargar en móvil
    if (isMobileViewport()) {
        $('body').addClass('sidebar-collapse');
    }

    // Cerrar menú lateral al elegir una opción en celular
    $(document).on('click', '.main-sidebar .nav-link', function () {
        if (isMobileViewport()) {
            setTimeout(cerrarSidebarMovil, 150);
        }
    });

    // Cerrar al tocar el overlay del contenido
    $(document).on('click', '.content-wrapper', function (e) {
        if (isMobileViewport() && $('body').hasClass('sidebar-open')) {
            if (!$(e.target).closest('.main-sidebar').length) {
                cerrarSidebarMovil();
            }
        }
    });

    // Reajustar tablas DataTables al rotar o redimensionar
    var resizeTimer;
    $(window).on('resize orientationchange', function () {
        aplicarClasesViewport();

        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(function () {
            if (typeof $.fn.DataTable !== 'undefined') {
                $.fn.dataTable.tables({ visible: true, api: true }).columns.adjust();
                $.fn.dataTable.tables({ visible: true, api: true }).each(function () {
                    if (this.responsive) {
                        this.responsive.recalc();
                    }
                });
            }
        }, 250);
    });

    // En móvil, modales largos arrancan arriba del scroll
    $(document).on('show.bs.modal', '.modal', function () {
        if (window.innerWidth < 576) {
            $(this).find('.modal-dialog').scrollTop(0);
        }
    });

    // Evitar que el foco quede detrás del sidebar abierto
    $(document).on('shown.lte.pushmenu', function () {
        if (isMobileViewport() && !$('body').hasClass('sidebar-open')) {
            $('body').addClass('sidebar-collapse');
        }
    });
});
