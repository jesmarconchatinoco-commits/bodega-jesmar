(function () {
    'use strict';

    var STORAGE_CART = 'jesmar_catalogo_carrito';
    var STORAGE_FAVS = 'jesmar_catalogo_favoritos';

    var state = {
        productos: [],
        items: [],
        categorias: [],
        checkout: { costoEnvio: 0, yapeCelular: '', plinCelular: '' },
        filtros: { busqueda: '', categoriaId: '', presentacion: '', orden: 'default' },
        carrito: [],
        favoritos: new Set()
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    function init() {
        cargarDatosIniciales(function () {
            cargarCarrito();
            normalizarCarritoLegacy();
            cargarFavoritos();
            initHeroSlider();
            initCheckout();
            initCliente();
            bindEventos();
            construirItemsCatalogo();
            renderFiltros();
            renderProductos();
            renderCarrito();
        });
    }

    function initCheckout() {
        if (window.CatalogoCheckout) {
            CatalogoCheckout.init(state.checkout, {
                onSuccess: function (res) {
                    state.carrito = [];
                    guardarCarrito();
                    renderCarrito();
                    mostrarAlerta(res.message, 'success');
                },
                onAlert: mostrarAlerta
            });
        }
    }

    function initCliente() {
        if (window.CatalogoCliente) {
            CatalogoCliente.init({ onAlert: mostrarAlerta });
        }
    }

    function aplicarDatosCatalogo(data) {
        state.productos = data.productos || [];
        state.categorias = data.categorias || [];
        if (data.checkout) {
            state.checkout = data.checkout;
        }
        construirItemsCatalogo();
    }

    function extraerMedida(nombre) {
        var n = (nombre || '').trim();
        var match = n.match(/^(\d+(?:[.,]\d+)?)\s*(.+)$/);
        if (match) {
            var cantidad = match[1].replace(',', '.');
            var unidad = match[2].trim();
            return cantidad + ' ' + unidad;
        }
        return n || '—';
    }

    function construirItemsCatalogo() {
        var items = [];
        state.productos.forEach(function (producto) {
            (producto.presentaciones || []).forEach(function (pres) {
                items.push({
                    key: producto.id + '-' + pres.id,
                    productoId: producto.id,
                    presentacionId: pres.id,
                    productoNombre: producto.nombre,
                    presentacionNombre: pres.nombre,
                    medida: pres.medida || extraerMedida(pres.nombre),
                    precio: Number(pres.precio),
                    stock: Number(pres.stock) || 0,
                    categoriaId: producto.categoriaId,
                    categoriaNombre: producto.categoriaNombre || 'Sin categoría',
                    imagen: pres.imagen || (pres.imagenes && pres.imagenes.length ? pres.imagenes[0] : null) ||
                        (producto.imagenes && producto.imagenes.length ? producto.imagenes[0] : null)
                });
            });
        });
        state.items = items;
    }

    function cargarDatosIniciales(callback) {
        var el = document.getElementById('catalogo-data');
        if (el && el.textContent.trim()) {
            try {
                aplicarDatosCatalogo(JSON.parse(el.textContent.trim()));
                callback();
                return;
            } catch (e) {
                console.error('Error al leer datos embebidos del catálogo', e);
            }
        }

        fetch('/catalogo/api/productos')
            .then(function (r) {
                if (!r.ok) throw new Error('HTTP ' + r.status);
                return r.json();
            })
            .then(function (data) {
                aplicarDatosCatalogo(data);
                callback();
            })
            .catch(function (e) {
                console.error('Error al cargar datos del catálogo', e);
                callback();
            });
    }

    function cargarCarrito() {
        try {
            var raw = localStorage.getItem(STORAGE_CART);
            state.carrito = raw ? JSON.parse(raw) : [];
        } catch (e) {
            state.carrito = [];
        }
    }

    function guardarCarrito() {
        localStorage.setItem(STORAGE_CART, JSON.stringify(state.carrito));
    }

    function normalizarCarritoLegacy() {
        if (!state.carrito.length || !state.productos.length) return;
        var cambio = false;
        state.carrito = state.carrito.filter(function (item) {
            if (item.presentacionId) return true;
            var prod = state.productos.find(function (p) { return p.id === item.productoId; });
            if (!prod || !prod.presentaciones || !prod.presentaciones.length) return false;
            var pres = prod.presentaciones[0];
            item.presentacionId = pres.id;
            item.precio = Number(pres.precio);
            item.stock = pres.stock;
            item.nombre = prod.nombre + ' (' + pres.nombre + ')';
            item.presentacionNombre = pres.nombre;
            cambio = true;
            return true;
        });
        if (cambio) guardarCarrito();
    }

    function cargarFavoritos() {
        try {
            var raw = localStorage.getItem(STORAGE_FAVS);
            var arr = raw ? JSON.parse(raw) : [];
            state.favoritos = new Set(arr);
        } catch (e) {
            state.favoritos = new Set();
        }
    }

    function guardarFavoritos() {
        localStorage.setItem(STORAGE_FAVS, JSON.stringify(Array.from(state.favoritos)));
    }

    function bindEventos() {
        onClick('btnAbrirCarrito', abrirCarrito);
        onClick('btnCerrarCarrito', cerrarCarrito);
        onClick('catOverlay', cerrarCarrito);
        onClick('btnVaciarCarrito', vaciarCarrito);
        onClick('btnRealizarCompra', abrirCheckout);
        onClick('btnBuscarProducto', aplicarFiltroBusqueda);

        var filtros = document.querySelector('.cat-filters');
        if (filtros) {
            filtros.addEventListener('input', function (e) {
                if (e.target.id === 'filtroBusqueda') {
                    aplicarFiltroBusqueda();
                }
            });
            filtros.addEventListener('change', function (e) {
                var target = e.target;
                if (target.id === 'filtroOrden') {
                    state.filtros.orden = target.value;
                    renderProductos();
                } else if (target.id === 'filtroCategoria') {
                    state.filtros.categoriaId = target.value;
                    actualizarOpcionesPresentacion();
                    renderProductos();
                } else if (target.id === 'filtroPresentacion') {
                    state.filtros.presentacion = target.value;
                    renderProductos();
                }
            });
        }

        var busqueda = document.getElementById('filtroBusqueda');
        if (busqueda) {
            busqueda.addEventListener('keydown', function (e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    aplicarFiltroBusqueda();
                }
            });
        }
    }

    function onClick(id, handler) {
        var el = document.getElementById(id);
        if (el) el.addEventListener('click', handler);
    }

    function aplicarFiltroBusqueda() {
        var input = document.getElementById('filtroBusqueda');
        state.filtros.busqueda = input ? input.value.trim().toLowerCase() : '';
        var selCat = document.getElementById('filtroCategoria');
        if (selCat) state.filtros.categoriaId = selCat.value;
        var selPres = document.getElementById('filtroPresentacion');
        if (selPres) state.filtros.presentacion = selPres.value;
        renderProductos();
    }

    function renderFiltros() {
        var selCat = document.getElementById('filtroCategoria');
        if (selCat) {
            var valorCat = selCat.value;
            selCat.innerHTML = '<option value="">Todas las categorías</option>';

            var categorias = state.categorias.length ? state.categorias : obtenerCategoriasDesdeItems();
            categorias.forEach(function (cat) {
                var count = state.items.filter(function (item) {
                    return String(item.categoriaId) === String(cat.id);
                }).length;
                if (count > 0) {
                    var opt = document.createElement('option');
                    opt.value = cat.id;
                    opt.textContent = cat.nombre + ' (' + count + ')';
                    selCat.appendChild(opt);
                }
            });
            if (valorCat) selCat.value = valorCat;
            state.filtros.categoriaId = selCat.value;
        }
        actualizarOpcionesPresentacion();
    }

    function obtenerCategoriasDesdeItems() {
        var mapa = {};
        state.items.forEach(function (item) {
            if (item.categoriaId == null) return;
            mapa[item.categoriaId] = item.categoriaNombre || 'Sin categoría';
        });
        return Object.keys(mapa).map(function (id) {
            return { id: parseInt(id, 10), nombre: mapa[id] };
        }).sort(function (a, b) {
            return a.nombre.localeCompare(b.nombre, 'es');
        });
    }

    function actualizarOpcionesPresentacion() {
        var selPres = document.getElementById('filtroPresentacion');
        if (!selPres) return;

        var catId = state.filtros.categoriaId || (document.getElementById('filtroCategoria') || {}).value || '';
        var base = state.items;
        if (catId) {
            base = base.filter(function (item) {
                return String(item.categoriaId) === String(catId);
            });
        }

        var conteo = {};
        base.forEach(function (item) {
            var nombre = (item.presentacionNombre || '').trim();
            if (!nombre) return;
            conteo[nombre] = (conteo[nombre] || 0) + 1;
        });

        var nombres = Object.keys(conteo).sort(function (a, b) {
            return a.localeCompare(b, 'es');
        });

        var actual = selPres.value;
        selPres.innerHTML = '<option value="">Todas las presentaciones</option>';
        nombres.forEach(function (nombre) {
            var opt = document.createElement('option');
            opt.value = nombre;
            opt.textContent = nombre + ' (' + conteo[nombre] + ')';
            selPres.appendChild(opt);
        });

        if (actual && conteo[actual]) {
            selPres.value = actual;
            state.filtros.presentacion = actual;
        } else {
            selPres.value = '';
            state.filtros.presentacion = '';
        }
    }

    function itemsFiltrados() {
        var lista = state.items.slice();
        var busqueda = state.filtros.busqueda;
        var catId = state.filtros.categoriaId;
        var pres = state.filtros.presentacion;

        if (busqueda) {
            lista = lista.filter(function (item) {
                return (item.productoNombre || '').toLowerCase().indexOf(busqueda) >= 0
                    || (item.presentacionNombre || '').toLowerCase().indexOf(busqueda) >= 0
                    || (item.medida || '').toLowerCase().indexOf(busqueda) >= 0
                    || (item.categoriaNombre || '').toLowerCase().indexOf(busqueda) >= 0;
            });
        }

        if (catId) {
            lista = lista.filter(function (item) {
                return String(item.categoriaId) === String(catId);
            });
        }

        if (pres) {
            lista = lista.filter(function (item) {
                return (item.presentacionNombre || '').trim() === pres;
            });
        }

        switch (state.filtros.orden) {
            case 'precio-asc':
                lista.sort(function (a, b) { return a.precio - b.precio; });
                break;
            case 'precio-desc':
                lista.sort(function (a, b) { return b.precio - a.precio; });
                break;
            case 'nombre':
                lista.sort(function (a, b) {
                    return (a.productoNombre || '').localeCompare(b.productoNombre || '', 'es');
                });
                break;
            default:
                break;
        }
        return lista;
    }

    function renderProductos() {
        var grid = document.getElementById('gridProductos');
        var lista = itemsFiltrados();
        document.getElementById('contadorProductos').textContent =
            'Mostrando ' + lista.length + ' de ' + state.items.length + ' presentaciones';

        if (!lista.length) {
            grid.innerHTML = '<div class="cat-empty"><i class="fas fa-box-open fa-2x mb-3 d-block"></i>No hay productos con esos filtros.</div>';
            return;
        }

        grid.innerHTML = lista.map(function (item) { return crearCardHtml(item); }).join('');
        bindCardEvents(grid);
    }

    function crearCardHtml(item) {
        var sinStock = item.stock <= 0;
        var imgHtml = item.imagen
            ? '<img src="' + escapeHtml(item.imagen) + '" alt="' + escapeHtml(item.productoNombre) + '" loading="lazy">'
            : '<div class="cat-card-placeholder"><i class="fas fa-image"></i></div>';

        var stockClass = sinStock ? ' cat-card-stock-agotado' : (item.stock <= 5 ? ' cat-card-stock-bajo' : '');
        var stockTexto = sinStock ? 'Sin stock' : ('Stock: ' + item.stock);

        return '<article class="cat-card cat-card-mini' + (sinStock ? ' cat-card-sin-stock' : '') + '" data-key="' + item.key + '">' +
            '<div class="cat-card-media-mini">' + imgHtml +
            '<button class="cat-card-cart-icon" type="button" data-producto-id="' + item.productoId +
            '" data-presentacion-id="' + item.presentacionId + '"' +
            (sinStock ? ' disabled title="Sin stock"' : ' title="Agregar al carrito"') + '>' +
            '<i class="fas fa-cart-plus"></i></button></div>' +
            '<div class="cat-card-body-mini">' +
            '<h3 class="cat-card-title-mini" title="' + escapeHtml(item.productoNombre) + '">' +
            escapeHtml(item.productoNombre) + '</h3>' +
            '<span class="cat-card-medida">' + escapeHtml(item.medida) + '</span>' +
            '<span class="cat-card-stock-mini' + stockClass + '"><i class="fas fa-cubes"></i> ' + stockTexto + '</span>' +
            '<span class="cat-card-price-mini">S/ ' + formatearPrecio(item.precio) + '</span>' +
            '</div></article>';
    }

    function bindCardEvents(grid) {
        grid.querySelectorAll('.cat-card-cart-icon[data-presentacion-id]').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                agregarPresentacionDirecta(
                    parseInt(btn.dataset.productoId, 10),
                    parseInt(btn.dataset.presentacionId, 10)
                );
            });
        });
    }

    function agregarPresentacionDirecta(productoId, presentacionId) {
        var producto = state.productos.find(function (p) { return p.id === productoId; });
        if (!producto) return;
        var presentacion = (producto.presentaciones || []).find(function (pr) {
            return pr.id === presentacionId;
        });
        if (!presentacion) return;
        agregarPresentacionAlCarrito(producto, presentacion);
    }

    function initCardSlider(card) {
        var slider = card.querySelector('.cat-card-slider');
        if (!slider) return;
        var slides = slider.children.length;
        if (slides <= 1) return;

        var index = 0;
        function goTo(i) {
            index = (i + slides) % slides;
            slider.style.transform = 'translateX(-' + (index * 100) + '%)';
            card.querySelectorAll('.cat-card-dot').forEach(function (dot, di) {
                dot.classList.toggle('active', di === index);
            });
        }

        var prev = card.querySelector('.cat-card-arrow.prev');
        var next = card.querySelector('.cat-card-arrow.next');
        if (prev) prev.addEventListener('click', function (e) { e.stopPropagation(); goTo(index - 1); });
        if (next) next.addEventListener('click', function (e) { e.stopPropagation(); goTo(index + 1); });
        card.querySelectorAll('.cat-card-dot').forEach(function (dot) {
            dot.addEventListener('click', function (e) {
                e.stopPropagation();
                goTo(parseInt(dot.dataset.index, 10));
            });
        });
    }

    function initHeroSlider() {
        var track = document.getElementById('heroTrack');
        if (!track || track.children.length <= 1) return;

        var slides = track.children.length;
        var index = 0;
        var autoplay;

        function goTo(i) {
            index = (i + slides) % slides;
            track.style.transform = 'translateX(-' + (index * 100) + '%)';
            document.querySelectorAll('.cat-hero-dot').forEach(function (dot, di) {
                dot.classList.toggle('active', di === index);
            });
        }

        function startAutoplay() {
            stopAutoplay();
            autoplay = setInterval(function () { goTo(index + 1); }, 5000);
        }

        function stopAutoplay() {
            if (autoplay) clearInterval(autoplay);
        }

        var prev = document.getElementById('heroPrev');
        var next = document.getElementById('heroNext');
        if (prev) prev.addEventListener('click', function () { goTo(index - 1); startAutoplay(); });
        if (next) next.addEventListener('click', function () { goTo(index + 1); startAutoplay(); });
        document.querySelectorAll('.cat-hero-dot').forEach(function (dot) {
            dot.addEventListener('click', function () {
                goTo(parseInt(dot.dataset.index, 10));
                startAutoplay();
            });
        });

        var hero = document.querySelector('.cat-hero-slider');
        if (hero) {
            hero.addEventListener('mouseenter', stopAutoplay);
            hero.addEventListener('mouseleave', startAutoplay);
        }
        startAutoplay();
    }

    function etiquetaPrecioProducto(p) {
        var pres = p.presentaciones || [];
        if (pres.length <= 1) {
            return formatearPrecio(p.precio);
        }
        var precios = pres.map(function (pr) { return Number(pr.precio); });
        var min = Math.min.apply(null, precios);
        var max = Math.max.apply(null, precios);
        if (min === max) {
            return formatearPrecio(min);
        }
        return 'Desde ' + formatearPrecio(min);
    }

    function presentacionesDisponibles(producto) {
        return (producto.presentaciones || []).filter(function (pr) {
            return pr.stock > 0;
        });
    }

    function agregarAlCarrito(productoId) {
        var producto = state.productos.find(function (p) { return p.id === productoId; });
        if (!producto) return;

        var activas = presentacionesDisponibles(producto);
        if (!activas.length) {
            mostrarAlerta('Sin stock disponible para este producto.', 'warning');
            return;
        }

        if (activas.length === 1) {
            agregarPresentacionAlCarrito(producto, activas[0]);
            return;
        }

        abrirModalPresentacion(producto, activas);
    }

    function abrirModalPresentacion(producto, presentaciones) {
        document.getElementById('presSelProductoNombre').textContent = producto.nombre;
        var lista = document.getElementById('presSelLista');
        lista.innerHTML = presentaciones.map(function (pr) {
            return '<button type="button" class="cat-pres-option" data-producto-id="' + producto.id +
                '" data-presentacion-id="' + pr.id + '">' +
                '<span class="cat-pres-option-name">' + escapeHtml(pr.nombre) + '</span>' +
                '<span class="cat-pres-option-meta">S/ ' + formatearPrecio(pr.precio) +
                ' · Stock: ' + pr.stock + '</span></button>';
        }).join('');

        lista.querySelectorAll('.cat-pres-option').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var presId = parseInt(btn.dataset.presentacionId, 10);
                var pres = presentaciones.find(function (x) { return x.id === presId; });
                if (pres) {
                    agregarPresentacionAlCarrito(producto, pres);
                    if (window.bootstrap) {
                        var modalEl = document.getElementById('modalPresentacion');
                        var inst = bootstrap.Modal.getInstance(modalEl);
                        if (inst) inst.hide();
                    }
                }
            });
        });

        var modalEl = document.getElementById('modalPresentacion');
        if (modalEl && window.bootstrap) {
            bootstrap.Modal.getOrCreateInstance(modalEl).show();
        }
    }

    function agregarPresentacionAlCarrito(producto, presentacion) {
        if (!presentacion || presentacion.stock <= 0) {
            mostrarAlerta('Sin stock disponible para esta presentación.', 'warning');
            return;
        }

        var nombreLinea = producto.nombre;
        if (presentacion.nombre) {
            nombreLinea += ' (' + presentacion.nombre + ')';
        }

        var item = state.carrito.find(function (c) {
            return c.presentacionId === presentacion.id;
        });

        if (item) {
            if (item.cantidad >= presentacion.stock) {
                mostrarAlerta('No hay más stock disponible.', 'warning');
                return;
            }
            item.cantidad++;
        } else {
            state.carrito.push({
                productoId: producto.id,
                presentacionId: presentacion.id,
                nombre: nombreLinea,
                presentacionNombre: presentacion.nombre,
                precio: Number(presentacion.precio),
                cantidad: 1,
                stock: presentacion.stock,
                imagen: presentacion.imagen || (presentacion.imagenes && presentacion.imagenes.length ? presentacion.imagenes[0] : null) ||
                    (producto.imagenes && producto.imagenes.length ? producto.imagenes[0] : null)
            });
        }
        guardarCarrito();
        renderCarrito();
        mostrarAlerta('Producto agregado al carrito.', 'success');
        abrirCarrito();
    }

    function renderCarrito() {
        var badge = document.getElementById('cartBadge');
        var totalItems = state.carrito.reduce(function (s, i) { return s + i.cantidad; }, 0);
        badge.textContent = totalItems;
        badge.style.display = totalItems > 0 ? 'flex' : 'none';

        var container = document.getElementById('cartItems');
        var btnCompra = document.getElementById('btnRealizarCompra');

        if (!state.carrito.length) {
            container.innerHTML = '<div class="cat-cart-empty"><i class="fas fa-shopping-cart fa-2x mb-3 d-block"></i>Tu carrito está vacío</div>';
            document.getElementById('cartTotal').textContent = 'S/ 0.00';
            btnCompra.disabled = true;
            return;
        }

        btnCompra.disabled = false;
        var total = 0;
        container.innerHTML = state.carrito.map(function (item, idx) {
            var subtotal = item.precio * item.cantidad;
            total += subtotal;
            var img = item.imagen
                ? '<div class="cat-cart-item-thumb"><img src="' + item.imagen + '" alt=""></div>'
                : '<div class="cat-cart-item-thumb"><div class="cat-card-placeholder"><i class="fas fa-image"></i></div></div>';
            return '<div class="cat-cart-item" data-idx="' + idx + '">' +
                img +
                '<div class="cat-cart-item-info">' +
                '<h4>' + escapeHtml(item.nombre) + '</h4>' +
                '<div class="cat-cart-qty">' +
                '<button type="button" class="qty-minus" data-idx="' + idx + '">-</button>' +
                '<span>' + item.cantidad + '</span>' +
                '<button type="button" class="qty-plus" data-idx="' + idx + '">+</button>' +
                '</div>' +
                '<div class="cat-cart-item-price">Precio unitario: S/ ' + formatearPrecio(item.precio) + '</div>' +
                '<small class="text-muted">Stock: ' + item.stock + '</small>' +
                '</div>' +
                '<div class="cat-cart-item-right">' +
                '<div class="cat-cart-item-subtotal">S/ ' + formatearPrecio(subtotal) + '</div>' +
                '<button class="cat-cart-remove" type="button" data-remove="' + idx + '"><i class="fas fa-trash"></i></button>' +
                '</div></div>';
        }).join('');

        document.getElementById('cartTotal').textContent = 'S/ ' + formatearPrecio(total);

        container.querySelectorAll('.qty-minus').forEach(function (btn) {
            btn.addEventListener('click', function () { cambiarCantidad(parseInt(btn.dataset.idx, 10), -1); });
        });
        container.querySelectorAll('.qty-plus').forEach(function (btn) {
            btn.addEventListener('click', function () { cambiarCantidad(parseInt(btn.dataset.idx, 10), 1); });
        });
        container.querySelectorAll('[data-remove]').forEach(function (btn) {
            btn.addEventListener('click', function () { eliminarItem(parseInt(btn.dataset.remove, 10)); });
        });
    }

    function cambiarCantidad(idx, delta) {
        var item = state.carrito[idx];
        if (!item) return;
        var nueva = item.cantidad + delta;
        if (nueva < 1) return;
        if (nueva > item.stock) {
            mostrarAlerta('Stock insuficiente.', 'warning');
            return;
        }
        item.cantidad = nueva;
        guardarCarrito();
        renderCarrito();
    }

    function eliminarItem(idx) {
        state.carrito.splice(idx, 1);
        guardarCarrito();
        renderCarrito();
    }

    function vaciarCarrito() {
        if (!state.carrito.length) return;
        if (!confirm('¿Vaciar el carrito?')) return;
        state.carrito = [];
        guardarCarrito();
        renderCarrito();
    }

    function abrirCarrito() {
        document.getElementById('catOverlay').classList.add('open');
        document.getElementById('catCart').classList.add('open');
        document.body.style.overflow = 'hidden';
    }

    function cerrarCarrito() {
        document.getElementById('catOverlay').classList.remove('open');
        document.getElementById('catCart').classList.remove('open');
        document.body.style.overflow = '';
    }

    function abrirCheckout() {
        if (!state.carrito.length) return;
        cerrarCarrito();
        if (window.CatalogoCheckout) {
            CatalogoCheckout.abrir(state.carrito.slice());
        }
    }

    function toggleFavorito(id, btn) {
        if (state.favoritos.has(id)) {
            state.favoritos.delete(id);
            btn.classList.remove('active');
        } else {
            state.favoritos.add(id);
            btn.classList.add('active');
        }
        guardarFavoritos();
    }

    function formatearPrecio(valor) {
        return Number(valor).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function escapeHtml(text) {
        var div = document.createElement('div');
        div.textContent = text || '';
        return div.innerHTML;
    }

    function mostrarAlerta(msg, tipo) {
        var area = document.getElementById('catToast');
        if (!area) return;
        var cls = tipo === 'success' ? 'alert-success' : (tipo === 'warning' ? 'alert-warning' : 'alert-danger');
        area.innerHTML = '<div class="alert ' + cls + ' alert-dismissible fade show shadow-sm" role="alert">' +
            msg + '<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Cerrar"></button></div>';
        setTimeout(function () { area.innerHTML = ''; }, 4500);
    }
})();
