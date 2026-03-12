package com.farmacia.sistema.tenant;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class TenantEntityListener {

    @PrePersist
    @PreUpdate
    public void applyTenant(Object entity) {
        if (entity instanceof TenantSupport support) {
            Long current = TenantContext.getTenantId();
            if (current != null && (support.getTenantId() == null || !current.equals(support.getTenantId()))) {
                support.setTenantId(current);
            }
        }
    }
}

