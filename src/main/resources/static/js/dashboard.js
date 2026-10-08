(function () {
    'use strict';

    var chartGanancias = null;
    var chartPago = null;
    var periodoGananciasActual = 'DIA';

    document.addEventListener('DOMContentLoaded', function () {
        var dataEl = document.getElementById('dashboard-chart-data');
        if (!dataEl) return;

        var data;
        try {
            data = JSON.parse(dataEl.textContent);
        } catch (e) {
            console.error('Error al leer datos del dashboard', e);
            return;
        }

        initCharts(data);
        initFiltroGanancias();
        initAnimacionContadores();
        initAutoRefresh();
    });

    function initCharts(data) {
        if (typeof Chart === 'undefined') return;

        var ctxGanancias = document.getElementById('chartGanancias');
        if (ctxGanancias) {
            var datosIniciales = (data && data.gananciasVentas) ? data.gananciasVentas : {
                labels: [], valores: [], total: 0, titulo: 'Últimos 7 días', periodo: 'DIA'
            };
            chartGanancias = crearChartGanancias(ctxGanancias, datosIniciales);
            actualizarChartGanancias(datosIniciales);
            actualizarResumenGanancias(datosIniciales);
        }

        var ctxPago = document.getElementById('chartFormaPago');
        if (ctxPago && data.ventasPorCanal) {
            var fp = data.ventasPorCanal;
            var labels = fp.labels || [];
            if (labels.length) {
                chartPago = new Chart(ctxPago, {
                    type: 'doughnut',
                    data: {
                        labels: labels,
                        datasets: [{
                            data: (fp.valores || []).map(Number),
                            backgroundColor: ['#10b981', '#667eea'],
                            borderWidth: 2,
                            borderColor: '#fff'
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: { position: 'bottom', labels: { boxWidth: 12, padding: 14 } }
                        }
                    }
                });
            }
        }
    }

    function crearChartGanancias(ctx, datos) {
        return new Chart(ctx, {
            type: 'bar',
            data: {
                labels: datos.labels || [],
                datasets: [{
                    label: 'Ganancias (S/)',
                    data: (datos.valores || []).map(Number),
                    backgroundColor: function (context) {
                        var value = context.raw || 0;
                        return value > 0
                            ? 'rgba(16, 185, 129, 0.8)'
                            : 'rgba(203, 213, 225, 0.55)';
                    },
                    borderColor: function (context) {
                        var value = context.raw || 0;
                        return value > 0 ? '#10b981' : '#cbd5e1';
                    },
                    borderWidth: 1,
                    borderRadius: 6,
                    maxBarThickness: 48
                }]
            },
            options: chartOptionsMoneda()
        });
    }

    function chartOptionsMoneda() {
        return {
            responsive: true,
            maintainAspectRatio: false,
            animation: {
                duration: 600,
                easing: 'easeOutQuart'
            },
            plugins: {
                legend: { display: false },
                title: { display: false },
                tooltip: {
                    callbacks: {
                        label: function (ctx) {
                            var valor = Number(ctx.raw || 0);
                            return 'Ganancia: S/ ' + valor.toLocaleString('es-PE', {
                                minimumFractionDigits: 2,
                                maximumFractionDigits: 2
                            });
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function (v) {
                            return 'S/ ' + Number(v).toLocaleString('es-PE');
                        }
                    },
                    grid: { color: 'rgba(0,0,0,0.05)' }
                },
                x: {
                    grid: { display: false }
                }
            }
        };
    }

    function initFiltroGanancias() {
        var grupo = document.getElementById('filtroGanancias');
        if (!grupo) return;

        grupo.addEventListener('click', function (e) {
            var btn = e.target.closest('[data-periodo]');
            if (!btn) return;

            var periodo = btn.getAttribute('data-periodo');
            if (!periodo || periodo === periodoGananciasActual) return;

            periodoGananciasActual = periodo;
            grupo.querySelectorAll('[data-periodo]').forEach(function (b) {
                b.classList.toggle('active', b === btn);
            });

            cargarGanancias(periodo);
        });
    }

    function cargarGanancias(periodo) {
        mostrarCargaGanancias(true);
        fetch('/dashboard/api/ganancias?periodo=' + encodeURIComponent(periodo))
            .then(function (r) {
                if (!r.ok) throw new Error('Error al cargar ganancias');
                return r.json();
            })
            .then(function (data) {
                if (!chartGanancias) {
                    var ctx = document.getElementById('chartGanancias');
                    if (ctx) chartGanancias = crearChartGanancias(ctx, data);
                }
                actualizarChartGanancias(data);
                actualizarResumenGanancias(data);
            })
            .catch(function () {
                var empty = document.getElementById('chartGananciasEmpty');
                var canvas = document.getElementById('chartGanancias');
                if (empty) {
                    empty.style.display = 'flex';
                    empty.innerHTML = '<span class="text-danger"><i class="fas fa-exclamation-circle mr-1"></i>No se pudo cargar el gráfico</span>';
                }
                if (canvas) canvas.style.display = 'none';
            })
            .finally(function () {
                mostrarCargaGanancias(false);
            });
    }

    function mostrarCargaGanancias(activo) {
        var loading = document.getElementById('gananciasLoading');
        if (loading) loading.classList.toggle('active', !!activo);
    }

    function actualizarChartGanancias(data) {
        if (!data) return;

        var valores = (data.valores || []).map(Number);
        var labels = data.labels || [];
        var canvas = document.getElementById('chartGanancias');
        var empty = document.getElementById('chartGananciasEmpty');
        var tieneDatos = valores.some(function (v) { return v > 0; });

        if (!chartGanancias && canvas) {
            chartGanancias = crearChartGanancias(canvas, data);
        }
        if (!chartGanancias) return;

        if (empty) {
            empty.style.display = labels.length && !tieneDatos ? 'flex' : 'none';
        }
        if (canvas) canvas.style.display = labels.length ? 'block' : 'none';

        chartGanancias.data.labels = labels;
        chartGanancias.data.datasets[0].data = valores;
        chartGanancias.update();
    }

    function actualizarResumenGanancias(data) {
        if (!data) return;

        var subtitulo = document.getElementById('subtituloGanancias');
        var total = document.getElementById('totalGanancias');

        if (subtitulo) {
            subtitulo.textContent = data.titulo || 'Ganancias de ventas';
        }
        if (total) {
            var monto = Number(data.total || 0);
            total.textContent = 'Total: S/ ' + monto.toLocaleString('es-PE', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            });
        }
    }

    function initAnimacionContadores() {
        document.querySelectorAll('.stat-value[data-animate]').forEach(function (el) {
            var numero = parseFloat(el.dataset.animate);
            if (isNaN(numero) || numero === 0) return;
            var esMoneda = el.dataset.moneda === 'true';
            var duracion = 800;
            var inicio = performance.now();

            function animar(tiempo) {
                var progreso = Math.min((tiempo - inicio) / duracion, 1);
                var actual = numero * progreso;
                if (esMoneda) {
                    el.textContent = 'S/ ' + actual.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                } else {
                    el.textContent = Math.round(actual).toLocaleString('es-PE');
                }
                if (progreso < 1) requestAnimationFrame(animar);
            }
            if (esMoneda) el.textContent = 'S/ 0.00';
            else el.textContent = '0';
            requestAnimationFrame(animar);
        });
    }

    function initAutoRefresh() {
        var badge = document.getElementById('dashUltimaActualizacion');
        setInterval(function () {
            fetch('/dashboard/api/stats')
                .then(function (r) { return r.json(); })
                .then(function (data) {
                    actualizarTarjetas(data);
                    actualizarGraficos(data);
                    if (badge) {
                        var ahora = new Date();
                        badge.textContent = 'Actualizado ' + ahora.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
                    }
                })
                .catch(function () { /* silencioso */ });
        }, 60000);
    }

    function actualizarTarjetas(data) {
        setStat('statVentasDia', data.ventasDelDia, true);
        setStat('statVentasMes', data.ventasDelMes, true);
        setStat('statVentasHoy', data.cantidadVentasHoy, false);
        setStat('statClientes', data.totalClientes, false);
        setStat('statProductos', data.totalProductosActivos, false);
        setStat('statPedidos', data.pedidosWebPendientes, false);
        setStat('statStockBajo', data.productosStockBajo, false);
    }

    function setStat(id, valor, moneda) {
        var el = document.getElementById(id);
        if (!el || valor == null) return;
        if (moneda) {
            el.textContent = 'S/ ' + Number(valor).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        } else {
            el.textContent = Number(valor).toLocaleString('es-PE');
        }
    }

    function actualizarGraficos(data) {
        if (periodoGananciasActual === 'DIA' && data.gananciasVentas) {
            actualizarChartGanancias(data.gananciasVentas);
            actualizarResumenGanancias(data.gananciasVentas);
        } else {
            cargarGanancias(periodoGananciasActual);
        }
        if (chartPago && data.ventasPorCanal && data.ventasPorCanal.labels) {
            chartPago.data.labels = data.ventasPorCanal.labels;
            chartPago.data.datasets[0].data = (data.ventasPorCanal.valores || []).map(Number);
            chartPago.update();
        }
    }
})();
