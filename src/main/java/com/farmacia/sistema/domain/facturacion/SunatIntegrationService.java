package com.farmacia.sistema.domain.facturacion;

import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.venta.Venta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Servicio preparatorio para integración con SUNAT.
 * Genera XML UBL 2.1 y gestiona el envío cuando se configure el certificado digital.
 * Actualmente genera la estructura XML pero NO envía a SUNAT (requiere certificado).
 */
@Service
public class SunatIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(SunatIntegrationService.class);
    private static final DateTimeFormatter FMT_SUNAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${app.sunat.certificado.habilitado:false}")
    private boolean certificadoHabilitado;

    @Value("${app.sunat.modo:DEMO}")
    private String modo;

    public boolean isHabilitado() {
        return certificadoHabilitado;
    }

    public String getModo() {
        return modo;
    }

    /**
     * Genera el XML UBL 2.1 para una factura o boleta.
     * Este XML sigue el estándar requerido por SUNAT para comprobantes electrónicos.
     */
    public String generarXmlComprobante(Empresa empresa, Venta venta) {
        String tipoDoc = "FAC".equals(venta.getTipoComprobante()) ? "01" : "03";
        String serie = venta.getSerieComprobante() != null ? venta.getSerieComprobante() : "001";
        String numero = venta.getNumeroComprobante() != null ? venta.getNumeroComprobante() : "1";
        String rucEmisor = empresa.getRuc() != null ? empresa.getRuc() : "";
        String nombreEmisor = empresa.getNombre() != null ? empresa.getNombre() : "";
        String dirEmisor = empresa.getDireccion() != null ? empresa.getDireccion() : "";

        BigDecimal total = venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO;
        BigDecimal baseImponible = total.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(baseImponible);

        String fechaEmision = venta.getFechaHora() != null ? venta.getFechaHora().format(FMT_SUNAT) : "";

        String tipoDocCliente = "1";
        String numDocCliente = "";
        String nombreCliente = venta.getNombreClienteVenta() != null ? venta.getNombreClienteVenta() : "";
        if (venta.getCliente() != null) {
            String td = venta.getCliente().getTipoDocumento();
            tipoDocCliente = "RUC".equalsIgnoreCase(td) ? "6" : "1";
            numDocCliente = venta.getCliente().getNumeroDocumento() != null ? venta.getCliente().getNumeroDocumento() : "";
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\"\n");
        xml.append("  xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"\n");
        xml.append("  xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\"\n");
        xml.append("  xmlns:ext=\"urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2\">\n");

        xml.append("  <ext:UBLExtensions><ext:UBLExtension><ext:ExtensionContent>\n");
        xml.append("    <!-- Firma digital: se inserta cuando el certificado está configurado -->\n");
        xml.append("  </ext:ExtensionContent></ext:UBLExtension></ext:UBLExtensions>\n");

        xml.append("  <cbc:UBLVersionID>2.1</cbc:UBLVersionID>\n");
        xml.append("  <cbc:CustomizationID>2.0</cbc:CustomizationID>\n");
        xml.append("  <cbc:ID>").append(formatSerie(tipoDoc, serie)).append("-").append(numero).append("</cbc:ID>\n");
        xml.append("  <cbc:IssueDate>").append(fechaEmision).append("</cbc:IssueDate>\n");
        xml.append("  <cbc:InvoiceTypeCode listID=\"0101\">").append(tipoDoc).append("</cbc:InvoiceTypeCode>\n");
        xml.append("  <cbc:DocumentCurrencyCode>PEN</cbc:DocumentCurrencyCode>\n");

        xml.append("  <cac:AccountingSupplierParty>\n");
        xml.append("    <cac:Party>\n");
        xml.append("      <cac:PartyIdentification><cbc:ID schemeID=\"6\">").append(rucEmisor).append("</cbc:ID></cac:PartyIdentification>\n");
        xml.append("      <cac:PartyLegalEntity><cbc:RegistrationName>").append(escapeXml(nombreEmisor)).append("</cbc:RegistrationName>\n");
        xml.append("        <cac:RegistrationAddress><cbc:AddressTypeCode>0000</cbc:AddressTypeCode>\n");
        xml.append("          <cac:AddressLine><cbc:Line>").append(escapeXml(dirEmisor)).append("</cbc:Line></cac:AddressLine>\n");
        xml.append("        </cac:RegistrationAddress>\n");
        xml.append("      </cac:PartyLegalEntity>\n");
        xml.append("    </cac:Party>\n");
        xml.append("  </cac:AccountingSupplierParty>\n");

        xml.append("  <cac:AccountingCustomerParty>\n");
        xml.append("    <cac:Party>\n");
        xml.append("      <cac:PartyIdentification><cbc:ID schemeID=\"").append(tipoDocCliente).append("\">").append(numDocCliente).append("</cbc:ID></cac:PartyIdentification>\n");
        xml.append("      <cac:PartyLegalEntity><cbc:RegistrationName>").append(escapeXml(nombreCliente)).append("</cbc:RegistrationName></cac:PartyLegalEntity>\n");
        xml.append("    </cac:Party>\n");
        xml.append("  </cac:AccountingCustomerParty>\n");

        xml.append("  <cac:TaxTotal>\n");
        xml.append("    <cbc:TaxAmount currencyID=\"PEN\">").append(igv.toPlainString()).append("</cbc:TaxAmount>\n");
        xml.append("    <cac:TaxSubtotal>\n");
        xml.append("      <cbc:TaxableAmount currencyID=\"PEN\">").append(baseImponible.toPlainString()).append("</cbc:TaxableAmount>\n");
        xml.append("      <cbc:TaxAmount currencyID=\"PEN\">").append(igv.toPlainString()).append("</cbc:TaxAmount>\n");
        xml.append("      <cac:TaxCategory><cbc:ID>S</cbc:ID><cbc:Percent>18</cbc:Percent>\n");
        xml.append("        <cac:TaxScheme><cbc:ID>1000</cbc:ID><cbc:Name>IGV</cbc:Name><cbc:TaxTypeCode>VAT</cbc:TaxTypeCode></cac:TaxScheme>\n");
        xml.append("      </cac:TaxCategory>\n");
        xml.append("    </cac:TaxSubtotal>\n");
        xml.append("  </cac:TaxTotal>\n");

        xml.append("  <cac:LegalMonetaryTotal>\n");
        xml.append("    <cbc:LineExtensionAmount currencyID=\"PEN\">").append(baseImponible.toPlainString()).append("</cbc:LineExtensionAmount>\n");
        xml.append("    <cbc:TaxInclusiveAmount currencyID=\"PEN\">").append(total.toPlainString()).append("</cbc:TaxInclusiveAmount>\n");
        xml.append("    <cbc:PayableAmount currencyID=\"PEN\">").append(total.toPlainString()).append("</cbc:PayableAmount>\n");
        xml.append("  </cac:LegalMonetaryTotal>\n");

        if (venta.getItems() != null) {
            int lineNum = 1;
            for (var item : venta.getItems()) {
                BigDecimal precioUnit = item.getPrecioUnitario() != null ? item.getPrecioUnitario() : BigDecimal.ZERO;
                BigDecimal subtotalItem = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                BigDecimal baseItem = subtotalItem.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
                BigDecimal igvItem = subtotalItem.subtract(baseItem);
                int cant = item.getCantidad() != null ? item.getCantidad() : 0;
                String prodNombre = item.getProducto() != null ? item.getProducto().getNombre() : "";

                xml.append("  <cac:InvoiceLine>\n");
                xml.append("    <cbc:ID>").append(lineNum++).append("</cbc:ID>\n");
                xml.append("    <cbc:InvoicedQuantity unitCode=\"NIU\">").append(cant).append("</cbc:InvoicedQuantity>\n");
                xml.append("    <cbc:LineExtensionAmount currencyID=\"PEN\">").append(baseItem.toPlainString()).append("</cbc:LineExtensionAmount>\n");
                xml.append("    <cac:PricingReference><cac:AlternativeConditionPrice>\n");
                xml.append("      <cbc:PriceAmount currencyID=\"PEN\">").append(precioUnit.toPlainString()).append("</cbc:PriceAmount>\n");
                xml.append("      <cbc:PriceTypeCode>01</cbc:PriceTypeCode>\n");
                xml.append("    </cac:AlternativeConditionPrice></cac:PricingReference>\n");
                xml.append("    <cac:TaxTotal><cbc:TaxAmount currencyID=\"PEN\">").append(igvItem.toPlainString()).append("</cbc:TaxAmount></cac:TaxTotal>\n");
                xml.append("    <cac:Item><cbc:Description>").append(escapeXml(prodNombre)).append("</cbc:Description></cac:Item>\n");
                xml.append("    <cac:Price><cbc:PriceAmount currencyID=\"PEN\">").append(baseItem.divide(BigDecimal.valueOf(cant > 0 ? cant : 1), 2, RoundingMode.HALF_UP).toPlainString()).append("</cbc:PriceAmount></cac:Price>\n");
                xml.append("  </cac:InvoiceLine>\n");
            }
        }

        xml.append("</Invoice>\n");

        log.debug("XML UBL 2.1 generado para comprobante {}", venta.getNumeroComprobante());
        return xml.toString();
    }

    /**
     * Envía el comprobante a SUNAT. Solo funciona si el certificado digital está configurado.
     * @return Estado del envío: "ACEPTADO", "RECHAZADO", o "NO_CONFIGURADO"
     */
    public String enviarASunat(String xml, Empresa empresa) {
        if (!certificadoHabilitado) {
            log.info("Envío a SUNAT deshabilitado. Configure el certificado digital para activar.");
            return "NO_CONFIGURADO";
        }

        // TODO: Implementar envío real a SUNAT vía SOAP cuando el certificado esté configurado
        // 1. Firmar XML con certificado digital
        // 2. Comprimir en ZIP
        // 3. Enviar vía SOAP a SUNAT (beta o producción según modo)
        // 4. Procesar CDR (Constancia de Recepción)
        log.info("Modo SUNAT: {}. Envío pendiente de implementación con certificado.", modo);
        return "PENDIENTE";
    }

    private String formatSerie(String tipoDoc, String serie) {
        if ("01".equals(tipoDoc)) return "F" + serie;
        return "B" + serie;
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
