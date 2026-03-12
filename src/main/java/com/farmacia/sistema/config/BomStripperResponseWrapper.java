package com.farmacia.sistema.config;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Envuelve la respuesta para capturar el cuerpo y poder eliminar el BOM si existe.
 */
public class BomStripperResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    public BomStripperResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() ya fue llamado");
        }
        if (outputStream == null) {
            outputStream = new ServletOutputStream() {
                @Override
                public boolean isReady() { return true; }
                @Override
                public void setWriteListener(jakarta.servlet.WriteListener listener) { }
                @Override
                public void write(int b) { buffer.write(b); }
                @Override
                public void write(byte[] b, int off, int len) { buffer.write(b, off, len); }
            };
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStream != null) {
            throw new IllegalStateException("getOutputStream() ya fue llamado");
        }
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(buffer, StandardCharsets.UTF_8));
        }
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) writer.flush();
        if (outputStream != null) outputStream.flush();
    }

    public byte[] getContent() throws IOException {
        flushBuffer();
        return buffer.toByteArray();
    }
}
