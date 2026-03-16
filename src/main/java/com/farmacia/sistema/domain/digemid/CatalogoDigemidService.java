package com.farmacia.sistema.domain.digemid;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogoDigemidService {

    private final CatalogoDigemidRepository repository;

    public CatalogoDigemidService(CatalogoDigemidRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "catalogoDigemid", key = "#termino")
    public List<CatalogoDigemid> buscar(String termino) {
        if (termino == null || termino.trim().length() < 3) {
            return List.of();
        }
        return repository.buscarPorTermino(termino.trim());
    }

    public List<CatalogoDigemid> listarTodos() {
        return repository.findAll();
    }
}
