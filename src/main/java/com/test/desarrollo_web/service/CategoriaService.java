package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.Categoria;
import com.test.desarrollo_web.Repository.CategoriaRepository;
import com.test.desarrollo_web.Repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository,
                            ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Map<Integer, Long> contarProductosPorCategoria() {
        Map<Integer, Long> conteos = new HashMap<>();
        for (Categoria categoria : categoriaRepository.findAll()) {
            conteos.put(categoria.getId(), productoRepository.countByCategoria_Id(categoria.getId()));
        }
        return conteos;
    }

    @Transactional
    public void eliminarSiNoTieneProductos(Integer id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("La categoría no existe."));

        long totalProductos = productoRepository.countByCategoria_Id(id);
        if (totalProductos > 0) {
            throw new RuntimeException(
                    "No se puede eliminar la categoría \"" + categoria.getNombre()
                            + "\" porque tiene " + totalProductos + " producto(s) asociado(s). "
                            + "Reasigne o elimine esos productos primero.");
        }

        categoriaRepository.delete(categoria);
    }
}
