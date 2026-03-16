package com.farmacia.sistema.domain.catalogo;

import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class CatalogoService {

    private final CategoriaRepository categoriaRepo;
    private final LaboratorioRepository laboratorioRepo;
    private final PresentacionRepository presentacionRepo;

    public CatalogoService(CategoriaRepository categoriaRepo, LaboratorioRepository laboratorioRepo,
                           PresentacionRepository presentacionRepo) {
        this.categoriaRepo = categoriaRepo;
        this.laboratorioRepo = laboratorioRepo;
        this.presentacionRepo = presentacionRepo;
    }

    // ── Categorías ──
    public List<Categoria> listarCategorias() {
        return categoriaRepo.findByTenantIdOrderByNombreAsc(TenantContext.getTenantId());
    }

    public List<Categoria> listarCategoriasActivas() {
        return categoriaRepo.findByTenantIdAndActivoTrueOrderByNombreAsc(TenantContext.getTenantId());
    }

    public Categoria crearCategoria(String nombre, String descripcion) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        return categoriaRepo.save(c);
    }

    public void toggleCategoria(Long id) {
        Categoria c = categoriaRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));
        c.setActivo(!c.isActivo());
        categoriaRepo.save(c);
    }

    // ── Laboratorios ──
    public List<Laboratorio> listarLaboratorios() {
        return laboratorioRepo.findByTenantIdOrderByNombreAsc(TenantContext.getTenantId());
    }

    public List<Laboratorio> listarLaboratoriosActivos() {
        return laboratorioRepo.findByTenantIdAndActivoTrueOrderByNombreAsc(TenantContext.getTenantId());
    }

    public Laboratorio crearLaboratorio(String nombre, String pais) {
        Laboratorio l = new Laboratorio();
        l.setNombre(nombre);
        l.setPais(pais);
        return laboratorioRepo.save(l);
    }

    public void toggleLaboratorio(Long id) {
        Laboratorio l = laboratorioRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Laboratorio no encontrado"));
        l.setActivo(!l.isActivo());
        laboratorioRepo.save(l);
    }

    // ── Presentaciones ──
    public List<Presentacion> listarPresentaciones() {
        return presentacionRepo.findByTenantIdOrderByNombreAsc(TenantContext.getTenantId());
    }

    public List<Presentacion> listarPresentacionesActivas() {
        return presentacionRepo.findByTenantIdAndActivoTrueOrderByNombreAsc(TenantContext.getTenantId());
    }

    public Presentacion crearPresentacion(String nombre) {
        Presentacion p = new Presentacion();
        p.setNombre(nombre);
        return presentacionRepo.save(p);
    }

    public void togglePresentacion(Long id) {
        Presentacion p = presentacionRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Presentación no encontrada"));
        p.setActivo(!p.isActivo());
        presentacionRepo.save(p);
    }
}
