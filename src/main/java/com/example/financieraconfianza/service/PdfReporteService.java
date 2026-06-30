package com.example.financieraconfianza.service;

import com.example.financieraconfianza.model.Movimiento;
import com.example.financieraconfianza.model.entity.Usuario;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfReporteService {

    public void generarEstadoCuentaPdf(Usuario usuario, List<Movimiento> movimientos, HttpServletResponse response) throws IOException {

        // 1. Configuramos el documento PDF (Tamaño A4 y márgenes)
        Document documento = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(documento, response.getOutputStream());

        documento.open();

        // 2. Fuentes personalizadas
        Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.decode("#004b87"));
        Font fuenteSubtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
        Font fuenteNormal = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);
        Font fuenteCabeceraTabla = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);

        // 3. Título del Documento
        Paragraph titulo = new Paragraph("ESTADO DE CUENTA", fuenteTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(20);
        documento.add(titulo);

        // 4. Datos del Cliente
        documento.add(new Paragraph("Datos del Titular", fuenteSubtitulo));
        documento.add(new Paragraph("Correo: " + usuario.getCorreo(), fuenteNormal));
        documento.add(new Paragraph("Documento: " + usuario.getTipoDoc() + " " + usuario.getNumDoc(), fuenteNormal));
        documento.add(new Paragraph("Número de Cuenta: " + usuario.getNumeroCuenta(), fuenteNormal));
        documento.add(new Paragraph("CCI: " + (usuario.getCci() != null ? usuario.getCci() : "No registrado"), fuenteNormal));
        documento.add(new Paragraph("Saldo Actual: S/ " + String.format("%.2f", usuario.getSaldo()), fuenteSubtitulo));

        Paragraph espacio = new Paragraph(" ");
        espacio.setSpacingAfter(15);
        documento.add(espacio);

        // 5. Creación de la Tabla de Movimientos
        PdfPTable tabla = new PdfPTable(4); // 4 columnas: Fecha, Concepto, Tipo, Monto
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{2f, 4f, 2f, 2f}); // Proporción de ancho de columnas

        // Cabeceras de la tabla
        String[] cabeceras = {"Fecha", "Concepto", "Operación", "Monto (S/)"};
        for (String cabecera : cabeceras) {
            PdfPCell celda = new PdfPCell(new Phrase(cabecera, fuenteCabeceraTabla));
            celda.setBackgroundColor(Color.decode("#004b87")); // Azul institucional
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            celda.setPadding(8);
            tabla.addCell(celda);
        }

        // 6. Llenado de la tabla con los movimientos de la base de datos
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Movimiento mov : movimientos) {
            // Fecha
            PdfPCell celdaFecha = new PdfPCell(new Phrase(mov.getFecha().format(formatoFecha), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            celdaFecha.setPadding(6);
            tabla.addCell(celdaFecha);

            // Concepto
            PdfPCell celdaConcepto = new PdfPCell(new Phrase(mov.getConcepto(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            celdaConcepto.setPadding(6);
            tabla.addCell(celdaConcepto);

            // Tipo (Ingreso / Salida)
            String tipoOperacion = mov.getMonto() > 0 ? "INGRESO" : "SALIDA";
            PdfPCell celdaTipo = new PdfPCell(new Phrase(tipoOperacion, FontFactory.getFont(FontFactory.HELVETICA, 10, mov.getMonto() > 0 ? Color.decode("#166534") : Color.decode("#991b1b"))));
            celdaTipo.setHorizontalAlignment(Element.ALIGN_CENTER);
            celdaTipo.setPadding(6);
            tabla.addCell(celdaTipo);

            // Monto
            PdfPCell celdaMonto = new PdfPCell(new Phrase(String.format("%.2f", Math.abs(mov.getMonto())), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            celdaMonto.setHorizontalAlignment(Element.ALIGN_RIGHT);
            celdaMonto.setPadding(6);
            tabla.addCell(celdaMonto);
        }

        documento.add(tabla);
        documento.close();
    }
}